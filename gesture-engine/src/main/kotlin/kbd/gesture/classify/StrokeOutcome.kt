package kbd.gesture.classify

import kbd.hangul.cheonjiin.CjElement
import kbd.hangul.stroke.VowelStroke

/** Result of one stroke (LLD-04 §4.1). Every stroke ends as exactly one of these. */
sealed interface StrokeOutcome {
    /** Consonant key: a tap, the key's consonant (FR-SWP-36). */
    data object Tap : StrokeOutcome

    /**
     * Consonant key: one handwriting vowel stroke → or ↓ (SWIPE 3.1, FR-SWP-10, issue #173). The key it
     * started on does not matter; `SwipeInputMapper` assembles it by stroke order and position.
     */
    data class Stroke(val s: VowelStroke) : StrokeOutcome

    /** Consonant key: a stroke ← or ↑, against the stroke order: no input, the error vibration (SWIPE 3.2, FR-SWP-12). */
    data object Reverse : StrokeOutcome

    /** Cheonjiin key: one element (tap ㆍ, ↑·↓ ㅣ, → ㅡ; FR-SWP-61, issue #160); `SwipeInputMapper` assembles it. */
    data class Element(val e: CjElement) : StrokeOutcome

    /** Number and symbol keys: [text] to type, with the error vibration when [errorVibe]. */
    data class Text(val text: String, val errorVibe: Boolean) : StrokeOutcome
}

/**
 * A finished stroke (LLD-04 §4.1). [firstCrossTime] is the first edge crossing, null for a tap.
 */
data class StrokeResult(
    val keyId: String, val kind: SwipeKeyKind, val outcome: StrokeOutcome,
    val downTime: Long,
    val firstCrossTime: Long?,
    val upTime: Long,
)
