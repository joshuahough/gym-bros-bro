package com.gymbrosbro.app.avatar

/**
 * One piece of the avatar, drawn over the layers below it. [top] is the body
 * row of the first line (negative rows sit above the head); `.` keeps
 * whatever is underneath.
 */
class Layer(val top: Int, val rows: List<String>) {
    init {
        require(rows.all { it.length == AvatarArt.WIDTH }) { "Avatar layer rows must be ${AvatarArt.WIDTH} wide" }
    }
}

private fun layer(top: Int, vararg rows: String) = Layer(top, rows.toList())

/**
 * Avatar pixel art. Each layer owns its palette chars so the locker can
 * recolor them independently:
 * `o` outline, `s`/`d` skin and shade, `k` eyes, `h` hair (and brows),
 * `f`/`g` top, `p`/`q` bottom, `b` shoes, `w` socks and soles,
 * `c`/`e` headwear, `a`/`y` accessory.
 */
object AvatarArt {
    const val WIDTH = 22
    const val BODY_HEIGHT = 34

    /** Rows of headroom above the body for spiky hair and crowns. */
    const val HEADROOM = 3
    const val HEIGHT = BODY_HEIGHT + HEADROOM

    /** Body row dropped in the second idle frame, so the upper body bobs down a pixel. */
    const val BOB_ROW = 22

    /** Bald, shirtless and barefoot; everything else goes on top. */
    val Body = layer(
        0,
        ".......oooooooo.......",
        "......osssssssso......",
        ".....osssssssssso.....",
        ".....osssssssssso.....",
        ".....osssssssssso.....",
        ".....osshsssshsso.....",
        ".....osskssssksso.....",
        ".....osssssssssso.....",
        "......ossskkssso......",
        ".......osssssso.......",
        ".......odssssdo.......",
        "....oossssssssssoo....",
        "..osssssssssssssssso..",
        ".osssssdssssssdssssso.",
        "osssssdssssssssdssssso",
        "ossdosssddssddsssodsso",
        "osdo.osssssssssso.odso",
        "osdo.ossssddsssso.odso",
        ".ossdoossssssssoodsso.",
        "..ossdosssddsssodsso..",
        "..osssdssssssssdssso..",
        "...oooossssssssoooo...",
        "......osssssssso......",
        "......osssssssso......",
        "......osssssssso......",
        "......osssoossso......",
        "......osssoossso......",
        "......osdsoosdso......",
        "......osssoossso......",
        "......osssoossso......",
        "......osssoossso......",
        ".....ossssoosssso.....",
        ".....ossssoosssso.....",
        ".....oooooooooooo.....",
    )

    // Hair

    val HairShort = layer(
        1,
        ".......hhhhhhhh.......",
        "......hhhhhhhhhh......",
        "......hhhhhhhhhh......",
        "......hh......hh......",
        "......h........h......",
        "......h........h......",
    )

    val HairBuzz = layer(
        1,
        ".......hhhhhhhh.......",
        "......hhhhhhhhhh......",
        "......h........h......",
    )

    val HairSpiky = layer(
        -2,
        ".......o..oo..o.......",
        "......ohoohhooho......",
        ".....ohhhhhhhhhho.....",
        ".....ohhhhhhhhhho.....",
        "......hhhhhhhhhh......",
        "......hhhhhhhhhh......",
        "......hh......hh......",
        "......h........h......",
        "......h........h......",
    )

    val HairMohawk = layer(
        -2,
        ".........oooo.........",
        "........ohhhho........",
        "........ohhhho........",
        ".........hhhh.........",
        ".........hhhh.........",
        ".........hhhh.........",
    )

    val HairLong = layer(
        1,
        ".......hhhhhhhh.......",
        "......hhhhhhhhhh......",
        "....ohhhhhhhhhhhho....",
        "....ohhh......hhho....",
        "....ohh........hho....",
        "....ohh........hho....",
        "....ohh........hho....",
        "....ohh........hho....",
        "....ohh........hho....",
        "....ooo........ooo....",
    )

    // Headwear

    val Headband = layer(
        3,
        "......cccccccccc......",
        "......eeeeeeeeee......",
    )

    val Cap = layer(
        -1,
        ".......oooooooo.......",
        "......occcccccco......",
        ".....occcceecccco.....",
        ".....occcccccccco.....",
        "....oeeeeeeeeeeeeo....",
        "....oooooooooooooo....",
    )

    val Beanie = layer(
        -2,
        "..........ee..........",
        ".......occcccco.......",
        "......occcccccco......",
        ".....occcccccccco.....",
        ".....occcccccccco.....",
        ".....oeeeeeeeeeeo.....",
        ".....oeeeeeeeeeeo.....",
    )

    val Crown = layer(
        -3,
        ".......o..oo..o.......",
        "......ocooccooco......",
        ".....occcccccccco.....",
        ".....occeccccecco.....",
        ".....occcccccccco.....",
    )

    // Tops

    val Tank = layer(
        11,
        "........f....f........",
        "........f....f........",
        "........ff..ff........",
        ".......ffffffff.......",
        ".......ffffffff.......",
        ".......ffffffff.......",
        ".......ffffffff.......",
        ".......ffffffff.......",
        ".......ffffffff.......",
    )

    val Tee = layer(
        11,
        "......fff....fff......",
        "...fffffff..fffffff...",
        "..ffffffffffffffffff..",
        ".gggggoffffffffoggggg.",
        ".....ffffffffffff.....",
        "......ffffffffff......",
        "......ffffffffff......",
        ".......ffffffff.......",
        ".......ffffffff.......",
    )

    val Stringer = layer(
        11,
        ".........f..f.........",
        ".........f..f.........",
        "........ff..ff........",
        "........ffffff........",
        "........ffffff........",
        "........ffffff........",
        "........ffffff........",
        "........ffffff........",
        "........ffffff........",
    )

    val Hoodie = layer(
        11,
        "......fff....fff......",
        "...ffffffg..gffffff...",
        "..fffffffwffwfffffff..",
        ".ffffgoffwffwffogffff.",
        ".fff.ffffffffffff.fff.",
        ".ff...ffffffffff...ff.",
        ".ff...ffffffffff...ff.",
        "..fff..ffggggff..fff..",
        "...ggg.ffffffff.ggg...",
        ".......gggggggg.......",
    )

    // Bottoms

    val Shorts = layer(
        20,
        ".......qqqqqqqq.......",
        ".......pppppppp.......",
        ".......pppppppp.......",
        ".......pppppppp.......",
        ".......pppppppp.......",
        ".......ppp..ppp.......",
    )

    val Joggers = layer(
        20,
        ".......qqqqqqqq.......",
        ".......pppppppp.......",
        ".......pppppppp.......",
        ".......pppppppp.......",
        ".......pppppppp.......",
        ".......ppp..ppp.......",
        ".......ppp..ppp.......",
        ".......pqp..pqp.......",
        ".......ppp..ppp.......",
        ".......ppp..ppp.......",
        ".......qqq..qqq.......",
    )

    val Sneakers = layer(
        30,
        ".......www..www.......",
        "......bbbb..bbbb......",
        "......wwww..wwww......",
    )

    // Accessories

    val Wristbands = layer(
        19,
        "...aaa..........aaa...",
    )

    val Headphones = layer(
        -1,
        ".......aaaaaaaa.......",
        "......a........a......",
        ".....a..........a.....",
        "....a............a....",
        "....a............a....",
        "...yyy..........yyy...",
        "...yyy..........yyy...",
        "...yyy..........yyy...",
        "....y............y....",
    )

    val Belt = layer(
        19,
        ".......aaaaaaaa.......",
        ".......aaayyaaa.......",
    )

    val Chain = layer(
        11,
        "........y....y........",
        ".........y..y.........",
        "..........yy..........",
    )

    val Gloves = layer(
        19,
        "...yyy..........yyy...",
        "...aaaa........aaaa...",
    )
}
