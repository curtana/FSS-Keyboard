package kbd.gesture.input

import kbd.gesture.classify.StrokeOutcome
import kbd.gesture.classify.StrokeResult
import kbd.gesture.classify.SwipeKeyKind
import kbd.gesture.layout.SwipeLayout
import kbd.hangul.cheonjiin.CheonjiinAssembler
import kbd.hangul.cheonjiin.CjElement
import kbd.hangul.cheonjiin.CjPop
import kbd.hangul.cheonjiin.CjPush
import kbd.hangul.cheonjiin.CjSnapshot
import kbd.hangul.cheonjiin.CjState
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.hangul.stroke.ConsonantBox
import kbd.hangul.stroke.OtherRole
import kbd.hangul.stroke.StrokePop
import kbd.hangul.stroke.StrokePush
import kbd.hangul.stroke.StrokeSnapshot
import kbd.hangul.stroke.VowelStroke
import kbd.hangul.stroke.VowelStrokeAssembler
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds

/** What one finished stroke gives (LLD-04 §6.1). */
sealed interface MapResult {
    /**
     * Composer events of a consonant tap (its consonant), a vowel stroke or a cheonjiin element
     * (`Vowel` when the vowel starts, `ReplaceVowel` when it changes, none while it is not a vowel
     * yet: the app shows [SwipeInputMapper.pendingGlyph]). [errorVibe]: this input closed a vowel
     * assembly and dropped strokes that made no vowel (SWIPE 3.3 "조립 끝").
     */
    data class Jamo(val events: List<JamoEvent>, val errorVibe: Boolean = false) : MapResult

    /** A stroke or element that fits nothing (reversed, out of order or position): no input, the error vibration (FR-SWP-12, 14, 62). */
    data object Ignored : MapResult

    /** A number or symbol key: [text] to commit, with the error vibration when [errorVibe]. */
    data class Text(val text: String, val errorVibe: Boolean) : MapResult
}

/** Value copy of the mapper state for error recovery (HLD 9, LLD-09 §2): both assemblers' contents. */
data class SwipeMapperSnapshot(
    val cj: CjSnapshot = CjSnapshot(emptyList()),
    val strokes: StrokeSnapshot = StrokeSnapshot(emptyList()),
)

/**
 * Turns swipe-layout strokes into composer events (LLD-04 §6, HLD 4.3, SWIPE 3 and 5, issue #173).
 * "Consonants are taps, vowels are handwriting strokes, finals are taps again": a tap types its key's
 * consonant (a double-consonant key's is the double; two taps are two consonants, no time judgement);
 * a vowel stroke (→ / ↓ from any consonant key) goes to the [VowelStrokeAssembler], which builds the
 * vowel by stroke order and position. Finals, compound finals and dokkaebibul are the composer's.
 *
 * The cheonjiin key (SWIPE 5.4, issue #160, kept by #173) gives one element per stroke to its own
 * [CheonjiinAssembler]. The two assemblies never mix: any other input closes the open one first. A
 * vowel assembly closed with strokes that made no vowel yet drops them with the error vibration.
 */
class SwipeInputMapper(th: Thresholds) {

    /** The thresholds in use; [setKeyWidth] replaces their `posTolDp`. */
    var th: Thresholds = th
        private set

    private val cj = CheonjiinAssembler()
    private val vs = VowelStrokeAssembler(th.posTolDp)

    /**
     * The swipe keys are [keyWidthDp] wide as drawn now: t = key width × 0.4 from the next stroke on (SRS 1.3, HLD 7.2,
     * PM decision 2026-11-02, FSS#17 — tablets, landscape and a width setting follow the real key size).
     */
    fun setKeyWidth(keyWidthDp: Float) {
        val t = ThresholdDefaults.posTolFor(keyWidthDp)
        th = th.copy(posTolDp = t)
        vs.tol = t
    }

    /**
     * The drawn consonant's place C while a consonant is composing, else null (SWIPE 3.6.3, 9.5, TBD-24):
     * the caller sets it from the composer before each stroke (`SwipeLayout.consonantBox` when the center
     * consonant shows). The assembler reads it at an assembly's first stroke and keeps it until it closes.
     */
    var consonant: ConsonantBox? = null

    /** ㆍ / ‥ of a vowel not made yet (either assembly), shown after the composition (SWIPE 3.4, FR-SWP-64). */
    fun pendingGlyph(): String = vs.pendingGlyph() + cj.pendingGlyph()

    /** The bare ‥ states (SWIPE 0.36 3.4, D-64): [pendingGlyph] is shown in place of the composer's last character. */
    fun pendingReplacesVowel(): Boolean = vs.pendingReplacesVowel

    /** What vowel stroke [s] would show now, null if it would be ignored (the guide, SWIPE 9). */
    fun preview(s: VowelStroke): String? = vs.preview(s, consonant)

    /** The open vowel strokes (the trail, SWIPE 9.4). */
    val strokes: List<VowelStroke> get() = vs.strokes

    /** = `VowelStrokeAssembler.otherRole`: the lone first stroke's other role, for the center syllable's bracket (SWIPE 9.5, FSS#36). */
    fun otherRole(): OtherRole? = vs.otherRole

    // Debug text for the personal debug build's stroke log (issue #217); never judged with.

    /** The C vowel stroke [s] would be judged against now: the open assembly's, else [consonant]. */
    fun debugConsonant(): ConsonantBox? = if (vs.isEmpty) consonant else vs.consonant

    /** [s]'s role by C before [accept] (SWIPE 3.6.3). */
    fun debugRole(s: VowelStroke): String = vs.roleText(s, consonant)

    /** Why [s] would be ignored before [accept]; null when it would be taken. */
    fun debugIgnoreReason(s: VowelStroke): String? = vs.ignoreReason(s, consonant)

    /** The vowel assembly now: state, candidates, shown vowel, ㆍ / ‥. */
    fun debugState(): String = vs.stateText()

    /** ACTION_UP of a consonant, cheonjiin, number or symbol key (HLD 4.3 accept table). */
    fun accept(r: StrokeResult): MapResult = when (val o = r.outcome) {
        is StrokeOutcome.Text -> {
            val dropped = closeAll()   // the app commits the composition and writes the text
            MapResult.Text(o.text, o.errorVibe || dropped)
        }
        StrokeOutcome.Tap -> {
            val dropped = closeAll()
            MapResult.Jamo(listOf(JamoEvent.Consonant(consonantOf(r.keyId))), errorVibe = dropped)
        }
        is StrokeOutcome.Stroke -> {
            cj.clear()   // the two assemblies never mix (SWIPE 5.4)
            when (val p = vs.push(o.s, consonant)) {
                is StrokePush.Started -> MapResult.Jamo(listOf(JamoEvent.Vowel(p.v)))
                is StrokePush.Changed -> MapResult.Jamo(listOf(JamoEvent.ReplaceVowel(p.v)))
                StrokePush.Pending -> MapResult.Jamo(emptyList())
                StrokePush.Ignored -> MapResult.Ignored
            }
        }
        StrokeOutcome.Reverse -> MapResult.Ignored   // the assembly stays (SWIPE 3.2)
        is StrokeOutcome.Element -> {
            val dropped = vs.close()
            element(o.e, dropped)
        }
    }

    /** A cheonjiin element (FR-SWP-62, 63). */
    private fun element(e: CjElement, dropped: Boolean): MapResult = when (val p = cj.push(e)) {
        is CjPush.Started -> MapResult.Jamo(listOf(JamoEvent.Vowel(p.v)), dropped)
        is CjPush.Changed -> MapResult.Jamo(listOf(JamoEvent.ReplaceVowel(p.v)), dropped)
        is CjPush.PendingShown -> MapResult.Jamo(emptyList(), dropped)
        CjPush.Ignored, CjPush.NotExtendable -> if (dropped) MapResult.Jamo(emptyList(), true) else MapResult.Ignored
    }

    /** Closes both assemblies; true when vowel strokes that made no vowel were dropped. */
    private fun closeAll(): Boolean {
        cj.clear()
        return vs.close()
    }

    /**
     * ⌫ asks the open assembly first (SWIPE 3.5, FR-CMN-07b, FR-SWP-64): one stroke or element goes and
     * the app undoes the composer's last unit when the shown vowel changed. [StrokePop.Empty] = nothing
     * open: the composer's ⌫ as usual (a closed vowel goes one change at a time, never reopens).
     */
    fun popElement(): StrokePop {
        if (!vs.isEmpty) return vs.pop()
        val wasVowel = cj.state is CjState.Vowel
        return if (cj.pop() == CjPop.Empty) StrokePop.Empty else StrokePop.Popped(undoComposer = wasVowel)
    }

    /**
     * Space, enter, tab, mode change, settings key, cursor move, field change (FR-CMN-03d): both
     * assemblies close. True when vowel strokes that made no vowel were dropped.
     */
    fun reset(): Boolean = closeAll()

    fun snapshot(): SwipeMapperSnapshot = SwipeMapperSnapshot(cj.snapshot(), vs.snapshot())

    fun restore(s: SwipeMapperSnapshot) {
        cj.restore(s.cj)
        vs.restore(s.strokes)
    }

    companion object {
        private val CONSONANT_OF: Map<String, Jamo> =
            SwipeLayout.KEYS.filter { it.kind == SwipeKeyKind.CONSONANT }.associate { it.id to checkNotNull(it.consonant) }

        private fun consonantOf(keyId: String): Jamo = requireNotNull(CONSONANT_OF[keyId]) { "not a consonant key: $keyId" }
    }
}
