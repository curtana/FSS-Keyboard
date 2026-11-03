package kbd.gesture.layout

import kbd.gesture.classify.SwipeKeyKind
import kbd.hangul.jamo.Jamo
import kbd.hangul.stroke.ConsonantBox
import kbd.stroke.layout.FunctionKey

/**
 * One cell of the swipe layout (LLD-04 §8.1). [col]/[row] are grid indices of the left cell;
 * [colSpan] counts grid columns. Exactly one of [consonant]/[digit]/[function] is set for
 * CONSONANT/NUMBER/FUNCTION keys; SYMBOL, CHEONJIIN and EMPTY keys set none.
 */
data class SwipeKeyDef(val id: String, val col: Int, val row: Int, val colSpan: Int,
                       val kind: SwipeKeyKind, val label: String,
                       val consonant: Jamo? = null, val digit: Char? = null, val function: FunctionKey? = null)

/**
 * Swipe keyboard grid, 7 columns × 6 rows (LLD-04 §8.1, SWIPE 2.1, FR-SWP-50·51). Row 1 holds the
 * double-consonant keys ㅃ ㅉ ㄸ ㄲ ㅆ above their pairs (issue #168 layout A). Issue #173 moved both
 * end columns up one cell: left 1 8 0 ㆍ, right 7 9 ⌫ ↵. The cheonjiin key is row 3 col 0 (FR-SWP-60).
 * Issue #205 removed the ⚙ key (row 4 col 0 above the mode key, FR-SWP-66): the settings open with an up
 * stroke on the mode key. Issue #208 put the symbol3 key in that cell and the symbol2 key in the empty cell
 * right of the symbol key (row 4 col 6), FR-SWP-43. The only empty cell left (row 5 cols 5–6) takes touches
 * but gives no input. Key rectangles are built by the view.
 */
object SwipeLayout {
    const val COLS = 7
    const val ROWS = 6

    /** Id of the cheonjiin key (FR-SWP-60). */
    const val CHEONJIIN_ID = "cheonjiin"

    /*
     * The center syllable's place C (SWIPE 9.5 "자리와 크기", design defaults; LLD-04 §8.1): the top of the
     * consonant key area half a row down (owner decision FSS#37), centred across the keyboard (the middle ㄸ
     * column, owner decision #213), in view dp. The view draws the composing syllable in it and the vowel
     * assembler judges against it (HLD 6: one constant for both).
     */
    private const val C_TOP_ROW = 1             // the double-consonant row
    private const val C_TOP_INSET_DP = 4f       // C top = that row's top + 4 dp + half a row
    private const val C_DROP_ROWS = 0.5f        // the half row (24 dp at 48 dp rows; follows FR-SET-06, FSS#37)
    private const val C_ROWS = 2f               // height = two key rows (96 dp at 48 dp rows; follows FR-SET-06)
    private const val C_ASPECT = 0.85f          // width = height × 0.85

    /** C for a swipe view [widthDp] wide with rows [rowHeightDp] high: ≈ (139, 76)–(221, 172) on 360 × 48 dp. */
    fun consonantBox(widthDp: Float, rowHeightDp: Float): ConsonantBox {
        val top = (C_TOP_ROW + C_DROP_ROWS) * rowHeightDp + C_TOP_INSET_DP
        val h = C_ROWS * rowHeightDp
        val w = C_ASPECT * h
        val left = widthDp / 2 - w / 2   // horizontal centre of C = horizontal centre of the keyboard (#213)
        return ConsonantBox(left, top, left + w, top + h)
    }

    private fun num(d: Char, col: Int, row: Int) =
        SwipeKeyDef(d.toString(), col, row, 1, SwipeKeyKind.NUMBER, d.toString(), digit = d)

    private fun con(j: Jamo, col: Int, row: Int) =
        SwipeKeyDef(j.compat.toString(), col, row, 1, SwipeKeyKind.CONSONANT, j.compat.toString(), consonant = j)

    private fun fn(id: String, f: FunctionKey, label: String, col: Int, row: Int, colSpan: Int = 1) =
        SwipeKeyDef(id, col, row, colSpan, SwipeKeyKind.FUNCTION, label, function = f)

    /** Symbol keys are labelled with their tap symbol; the view draws the exit symbols in the corners (FR-SWP-43a). */
    private fun sym(id: String, t: SymbolKeyTable, col: Int, row: Int) =
        SwipeKeyDef(id, col, row, 1, SwipeKeyKind.SYMBOL, t.tap)

    private fun empty(id: String, col: Int, row: Int, colSpan: Int) =
        SwipeKeyDef(id, col, row, colSpan, SwipeKeyKind.EMPTY, "")

    val KEYS: List<SwipeKeyDef> = listOf(
        // row 0
        num('1', 0, 0), num('2', 1, 0), num('3', 2, 0), num('4', 3, 0), num('5', 4, 0), num('6', 5, 0), num('7', 6, 0),
        // row 1: the double consonants, each above its pair (layout A, issue #168)
        num('8', 0, 1), con(Jamo.BB, 1, 1), con(Jamo.JJ, 2, 1), con(Jamo.DD, 3, 1), con(Jamo.GG, 4, 1),
        con(Jamo.SS, 5, 1), num('9', 6, 1),
        // row 2
        num('0', 0, 2), con(Jamo.B, 1, 2), con(Jamo.J, 2, 2), con(Jamo.D, 3, 2), con(Jamo.G, 4, 2), con(Jamo.S, 5, 2),
        fn("backspace", FunctionKey.BACKSPACE, "⌫", 6, 2),
        // row 3
        SwipeKeyDef(CHEONJIIN_ID, 0, 3, 1, SwipeKeyKind.CHEONJIIN, "ㆍ ㅡ ㅣ"), con(Jamo.M, 1, 3), con(Jamo.N, 2, 3),
        con(Jamo.NG, 3, 3), con(Jamo.R, 4, 3), con(Jamo.H, 5, 3),
        fn("enter", FunctionKey.ENTER, "↵", 6, 3),
        // row 4
        sym(SymbolKeyTable.SYMBOL3_ID, SymbolKeyTable.SYMBOL3, 0, 4), con(Jamo.K, 1, 4), con(Jamo.T, 2, 4), con(Jamo.C, 3, 4),
        con(Jamo.P, 4, 4), sym(SymbolKeyTable.SYMBOL_ID, SymbolKeyTable.SYMBOL, 5, 4),
        sym(SymbolKeyTable.SYMBOL2_ID, SymbolKeyTable.SYMBOL2, 6, 4),   // issue #208, was an empty cell
        // row 5
        fn("mode", FunctionKey.MODE, "한/영", 0, 5, colSpan = 2),
        fn("space", FunctionKey.SPACE, "스페이스", 2, 5, colSpan = 3),
        empty("empty_r5c5", 5, 5, 2),
    )
}
