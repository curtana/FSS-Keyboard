package kbd.stroke.layout

/**
 * QWERTY and Dubeolsik tap layouts: six rows filling the window, 360 dp per row including
 * blanks (LLD-05A §3.2, SRS 3.3). Both layouts share key positions, key ids and symbol
 * assignments (the ㅣ key is the l key, §3.3); numeric-like fields use [QWERTY] (§3.5).
 */
object TapLayouts {

    /** Edit row key width: 8 × 45 = 360; below NFR-06's 48 dp by owner decision (#198, LLD-05A §3.2). */
    private const val EDIT_W = 45f
    private const val KEY_W = 36f
    private const val INDENT_W = 18f

    private val LETTER_ROWS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val DUBEOLSIK_ROWS = listOf("ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", "ㅋㅌㅊㅍㅠㅜㅡ")
    private val SHIFT_JAMO = mapOf('ㅂ' to 'ㅃ', 'ㅈ' to 'ㅉ', 'ㄷ' to 'ㄸ', 'ㄱ' to 'ㄲ', 'ㅅ' to 'ㅆ', 'ㅐ' to 'ㅒ', 'ㅔ' to 'ㅖ')

    /** Compound finals on the Dubeolsik consonant keys (owner, issue #242): all 11 in one stroke each. QWERTY has none. */
    private val COMPOUND_JAMO: Map<Char, Map<kbd.stroke.geom.Dir, Char>> = run {
        val up = kbd.stroke.geom.Dir.UP
        val left = kbd.stroke.geom.Dir.LEFT
        val right = kbd.stroke.geom.Dir.RIGHT
        val down = kbd.stroke.geom.Dir.DOWN
        mapOf(
            'ㄱ' to mapOf(left to 'ㄳ'),
            'ㄴ' to mapOf(left to 'ㄵ', right to 'ㄶ'),
            'ㄹ' to mapOf(up to 'ㅀ', left to 'ㄺ', right to 'ㄻ', down to 'ㄼ'),
            'ㅂ' to mapOf(right to 'ㅄ'),   // → (owner, issue #246): ㅂ is the window's left-edge key, ← is hard there
            'ㅅ' to mapOf(left to 'ㄽ'),
            'ㅌ' to mapOf(left to 'ㄾ'),
            'ㅍ' to mapOf(left to 'ㄿ'),
        )
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
        val blanks = mutableListOf<BlankDef>()

        var x = 0f
        fun fn(row: Int, id: String, w: Float, label: String, f: FunctionKey) {
            keys += TapKeyDef(id, row, x, w, TapKeyKind.FUNCTION, label, function = f)
            x += w
        }
        fun sym(row: Int, id: String, w: Float, a: KeyAssign) {
            keys += TapKeyDef(id, row, x, w, TapKeyKind.SYMBOL, a.tap, assign = a)
            x += w
        }
        // Row 0, edit row: eight 45 dp keys, ⌫ at the right end (#198).
        fn(0, "esc", EDIT_W, "Esc", FunctionKey.ESC)
        fn(0, "tab", EDIT_W, "⇥", FunctionKey.TAB)
        fn(0, "left", EDIT_W, "←", FunctionKey.ARROW_LEFT)
        fn(0, "up", EDIT_W, "↑", FunctionKey.ARROW_UP)
        fn(0, "down", EDIT_W, "↓", FunctionKey.ARROW_DOWN)
        fn(0, "right", EDIT_W, "→", FunctionKey.ARROW_RIGHT)
        fn(0, "delete", EDIT_W, "Del", FunctionKey.DELETE)
        fn(0, "backspace", EDIT_W, "⌫", FunctionKey.BACKSPACE)

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

        // Rows 2..4, letters; rows 3 and 4 start with an indent blank; row 4 ends with `-` and ↵.
        LETTER_ROWS.forEachIndexed { r, ids ->
            val row = r + 2
            x = 0f
            if (row >= 3) {
                blanks += BlankDef(row, x, INDENT_W, ids.first().toString())
                x += INDENT_W
            }
            ids.forEachIndexed { i, id ->
                val symbols = LETTER_SYMBOLS[id]
                keys += if (dubeolsik) {
                    val jamo = DUBEOLSIK_ROWS[r][i]
                    TapKeyDef(
                        id.toString(), row, x, KEY_W, TapKeyKind.DUBEOLSIK_LETTER, jamo.toString(),
                        assign = symbols?.assign(jamo.toString()),
                        jamo = jamo, shiftJamo = SHIFT_JAMO[jamo], compoundJamo = COMPOUND_JAMO[jamo].orEmpty(),
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
                blanks += BlankDef(row, x, INDENT_W, ids.last().toString())
                x += INDENT_W
            }
            if (row == 4) {
                // `-` after m, then ↵ at the right end (#198).
                sym(row, "minus", KEY_W, KeyAssign("-", up = "_", left = "=", down = "+"))
                fn(row, "enter", 54f, "↵", FunctionKey.ENTER)
            }
        }

        // Row 5, function row; the mode key is bottom-left in every mode (SRS 3.3).
        x = 0f
        fn(5, "mode", 72f, "한/영", FunctionKey.MODE)
        fn(5, "space", 180f, "", FunctionKey.SPACE)
        sym(5, "comma", KEY_W, KeyAssign(",", up = "<", left = "'", right = "\""))
        sym(5, "period", KEY_W, KeyAssign(".", up = ">"))
        sym(5, "slash", KEY_W, KeyAssign("/", up = "?"))   // right end, no blank (#198)

        return TapLayout(name, rows = 6, keys = keys, blanks = blanks)
    }
}
