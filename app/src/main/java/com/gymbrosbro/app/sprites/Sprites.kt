package com.gymbrosbro.app.sprites

import com.gymbrosbro.app.ui.theme.GbbColors

/**
 * Hand-authored 12x12 icons. Convention: `o` = dark outline, `f` = the
 * recolorable fill (nav highlight, shirt), `w` = white. The avatar lives in
 * [com.gymbrosbro.app.avatar] and muscle icons in [MuscleSprites].
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

    val all: List<Sprite> = listOf(Home, Dumbbell, Calendar, BarChart, Player, ArrowLeft, Check, Star, Dots)
}
