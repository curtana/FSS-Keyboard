package kbd.stroke.geom

/**
 * A key rectangle and its outer edges (LLD-03 §3.3). An outer edge lies on the window's
 * left, right or bottom border; the window's top border is never outer, and edges next to
 * blanks, empty cells or the empty band are not outer either.
 *
 * [topAtWindow]: the key's top edge lies on the window's top border (the top row, issue #237).
 *
 * The platform does not report touch points beyond the window: it pins them to its border. So a point on the border
 * at one of the key's window edges ([windowEdges] = [outerEdges] and the top when [topAtWindow]) counts as
 * `minCrossDp` past that edge — an exit (`EdgeCrossingTracker`, issue #237: the four borders alike). An outer edge is
 * no longer "never crossed": it is crossed by reaching the border.
 */
data class KeyGeom(val id: String, val rect: RectDp, val outerEdges: Set<Edge>, val topAtWindow: Boolean = false) {
    /** The key's edges on the window border (issue #237). */
    val windowEdges: Set<Edge> get() = if (topAtWindow) outerEdges + Edge.TOP else outerEdges

    companion object {
        /** Float-error margin when comparing a key edge with the window border (LLD-04 §2). */
        const val OUTER_TOLERANCE_DP = 0.5f

        /** Computes [outerEdges] from the window size once, when the layout is built (LLD-04 §2). */
        fun build(id: String, rect: RectDp, windowW: Float, windowH: Float): KeyGeom {
            val outer = mutableSetOf<Edge>()
            if (rect.left <= OUTER_TOLERANCE_DP) outer += Edge.LEFT
            if (rect.right >= windowW - OUTER_TOLERANCE_DP) outer += Edge.RIGHT
            if (rect.bottom >= windowH - OUTER_TOLERANCE_DP) outer += Edge.BOTTOM
            return KeyGeom(id, rect, outer, topAtWindow = rect.top <= OUTER_TOLERANCE_DP)
        }
    }
}
