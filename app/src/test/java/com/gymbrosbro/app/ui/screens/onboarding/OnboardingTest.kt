package com.gymbrosbro.app.ui.screens.onboarding

import com.gymbrosbro.app.domain.units.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

class OnboardingTest {
    @Test
    fun `day mask matches schedule rule bits`() {
        assertEquals(0b0010101, Onboarding.dayMask(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)))
        assertEquals(0b1000000, Onboarding.dayMask(setOf(DayOfWeek.SUNDAY)))
        assertEquals(0, Onboarding.dayMask(emptySet()))
    }

    @Test
    fun `parses bodyweight in the chosen units`() {
        assertEquals(80.0, Onboarding.parseBodyweightKg("80", UnitSystem.METRIC)!!, 0.001)
        assertEquals(80.0, Onboarding.parseBodyweightKg("80,0", UnitSystem.METRIC)!!, 0.001)
        assertEquals(81.65, Onboarding.parseBodyweightKg("180", UnitSystem.IMPERIAL)!!, 0.01)
    }

    @Test
    fun `rejects blank and implausible bodyweights`() {
        assertNull(Onboarding.parseBodyweightKg("", UnitSystem.METRIC))
        assertNull(Onboarding.parseBodyweightKg("8", UnitSystem.METRIC))
        assertNull(Onboarding.parseBodyweightKg("1800", UnitSystem.IMPERIAL))
        assertNull(Onboarding.parseBodyweightKg("1.2.3", UnitSystem.METRIC))
    }
}
