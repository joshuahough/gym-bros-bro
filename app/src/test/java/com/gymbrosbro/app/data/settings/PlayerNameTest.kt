package com.gymbrosbro.app.data.settings

import com.gymbrosbro.app.data.settings.SettingsRepository.Companion.cleanPlayerName
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerNameTest {
    @Test
    fun `keeps letters, digits and single spaces`() {
        assertEquals("Big Josh 99", cleanPlayerName("  Big   Josh!! 99 "))
    }

    @Test
    fun `caps the length without a trailing space`() {
        assertEquals("ABCDEFGHIJK", cleanPlayerName("ABCDEFGHIJK LMNOP"))
        assertEquals(SettingsRepository.PLAYER_NAME_MAX, cleanPlayerName("x".repeat(40)).length)
    }

    @Test
    fun `nothing usable is blank`() {
        assertEquals("", cleanPlayerName(" !?. "))
    }
}
