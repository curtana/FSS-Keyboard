package kbd.stroke.layout

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
    val shiftJamo: Char? = null,
    val function: FunctionKey? = null,
    /** Dubeolsik letter keys only: the compound final each direction types (issue #242), e.g. ㄹ ← ㄺ. */
    val compoundJamo: Map<kbd.stroke.geom.Dir, Char> = emptyMap(),
)

/** An indent blank; a touch inside it goes to [nearestKeyId] in the same row (SRS 3.3). */
data class BlankDef(val row: Int, val x: Float, val w: Float, val nearestKeyId: String)

data class TapLayout(val name: String, val rows: Int, val keys: List<TapKeyDef>, val blanks: List<BlankDef>)
