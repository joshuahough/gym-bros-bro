package com.gymbrosbro.app.avatar

import com.gymbrosbro.app.domain.model.MuscleGroup
import com.gymbrosbro.app.domain.progress.Trophies
import com.gymbrosbro.app.sprites.MuscleSprites
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarTest {
    @Test
    fun `item ids are unique`() {
        assertEquals(Locker.items.size, Locker.items.map { it.id }.toSet().size)
    }

    @Test
    fun `defaults are starter items in the right slots`() {
        val defaults = AvatarConfig()
        Locker.sections.forEach { section ->
            val item = Locker.item(section.get(defaults))
            assertNotNull(section.title, item)
            assertEquals(section.slot, item!!.slot)
            assertEquals(Unlock.Starter, item.unlock)
        }
    }

    @Test
    fun `trophy unlocks point at real trophies`() {
        Locker.items.mapNotNull { it.unlock as? Unlock.ByTrophy }.forEach {
            assertNotNull(it.trophyId, Trophies.byId(it.trophyId))
        }
    }

    @Test
    fun `every item renders, in both idle frames`() {
        // Sprite's constructor checks sizes and that every pixel has a color.
        Locker.sections.forEach { section ->
            Locker.inSlot(section.slot).forEach { item ->
                val frames = AvatarRenderer.frames(section.set(AvatarConfig(), item.id))
                frames.forEach {
                    assertEquals(AvatarArt.WIDTH, it.width)
                    assertEquals(AvatarArt.HEIGHT, it.height)
                }
            }
        }
    }

    @Test
    fun `hats hide hair poking out above them, headbands don't`() {
        val spiky = AvatarConfig(hair = "hair-spiky")
        val topRow = AvatarArt.HEADROOM + AvatarArt.HairSpiky.top
        val bare = AvatarRenderer.sprite(spiky).rows[topRow]
        assertTrue(bare.any { it != '.' })
        assertTrue(AvatarRenderer.sprite(spiky.copy(headwear = "headwear-cap")).rows[topRow].all { it == '.' })
        assertEquals(bare, AvatarRenderer.sprite(spiky.copy(headwear = "headwear-headband")).rows[topRow])
    }

    @Test
    fun `sanitize swaps locked, unknown and wrong-slot items for defaults`() {
        val config = AvatarConfig(hair = "hair-mohawk", topColor = "nope", shoes = "hair-brown", top = "top-tee")
        val clean = Locker.sanitize(config, unlockedIds = emptySet())
        assertEquals(AvatarConfig(top = "top-tee"), clean)
        assertEquals("hair-mohawk", Locker.sanitize(config, setOf("hair-mohawk")).hair)
    }

    @Test
    fun `qualifies by level or trophy`() {
        val cap = Locker.item("headwear-cap")!!
        assertFalse(Locker.qualifies(cap, level = 3, trophyIds = emptySet()))
        assertTrue(Locker.qualifies(cap, level = 4, trophyIds = emptySet()))
        val crown = Locker.item("headwear-crown")!!
        assertTrue(Locker.qualifies(crown, level = 1, trophyIds = setOf("workouts-100")))
    }

    @Test
    fun `every muscle group has a 12x12 icon`() {
        MuscleGroup.entries.forEach {
            val icon = MuscleSprites.of(it)
            assertEquals(12, icon.width)
            assertEquals(12, icon.height)
        }
    }
}
