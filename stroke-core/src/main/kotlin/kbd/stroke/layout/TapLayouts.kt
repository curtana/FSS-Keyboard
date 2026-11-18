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
    /** The vowel keys' ↑ (ㅐ ↑ ㅒ, ㅔ ↑ ㅖ). The consonant keys have none since FSS#124: their ↑ is a consonant + ㅗ. */
    private val SHIFT_JAMO = mapOf('ㅐ' to 'ㅒ', 'ㅔ' to 'ㅖ')

    /**
     * The tense consonants on the Dubeolsik number row (owner, FSS#124, SRS FR-TAP-02i): a stroke down out of 1..5
     * types the tense form of the consonant key right below (1 ↓ ㅃ over ㅂ … 5 ↓ ㅆ over ㅅ), one jamo into the
     * composer. TL-14 checks each against the key below. Until FSS#124 they were ↑ on those consonant keys.
     */
    internal val TENSE_JAMO: Map<Char, Char> = mapOf('1' to 'ㅃ', '2' to 'ㅉ', '3' to 'ㄸ', '4' to 'ㄲ', '5' to 'ㅆ')

    /**
     * Paired vowels on four Dubeolsik vowel keys (owner, FSS#104): a stroke towards the pair's direction types it. The
     * ㅛ ㅕ ㅑ ㅠ keys stay. Kept apart from [COMPOUND_VOWEL]; both go into [TapKeyDef.dirJamo].
     */
    internal val PAIR_VOWEL: Map<Char, Map<Way, Char>> = mapOf(
        'ㅗ' to mapOf(Dir.UP to 'ㅛ'),
        'ㅓ' to mapOf(Dir.LEFT to 'ㅕ'),
        'ㅏ' to mapOf(Dir.RIGHT to 'ㅑ'),
        'ㅜ' to mapOf(Dir.DOWN to 'ㅠ'),
    )

    /**
     * Compound vowels on the Dubeolsik ㅗ ㅜ ㅡ keys (owner, FSS#110, SRS FR-TAP-02g). The direction is one rule, not the
     * key positions of the compound finals: a vowel goes the way its added strokes go — ㅏ → (1, 0), ㅓ ← (−1, 0), ㅣ ↓
     * (0, 1), summed and taken by sign as the 3 × 3 direction, the vertical flipped where the key's paired vowel is
     * (ㅜ + ㅣ = ↓ is ㅠ's, so ㅟ is ↑). This table is its result, and TL-12 computes the rule again. ㅙ and ㅞ are
     * diagonals, so ㅗ and ㅜ are judged in eight directions by this data alone (FR-TAP-02e) — with ㅓ ㅏ
     * ([DIAG_VOWEL], FSS#126) the only 8-way keys since FSS#124 took the compound finals off ㄹ; ㅡ stays 4-way. A direction without a jamo types the plain vowel,
     * like the tap. Kept apart from [PAIR_VOWEL].
     */
    internal val COMPOUND_VOWEL: Map<Char, Map<Way, Char>> = mapOf(
        'ㅗ' to mapOf(Dir.RIGHT to 'ㅘ', Dir.DOWN to 'ㅚ', Diag.DOWN_RIGHT to 'ㅙ'),   // + ㅏ, + ㅣ, + ㅏ ㅣ
        'ㅜ' to mapOf(Dir.LEFT to 'ㅝ', Dir.UP to 'ㅟ', Diag.DOWN_LEFT to 'ㅞ'),      // + ㅓ, + ㅣ (↓ is ㅠ), + ㅓ ㅣ
        'ㅡ' to mapOf(Dir.DOWN to 'ㅢ'),                                               // + ㅣ
    )

    /**
     * Diagonal vowels on the Dubeolsik ㅓ ㅏ keys (owner, FSS#126, SRS FR-TAP-02j): ㅓ ↙ ㅔ ↖ ㅖ, ㅏ ↘ ㅐ ↗ ㅒ. ㅔ = ㅓ + ㅣ and
     * ㅐ = ㅏ + ㅣ go the way of the stroke rule above (← or → plus ㅣ ↓); ㅖ and ㅒ are their upper pairs. Each types
     * the same single jamo as the ㅔ ㅐ keys (↑ ㅖ ㅒ), so composing, merging and ⌫ are the keys'. The diagonals make ㅓ
     * and ㅏ 8-way keys by this data alone (FR-TAP-02e); their other diagonals are the tap. Kept apart from
     * [PAIR_VOWEL] and [COMPOUND_VOWEL].
     */
    internal val DIAG_VOWEL: Map<Char, Map<Way, Char>> = mapOf(
        'ㅓ' to mapOf(Diag.DOWN_LEFT to 'ㅔ', Diag.UP_LEFT to 'ㅖ'),
        'ㅏ' to mapOf(Diag.DOWN_RIGHT to 'ㅐ', Diag.UP_RIGHT to 'ㅒ'),
    )

    /**
     * The vowel of each up/down/left/right direction on the Dubeolsik consonant keys (owner, FSS#122 and FSS#124, SRS
     * FR-TAP-02h): a stroke out of any of the four sides types the consonant and then this vowel, like tapping the two
     * keys (ㄱ ← = 거, ㅁ ↑ = 모, ㄱ ↑ = 고). The direction is the way the vowel's stroke goes, as for the paired vowels
     * (FSS#104, #110). Since FSS#124 every consonant key has all four: no tense consonant, no compound final.
     */
    val STROKE_VOWEL: Map<Dir, Char> = mapOf(Dir.LEFT to 'ㅓ', Dir.RIGHT to 'ㅏ', Dir.UP to 'ㅗ', Dir.DOWN to 'ㅜ')

    /** The [STROKE_VOWEL] of [jamo]'s key: all four on consonant keys (compatibility jamo ㄱ..ㅎ), none on vowel keys. */
    private fun dirVowel(jamo: Char): Map<Dir, Char> = if (jamo in 'ㄱ'..'ㅎ') STROKE_VOWEL else emptyMap()

    /** The direction jamo of [jamo]'s key: its paired, compound and diagonal vowels, never two in one direction. */
    private fun dirJamo(jamo: Char): Map<Way, Char> {
        val tables = listOf(PAIR_VOWEL, COMPOUND_VOWEL, DIAG_VOWEL).map { it[jamo].orEmpty() }
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
                '1' -> KeyAssign("1", up = "!", left = "~", right = "`")   // ~ ← since FSS#124 (↓ until then)
                else -> KeyAssign(d.toString(), up = NUMBER_UPS[i].toString())
            }
            // Dubeolsik 1..5 ↓: the tense consonant of the key below (FSS#124); QWERTY's number row ↓ stays empty.
            val tense = TENSE_JAMO[d]?.takeIf { dubeolsik }?.let { mapOf<Way, Char>(Dir.DOWN to it) }.orEmpty()
            keys += TapKeyDef(d.toString(), 1, x, KEY_W, TapKeyKind.NUMBER_ROW, d.toString(), assign = a, dirJamo = tense)
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
                    require(symbols == null || jamo !in 'ㄱ'..'ㅎ') { "$id: symbols on a consonant key" }   // FR-TAP-02h
                    TapKeyDef(
                        id.toString(), row, x, KEY_W, TapKeyKind.DUBEOLSIK_LETTER, jamo.toString(),
                        assign = symbols?.assign(jamo.toString()),
                        jamo = jamo, shiftJamo = SHIFT_JAMO[jamo], dirJamo = dirJamo(jamo), dirVowel = dirVowel(jamo),
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
