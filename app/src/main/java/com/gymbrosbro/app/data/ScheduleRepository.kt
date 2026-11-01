package com.gymbrosbro.app.data

import androidx.room.withTransaction
import com.gymbrosbro.app.data.db.GymDatabase
import com.gymbrosbro.app.data.db.entity.ScheduleExceptionEntity
import com.gymbrosbro.app.data.db.entity.SchedulePauseEntity
import com.gymbrosbro.app.data.db.entity.ScheduleRuleEntity
import com.gymbrosbro.app.domain.model.ScheduleAction
import com.gymbrosbro.app.domain.model.ScheduleKind
import com.gymbrosbro.app.domain.schedule.Occurrence
import com.gymbrosbro.app.domain.schedule.ScheduleData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate

/** Schedule rules, per-day skips and moves, and pauses (SPEC §7.5). */
class ScheduleRepository(private val database: GymDatabase) {
    private val dao = database.scheduleDao()

    fun observe(): Flow<ScheduleData> =
        combine(dao.observeRules(), dao.observeExceptions(), dao.observePauses(), ::ScheduleData)

    suspend fun get(): ScheduleData = ScheduleData(dao.getRules(), dao.getExceptions(), dao.getPauses())

    suspend fun hasRules(): Boolean = dao.getRules().isNotEmpty()

    suspend fun addRule(
        templateId: Long?,
        kind: ScheduleKind,
        startDate: LocalDate,
        daysOfWeek: Int = 0,
        intervalDays: Int? = null,
    ): Long = dao.insertRule(
        ScheduleRuleEntity(
            templateId = templateId,
            kind = kind,
            daysOfWeek = daysOfWeek,
            intervalDays = intervalDays,
            startDate = startDate,
            endDate = if (kind == ScheduleKind.ONE_OFF) startDate else null,
            createdAt = Instant.now(),
        ),
    )

    /**
     * Stops a rule from today on. Its past days stay so streaks don't change;
     * a rule that hasn't started yet is simply deleted.
     */
    suspend fun removeRule(id: Long, today: LocalDate = LocalDate.now()) = database.withTransaction {
        val rule = dao.getRule(id) ?: return@withTransaction
        if (rule.startDate >= today) {
            dao.deleteRule(id)
        } else {
            val yesterday = today.minusDays(1)
            dao.updateRule(rule.copy(endDate = minOf(rule.endDate ?: yesterday, yesterday)))
            dao.deleteExceptionsAfter(id, yesterday)
        }
    }

    suspend fun skip(occurrence: Occurrence) = setException(occurrence, ScheduleAction.SKIP, newDate = null)

    /** Moves within the occurrence's week; moving back to its own day clears the move. */
    suspend fun move(occurrence: Occurrence, newDate: LocalDate) {
        if (newDate == occurrence.originalDate) restore(occurrence) else setException(occurrence, ScheduleAction.MOVE, newDate)
    }

    /** Undoes a skip or move. */
    suspend fun restore(occurrence: Occurrence) = dao.deleteException(occurrence.ruleId, occurrence.originalDate)

    private suspend fun setException(occurrence: Occurrence, action: ScheduleAction, newDate: LocalDate?) =
        database.withTransaction {
            val existing = dao.getException(occurrence.ruleId, occurrence.originalDate)
            dao.upsertException(
                ScheduleExceptionEntity(
                    id = existing?.id ?: 0,
                    ruleId = occurrence.ruleId,
                    originalDate = occurrence.originalDate,
                    action = action,
                    newDate = newDate,
                ),
            )
        }

    suspend fun pause(startDate: LocalDate, endDate: LocalDate) =
        dao.insertPause(SchedulePauseEntity(startDate = startDate, endDate = endDate))

    /** Ends a pause early: it stops yesterday, or disappears if it hadn't started. */
    suspend fun endPause(pause: SchedulePauseEntity, today: LocalDate = LocalDate.now()) {
        if (pause.startDate >= today) dao.deletePause(pause.id) else dao.updatePause(pause.copy(endDate = today.minusDays(1)))
    }
}
