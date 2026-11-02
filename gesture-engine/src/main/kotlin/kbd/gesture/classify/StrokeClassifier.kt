package kbd.gesture.classify

import kbd.gesture.layout.NumberKeyTable
import kbd.gesture.layout.SwipeKeyDef
import kbd.gesture.layout.SymbolKeyTable
import kbd.gesture.progress.ProgressState
import kbd.hangul.stroke.VowelStroke
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.StrokePoint
import kbd.stroke.threshold.Thresholds

/**
 * Classifies one stroke on a consonant, number or symbol key of the swipe layout (LLD-04 §4.6).
 * [geom] is the key rectangle built by the view (LLD-04 §9.2); the LLD signature has only the
 * layout cell, which carries no rectangle (LLD gap, issue #13).
 */
class StrokeClassifier(
    val key: SwipeKeyDef, val th: Thresholds, val geom: KeyGeom,
    /** What a vowel stroke would show if released now (the mapper's assembler, SWIPE 9.2); null = ignored. */
    internal val preview: (VowelStroke) -> String? = { null },
) {

    private val s = StrokeState(geom, th)
    private var downTime = 0L

    init {
        require(key.kind == SwipeKeyKind.CONSONANT || key.kind == SwipeKeyKind.NUMBER || key.kind == SwipeKeyKind.SYMBOL ||
            key.kind == SwipeKeyKind.CHEONJIIN) {
            "no classifier for ${key.kind} keys"
        }
    }

    fun begin(p: StrokePoint) {
        s.begin(p)
        downTime = p.t
    }

    fun update(p: StrokePoint): ProgressState {
        s.add(p)
        return ProgressState.of(this)
    }

    fun finish(p: StrokePoint): StrokeResult {
        update(p)
        return StrokeResult(
            keyId = key.id, kind = key.kind, outcome = outcomeOf(s),
            downTime = downTime, firstCrossTime = s.exitPt?.t, upTime = p.t,
        )
    }

    /** Read access for tests and probes. */
    fun state(): StrokeState = s

    /** Judgement of [state] by this key's kind (LLD-04 §4.3–4.5); [state] may be a probe copy. */
    fun outcomeOf(state: StrokeState): StrokeOutcome = when (key.kind) {
        SwipeKeyKind.CONSONANT -> classifyConsonant(state, th)
        SwipeKeyKind.NUMBER -> classifyNumber(state, checkNotNull(key.digit), NumberKeyTable)
        SwipeKeyKind.CHEONJIIN -> classifyCheonjiin(state)
        else -> classifySymbol(state, SymbolKeyTable.of(key.id))
    }

    /**
     * Display text of [o] (LLD-04 §5.2): the consonant for a tap; for a vowel stroke what the assembly
     * would show with it (the vowel or ㆍ / ‥, SWIPE 9.2), nothing for a reversed or ignored stroke; else
     * the text. How long the key is pressed does not matter (issue #142).
     */
    fun textOf(o: StrokeOutcome): String = when (o) {
        StrokeOutcome.Tap -> checkNotNull(key.consonant).compat.toString()
        is StrokeOutcome.Stroke -> preview(o.s) ?: ""
        StrokeOutcome.Reverse -> ""
        is StrokeOutcome.Element -> elementText(o.e)
        is StrokeOutcome.Text -> o.text
    }
}
