package kbd.stroke.layout

import kbd.stroke.geom.Diag
import kbd.stroke.geom.Dir
import kbd.stroke.geom.Way

/**
 * QWERTY and Dubeolsik tap layouts: six rows filling the window, 360 dp of keys per row, no
 * blanks, ↵ two rows high (LLD-05A §3.2, SRS 3.3, FSS#105). Both layouts share key positions, key ids and symbol
 * assignments (the ㅣ key is the l key, §3.3); numeric-like fields use [QWERTY] (§3.5).
 */
object TapLayouts {

    /** One of ten units of a row: letter, number and symbol keys, and since FSS#98 the edit row keys too (LLD-05A §3.2). */
    private const val KEY_W = 36f

    private val LETTER_ROWS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val DUBEOLSIK_ROWS = listOf("ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", "ㅋㅌㅊㅍㅠㅜㅡ")
    private val SHIFT_JAMO = mapOf('ㅂ' to 'ㅃ', 'ㅈ' to 'ㅉ', 'ㄷ' to 'ㄸ', 'ㄱ' to 'ㄲ', 'ㅅ' to 'ㅆ', 'ㅐ' to 'ㅒ', 'ㅔ' to 'ㅖ')

    /**
     * Compound finals on the Dubeolsik consonant keys (owner, issue #242): all 11 in one stroke each, on the key of the
     * first consonant only. QWERTY has none. The direction is one rule (owner, FSS#109, SRS FR-TAP-02c ①): (sign of
     * the second consonant key's column − the first's, sign of its row − the first's), column = x ÷ 36, row = row
     * number; this table is its result, and TL-11 computes the rule from these coordinates again. ㄹ points to seven
     * keys around it, so it has diagonals and is judged in eight directions (SRS FR-TAP-02e). A direction without a
     * jamo types the plain jamo, like the tap (FR-TAP-02c ④: ㄹ ↘, ㄱ ↓, ㅂ ↓ …). FSS#97 and FSS#107 had other
     * directions, and ㅅ ㅌ ㅍ keys of their own for ㄽ ㄾ ㄿ.
     */
    internal val COMPOUND_JAMO: Map<Char, Map<Way, Char>> = mapOf(
        'ㄱ' to mapOf(Dir.RIGHT to 'ㄳ'),                                      // ㅅ (t) is right of ㄱ (r)
        'ㄴ' to mapOf(Dir.UP to 'ㄵ', Dir.RIGHT to 'ㄶ'),                      // ㅈ (w) above, ㅎ (g) right
        'ㄹ' to mapOf(
            Diag.UP_LEFT to 'ㄼ', Dir.UP to 'ㄺ', Diag.UP_RIGHT to 'ㄽ',       // ㅂ (q), ㄱ (r), ㅅ (t)
            Dir.LEFT to 'ㄻ', Dir.RIGHT to 'ㅀ',                                // ㅁ (a), ㅎ (g)
            Diag.DOWN_LEFT to 'ㄾ', Dir.DOWN to 'ㄿ',                           // ㅌ (x), ㅍ (v); ↘ has none
        ),
        'ㅂ' to mapOf(Dir.RIGHT to 'ㅄ'),                                      // ㅅ (t) is right of ㅂ (q)
    )

    /**
     * Compound vowels on the Dubeolsik ㅗ ㅜ ㅡ keys (owner, FSS#110, SRS FR-TAP-02g). The direction is one rule, not the
     * key positions of the compound finals: a vowel goes the way its added strokes go — ㅏ → (1, 0), ㅓ ← (−1, 0), ㅣ ↓
     * (0, 1), summed and taken by sign as the 3 × 3 direction, the vertical flipped where that is the way of the key
     * vowel's own short stroke (ㅗ ↑, ㅜ ↓: ㅜ + ㅣ = ↓ is ㅜ's own, so ㅟ is ↑). This table is its result, and TL-12
     * computes the rule again. ㅙ and ㅞ are diagonals, so ㅗ and ㅜ are judged in eight directions by this data alone
     * (FR-TAP-02e); ㅡ stays 4-way. A direction without a jamo types the plain vowel, like the tap (ㅗ ↑, ㅜ ↓ and every
     * way of ㅓ ㅏ since FSS#141, which took out the paired vowels of FSS#104). Kept apart from [COMPOUND_JAMO].
     */
    internal val COMPOUND_VOWEL: Map<Char, Map<Way, Char>> = mapOf(
        'ㅗ' to mapOf(Dir.RIGHT to 'ㅘ', Dir.DOWN to 'ㅚ', Diag.DOWN_RIGHT to 'ㅙ'),   // + ㅏ, + ㅣ, + ㅏ ㅣ
        'ㅜ' to mapOf(Dir.LEFT to 'ㅝ', Dir.UP to 'ㅟ', Diag.DOWN_LEFT to 'ㅞ'),      // + ㅓ, + ㅣ (↓ is ㅜ's own stroke), + ㅓ ㅣ
        'ㅡ' to mapOf(Dir.DOWN to 'ㅢ'),                                               // + ㅣ
    )

    /**
     * The direction jamo of [jamo]'s key: its compound finals and compound vowels, never two in one direction.
     */
    private fun dirJamo(jamo: Char): Map<Way, Char> {
        val tables = listOf(COMPOUND_JAMO, COMPOUND_VOWEL).map { it[jamo].orEmpty() }
        val ways = tables.flatMap { it.keys }
        require(ways.size == ways.toSet().size) { "$jamo: two direction jamo in one direction" }
        return tables.fold(emptyMap()) { all, t -> all + t }
    }

    /** Letter key symbols by key position (LLD-05A §3.3); i and o carry brackets left/right (#198). */
    private class LetterSymbols(val left: String? = null, val right: String? = null, val down: String? = null)

    private val LETTER_SYMBOLS = mapOf(
        'i' to LetterSymbols(left = "[", right = "]"),
        'o' to LetterSymbols(left = "{", right = "}"),
        'p' to LetterSymbols(left = "\\", down = "|"),
        'l' to LetterSymbols(left = ";", down = ":"),
    )

    private fun LetterSymbols.assign(tap: String) = KeyAssign(tap, left = left, right = right, down = down)

    /** Number row up symbols for 1..9 then 0 (LLD-05A §3.3). */
    private const val NUMBER_DIGITS = "1234567890"
    private const val NUMBER_UPS = "!@#$%^&*()"

    val QWERTY: TapLayout = build("QWERTY", dubeolsik = false)
    val DUBEOLSIK: TapLayout = build("DUBEOLSIK", dubeolsik = true)

    /**
     * The layout drawn for [layout] (SRS FR-TAP-15 ⑥, FSS#117): while Ctrl or Alt is on ([chordOn]) Dubeolsik is drawn
     * as QWERTY — labels, edge hints and screen reader names — since its keys then send the QWERTY key of their place
     * (FR-TAP-16 ③). Only the drawing: touch, keys sent and composing stay [layout]'s. The two share key order, ids and
     * places (TL-13), so key i of the result is drawn where key i of [layout] is.
     */
    fun shown(layout: TapLayout, chordOn: Boolean): TapLayout = if (chordOn && layout === DUBEOLSIK) QWERTY else layout

    private fun build(name: String, dubeolsik: Boolean): TapLayout {
        val keys = mutableListOf<TapKeyDef>()

        var x = 0f
        fun fn(row: Int, id: String, w: Float, label: String, f: FunctionKey, rowSpan: Int = 1) {
            keys += TapKeyDef(id, row, x, w, TapKeyKind.FUNCTION, label, function = f, rowSpan = rowSpan)
            x += w
        }
        fun sym(row: Int, id: String, w: Float, a: KeyAssign) {
            keys += TapKeyDef(id, row, x, w, TapKeyKind.SYMBOL, a.tap, assign = a)
            x += w
        }
        // Row 0, edit row: ten keys as wide as a letter key, Ctrl and Alt right of Esc (FSS#98), ⌫ at the right end (#198).
        fn(0, "esc", KEY_W, "Esc", FunctionKey.ESC)
        fn(0, "ctrl", KEY_W, "Ctrl", FunctionKey.CTRL)
        fn(0, "alt", KEY_W, "Alt", FunctionKey.ALT)
        fn(0, "tab", KEY_W, "⇥", FunctionKey.TAB)
        fn(0, "left", KEY_W, "←", FunctionKey.ARROW_LEFT)
        fn(0, "up", KEY_W, "↑", FunctionKey.ARROW_UP)
        fn(0, "down", KEY_W, "↓", FunctionKey.ARROW_DOWN)
        fn(0, "right", KEY_W, "→", FunctionKey.ARROW_RIGHT)
        fn(0, "delete", KEY_W, "Del", FunctionKey.DELETE)
        fn(0, "backspace", KEY_W, "⌫", FunctionKey.BACKSPACE)

        // Row 1, number row.
        x = 0f
        NUMBER_DIGITS.forEachIndexed { i, d ->
            val a = when (d) {
                '1' -> KeyAssign("1", up = "!", right = "`", down = "~")
                else -> KeyAssign(d.toString(), up = NUMBER_UPS[i].toString())
            }
            keys += TapKeyDef(d.toString(), 1, x, KEY_W, TapKeyKind.NUMBER_ROW, d.toString(), assign = a)
            x += KEY_W
        }

        // Rows 2..4, letters from x 0 (FSS#105: no indent blanks); ↵ two rows high at the right of rows 3 and 4,
        // listed after l; row 4 ends with the `=`/`-` key and `/` left of ↵.
        LETTER_ROWS.forEachIndexed { r, ids ->
            val row = r + 2
            x = 0f
            ids.forEachIndexed { i, id ->
                val symbols = LETTER_SYMBOLS[id]
                keys += if (dubeolsik) {
                    val jamo = DUBEOLSIK_ROWS[r][i]
                    TapKeyDef(
                        id.toString(), row, x, KEY_W, TapKeyKind.DUBEOLSIK_LETTER, jamo.toString(),
                        assign = symbols?.assign(jamo.toString()),
                        jamo = jamo, shiftJamo = SHIFT_JAMO[jamo], dirJamo = dirJamo(jamo),
                    )
                } else {
                    TapKeyDef(
                        id.toString(), row, x, KEY_W, TapKeyKind.QWERTY_LETTER, id.toString(),
                        assign = symbols?.assign(id.toString()),
                    )
                }
                x += KEY_W
            }
            if (row == 3) {
                // ↵ at x 324 over rows 3 and 4 (FSS#105): one key, so one press, one drawing, one accessibility node.
                fn(row, "enter", KEY_W, "↵", FunctionKey.ENTER, rowSpan = 2)
            }
            if (row == 4) {
                // The `=`/`-` key after m (#198; FSS#62 swapped tap and left), then `/` between it and ↵ (FSS#105).
                sym(row, "minus", KEY_W, KeyAssign("=", up = "_", left = "-", down = "+"))
                sym(row, "slash", KEY_W, KeyAssign("/", up = "?"))
            }
        }

        // Row 5, function row; the mode key is bottom-left in every mode (SRS 3.3).
        x = 0f
        fn(5, "mode", 72f, "한/영", FunctionKey.MODE)
        fn(5, "space", 216f, "", FunctionKey.SPACE)   // 216 since `/` moved up (FSS#105, SRS TBD-36 ①)
        sym(5, "comma", KEY_W, KeyAssign(",", up = "<", left = "'", right = "\""))
        sym(5, "period", KEY_W, KeyAssign(".", up = ">"))   // right end (FSS#105)

        return TapLayout(name, rows = 6, keys = keys)
    }
}
