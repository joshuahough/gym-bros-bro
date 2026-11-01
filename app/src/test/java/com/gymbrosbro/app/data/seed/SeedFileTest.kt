package com.gymbrosbro.app.data.seed

import com.gymbrosbro.app.domain.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/** Sanity checks on the generated library, so a bad pipeline run fails the build. */
class SeedFileTest {
    companion object {
        private lateinit var seed: SeedFile

        @BeforeClass
        @JvmStatic
        fun load() {
            // Unit tests run with the module directory as the working directory.
            val text = File("src/main/assets/${SeedLoader.SEED_ASSET}").readText()
            seed = SeedLoader.json.decodeFromString(text)
        }
    }

    private fun named(name: String) = seed.exercises.single { it.name == name }

    @Test
    fun `library is large and ids are unique`() {
        assertTrue(seed.version >= 1)
        assertTrue("expected a large library", seed.exercises.size > 800)
        assertEquals(seed.exercises.size, seed.exercises.map { it.id }.toSet().size)
        assertTrue(seed.exercises.all { it.name.isNotBlank() })
    }

    @Test
    fun `only bodyweight exercises carry a bodyweight factor`() {
        seed.exercises.filter { it.trackingType != TrackingType.BODYWEIGHT_REPS }.forEach {
            assertNull("${it.name} should not have a factor", it.bodyweightFactor)
        }
    }

    @Test
    fun `rep-based exercises have a recommended rep range`() {
        seed.exercises
            .filter { it.trackingType == TrackingType.WEIGHT_REPS || it.trackingType == TrackingType.BODYWEIGHT_REPS }
            .forEach {
                val min = assertNotNull(it.recommendedRepsMin, it.name)
                val max = assertNotNull(it.recommendedRepsMax, it.name)
                assertTrue(it.name, min <= max)
            }
    }

    @Test
    fun `curated staples map the way the spec says`() {
        assertEquals(TrackingType.WEIGHT_REPS, named("Barbell Bench Press").trackingType)
        assertEquals(0.65, named("Push-Up").bodyweightFactor!!, 0.0)
        assertEquals(1.0, named("Pull-Up").bodyweightFactor!!, 0.0)
        assertEquals(TrackingType.DURATION, named("Plank").trackingType)
        assertEquals(TrackingType.DISTANCE_DURATION, named("Running").trackingType)
        assertEquals(TrackingType.WEIGHT_DISTANCE, named("Farmer's Walk").trackingType)
    }

    private fun <T : Any> assertNotNull(value: T?, name: String): T {
        assertNotNull("$name is missing a value", value)
        return value!!
    }
}
