package com.gymbrosbro.app.domain.units

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {
    @Test
    fun `pounds round-trip through canonical kilograms`() {
        val kg = Units.displayToKg(45.0, UnitSystem.IMPERIAL)
        assertEquals(20.41165665, kg, 1e-8)
        assertEquals(45.0, Units.kgToDisplay(kg, UnitSystem.IMPERIAL), 1e-9)
    }

    @Test
    fun `metric weight is stored as-is`() {
        assertEquals(20.0, Units.displayToKg(20.0, UnitSystem.METRIC), 0.0)
    }

    @Test
    fun `formats weights without trailing zeros`() {
        val kg = Units.displayToKg(45.0, UnitSystem.IMPERIAL)
        assertEquals("45 lb", Units.formatWeight(kg, UnitSystem.IMPERIAL))
        assertEquals("20.4 kg", Units.formatWeight(kg, UnitSystem.METRIC))
        assertEquals("2.5 kg", Units.formatWeight(2.5, UnitSystem.METRIC))
    }

    @Test
    fun `formats distances in miles and kilometers`() {
        assertEquals("1 mi", Units.formatDistance(1609.344, UnitSystem.IMPERIAL))
        assertEquals("5 km", Units.formatDistance(5000.0, UnitSystem.METRIC))
        assertEquals("3.11 mi", Units.formatDistance(5000.0, UnitSystem.IMPERIAL))
    }

    @Test
    fun `formatNumber handles rounding and negatives`() {
        assertEquals("10", Units.formatNumber(9.96, decimals = 1))
        assertEquals("-2.5", Units.formatNumber(-2.5, decimals = 1))
        assertEquals("0", Units.formatNumber(-0.01, decimals = 1))
    }
}
