package kbd.app.tutor

import kbd.stroke.cross.CrossDir
import kbd.stroke.cross.EdgeCrossingTracker
import kbd.stroke.geom.Edge
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint

/**
 * Raw stroke measures shared by the tutor steps (LLD-07 §2.2). Distances are measured from the
 * key edges and are never cut by a threshold; only the first exit is found with stroke-core and
 * the current `minCrossDp`, so later steps use the value an earlier step set.
 */
object TutorMeasure {
    /** First exit of a stroke: the edge, the index of the OUT sample and of the first IN after it (or `points.size`). */
    data class Exit(val edge: Edge, val index: Int, val t: Long, val backIn: Int)

    /**
     * Tap jitter and stroke length alike (HLD 7.1, issue #173): the larger component of the
     * press-to-release displacement — the value the keyboard's tap / dot / long decision uses (LLD-04 §4.3).
     */
    fun length(raw: RawStroke): Float {
        val a = raw.points.first()
        val b = raw.points.last()
        return maxOf(kotlin.math.abs(b.x - a.x), kotlin.math.abs(b.y - a.y))
    }

    /**
     * Tap sample ([length]) with the outlier rule (LLD-07 §3, D-51): null when the touch went more than
     * [ThresholdEstimator.JITTER_OUTLIER_DP] past the key rectangle — clearly not a tap. The public 1-1 jitter
     * (farthest distance) went with the public edition (issue #251).
     */
    fun tapSample(raw: RawStroke): Float? =
        length(raw).takeIf { outside(raw) <= ThresholdEstimator.JITTER_OUTLIER_DP }

    /** The farthest any point lies beyond the key rectangle; 0 or less when it stayed inside. */
    fun outside(raw: RawStroke): Float = raw.points.maxOf { p -> Edge.entries.maxOf { beyond(p, it, raw.keyRect) } }

    /** First OUT with the edge-crossing tracker (LLD-04 §3); null = the stroke is a tap. */
    fun firstExit(raw: RawStroke, minCrossDp: Float): Exit? {
        // The practice key is surrounded by other keys, so no edge is outer (FR-SWP-45).
        val tracker = EdgeCrossingTracker(KeyGeom("tutor", raw.keyRect, emptySet()), minCrossDp)
        tracker.begin(raw.points.first())
        var out: Exit? = null
        for (i in 1 until raw.points.size) {
            val c = tracker.add(raw.points[i]) ?: continue
            if (out == null && c.dir == CrossDir.OUT) out = Exit(c.edge, i, c.t, raw.points.size)
            else if (out != null && c.dir == CrossDir.IN) return out.copy(backIn = i)
        }
        return out
    }

    /** How far [p] lies beyond [edge] along its axis; negative inside. */
    fun beyond(p: StrokePoint, edge: Edge, rect: RectDp): Float = when (edge) {
        Edge.LEFT -> rect.left - p.x
        Edge.RIGHT -> p.x - rect.right
        Edge.TOP -> rect.top - p.y
        Edge.BOTTOM -> p.y - rect.bottom
    }

    /** Step 2: largest distance beyond the first exit edge during the first excursion. */
    fun maxBeyond(raw: RawStroke, exit: Exit): Float =
        (exit.index until exit.backIn).maxOf { beyond(raw.points[it], exit.edge, raw.keyRect) }

    // Issue #173: longIndex, rubExcursions, vertAfter, maxReturn and maxTurn (3-1 / 3-2 one-stroke vowel measures) are gone.
}
