package kbd.stroke.tap

import kbd.stroke.cross.EdgeCrossingTracker
import kbd.stroke.cross.countOf
import kbd.stroke.cross.firstOut
import kbd.stroke.geom.Diag
import kbd.stroke.geom.Dir
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.StrokePoint
import kbd.stroke.geom.Way
import kotlin.math.atan2

/**
 * Classifies one stroke on a tap-keyboard key from its edge crossings (LLD-05A §2.1).
 * Length and turns are not looked at; [minCrossDp] is the minimum crossing distance
 * (FR-TAP-02b). Outer edges record nothing, so leaving only through them is a [TapGesture.Tap].
 *
 * [eightWay] (FSS#109, SRS FR-TAP-02e): the key has a diagonal assignment, so the direction is the angle from the
 * pressed point to the point that counted as the first exit, in eight sectors ([wayOfVector]); the count of the first
 * exit edge still tells exit, exit-and-return and rub apart. The caller decides it from the key's data
 * (`TapKeyDef.eightWay`), never from the key's name.
 */
class TapGestureClassifier(key: KeyGeom, minCrossDp: Float, private val eightWay: Boolean = false) {

    private val tracker = EdgeCrossingTracker(key, minCrossDp)
    private var start: StrokePoint? = null

    /** Starts a stroke; calling it again restarts. */
    fun begin(p: StrokePoint) {
        start = p
        tracker.begin(p)
    }

    fun add(p: StrokePoint) {
        tracker.add(p)
    }

    /** Feeds the release point and returns the gesture: count of the first exit edge 1 → Exit, 2 → ExitReturn, ≥3 → Rub. */
    fun finish(p: StrokePoint): TapGesture {
        tracker.add(p)
        val out = tracker.firstOut() ?: return TapGesture.Tap
        val s = start
        val dir: Way = if (eightWay && s != null) wayOfVector(out.x - s.x, out.y - s.y) else out.edge.dir
        return when (tracker.countOf(out.edge)) {
            1 -> TapGesture.Exit(dir)
            2 -> TapGesture.ExitReturn(dir)
            else -> TapGesture.Rub(dir)
        }
    }

    companion object {
        /** The direction of the vector ([dx], [dy]) in dp, y down (SRS FR-TAP-02e ②): its angle in [wayOfAngle]. */
        fun wayOfVector(dx: Float, dy: Float): Way {
            val deg = Math.toDegrees(atan2(-dy.toDouble(), dx.toDouble()))
            return wayOfAngle(if (deg < 0) deg + 360 else deg)
        }

        /**
         * The sector of [deg] (0° = right, 90° = up, counter-clockwise, 0 ≤ deg < 360; SRS FR-TAP-02e ②·③):
         * up, down, left and right 60° wide, the diagonals 30°. A boundary (30°, 60°, 120° …) belongs to the
         * up/down/left/right side: their sectors include both ends, the diagonals' exclude them.
         */
        fun wayOfAngle(deg: Double): Way = when {
            deg <= 30.0 -> Dir.RIGHT
            deg < 60.0 -> Diag.UP_RIGHT
            deg <= 120.0 -> Dir.UP
            deg < 150.0 -> Diag.UP_LEFT
            deg <= 210.0 -> Dir.LEFT
            deg < 240.0 -> Diag.DOWN_LEFT
            deg <= 300.0 -> Dir.DOWN
            deg < 330.0 -> Diag.DOWN_RIGHT
            else -> Dir.RIGHT
        }
    }
}
