package kbd.gesture.classify

import kbd.stroke.cross.CrossDir
import kbd.stroke.cross.Crossing
import kbd.stroke.cross.EdgeCrossingTracker
import kbd.stroke.geom.Edge
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.StrokePoint
import kbd.stroke.threshold.Thresholds

/**
 * Fixed-size running state of one stroke on one key (LLD-04 §4.2). It keeps no point list, so
 * [copy] is cheap and progress probes (§5) feed the same [add] code as the real stroke.
 *
 * Issue #173: the consonant-key vowel rules of the edge crossings (long first exit, return, turn,
 * rub, vertical +ㅣ) are gone, and with them their fields. What is left serves the number, symbol
 * and cheonjiin keys (edge crossings) and the stroke's start and end points.
 */
class StrokeState(val key: KeyGeom, val th: Thresholds) {

    var tracker: EdgeCrossingTracker = EdgeCrossingTracker(key, th.minCrossDp)
        private set

    /** First exit edge. */
    var e1: Edge? = null
        private set

    /** Sample of the first OUT record. */
    var exitPt: StrokePoint? = null
        private set

    /** The press point. */
    var first: StrokePoint = StrokePoint(0f, 0f, 0L)
        private set

    var last: StrokePoint = StrokePoint(0f, 0f, 0L)
        private set

    /** Starts a stroke at [p]; calling it again restarts. */
    fun begin(p: StrokePoint) {
        tracker = EdgeCrossingTracker(key, th.minCrossDp).also { it.begin(p) }
        e1 = null
        exitPt = null
        first = p
        last = p
    }

    /** Feeds one sample; returns its crossing, or null. NaN samples are dropped (D-06). */
    fun add(p: StrokePoint): Crossing? {
        if (p.x.isNaN() || p.y.isNaN()) return null
        val c = tracker.add(p)
        if (c != null && c.dir == CrossDir.OUT && e1 == null) {
            e1 = c.edge
            exitPt = p
        }
        last = p
        return c
    }

    /** Independent copy for progress probes (LLD-04 §5.2). */
    fun copy(): StrokeState = StrokeState(key, th).also {
        it.tracker = tracker.copy()
        it.e1 = e1
        it.exitPt = exitPt
        it.first = first
        it.last = last
    }
}
