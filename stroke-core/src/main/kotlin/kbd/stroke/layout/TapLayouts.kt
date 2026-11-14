package kbd.stroke.layout

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
     * Compound finals on the Dubeolsik consonant keys (owner, issue #242): all 11 in one stroke each. QWERTY has none.
     * FSS#97 (owner) moved six of them: ㄳ ㄶ ㅄ ㄽ to ↓ and ㄵ ㄿ to ↑, FSS#107 (owner) ㄾ to ↑; the directions they
     * left type the plain jamo.
     */
    private val COMPOUND_JAMO: Map<Char, Map<kbd.stroke.geom.Dir, Char>> = run {
        val up = kbd.stroke.geom.Dir.UP
        val left = kbd.stroke.geom.Dir.LEFT
        val right = kbd.stroke.geom.Dir.RIGHT
        val down = kbd.stroke.geom.Dir.DOWN
        mapOf(
            'ㄱ' to mapOf(down to 'ㄳ'),
            'ㄴ' to mapOf(up to 'ㄵ', down to 'ㄶ'),
            'ㄹ' to mapOf(up to 'ㅀ', left to 'ㄺ', right to 'ㄻ', down to 'ㄼ'),
            'ㅂ' to mapOf(down to 'ㅄ'),   // ↓ (FSS#97); it was ← (#242), then → (#246)
            'ㅅ' to mapOf(down to 'ㄽ'),
            'ㅌ' to mapOf(up to 'ㄾ'),     // ↑ (FSS#107); it was ← (#242)
            'ㅍ' to mapOf(up to 'ㄿ'),
        )
    }

    /**
     * Paired vowels on four Dubeolsik vowel keys (owner, FSS#104): a stroke towards the pair's direction types it. The
     * ㅛ ㅕ ㅑ ㅠ keys stay. Kept apart from [COMPOUND_JAMO]; both go into [TapKeyDef.dirJamo].
     */
    private val PAIR_VOWEL: Map<Char, Map<kbd.stroke.geom.Dir, Char>> = mapOf(
        'ㅗ' to mapOf(kbd.stroke.geom.Dir.UP to 'ㅛ'),
        'ㅓ' to mapOf(kbd.stroke.geom.Dir.LEFT to 'ㅕ'),
        'ㅏ' to mapOf(kbd.stroke.geom.Dir.RIGHT to 'ㅑ'),
        'ㅜ' to mapOf(kbd.stroke.geom.Dir.DOWN to 'ㅠ'),
    )

    /** The direction jamo of [jamo]'s key: its compound finals and paired vowels, never two in one direction. */
    private fun dirJamo(jamo: Char): Map<kbd.stroke.geom.Dir, Char> {
        val compound = COMPOUND_JAMO[jamo].orEmpty()
        val pair = PAIR_VOWEL[jamo].orEmpty()
        require(compound.keys.none { it in pair }) { "$jamo: a compound final and a paired vowel in one direction" }
        return compound + pair
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
