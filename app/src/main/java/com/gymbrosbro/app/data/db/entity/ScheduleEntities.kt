@file:UseSerializers(InstantSerializer::class, LocalDateSerializer::class)

package com.gymbrosbro.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gymbrosbro.app.data.backup.InstantSerializer
import com.gymbrosbro.app.data.backup.LocalDateSerializer
import com.gymbrosbro.app.domain.model.ScheduleAction
import com.gymbrosbro.app.domain.model.ScheduleKind
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

// SPEC §4.5 / §7.5.

/**
 * A repeating (or one-off) plan: "Mon/Wed/Fri = Push". Rules are never edited
 * in place once they have past days — they're ended and replaced — so past
 * occurrences, and the streaks built on them, don't change after the fact.
 */
@Serializable
@Entity(
    tableName = "schedule_rule",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("templateId")],
)
data class ScheduleRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** null = any workout. */
    val templateId: Long?,
    val kind: ScheduleKind,
    /** WEEKLY: bit 0 = Monday … bit 6 = Sunday. */
    val daysOfWeek: Int = 0,
    /** EVERY_N_DAYS: N, counted from [startDate]. */
    val intervalDays: Int? = null,
    /** First day the rule applies; for ONE_OFF, its only day. */
    val startDate: LocalDate,
    /** Last day the rule applies, inclusive; null = open-ended. */
    val endDate: LocalDate? = null,
    val createdAt: Instant,
)

/** Skip or move one occurrence of a rule. */
@Serializable
@Entity(
    tableName = "schedule_exception",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["ruleId", "originalDate"], unique = true)],
)
data class ScheduleExceptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long,
    val originalDate: LocalDate,
    val action: ScheduleAction,
    /** MOVE only; always in the same week as [originalDate]. */
    val newDate: LocalDate? = null,
)

/** Vacation / sick days: occurrences inside are off, and streaks freeze. Dates inclusive. */
@Serializable
@Entity(tableName = "schedule_pause")
data class SchedulePauseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate,
)
