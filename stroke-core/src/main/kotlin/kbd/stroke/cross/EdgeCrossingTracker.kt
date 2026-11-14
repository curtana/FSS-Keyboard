package kbd.stroke.cross

import kbd.stroke.geom.Edge
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.StrokePoint

/**
 * Records edge crossings of one stroke on one key (LLD-04 §3.2–3.3), for the tap keyboards' keys
 * and every keyboard's mode key. Each axis has a hysteresis zone: a stroke is out once it is
 * [minCrossDp] beyond an edge, and back in once it is [minCrossDp] inside it. An edge on the window border
 * ([KeyGeom.windowEdges]) is crossed when a point reaches the border (issue #237: the platform pins points beyond the
 * window to its border); 0.40's "outer edges are never crossed" (FR-SWP-45) went with it.
 */
class EdgeCrossingTracker(val key: KeyGeom, val minCrossDp: Float) {

    private val records = ArrayList<Crossing>()
    private var prev: StrokePoint? = null

    /** The pressed point: a border counts only after the stroke moved [minCrossDp] towards it from here (SRS FR-SWP-45). */
    private var start: StrokePoint? = null

    val crossings: List<Crossing> get() = records

    var zone: ZoneState = ZoneState.INSIDE
        private set

    /** Starts a stroke; the pressed point is inside by definition (FR-SWP-02). Calling it again restarts. */
    fun begin(p: StrokePoint) {
        zone = ZoneState.INSIDE
        records.clear()
        prev = p
        start = p
    }

    /**
     * The point as judged (issue #237): on a key edge at the window border ([KeyGeom.windowEdges]) a point on that border
     * (within [KeyGeom.OUTER_TOLERANCE_DP]) is [minCrossDp] past the edge — the platform gives no point beyond the
     * window — but only once the stroke has moved [minCrossDp] towards that border from the pressed point (SRS 0.60
     * FR-SWP-45 design decision: a jitter or a press right at the border is no exit, nor is a diagonal's side touch
     * that has not moved that way). Coming back into the key is judged on the real points as before.
     */
    private fun judged(p: StrokePoint): StrokePoint {
        val r = key.rect
        val w = key.windowEdges
        val tol = KeyGeom.OUTER_TOLERANCE_DP
        val s = start ?: p
        val m = minCrossDp
        val x = when {
            Edge.LEFT in w && p.x <= r.left + tol && s.x - p.x >= m -> r.left - m
            Edge.RIGHT in w && p.x >= r.right - tol && p.x - s.x >= m -> r.right + m
            else -> p.x
        }
        val y = when {
            Edge.TOP in w && p.y <= r.top + tol && s.y - p.y >= m -> r.top - m
            Edge.BOTTOM in w && p.y >= r.bottom - tol && p.y - s.y >= m -> r.bottom + m
            else -> p.y
        }
        return if (x == p.x && y == p.y) p else p.copy(x = x, y = y)
    }

    /** Feeds one sample; returns the crossing it produced (at most one), or null. NaN samples are dropped (D-06). */
    fun add(p: StrokePoint): Crossing? {
        val last = checkNotNull(prev) { "begin() must be called before add()" }
        if (p.x.isNaN() || p.y.isNaN()) return null
        val q = judged(p)
        prev = q

        val old = zone
        val now = ZoneState(nextX(old.x, q.x), nextY(old.y, q.y))
        zone = now

        val edge = when {
            old.inside && !now.inside -> {
                val xOut = now.x != XZone.IN
                val yOut = now.y != YZone.IN
                when {
                    xOut && yOut -> {
                        // FR-SWP-04: the axis that reached its threshold first; ties go horizontal (D-04).
                        val fx = ratio(last.x, q.x, outThreshold(now.x))
                        val fy = ratio(last.y, q.y, outThreshold(now.y))
                        if (fy < fx) edgeOf(now.y) else edgeOf(now.x)
                    }
                    xOut -> edgeOf(now.x)
                    else -> edgeOf(now.y)
                }
            }
            !old.inside && now.inside -> {
                val xIn = old.x != XZone.IN
                val yIn = old.y != YZone.IN
                when {
                    xIn && yIn -> {
                        // The axis that became IN last; ties go horizontal (D-04).
                        val fx = ratio(last.x, q.x, inThreshold(old.x))
                        val fy = ratio(last.y, q.y, inThreshold(old.y))
                        if (fy > fx) edgeOf(old.y) else edgeOf(old.x)
                    }
                    xIn -> edgeOf(old.x)
                    else -> edgeOf(old.y)
                }
            }
            else -> return null
        }
        // D-05: the time of the sample that met the condition, not interpolated.
        val dir = if (now.inside) CrossDir.IN else CrossDir.OUT
        return Crossing(edge, dir, p.t, p.x, p.y).also { records += it }
    }

    /** Independent copy with the records so far (LLD-04 §3.3). */
    fun copy(): EdgeCrossingTracker = EdgeCrossingTracker(key, minCrossDp).also {
        it.records.addAll(records)
        it.prev = prev
        it.start = start
        it.zone = zone
    }

    private fun nextX(z: XZone, x: Float): XZone {
        val r = key.rect
        var n = z
        if (n == XZone.RIGHT_OUT && x <= r.right - minCrossDp) n = XZone.IN
        if (n == XZone.LEFT_OUT && x >= r.left + minCrossDp) n = XZone.IN
        if (n == XZone.IN) {
            if (x >= r.right + minCrossDp) n = XZone.RIGHT_OUT
            else if (x <= r.left - minCrossDp) n = XZone.LEFT_OUT
        }
        return n
    }

    private fun nextY(z: YZone, y: Float): YZone {
        val r = key.rect
        var n = z
        if (n == YZone.BOTTOM_OUT && y <= r.bottom - minCrossDp) n = YZone.IN
        if (n == YZone.TOP_OUT && y >= r.top + minCrossDp) n = YZone.IN
        if (n == YZone.IN) {
            if (y >= r.bottom + minCrossDp) n = YZone.BOTTOM_OUT
            else if (y <= r.top - minCrossDp) n = YZone.TOP_OUT
        }
        return n
    }

    private fun outThreshold(z: XZone): Float =
        if (z == XZone.RIGHT_OUT) key.rect.right + minCrossDp else key.rect.left - minCrossDp

    private fun outThreshold(z: YZone): Float =
        if (z == YZone.BOTTOM_OUT) key.rect.bottom + minCrossDp else key.rect.top - minCrossDp

    private fun inThreshold(z: XZone): Float =
        if (z == XZone.RIGHT_OUT) key.rect.right - minCrossDp else key.rect.left + minCrossDp

    private fun inThreshold(z: YZone): Float =
        if (z == YZone.BOTTOM_OUT) key.rect.bottom - minCrossDp else key.rect.top + minCrossDp

    private fun edgeOf(z: XZone): Edge = if (z == XZone.RIGHT_OUT) Edge.RIGHT else Edge.LEFT

    private fun edgeOf(z: YZone): Edge = if (z == YZone.BOTTOM_OUT) Edge.BOTTOM else Edge.TOP

    /** Fraction of the segment a→b at which the coordinate reaches [threshold]; 0 when the segment has no extent. */
    private fun ratio(a: Float, b: Float, threshold: Float): Float =
        if (b == a) 0f else (threshold - a) / (b - a)
}

/** The first OUT record. */
fun EdgeCrossingTracker.firstOut(): Crossing? = crossings.firstOrNull { it.dir == CrossDir.OUT }

/** Number of OUT and IN records on [edge] — how many times the stroke crossed that edge (SRS 1.3). */
fun EdgeCrossingTracker.countOf(edge: Edge): Int = crossings.count { it.edge == edge }

/** The [n]-th record (1-based, OUT or IN) on [edge], or null if there are fewer. */
fun EdgeCrossingTracker.nthCrossingOf(edge: Edge, n: Int): Crossing? =
    if (n < 1) null else crossings.asSequence().filter { it.edge == edge }.elementAtOrNull(n - 1)
