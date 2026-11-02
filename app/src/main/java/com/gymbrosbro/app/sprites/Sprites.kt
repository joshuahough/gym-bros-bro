package com.gymbrosbro.app.sprites

import androidx.compose.ui.graphics.Color
import com.gymbrosbro.app.ui.theme.GbbColors

/** Shadow for the default skin tone; the locker will swap `s` and `d` together. */
private val SkinShade = Color(0xFFC86E2C)

/**
 * Hand-authored sprites: 12x12 icons plus the full-body [Avatar]. Convention:
 * `o` = dark outline, `f` = the recolorable fill (nav highlight, muscle color,
 * shirt), `w` = white.
 */
object Sprites {
    private val base = mapOf(
        'o' to GbbColors.Void,
        'f' to GbbColors.Fog,
        'w' to GbbColors.Chalk,
    )

    val Home = Sprite(
        listOf(
            ".....oo.....",
            "....offo....",
            "...offffo...",
            "..offffffo..",
            ".offffffffo.",
            "oooffffffooo",
            "..offffffo..",
            "..offffffo..",
            "..offooffo..",
            "..offooffo..",
            "..offooffo..",
            "..oooooooo..",
        ),
        base,
    )

    val Dumbbell = Sprite(
        listOf(
            "............",
            "............",
            ".oo......oo.",
            "offo....offo",
            "offo....offo",
            "offowwwwoffo",
            "offowwwwoffo",
            "offo....offo",
            "offo....offo",
            ".oo......oo.",
            "............",
            "............",
        ),
        base,
    )

    val Calendar = Sprite(
        listOf(
            "..o.....o...",
            "oooooooooooo",
            "offffffffffo",
            "oooooooooooo",
            "owwwwwwwwwwo",
            "owowowowowwo",
            "owwwwwwwwwwo",
            "owowowowowwo",
            "owwwwwwwwwwo",
            "owowowwwwwwo",
            "owwwwwwwwwwo",
            "oooooooooooo",
        ),
        base,
    )

    val BarChart = Sprite(
        listOf(
            "............",
            ".........ff.",
            ".........ff.",
            ".........ff.",
            ".....ff..ff.",
            ".....ff..ff.",
            ".....ff..ff.",
            ".ff..ff..ff.",
            ".ff..ff..ff.",
            ".ff..ff..ff.",
            ".ff..ff..ff.",
            "wwwwwwwwwwww",
        ),
        base,
    )

    /** Player-1 head for the Me tab icon: `h` hair, `s` skin, `k` eyes/mouth, `f` shirt. */
    val Player = Sprite(
        listOf(
            "...hhhhhh...",
            "..hhhhhhhh..",
            ".hhhhhhhhhh.",
            ".hssssssssh.",
            ".ssssssssss.",
            ".ssksssskss.",
            ".ssssssssss.",
            ".sssskkssss.",
            "..ssssssss..",
            "....ssss....",
            ".ffffffffff.",
            "ffffffffffff",
        ),
        base + mapOf(
            'h' to GbbColors.Brown,
            's' to GbbColors.Orange,
            'k' to GbbColors.Void,
        ),
    )

    /**
     * Player-1 full body, 20x34. Each palette char is a swappable layer for the
     * locker: `h` hair, `s`/`d` skin and its shade, `k` eyes/mouth, `f` top,
     * `p` shorts, `w` socks and soles, `b` shoes.
     */
    val Avatar = Sprite(
        listOf(
            "......oooooooo......",
            ".....ohhhhhhhho.....",
            "....ohhhhhhhhhho....",
            "....ohhhhhhhhhho....",
            "....ohhsssssshho....",
            "....ohshsssshsho....",
            "....ohskssssksho....",
            "....osssksskssso....",
            ".....ossskkssso.....",
            "......osssssso......",
            "......odssssdo......",
            "...oossfssssfssoo...",
            "..ossssfssssfsssso..",
            ".ossssdffssffdsssso.",
            ".osssdffffffffdssso.",
            ".ossdoffffffffodsso.",
            ".ossdoffffffffodsso.",
            ".osssoffffffffossso.",
            "..ossoffffffffosso..",
            "..ossoffffffffosso..",
            "..ossopppppppposso..",
            ".osssoppppppppossso.",
            ".osssoppppppppossso.",
            "..ooooppppppppoooo..",
            ".....oppppppppo.....",
            ".....opppoopppo.....",
            ".....osssoossso.....",
            ".....osdsoosdso.....",
            ".....osssoossso.....",
            ".....osssoossso.....",
            ".....owwwoowwwo.....",
            "....obbbboobbbbo....",
            "....owwwwoowwwwo....",
            "....oooooooooooo....",
        ),
        base + mapOf(
            'h' to GbbColors.Brown,
            's' to GbbColors.Orange,
            'd' to SkinShade,
            'k' to GbbColors.Void,
            'f' to GbbColors.Cyan,
            'p' to GbbColors.Blue,
            'b' to GbbColors.Red,
        ),
    )

    /** Flexed arm; recolor `f` per muscle group until Phase 3's unique icons. */
    val Flex = Sprite(
        listOf(
            "............",
            ".......ooo..",
            "......offfo.",
            "......offfo.",
            "...oo.offo..",
            "..offooffo..",
            ".offffffffo.",
            "offfffffffo.",
            "offffffffo..",
            ".offffffo...",
            "..oooooo....",
            "............",
        ),
        base,
    )

    val ArrowLeft = Sprite(
        listOf(
            "............",
            "....ff......",
            "...fff......",
            "..ffff......",
            ".fffff......",
            "ffffffffffff",
            "ffffffffffff",
            ".fffff......",
            "..ffff......",
            "...fff......",
            "....ff......",
            "............",
        ),
        base,
    )

    val Check = Sprite(
        listOf(
            "............",
            "............",
            "..........ff",
            ".........fff",
            "........fff.",
            ".ff....fff..",
            ".fff..fff...",
            "..ffffff....",
            "...ffff.....",
            "....ff......",
            "............",
            "............",
        ),
        base,
    )

    val Star = Sprite(
        listOf(
            ".....ff.....",
            ".....ff.....",
            "....ffff....",
            "ffffffffffff",
            ".ffffffffff.",
            "..ffffffff..",
            "...ffffff...",
            "...ffffff...",
            "..fff..fff..",
            "..ff....ff..",
            ".ff......ff.",
            "............",
        ),
        base,
    )

    val Dots = Sprite(
        listOf(
            "............",
            "............",
            "............",
            "............",
            "............",
            ".ff..ff..ff.",
            ".ff..ff..ff.",
            "............",
            "............",
            "............",
            "............",
            "............",
        ),
        base,
    )

    val all: List<Sprite> = listOf(Home, Dumbbell, Calendar, BarChart, Player, Flex, ArrowLeft, Check, Star, Dots)
}
