package com.gymbrosbro.app.data.seed

import com.gymbrosbro.app.domain.model.Discipline
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.LoadClass
import com.gymbrosbro.app.domain.model.MuscleGroup
import com.gymbrosbro.app.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SeedMergeTest {
    private fun seedExercise(id: String, name: String) = SeedExercise(
        id = id,
        name = name,
        discipline = Discipline.WEIGHTLIFTING,
        trackingType = TrackingType.WEIGHT_REPS,
        equipment = Equipment.BARBELL,
        primaryMuscle = MuscleGroup.CHEST,
        recommendedLoad = LoadClass.HEAVY,
    ).toEntity()

    @Test
    fun `new seed content wins but user flags survive`() {
        val old = seedExercise("bench", "Old Name").copy(isFavorite = true, userNotes = "pause reps")
        val new = seedExercise("bench", "Barbell Bench Press")

        val merged = SeedLoader.mergeSeed(listOf(new), mapOf("bench" to old)).single()

        assertEquals("Barbell Bench Press", merged.name)
        assertEquals(true, merged.isFavorite)
        assertEquals("pause reps", merged.userNotes)
    }

    @Test
    fun `exercises new to this seed are inserted untouched`() {
        val merged = SeedLoader.mergeSeed(listOf(seedExercise("squat", "Squat")), emptyMap()).single()
        assertFalse(merged.isFavorite)
    }
}
