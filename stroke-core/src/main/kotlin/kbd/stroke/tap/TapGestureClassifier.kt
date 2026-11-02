package kbd.stroke.tap

import kbd.stroke.cross.EdgeCrossingTracker
import kbd.stroke.cross.countOf
import kbd.stroke.cross.firstOut
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.StrokePoint

/**
 * Classifies one stroke on a tap-keyboard key from its edge crossings (LLD-05A §2.1).
 * Length and turns are not looked at; [minCrossDp] is the same threshold the swipe keyboard
 * uses (FR-TAP-02b). Outer edges record nothing, so leaving only through them is a [TapGesture.Tap].
 */
class TapGestureClassifier(key: KeyGeom, minCrossDp: Float) {

    private val tracker = EdgeCrossingTracker(key, minCrossDp)

    /** Starts a stroke; calling it again restarts. */
    fun begin(p: StrokePoint) = tracker.begin(p)

    fun add(p: StrokePoint) {
        tracker.add(p)
    }

    /** Feeds the release point and returns the gesture: count of the first exit edge 1 → Exit, 2 → ExitReturn, ≥3 → Rub. */
    fun finish(p: StrokePoint): TapGesture {
        tracker.add(p)
        val e1 = tracker.firstOut()?.edge ?: return TapGesture.Tap
        return when (tracker.countOf(e1)) {
            1 -> TapGesture.Exit(e1.dir)
            2 -> TapGesture.ExitReturn(e1.dir)
            else -> TapGesture.Rub(e1.dir)
        }
    }
}
