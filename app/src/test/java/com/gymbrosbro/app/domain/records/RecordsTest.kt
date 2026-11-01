package com.gymbrosbro.app.domain.records

import com.gymbrosbro.app.TestData.set
import com.gymbrosbro.app.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RecordsTest {
    private fun perf(id: Long, vararg sets: com.gymbrosbro.app.data.db.entity.SetEntryEntity) =
        Performance(sessionId = id, at = Instant.ofEpochSecond(id * 86_400), sets = sets.toList())

    @Test
    fun `epley estimate, singles are their own weight, high reps ignored`() {
        assertEquals(100.0 * (1 + 8 / 30.0), Records.e1rm(100.0, 8)!!, 1e-9)
        assertEquals(100.0, Records.e1rm(100.0, 1)!!, 1e-9)
        assertNull(Records.e1rm(100.0, 13))
    }

    @Test
    fun `first workout is a baseline, not a record`() {
        val history = Records.history(TrackingType.WEIGHT_REPS, listOf(perf(1, set(0, weightKg = 60.0, reps = 8))))
        assertTrue(history.events.isEmpty())
        assertEquals(60.0, history.bests.getValue(RecordType.MAX_WEIGHT).value, 1e-9)
    }

    @Test
    fun `beating an earlier workout is a record, a tie is not`() {
        val history = Records.history(
            TrackingType.WEIGHT_REPS,
            listOf(
                perf(1, set(0, weightKg = 60.0, reps = 8)),
                perf(2, set(0, weightKg = 60.0, reps = 8)),
                perf(3, set(0, weightKg = 62.5, reps = 6)),
            ),
        )
        val maxWeight = history.events.filter { it.type == RecordType.MAX_WEIGHT }
        assertEquals(listOf(3L), maxWeight.map { it.sessionId })
        assertEquals(60.0, maxWeight.single().previous, 1e-9)
    }

    @Test
    fun `warm-ups and unchecked sets never count`() {
        val history = Records.history(
            TrackingType.WEIGHT_REPS,
            listOf(
                perf(1, set(0, weightKg = 60.0, reps = 8)),
                perf(2, set(0, weightKg = 100.0, reps = 1, warmup = true), set(1, weightKg = 90.0, reps = 1, completed = false)),
            ),
        )
        assertTrue(history.events.isEmpty())
    }

    @Test
    fun `session volume record uses working sets only`() {
        val history = Records.history(
            TrackingType.WEIGHT_REPS,
            listOf(
                perf(1, set(0, weightKg = 50.0, reps = 10)),
                perf(2, set(0, weightKg = 20.0, reps = 10, warmup = true), set(1, weightKg = 50.0, reps = 10), set(2, weightKg = 50.0, reps = 2)),
            ),
        )
        val volume = history.events.single { it.type == RecordType.MAX_SESSION_VOLUME }
        assertEquals(600.0, volume.value, 1e-9)
    }

    @Test
    fun `bodyweight records count reps`() {
        val history = Records.history(
            TrackingType.BODYWEIGHT_REPS,
            listOf(perf(1, set(0, reps = 10), set(1, reps = 8)), perf(2, set(0, reps = 12))),
        )
        assertEquals(listOf(RecordType.MAX_REPS), history.events.map { it.type })
    }

    @Test
    fun `live check beats earlier workouts and earlier sets today`() {
        val prior = mapOf(RecordType.MAX_WEIGHT to 60.0, RecordType.BEST_E1RM to 80.0)
        val first = set(0, weightKg = 62.5, reps = 5)
        val hits = Records.brokenBy(TrackingType.WEIGHT_REPS, first, prior, listOf(first))
        assertEquals(listOf(RecordType.MAX_WEIGHT), hits.map { it.type })

        // Same weight again in the same workout is a tie with the set before it.
        val second = set(1, weightKg = 62.5, reps = 5)
        assertTrue(Records.brokenBy(TrackingType.WEIGHT_REPS, second, prior, listOf(first, second)).isEmpty())
    }

    @Test
    fun `no history means no pop-up`() {
        val s = set(0, weightKg = 100.0, reps = 5)
        assertTrue(Records.brokenBy(TrackingType.WEIGHT_REPS, s, emptyMap(), listOf(s)).isEmpty())
    }
}
