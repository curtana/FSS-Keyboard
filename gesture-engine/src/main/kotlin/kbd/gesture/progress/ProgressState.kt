package kbd.gesture.progress

import kbd.gesture.classify.StrokeClassifier
import kbd.stroke.geom.Dir

/** A next-move hint (LLD-04 §5.1): direction, display distance, result label and probe kind. */
data class Candidate(val dir: Dir, val offsetDp: Float, val label: String, val kind: ProbeKind)

enum class ProbeKind { RING, RUB, DOT, LONG }   // DOT / LONG: consonant-key vowel strokes (issue #173); STAGE2/3 went with the symbol stages (issue #208)

/**
 * Progress of the current stroke for the guide (LLD-04 §5.1). [current] is this stroke's own
 * judgement if released now (no block merge); [beforeExit] is true until the first exit (the
 * direction guide shows, after it the next characters).
 */
data class ProgressState(
    val current: String,
    val candidates: List<Candidate>,
    val beforeExit: Boolean,
) {
    companion object {
        /** LLD-04 §5.2: this stroke's result now, the probe candidates that change it, and before-exit. */
        fun of(c: StrokeClassifier): ProgressState {
            val s = c.state()
            val outcome = c.outcomeOf(s)
            val current = c.textOf(outcome)
            return ProgressState(
                current = current,
                candidates = probeCandidates(c, current),
                // A consonant key's stroke starts at minCrossDp of displacement (issue #173), the others' at the first exit.
                beforeExit = if (c.key.kind == kbd.gesture.classify.SwipeKeyKind.CONSONANT) outcome == kbd.gesture.classify.StrokeOutcome.Tap else s.e1 == null,
            )
        }
    }
}
