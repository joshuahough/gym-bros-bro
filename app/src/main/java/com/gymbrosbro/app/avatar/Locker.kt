package com.gymbrosbro.app.avatar

import androidx.compose.ui.graphics.Color
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.serialization.Serializable

enum class LockerSlot { SKIN, HAIR, HAIR_COLOR, HEADWEAR, TOP, BOTTOM, ACCESSORY, GEAR_COLOR }

/** How an item is earned. Earned items stay unlocked for good (SPEC §4.5). */
sealed interface Unlock {
    data object Starter : Unlock
    data class AtLevel(val level: Int) : Unlock
    data class ByTrophy(val trophyId: String) : Unlock
}

/**
 * Something in the locker. Style items carry a [layer] and colors for its
 * own palette chars; color items (skin, hair color, gear colors) carry a
 * [swatch] and optional [shade]. Ids are stored, so never change one.
 */
data class LockerItem(
    val id: String,
    val slot: LockerSlot,
    val name: String,
    val unlock: Unlock = Unlock.Starter,
    val layer: Layer? = null,
    val palette: Map<Char, Color> = emptyMap(),
    val swatch: Color = Color.Unspecified,
    val shade: Color? = null,
    /** Headwear that hides hair poking out above it. */
    val coversHair: Boolean = false,
)

/** What the player is wearing: one item id per slot. */
@Serializable
data class AvatarConfig(
    val skin: String = "skin-classic",
    val hair: String = "hair-short",
    val hairColor: String = "hair-brown",
    val headwear: String = "headwear-none",
    val top: String = "top-tank",
    val topColor: String = "color-cyan",
    val bottom: String = "bottom-shorts",
    val bottomColor: String = "color-blue",
    val shoes: String = "color-red",
    val accessory: String = "accessory-none",
)

/** A row in the locker screen: which items it offers and which config field it sets. */
data class LockerSection(
    val title: String,
    val slot: LockerSlot,
    val get: (AvatarConfig) -> String,
    val set: (AvatarConfig, String) -> AvatarConfig,
)

object Locker {
    private fun skin(id: String, name: String, skin: Long, shade: Long) =
        LockerItem("skin-$id", LockerSlot.SKIN, name, swatch = Color(skin), shade = Color(shade))

    private fun hairColor(id: String, name: String, color: Color, unlock: Unlock = Unlock.Starter) =
        LockerItem("hair-$id", LockerSlot.HAIR_COLOR, name, unlock, swatch = color)

    private fun gear(id: String, name: String, color: Color, unlock: Unlock = Unlock.Starter) =
        LockerItem("color-$id", LockerSlot.GEAR_COLOR, name, unlock, swatch = color)

    private fun level(n: Int) = Unlock.AtLevel(n)
    private fun trophy(id: String) = Unlock.ByTrophy(id)

    val items: List<LockerItem> = listOf(
        skin("classic", "Classic", 0xFFFF9A3C, 0xFFC86E2C),
        skin("porcelain", "Porcelain", 0xFFFFE3C8, 0xFFE8B996),
        skin("fair", "Fair", 0xFFF6CBA5, 0xFFD9A47A),
        skin("tan", "Tan", 0xFFE0A878, 0xFFBC8456),
        skin("olive", "Olive", 0xFFC68F5E, 0xFFA06D40),
        skin("bronze", "Bronze", 0xFFA5703F, 0xFF7F522B),
        skin("brown", "Brown", 0xFF7A4B2A, 0xFF5C361C),
        skin("deep", "Deep", 0xFF52311C, 0xFF3A2213),

        LockerItem("hair-none", LockerSlot.HAIR, "Bald"),
        LockerItem("hair-short", LockerSlot.HAIR, "Short", layer = AvatarArt.HairShort),
        LockerItem("hair-buzz", LockerSlot.HAIR, "Buzz", layer = AvatarArt.HairBuzz),
        LockerItem("hair-spiky", LockerSlot.HAIR, "Spiky", level(3), AvatarArt.HairSpiky),
        LockerItem("hair-long", LockerSlot.HAIR, "Long", level(5), AvatarArt.HairLong),
        LockerItem("hair-mohawk", LockerSlot.HAIR, "Mohawk", trophy("combo-10"), AvatarArt.HairMohawk),

        hairColor("brown", "Brown", GbbColors.Brown),
        hairColor("black", "Black", Color(0xFF2B2235)),
        hairColor("blonde", "Blonde", Color(0xFFF2C35B)),
        hairColor("ginger", "Ginger", Color(0xFFD2642B)),
        hairColor("gray", "Gray", GbbColors.Fog),
        hairColor("platinum", "Platinum", GbbColors.Chalk, level(4)),
        hairColor("pink", "Pink", GbbColors.Pink, level(6)),
        hairColor("blue", "Blue", GbbColors.Blue, level(8)),
        hairColor("green", "Green", GbbColors.Green, level(10)),
        hairColor("purple", "Purple", GbbColors.Purple, level(12)),

        LockerItem("headwear-none", LockerSlot.HEADWEAR, "None"),
        LockerItem(
            "headwear-headband", LockerSlot.HEADWEAR, "Headband", level(2), AvatarArt.Headband,
            mapOf('c' to GbbColors.Red, 'e' to GbbColors.Chalk),
        ),
        LockerItem(
            "headwear-cap", LockerSlot.HEADWEAR, "Cap", level(4), AvatarArt.Cap,
            mapOf('c' to GbbColors.Red, 'e' to GbbColors.Chalk), coversHair = true,
        ),
        LockerItem(
            "headwear-beanie", LockerSlot.HEADWEAR, "Beanie", level(7), AvatarArt.Beanie,
            mapOf('c' to GbbColors.Purple, 'e' to GbbColors.Chalk), coversHair = true,
        ),
        LockerItem(
            "headwear-crown", LockerSlot.HEADWEAR, "Crown", trophy("workouts-100"), AvatarArt.Crown,
            mapOf('c' to GbbColors.Gold, 'e' to GbbColors.Red), coversHair = true,
        ),

        LockerItem("top-tank", LockerSlot.TOP, "Tank top", layer = AvatarArt.Tank),
        LockerItem("top-tee", LockerSlot.TOP, "Tee", layer = AvatarArt.Tee),
        LockerItem("top-stringer", LockerSlot.TOP, "Stringer", level(5), AvatarArt.Stringer),
        LockerItem("top-hoodie", LockerSlot.TOP, "Hoodie", level(8), AvatarArt.Hoodie),
        LockerItem("top-none", LockerSlot.TOP, "Shirtless", trophy("volume-100k")),

        LockerItem("bottom-shorts", LockerSlot.BOTTOM, "Shorts", layer = AvatarArt.Shorts),
        LockerItem("bottom-joggers", LockerSlot.BOTTOM, "Joggers", level(3), AvatarArt.Joggers),

        LockerItem("accessory-none", LockerSlot.ACCESSORY, "None"),
        LockerItem(
            "accessory-wristbands", LockerSlot.ACCESSORY, "Wristbands", trophy("workouts-10"), AvatarArt.Wristbands,
            mapOf('a' to GbbColors.Chalk),
        ),
        LockerItem(
            "accessory-belt", LockerSlot.ACCESSORY, "Lifting belt", trophy("records-1"), AvatarArt.Belt,
            mapOf('a' to GbbColors.Brown, 'y' to GbbColors.Gold),
        ),
        LockerItem(
            "accessory-headphones", LockerSlot.ACCESSORY, "Headphones", level(6), AvatarArt.Headphones,
            mapOf('a' to GbbColors.Steel, 'y' to GbbColors.Pink),
        ),
        LockerItem(
            "accessory-gloves", LockerSlot.ACCESSORY, "Gloves", level(9), AvatarArt.Gloves,
            mapOf('a' to GbbColors.Steel, 'y' to GbbColors.Red),
        ),
        LockerItem(
            "accessory-chain", LockerSlot.ACCESSORY, "Gold chain", trophy("pull-ups-1000"), AvatarArt.Chain,
            mapOf('y' to GbbColors.Gold),
        ),

        gear("cyan", "Cyan", GbbColors.Cyan),
        gear("red", "Red", GbbColors.Red),
        gear("blue", "Blue", GbbColors.Blue),
        gear("white", "White", GbbColors.Chalk),
        gear("gray", "Gray", GbbColors.Steel),
        gear("black", "Black", GbbColors.Dusk),
        gear("green", "Green", GbbColors.Green, level(2)),
        gear("orange", "Orange", GbbColors.Orange, level(3)),
        gear("purple", "Purple", GbbColors.Purple, level(4)),
        gear("pink", "Pink", GbbColors.Pink, level(6)),
        gear("gold", "Gold", GbbColors.Gold, level(10)),
        gear("forest", "Forest", GbbColors.Forest, trophy("weekly-12")),
    )

    private val byId = items.associateBy { it.id }

    val sections: List<LockerSection> = listOf(
        LockerSection("SKIN", LockerSlot.SKIN, { it.skin }, { c, id -> c.copy(skin = id) }),
        LockerSection("HAIR", LockerSlot.HAIR, { it.hair }, { c, id -> c.copy(hair = id) }),
        LockerSection("HAIR COLOR", LockerSlot.HAIR_COLOR, { it.hairColor }, { c, id -> c.copy(hairColor = id) }),
        LockerSection("HEADWEAR", LockerSlot.HEADWEAR, { it.headwear }, { c, id -> c.copy(headwear = id) }),
        LockerSection("TOP", LockerSlot.TOP, { it.top }, { c, id -> c.copy(top = id) }),
        LockerSection("TOP COLOR", LockerSlot.GEAR_COLOR, { it.topColor }, { c, id -> c.copy(topColor = id) }),
        LockerSection("BOTTOM", LockerSlot.BOTTOM, { it.bottom }, { c, id -> c.copy(bottom = id) }),
        LockerSection("BOTTOM COLOR", LockerSlot.GEAR_COLOR, { it.bottomColor }, { c, id -> c.copy(bottomColor = id) }),
        LockerSection("SHOES", LockerSlot.GEAR_COLOR, { it.shoes }, { c, id -> c.copy(shoes = id) }),
        LockerSection("ACCESSORY", LockerSlot.ACCESSORY, { it.accessory }, { c, id -> c.copy(accessory = id) }),
    )

    fun item(id: String): LockerItem? = byId[id]

    fun inSlot(slot: LockerSlot): List<LockerItem> = items.filter { it.slot == slot }

    /** Whether [item] is wearable, given what's been earned so far. */
    fun isUnlocked(item: LockerItem, unlockedIds: Set<String>): Boolean =
        item.unlock == Unlock.Starter || item.id in unlockedIds

    /** Whether [item]'s requirement is met now; used to grant new unlocks. */
    fun qualifies(item: LockerItem, level: Int, trophyIds: Set<String>): Boolean = when (val u = item.unlock) {
        Unlock.Starter -> true
        is Unlock.AtLevel -> level >= u.level
        is Unlock.ByTrophy -> u.trophyId in trophyIds
    }

    /** Swaps anything unknown or locked back to the default, e.g. after a restore. */
    fun sanitize(config: AvatarConfig, unlockedIds: Set<String>): AvatarConfig {
        val defaults = AvatarConfig()
        return sections.fold(config) { c, section ->
            val item = item(section.get(c))
            if (item != null && item.slot == section.slot && isUnlocked(item, unlockedIds)) c
            else section.set(c, section.get(defaults))
        }
    }
}
