package kbd.stroke.tap

import kbd.stroke.geom.Dir
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyKind
import kbd.stroke.layout.TapLayouts

/**
 * The modifier keys of a chord (LLD-05A §2.4, SRS FR-TAP-15, 16). The declaration order is the order their key-down
 * events are sent in. Ctrl and Alt are the edit row keys; Shift has no key and only rides along with them.
 */
enum class ChordModifier { CTRL, ALT, SHIFT }

/**
 * A key of a PC keyboard (US layout) that a chord can carry, free of Android (LLD-05A §2.4). `D0`..`D9` are the
 * digit keys; the others have the names of the platform's key codes.
 */
enum class ChordKey {
    A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
    D0, D1, D2, D3, D4, D5, D6, D7, D8, D9,
    GRAVE, MINUS, EQUALS, LEFT_BRACKET, RIGHT_BRACKET, BACKSLASH, SEMICOLON, APOSTROPHE, COMMA, PERIOD, SLASH,
    SPACE, ENTER, TAB, DPAD_LEFT, DPAD_UP, DPAD_DOWN, DPAD_RIGHT, ESCAPE, FORWARD_DEL, DEL,
}

/** The key that types a character on a PC keyboard, and whether Shift is held for it. */
data class ChordStroke(val key: ChordKey, val shift: Boolean)

/**
 * What a key sends while Ctrl or Alt is on: the SRS chord table (FR-TAP-16, LLD-05A §2.4). One rule for character
 * keys: the character the gesture types on the QWERTY keyboard, sent as the PC key that types it, with Shift when
 * that key needs it. Pure.
 */
object ChordKeys {

    private val LETTER_KEYS = ChordKey.entries.subList(ChordKey.A.ordinal, ChordKey.Z.ordinal + 1)
    private val DIGIT_KEYS = ChordKey.entries.subList(ChordKey.D0.ordinal, ChordKey.D9.ordinal + 1)

    /** What Shift types on the digit keys 0..9. */
    private const val DIGIT_SHIFTED = ")!@#$%^&*("

    /** The symbol keys: the character each types, then the one it types with Shift. */
    private val SYMBOL_KEYS = listOf(
        "`~" to ChordKey.GRAVE, "-_" to ChordKey.MINUS, "=+" to ChordKey.EQUALS,
        "[{" to ChordKey.LEFT_BRACKET, "]}" to ChordKey.RIGHT_BRACKET, "\\|" to ChordKey.BACKSLASH,
        ";:" to ChordKey.SEMICOLON, "'\"" to ChordKey.APOSTROPHE,
        ",<" to ChordKey.COMMA, ".>" to ChordKey.PERIOD, "/?" to ChordKey.SLASH,
    )

    /** The 94 printable ASCII characters other than the space: 26 small and 26 capital letters, 10 digits, 32 symbols. */
    val CHARS: Map<Char, ChordStroke> = buildMap {
        LETTER_KEYS.forEachIndexed { i, k ->
            put('a' + i, ChordStroke(k, shift = false))
            put('A' + i, ChordStroke(k, shift = true))
        }
        DIGIT_KEYS.forEachIndexed { i, k ->
            put('0' + i, ChordStroke(k, shift = false))
            put(DIGIT_SHIFTED[i], ChordStroke(k, shift = true))
        }
        for ((pair, k) in SYMBOL_KEYS) {
            put(pair[0], ChordStroke(k, shift = false))
            put(pair[1], ChordStroke(k, shift = true))
        }
    }

    /** The PC key of [c], or null when a PC keyboard has no key for it. */
    fun ofChar(c: Char): ChordStroke? = CHARS[c]

    /**
     * The chord key of gesture [g] on the letter, number or symbol key [keyId] of either tap layout. The QWERTY key
     * of the same id decides, so a Dubeolsik key sends the English key of its place, and its ↑ carries Shift even
     * where it types the same jamo as the tap (ㅁ ↑ = Shift + M, FR-TAP-16 ③). An up or down rub counts as a tap:
     * a PC keyboard has no superscripts or subscripts (FR-TAP-16 ②).
     */
    fun ofTap(keyId: String, g: TapGesture): ChordStroke {
        val key = requireNotNull(TapLayouts.QWERTY.keys.firstOrNull { it.id == keyId && it.kind != TapKeyKind.FUNCTION }) { keyId }
        val gesture = if (g is TapGesture.Rub && (g.dir == Dir.UP || g.dir == Dir.DOWN)) TapGesture.Tap else g
        val text = checkNotNull(TapKeyResolver.resolve(key, gesture).text) { keyId }
        return checkNotNull(text.singleOrNull()?.let(::ofChar)) { "no PC key for the output of $keyId" }
    }

    /**
     * The chord key of a function key, without Shift (FR-TAP-16 ②); null for the keys that are not chord targets:
     * the mode key and the modifiers themselves (FR-TAP-16 ①).
     */
    fun ofFunction(f: FunctionKey): ChordKey? = when (f) {
        FunctionKey.SPACE -> ChordKey.SPACE
        FunctionKey.ENTER -> ChordKey.ENTER
        FunctionKey.TAB -> ChordKey.TAB
        FunctionKey.ARROW_LEFT -> ChordKey.DPAD_LEFT
        FunctionKey.ARROW_UP -> ChordKey.DPAD_UP
        FunctionKey.ARROW_DOWN -> ChordKey.DPAD_DOWN
        FunctionKey.ARROW_RIGHT -> ChordKey.DPAD_RIGHT
        FunctionKey.ESC -> ChordKey.ESCAPE
        FunctionKey.DELETE -> ChordKey.FORWARD_DEL
        FunctionKey.BACKSPACE -> ChordKey.DEL
        FunctionKey.MODE, FunctionKey.CTRL, FunctionKey.ALT -> null
    }
}
