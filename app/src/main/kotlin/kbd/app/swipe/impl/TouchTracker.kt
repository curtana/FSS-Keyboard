package kbd.app.swipe.impl

import android.view.MotionEvent
import kbd.stroke.geom.StrokePoint

/**
 * Follows the one pointer of the current stroke and turns its samples into dp points
 * (LLD-04 §9.1, HLD 4.1: history points included). [activePointerId] = [NONE] between strokes.
 */
class TouchTracker(private val density: Float) {
    var activePointerId: Int = NONE

    /** Takes the pointer at [index] as the stroke's pointer and returns its point. */
    fun onDown(e: MotionEvent, index: Int): StrokePoint {
        activePointerId = e.getPointerId(index)
        return pointAt(e, index)
    }

    /** History points of the active pointer, then its current point; empty if it is not in [e]. */
    fun pointsOf(e: MotionEvent): List<StrokePoint> {
        val idx = e.findPointerIndex(activePointerId)
        if (idx < 0) return emptyList()
        val out = ArrayList<StrokePoint>(e.historySize + 1)
        for (h in 0 until e.historySize) {
            out += StrokePoint(e.getHistoricalX(idx, h) / density, e.getHistoricalY(idx, h) / density, e.getHistoricalEventTime(h))
        }
        out += pointAt(e, idx)
        return out
    }

    /** Current point of the pointer at [index], in dp. */
    fun pointAt(e: MotionEvent, index: Int): StrokePoint =
        StrokePoint(e.getX(index) / density, e.getY(index) / density, e.eventTime)

    companion object {
        const val NONE = -1
    }
}
