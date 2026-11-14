package kbd.app.graffiti

import android.view.MotionEvent
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2

/**
 * Collects one pad stroke in dp (LLD-05 §8.2). One pointer only: while a stroke is in progress other
 * pointers are ignored (FR-CMN-11). The area is decided by the start point (GRAFFITI 6.3).
 * The `MotionEvent` functions convert px to dp; the dp functions ([down], [move], [up]) hold the logic.
 */
class InkCollector(private val density: Float) {

    private var pointerId = NONE
    private val pts = ArrayList<InkPoint>()
    private var area = PadArea.LETTER
    private var areaRect = RectF2(0f, 0f, 0f, 0f)

    val active: Boolean get() = pointerId != NONE

    /** Points of the stroke in progress, for drawing the ink. */
    val points: List<InkPoint> get() = pts

    /**
     * [rectOf] gives the start area's rectangle for [InkStroke.areaRect] (LLD-05 §2, corner tap); the
     * LLD-05 §8.2 signature has only [areaOf].
     */
    fun onDown(e: MotionEvent, idx: Int, areaOf: (Float, Float) -> PadArea, rectOf: (PadArea) -> RectF2) {
        down(e.getPointerId(idx), e.getX(idx) / density, e.getY(idx) / density, e.eventTime, areaOf, rectOf)
    }

    /** History points of the stroke's pointer, then its current point (HLD 5). */
    fun onMove(e: MotionEvent) {
        val idx = e.findPointerIndex(pointerId)
        if (idx < 0) return
        val out = ArrayList<InkPoint>(e.historySize + 1)
        for (h in 0 until e.historySize) {
            out += InkPoint(e.getHistoricalX(idx, h) / density, e.getHistoricalY(idx, h) / density, e.getHistoricalEventTime(h))
        }
        out += InkPoint(e.getX(idx) / density, e.getY(idx) / density, e.eventTime)
        move(pointerId, out)
    }

    /** The finished stroke when the lifted pointer ([MotionEvent.getActionIndex]) is the stroke's; else null. */
    fun onUp(e: MotionEvent): InkStroke? {
        val idx = e.actionIndex
        return up(e.getPointerId(idx), e.getX(idx) / density, e.getY(idx) / density, e.eventTime)
    }

    /** Drops the stroke in progress (ACTION_CANCEL, LLD-05 §8.3). */
    fun cancel() {
        pointerId = NONE
        pts.clear()
    }

    /** Starts a stroke unless one is in progress; false = ignored (FR-CMN-11). */
    fun down(id: Int, x: Float, y: Float, t: Long, areaOf: (Float, Float) -> PadArea, rectOf: (PadArea) -> RectF2): Boolean {
        if (active) return false
        pointerId = id
        pts.clear()
        pts += InkPoint(x, y, t)
        area = areaOf(x, y)
        areaRect = rectOf(area)
        return true
    }

    fun move(id: Int, points: List<InkPoint>) {
        if (id == pointerId) pts += points
    }

    fun up(id: Int, x: Float, y: Float, t: Long): InkStroke? {
        if (!active || id != pointerId) return null
        val last = pts.last()
        if (last.x != x || last.y != y || last.t != t) pts += InkPoint(x, y, t)
        val stroke = InkStroke(pts.toList(), area, areaRect)
        cancel()
        return stroke
    }

    private companion object {
        const val NONE = -1
    }
}
