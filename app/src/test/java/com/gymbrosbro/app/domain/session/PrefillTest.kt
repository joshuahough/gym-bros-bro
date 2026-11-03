package com.gymbrosbro.app.domain.session

import com.gymbrosbro.app.TestData.set
import org.junit.Assert.assertEquals
import org.junit.Test

class PrefillTest {
    @Test
    fun `copies last time's working sets position by position`() {
        val last = listOf(
            set(0, weightKg = 20.0, reps = 10, warmup = true),
            set(1, weightKg = 60.0, reps = 8),
            set(2, weightKg = 65.0, reps = 6),
        )
        val drafts = Prefill.sets(2, last)
        assertEquals(listOf(SetDraft(weightKg = 60.0, reps = 8), SetDraft(weightKg = 65.0, reps = 6)), drafts)
    }

    @Test
    fun `repeats the last set when the target has more sets`() {
        val drafts = Prefill.sets(3, listOf(set(0, weightKg = 60.0, reps = 8)))
        assertEquals(3, drafts.size)
        assertEquals(SetDraft(weightKg = 60.0, reps = 8), drafts[2])
    }

    @Test
    fun `ignores sets that were never completed`() {
        val drafts = Prefill.sets(1, listOf(set(0, weightKg = 99.0, reps = 1, completed = false)))
        assertEquals(listOf(SetDraft()), drafts)
    }

    @Test
    fun `no history gives empty rows, at least one`() {
        assertEquals(listOf(SetDraft(), SetDraft()), Prefill.sets(2, emptyList()))
        assertEquals(1, Prefill.sets(0, emptyList()).size)
    }

    @Test
    fun `checking off a set fills the next untouched row`() {
        val done = set(0, weightKg = 60.0, reps = 8)
        val empty = set(1, completed = false)
        val filled = Prefill.nextToFill(done, listOf(done, empty, set(2, completed = false)))
        assertEquals(empty.copy(weightKg = 60.0, reps = 8), filled)
    }

    @Test
    fun `rows you already typed in are left alone`() {
        val done = set(0, weightKg = 60.0, reps = 8)
        assertEquals(null, Prefill.nextToFill(done, listOf(done, set(1, weightKg = 70.0, completed = false))))
        assertEquals(null, Prefill.nextToFill(done, listOf(done)))
    }

    @Test
    fun `a warm-up exercise copies last time's warm-ups`() {
        val last = listOf(set(0, reps = 100, warmup = true), set(1, reps = 80, warmup = true))
        assertEquals(listOf(SetDraft(), SetDraft()), Prefill.sets(2, last))
        assertEquals(listOf(SetDraft(reps = 100), SetDraft(reps = 80)), Prefill.sets(2, last, warmup = true))
    }

    @Test
    fun `next set copies the bottom row`() {
        val existing = listOf(set(0, weightKg = 60.0, reps = 8), set(1, weightKg = 70.0, reps = 5, completed = false))
        assertEquals(SetDraft(weightKg = 70.0, reps = 5), Prefill.nextSet(existing))
    }
}
