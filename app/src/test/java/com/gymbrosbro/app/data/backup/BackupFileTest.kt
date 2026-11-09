package com.gymbrosbro.app.data.backup

import com.gymbrosbro.app.TestData
import com.gymbrosbro.app.avatar.AvatarConfig
import com.gymbrosbro.app.data.db.entity.BodyweightEntryEntity
import com.gymbrosbro.app.data.db.entity.ScheduleRuleEntity
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.data.settings.SettingsSnapshot
import com.gymbrosbro.app.domain.model.ExerciseSource
import com.gymbrosbro.app.domain.model.ScheduleKind
import com.gymbrosbro.app.domain.model.SessionStatus
import com.gymbrosbro.app.domain.units.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class BackupFileTest {
    private val sample = BackupFile(
        dbVersion = 5,
        exportedAt = Instant.parse("2026-11-05T18:30:00Z"),
        settings = SettingsSnapshot(
            unitSystem = UnitSystem.METRIC,
            weekStart = DayOfWeek.SUNDAY,
            avatar = AvatarConfig(),
            playerName = "TEST BRO",
        ),
        exercises = listOf(TestData.exercise(id = "custom-1").copy(source = ExerciseSource.CUSTOM)),
        sessions = listOf(
            WorkoutSessionEntity(
                id = 7, templateId = null, name = "Push", startedAt = Instant.parse("2026-11-04T17:00:00Z"),
                endedAt = Instant.parse("2026-11-04T18:00:00Z"), status = SessionStatus.COMPLETED,
                scheduledOccurrenceDate = LocalDate.of(2026, 11, 4),
            ),
            WorkoutSessionEntity(
                id = 8, templateId = null, name = "Pull", startedAt = Instant.parse("2026-11-05T17:00:00Z"),
                status = SessionStatus.IN_PROGRESS,
            ),
        ),
        bodyweight = listOf(BodyweightEntryEntity(LocalDate.of(2026, 11, 1), 82.5)),
        scheduleRules = listOf(
            ScheduleRuleEntity(
                id = 1, templateId = null, kind = ScheduleKind.WEEKLY, daysOfWeek = 0b10101,
                startDate = LocalDate.of(2026, 10, 1), createdAt = Instant.parse("2026-10-01T00:00:00Z"),
            ),
        ),
    )

    @Test
    fun `round trips through json`() {
        val text = BackupRepository.json.encodeToString(sample)
        assertEquals(sample, BackupRepository.parse(text))
    }

    @Test
    fun `dates are stored as readable text`() {
        val text = BackupRepository.json.encodeToString(sample)
        assertTrue(text.contains("\"2026-11-05T18:30:00Z\""))
        assertTrue(text.contains("\"2026-11-01\""))
    }

    @Test
    fun `counts only finished workouts`() {
        assertEquals(1, sample.workoutCount)
    }

    @Test
    fun `ignores fields it doesn't know`() {
        val text = """{"app":"gymbrosbro","format":1,"dbVersion":5,"exportedAt":"2026-11-05T00:00:00Z","fromTheFuture":true}"""
        assertEquals(5, BackupRepository.parse(text).dbVersion)
    }

    @Test(expected = BackupFormatException::class)
    fun `rejects other json`() {
        BackupRepository.parse("""{"app":"someotherapp","dbVersion":1,"exportedAt":"2026-11-05T00:00:00Z"}""")
    }

    @Test(expected = BackupFormatException::class)
    fun `rejects garbage`() {
        BackupRepository.parse("not json at all")
    }

    @Test(expected = BackupFormatException::class)
    fun `rejects a newer format`() {
        BackupRepository.parse("""{"app":"gymbrosbro","format":99,"dbVersion":9,"exportedAt":"2026-11-05T00:00:00Z"}""")
    }

    @Test
    fun `exports custom, flagged and used exercises only`() {
        val all = listOf(
            TestData.exercise(id = "plain"),
            TestData.exercise(id = "fav", isFavorite = true),
            TestData.exercise(id = "used"),
            TestData.exercise(id = "noted").copy(userNotes = "elbows in"),
            TestData.exercise(id = "custom-1").copy(source = ExerciseSource.CUSTOM),
        )
        val ids = BackupRepository.exercisesToExport(all, usedIds = setOf("used")).map { it.id }
        assertEquals(listOf("fav", "used", "noted", "custom-1"), ids)
    }

    @Test
    fun `restore keeps installed library content and takes backup flags`() {
        val installed = listOf(
            TestData.exercise(id = "bench", name = "Bench Press (new name)"),
            TestData.exercise(id = "squat", isFavorite = true),
        )
        val backup = listOf(
            TestData.exercise(id = "bench", name = "Bench Press", isFavorite = true).copy(userNotes = "arch"),
            TestData.exercise(id = "dropped-from-library"),
            TestData.exercise(id = "custom-1").copy(source = ExerciseSource.CUSTOM),
        )
        val rows = BackupRepository.exercisesToRestore(backup, installed).associateBy { it.id }

        val bench = rows.getValue("bench")
        assertEquals("Bench Press (new name)", bench.name)
        assertTrue(bench.isFavorite)
        assertEquals("arch", bench.userNotes)
        // Not in the backup, so its current flag is cleared like the rest of the app's data.
        assertFalse(rows.getValue("squat").isFavorite)
        assertTrue("dropped-from-library" in rows)
        assertTrue("custom-1" in rows)
        assertEquals(4, rows.size)
    }
}
