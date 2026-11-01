package com.gymbrosbro.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gymbrosbro.app.MainActivity
import com.gymbrosbro.app.R
import com.gymbrosbro.app.appContainer
import com.gymbrosbro.app.data.settings.ReminderSettings
import com.gymbrosbro.app.domain.schedule.Reminder
import com.gymbrosbro.app.domain.schedule.Reminders
import com.gymbrosbro.app.ui.localDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Workout reminders (SPEC §7.8): one inexact alarm a day at the chosen time.
 * When it fires, [ReminderReceiver] decides whether today deserves a
 * notification, then books tomorrow. Inexact alarms need no special
 * permission and may land a few minutes late, which is fine for a nudge.
 */
object ReminderScheduler {
    private const val CHANNEL_ID = "workout_reminders"
    private const val NOTIFICATION_ID = 1
    private const val REQUEST_CODE = 1

    /** Books the next alarm, or cancels it when reminders are off. */
    fun sync(context: Context, settings: ReminderSettings) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = alarmIntent(context)
        alarms.cancel(intent)
        if (!settings.enabled) return
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(settings.time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Checks today's plan and posts at most one notification. */
    suspend fun fire(context: Context) {
        val container = context.appContainer
        val settings = container.settings.reminders.first()
        if (!settings.enabled) return
        val today = LocalDate.now()
        val schedule = container.scheduleRepository.get()
        val workoutDays = container.sessionRepository.completedSince(today.minusDays(1))
            .map { it.startedAt.localDate() }.toSet()
        val reminder = Reminders.decide(today, schedule, workoutDays, settings.graceDay) ?: return

        val (title, text) = when (reminder) {
            is Reminder.WorkoutDay -> {
                val names = reminder.templateIds.map { id ->
                    id?.let { container.templateRepository.getTemplate(it)?.template?.name } ?: "Workout"
                }.distinct()
                "${names.joinToString(" + ")} today" to "PRESS START to keep your COMBO going."
            }
            is Reminder.GraceDay -> {
                val day = reminder.missed.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                "Missed $day" to "Train today to keep your COMBO."
            }
        }
        notify(context, title, text)
    }

    private fun notify(context: Context, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Workout reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "One reminder on days you have a workout planned."
            },
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

/** The daily alarm: maybe notify, then book tomorrow. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = context.appContainer
        container.appScope.launch {
            try {
                ReminderScheduler.fire(context)
            } finally {
                ReminderScheduler.sync(context, container.settings.reminders.first())
                pending.finish()
            }
        }
    }
}

/** Alarms don't survive a reboot or a clock change; book the next one again. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = context.appContainer
        container.appScope.launch {
            try {
                ReminderScheduler.sync(context, container.settings.reminders.first())
            } finally {
                pending.finish()
            }
        }
    }
}
