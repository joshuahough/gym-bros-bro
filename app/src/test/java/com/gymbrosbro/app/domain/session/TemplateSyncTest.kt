package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.TestData
import com.gymbrosbro.app.TestData.set
import com.gymbrosbro.app.data.db.SessionExerciseWithSets
import com.gymbrosbro.app.data.db.SessionWithExercises
import com.gymbrosbro.app.data.db.TemplateExerciseWithExercise
import com.gymbrosbro.app.data.db.TemplateWithExercises
import com.gymbrosbro.app.data.db.entity.SessionExerciseEntity
import com.gymbrosbro.app.data.db.entity.SetEntryEntity
import com.gymbrosbro.app.data.db.entity.TemplateExerciseEntity
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.data.db.entity.WorkoutTemplateEntity
import com.gymbrosbro.app.domain.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TemplateSyncTest {
    private val rope = TestData.exercise(id = "rope")
    private val bench = TestData.exercise(id = "bench")

    private fun template(vararg slots: Triple<String, Int, Boolean>) = TemplateWithExercises(
        WorkoutTemplateEntity(id = 1, name = "Push", sortOrder = 0, createdAt = Instant.EPOCH),
        slots.mapIndexed { i, (id, sets, warmup) ->
            TemplateExerciseWithExercise(
                TemplateExerciseEntity(templateId = 1, exerciseId = id, position = i, targetSets = sets, isWarmup = warmup),
                if (id == rope.id) rope else bench,
            )
        },
    )

    private fun session(vararg blocks: Pair<String, List<SetEntryEntity>>) = SessionWithExercises(
        WorkoutSessionEntity(id = 1, templateId = 1, name = "Push", startedAt = Instant.EPOCH, status = SessionStatus.COMPLETED),
        blocks.mapIndexed { i, (id, sets) ->
            SessionExerciseWithSets(
                SessionExerciseEntity(id = i + 1L, sessionId = 1, exerciseId = id, position = i),
                if (id == rope.id) rope else bench,
                sets,
            )
        },
    )

    @Test
    fun `a warm-up exercise done as planned is no change`() {
        val t = template(Triple("rope", 2, true), Triple("bench", 3, false))
        val s = session(
            "rope" to listOf(set(0, reps = 100, warmup = true), set(1, reps = 100, warmup = true)),
            "bench" to listOf(set(0, reps = 8, warmup = true), set(1, reps = 8), set(2, reps = 8), set(3, reps = 8)),
        )
        assertFalse(TemplateSync.differs(s, t))
    }

    @Test
    fun `flipping an exercise to warm-up is saved to the workout`() {
        val t = template(Triple("rope", 2, false))
        val s = session("rope" to listOf(set(0, reps = 100, warmup = true), set(1, reps = 100, warmup = true)))
        assertTrue(TemplateSync.differs(s, t))
        val draft = TemplateSync.drafts(s, t).single()
        assertTrue(draft.isWarmup)
        assertEquals(2, draft.targetSets)
    }
}
