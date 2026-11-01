package com.gymbrosbro.app.domain.library

import com.gymbrosbro.app.TestData.exercise
import com.gymbrosbro.app.domain.model.Equipment
import com.gymbrosbro.app.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFilterTest {
    private val bench = exercise("bench", "Barbell Bench Press", aliases = listOf("bench"))
    private val incline = exercise("incline", "Barbell Incline Bench Press - Medium Grip")
    private val dbBench = exercise("db-bench", "Dumbbell Bench Press", equipment = Equipment.DUMBBELL, isFavorite = true)
    private val curl = exercise("curl", "Dumbbell Curl", muscle = MuscleGroup.BICEPS, equipment = Equipment.DUMBBELL)
    private val all = listOf(bench, curl, dbBench, incline)

    private fun names(query: LibraryQuery) = LibraryFilter.apply(all, query).map { it.id }

    @Test
    fun `every word must match name or alias`() {
        assertEquals(listOf("db-bench", "curl"), names(LibraryQuery(text = "dumbbell")))
        assertEquals(listOf("db-bench"), names(LibraryQuery(text = "dumbbell bench")))
    }

    @Test
    fun `exact alias match ranks first`() {
        assertEquals("bench", names(LibraryQuery(text = "bench")).first())
    }

    @Test
    fun `filters combine`() {
        assertEquals(listOf("curl"), names(LibraryQuery(muscle = MuscleGroup.BICEPS)))
        assertEquals(listOf("curl", "db-bench"), names(LibraryQuery(equipment = Equipment.DUMBBELL)))
        assertEquals(listOf("db-bench"), names(LibraryQuery(favoritesOnly = true)))
    }

    @Test
    fun `blank search keeps original order`() {
        assertEquals(all.map { it.id }, names(LibraryQuery(text = "  ")))
    }
}
