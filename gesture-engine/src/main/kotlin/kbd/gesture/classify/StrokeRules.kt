package kbd.gesture.classify

import kbd.gesture.layout.NumberKeyTable
import kbd.gesture.layout.SymbolKeyTable
import kbd.hangul.cheonjiin.CjElement
import kbd.hangul.stroke.StrokeAxis
import kbd.hangul.stroke.VowelStroke
import kbd.stroke.cross.countOf
import kbd.stroke.threshold.Thresholds
import kotlin.math.abs
import kotlin.math.max
import kbd.stroke.geom.Edge
import kbd.stroke.tap.ScriptTables

/*
 * Pure judgement functions (LLD-04 §4.3–4.5). The same functions run at release and for the
 * progress state while the finger is down (HLD 12).
 */

/**
 * Cheonjiin key element (LLD-04 §4.7, SWIPE 5.4, FR-SWP-61, issue #160): no exit = ㆍ, first exit
 * up or down = ㅣ, right = ㅡ. Only the first exit counts; the key's left edge is the screen edge, crossed by
 * reaching the border (FR-SWP-45, issue #237), and a left exit gives the tap's ㆍ.
 */
fun classifyCheonjiin(s: StrokeState): StrokeOutcome = StrokeOutcome.Element(cheonjiinElementOf(s.e1))

/** The cheonjiin element of a first exit [e1] (null = tap): ↑ ↓ ㅣ, → ㅡ; ← (the screen edge, issue #237) is the tap's ㆍ. */
fun cheonjiinElementOf(e1: Edge?): CjElement = when (e1) {
    null, Edge.LEFT -> CjElement.DOT
    Edge.TOP, Edge.BOTTOM -> CjElement.I
    Edge.RIGHT -> CjElement.EU
}

/** What a stroke out of each edge of the cheonjiin key gives where it differs from the tap (owner #198 edge hints). */
fun cheonjiinEdgeHints(): Map<kbd.stroke.geom.Dir, String> =
    Edge.entries.filter { cheonjiinElementOf(it) != CjElement.DOT }.associate { it.dir to elementText(cheonjiinElementOf(it)) }

/** Display text of a cheonjiin element (SWIPE 9.2: the current character is the element). */
fun elementText(e: CjElement): String = when (e) {
    CjElement.DOT -> "ㆍ"
    CjElement.I -> "ㅣ"
    CjElement.EU -> "ㅡ"
}

/**
 * Consonant key (LLD-04 §4.3, SWIPE 3.1–3.2, 3.6 steps 1–3 and 5, HLD 4.2, issue #173): only the press
 * point and the release point count, in this order —
 * ① tap or stroke: the larger displacement component is under `minCrossDp` → [StrokeOutcome.Tap];
 * ② axis: the larger component (|dx| ≥ |dy| = horizontal, a diagonal is horizontal);
 * ③ direction: → and ↓ only, ← / ↑ → [StrokeOutcome.Reverse].
 * The stroke's length is not judged (issue #179, SWIPE 0.35 3.6.1: the 0.34 step ④ "longDp = long or dot"
 * is gone); its role comes from the assembler's candidates, C and places.
 * The path in between, the key edges and the key itself are not looked at. The 0.25–0.29 edge-crossing
 * vowel rules (first exit, long, return, turn, rub, +ㅣ) are gone.
 */
fun classifyConsonant(s: StrokeState, th: Thresholds): StrokeOutcome {
    val dx = s.last.x - s.first.x
    val dy = s.last.y - s.first.y
    // ① tap or stroke
    val major = max(abs(dx), abs(dy))
    if (major < th.minCrossDp) return StrokeOutcome.Tap
    // ② axis
    val axis = if (abs(dx) >= abs(dy)) StrokeAxis.HORIZONTAL else StrokeAxis.VERTICAL
    // ③ direction
    val forward = if (axis == StrokeAxis.HORIZONTAL) dx > 0f else dy > 0f
    if (!forward) return StrokeOutcome.Reverse
    return StrokeOutcome.Stroke(VowelStroke(axis, s.first.x, s.first.y, s.last.x, s.last.y))
}

/** Perimeter number key (LLD-04 §4.4, SWIPE 7, FR-SWP-42). Length, turns and returns are not judged. */
fun classifyNumber(s: StrokeState, digit: Char, table: NumberKeyTable): StrokeOutcome {
    val e1 = s.e1 ?: return StrokeOutcome.Text(digit.toString(), false)
    val n = s.tracker.countOf(e1)
    val text = when {
        n >= 3 && e1 == Edge.TOP -> ScriptTables.SUPER[digit]
        n >= 3 && e1 == Edge.BOTTOM -> ScriptTables.SUB[digit]
        // Exit, exit-and-return and left/right rubs all equal the exit (DCR-02).
        else -> table[digit][e1.dir]
    }
    // No entry = the tap (6 ←, issue #208; the border-side directions, FR-SWP-45): unassigned directions equal the tap (SWIPE 7 "–").
    return StrokeOutcome.Text(text ?: digit.toString(), false)
}

/**
 * Symbol and symbol2 keys (LLD-04 §4.5, SWIPE 8, FR-SWP-43, issue #208): only the first exit's direction
 * counts — exit-and-return and rubs equal the exit (the 0.37 return and rub stages are gone), ↓ included.
 */
fun classifySymbol(s: StrokeState, table: SymbolKeyTable): StrokeOutcome {
    val e1 = s.e1 ?: return StrokeOutcome.Text(table.tap, false)
    // No entry = the tap: the symbol3 key's ↓, and its ← and the symbol2 key's → (the border, crossed by reaching it, FR-SWP-45).
    return StrokeOutcome.Text(table[e1.dir] ?: table.tap, false)
}
