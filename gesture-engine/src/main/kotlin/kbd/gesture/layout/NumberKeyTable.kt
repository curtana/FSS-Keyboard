package kbd.gesture.layout

import kbd.stroke.geom.Dir

/**
 * Perimeter number-key table (LLD-04 §8.2, SWIPE 7). Directions toward the screen border have
 * no entry: reaching the border is an exit (FR-SWP-45, issue #237), and an unassigned exit equals the tap. Issue #208: 2 ← [ → ]
 * (its < > went to the symbol key, FSS#75), 6 ← has no symbol (its / is the symbol3 key's tap) = the tap.
 */
object NumberKeyTable {
    val MAP: Map<Char, Map<Dir, String>> = mapOf(
        '1' to mapOf(Dir.UP to "!", Dir.RIGHT to "`", Dir.DOWN to "·"),
        '2' to mapOf(Dir.UP to "@", Dir.LEFT to "[", Dir.RIGHT to "]", Dir.DOWN to "※"),
        '3' to mapOf(Dir.UP to "#", Dir.LEFT to "{", Dir.RIGHT to "}", Dir.DOWN to "±"),
        '4' to mapOf(Dir.UP to "$", Dir.LEFT to ";", Dir.RIGHT to ":", Dir.DOWN to "€"),   // ← ; (owner #208)
        '5' to mapOf(Dir.UP to "%", Dir.LEFT to "-", Dir.RIGHT to "+", Dir.DOWN to "×"),   // ← - (owner FSS#75: swapped with the symbol key's _)
        '6' to mapOf(Dir.UP to "^", Dir.RIGHT to "~", Dir.DOWN to "÷"),
        '7' to mapOf(Dir.UP to "&", Dir.LEFT to "\\", Dir.DOWN to "≠"),
        '8' to mapOf(Dir.UP to "*", Dir.RIGHT to "|", Dir.DOWN to "∞"),
        '9' to mapOf(Dir.UP to "(", Dir.LEFT to "₩", Dir.DOWN to "≈"),
        '0' to mapOf(Dir.UP to ")", Dir.RIGHT to "…", Dir.DOWN to "°"),
    )

    /** Direction map of [digit]; empty for a character that is not a perimeter digit. */
    operator fun get(digit: Char): Map<Dir, String> = MAP[digit].orEmpty()
}
