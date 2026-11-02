package kbd.graffiti.recognize

import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Tap and straight-line rules, applied before template matching (LLD-05 §3.1 steps 1–2, GRAFFITI 4.4).
 * Coordinates are pad dp with y growing downwards, so 90° is "down".
 */
object StrokeRules {
    // D-11 initial values; recognition parameters to be tuned on real strokes (LLD-05 §3.1).
    const val TAP_MAX_DP = 8f
    const val CORNER_RATIO = 0.15f
    const val LINE_STRAIGHTNESS = 0.92f
    const val LINE_SECTOR_DEG = 20.0

    private val SECTORS: List<Pair<Double, StrokeToken>> = listOf(
        90.0 to StrokeToken.LineDown,
        -90.0 to StrokeToken.LineUp,
        0.0 to StrokeToken.LineRight,
        180.0 to StrokeToken.LineLeft,
        135.0 to StrokeToken.DiagDownLeft,
        45.0 to StrokeToken.DiagDownRight,
    )

    /**
     * Step 1: path length and bounding-box diagonal both below [TAP_MAX_DP] (a single point always) →
     * [StrokeToken.CornerTap] when it starts in the letter area's top-left corner, else [StrokeToken.Tap].
     * Null when the stroke is not a tap.
     */
    fun tap(stroke: InkStroke): StrokeToken? {
        val pts = stroke.points
        if (pathLength(pts) >= TAP_MAX_DP || bboxDiagonal(pts) >= TAP_MAX_DP) return null
        return if (stroke.area == PadArea.LETTER && inCorner(pts[0], stroke.areaRect)) StrokeToken.CornerTap
        else StrokeToken.Tap
    }

    /**
     * Step 2: straightness |end − start| / L ≥ [LINE_STRAIGHTNESS] and direction within [LINE_SECTOR_DEG]
     * of one of the six line directions. Null for a curve or a straight line in no sector (→ templates).
     */
    fun line(stroke: InkStroke): StrokeToken? {
        val pts = stroke.points
        val length = pathLength(pts)
        if (length <= 0f) return null
        val dx = (pts.last().x - pts.first().x).toDouble()
        val dy = (pts.last().y - pts.first().y).toDouble()
        if (hypot(dx, dy) / length < LINE_STRAIGHTNESS) return null
        val theta = Math.toDegrees(atan2(dy, dx))
        return SECTORS.firstOrNull { (center, _) -> angleDiff(theta, center) <= LINE_SECTOR_DEG }?.second
    }

    /** Start inside the top-left [CORNER_RATIO] of the area's width and height (FR-GRF-05a). */
    private fun inCorner(p: InkPoint, r: RectF2): Boolean =
        p.x - r.left < (r.right - r.left) * CORNER_RATIO && p.y - r.top < (r.bottom - r.top) * CORNER_RATIO

    internal fun pathLength(pts: List<InkPoint>): Float {
        var sum = 0.0
        for (i in 1 until pts.size) {
            sum += hypot((pts[i].x - pts[i - 1].x).toDouble(), (pts[i].y - pts[i - 1].y).toDouble())
        }
        return sum.toFloat()
    }

    private fun bboxDiagonal(pts: List<InkPoint>): Float =
        hypot(pts.maxOf { it.x } - pts.minOf { it.x }, pts.maxOf { it.y } - pts.minOf { it.y })

    /** Absolute difference of two angles in degrees, folded into 0..180 (so 180° and −180° are equal). */
    private fun angleDiff(a: Double, b: Double): Double {
        val d = abs(a - b) % 360.0
        return if (d > 180.0) 360.0 - d else d
    }
}
