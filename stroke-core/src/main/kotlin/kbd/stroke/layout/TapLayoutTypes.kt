package kbd.stroke.layout

import kbd.stroke.geom.RectDp

/*
 * Tap keyboard layout types (LLD-05A §3.1). x and w are dp on a 360 dp wide window;
 * the app scales x and w to the real width and keeps the row height (FR-CMN-12).
 */

enum class TapKeyKind { QWERTY_LETTER, DUBEOLSIK_LETTER, NUMBER_ROW, SYMBOL, FUNCTION }

/**
 * Symbol assignment of a key (LLD-05A §3.3). An "impossible" direction is simply null: it
 * leaves through an outer edge, which never records a crossing (LLD-03 §3.3). Letter keys
 * keep only [left] and [down]; their tap and up output comes from the key kind (TapKeyResolver).
 */
data class KeyAssign(
    val tap: String,
    val up: String? = null,
    val left: String? = null,
    val right: String? = null,
    val down: String? = null,
)

data class TapKeyDef(
    val id: String,
    val row: Int,
    val x: Float,
    val w: Float,
    val kind: TapKeyKind,
    val label: String,
    val assign: KeyAssign? = null,
    /** Dubeolsik letter keys only: Hangul compatibility jamo (D-25). */
    val jamo: Char? = null,
    /** Dubeolsik vowel keys ㅐ ㅔ only: the ↑ jamo (ㅒ ㅖ). The consonant keys lost theirs to the number row in FSS#124. */
    val shiftJamo: Char? = null,
    val function: FunctionKey? = null,
    /**
     * Dubeolsik keys only: the jamo each direction types (HLD 5A, LLD-05A §3.3) — on the vowel keys the paired and
     * compound vowels of [TapLayouts] (FSS#104, #110, e.g. ㅗ ↑ ㅛ, ㅗ ↘ ㅙ; a diagonal is a [kbd.stroke.geom.Diag]), on
     * the number keys 1..5 the tense consonant ↓ (FSS#124, 1 ↓ ㅃ). The consonant keys have none since FSS#124.
     */
    val dirJamo: Map<kbd.stroke.geom.Way, Char> = emptyMap(),
    /**
     * Dubeolsik consonant keys only (FSS#122, #124, SRS FR-TAP-02h): the vowel a stroke in each up/down/left/right
     * direction types after the key's jamo (ㄱ ← = 거, ㄱ ↑ = 고), all four on every consonant key. From
     * [TapLayouts.STROKE_VOWEL]; never a diagonal, so it does not make a key [eightWay].
     */
    val dirVowel: Map<kbd.stroke.geom.Dir, Char> = emptyMap(),
    /** Rows the key takes from [row] down (FSS#105): 2 for ↵, 1 for every other key. */
    val rowSpan: Int = 1,
) {
    init {
        require(rowSpan >= 1) { "$id rowSpan $rowSpan" }
    }

    /**
     * Whether strokes on this key are judged in eight directions (FSS#109, SRS FR-TAP-02c ②, 02e): it has a diagonal
     * assignment. Decided by the data only — no key is named in code — so a diagonal added to the tables needs no
     * code change.
     */
    val eightWay: Boolean get() = dirJamo.keys.any { it is kbd.stroke.geom.Diag }

    /** Whether the key takes row [r]: `row ≤ r < row + rowSpan`. */
    fun inRow(r: Int): Boolean = r >= row && r < row + rowSpan

    /**
     * The key rectangle, one formula for every key (LLD-05A §3.1, HLD 5A): (x, row × h, x + w, (row + rowSpan) × h),
     * x and w times [unit] (dp per layout unit).
     */
    fun rect(unit: Float, rowHeight: Float): RectDp =
        RectDp(x * unit, row * rowHeight, (x + w) * unit, (row + rowSpan) * rowHeight)
}

/** Since FSS#105 every row is filled with keys: there are no indent blanks (LLD-05A §3.1, D-73). */
data class TapLayout(val name: String, val rows: Int, val keys: List<TapKeyDef>)
