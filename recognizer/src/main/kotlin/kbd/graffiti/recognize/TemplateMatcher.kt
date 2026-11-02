package kbd.graffiti.recognize

import kbd.graffiti.ink.InkStroke
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Template matching (LLD-05 §3.2), written from scratch (no GPL code): resample to [N] points,
 * centre on the centroid, scale the bounding box's long side to 1 keeping the aspect ratio,
 * **no rotation normalisation** (direction tells shapes apart, GRAFFITI 7), mean point distance,
 * corner-count aid. Deterministic: no clock, no randomness (NFR-04).
 */
object TemplateMatcher {
    const val N = 32
    const val CORNER_MIN_DEG = 60.0
    const val CORNER_MISMATCH = 2
    const val CORNER_PENALTY = 0.1f

    /**
     * Points on each side of a resampled point whose chords give its direction change. With 1, a sharp
     * corner falling between two samples splits into two ~45° turns and is missed; 2 keeps a 90° corner
     * above 70° wherever it falls, while a smooth 32-point circle stays near 23°. (Interim, not in LLD-05.)
     */
    internal const val CORNER_WINDOW = 2

    private val SQRT2 = sqrt(2.0)

    /**
     * Best (id, score) in [set], score = 1 − d / √2 (best alternative stroke of the id), minus
     * [CORNER_PENALTY] when the stroke's corner count differs from the template's `corners` by
     * [CORNER_MISMATCH] or more. Ties go to the earlier template. Null when [set] has no strokes to match.
     */
    fun best(stroke: InkStroke, set: List<Template>): Pair<String, Float>? {
        if (stroke.points.isEmpty()) return null
        val input = normalize(resample(stroke.points.map { it.x to it.y }))
        val inputCorners = cornerCount(input)
        var bestId: String? = null
        var bestScore = 0f
        for (t in set) {
            var s = t.strokes.maxOfOrNull { score(input, normalize(resample(it))) } ?: continue
            if (abs(inputCorners - t.corners) >= CORNER_MISMATCH) s -= CORNER_PENALTY
            if (bestId == null || s > bestScore) {
                bestId = t.id
                bestScore = s
            }
        }
        return bestId?.let { it to bestScore }
    }

    /** Step 1: [n] points evenly spaced along the path, first and last points kept. */
    internal fun resample(points: List<Pair<Float, Float>>, n: Int = N): List<Pair<Float, Float>> {
        require(points.isNotEmpty() && n >= 2)
        var total = 0.0
        for (i in 1 until points.size) total += dist(points[i - 1], points[i])
        if (total == 0.0) return List(n) { points[0] }
        val interval = total / (n - 1)
        val out = ArrayList<Pair<Float, Float>>(n)
        out += points[0]
        var acc = 0.0
        var px = points[0].first.toDouble()
        var py = points[0].second.toDouble()
        for (i in 1 until points.size) {
            val qx = points[i].first.toDouble()
            val qy = points[i].second.toDouble()
            var d = hypot(qx - px, qy - py)
            while (acc + d >= interval && out.size < n - 1) {
                val f = (interval - acc) / d
                px += f * (qx - px)
                py += f * (qy - py)
                out += px.toFloat() to py.toFloat()
                d = hypot(qx - px, qy - py)
                acc = 0.0
            }
            acc += d
            px = qx
            py = qy
        }
        while (out.size < n) out += points.last()
        return out
    }

    /** Step 2: centroid to the origin, bounding box's long side to 1, same factor on both axes. */
    internal fun normalize(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val cx = points.sumOf { it.first.toDouble() } / points.size
        val cy = points.sumOf { it.second.toDouble() } / points.size
        val w = points.maxOf { it.first } - points.minOf { it.first }
        val h = points.maxOf { it.second } - points.minOf { it.second }
        val side = maxOf(w, h).toDouble()
        val k = if (side > 0.0) 1.0 / side else 1.0
        return points.map { ((it.first - cx) * k).toFloat() to ((it.second - cy) * k).toFloat() }
    }

    /** Step 4: 1 − mean point distance / √2 between two equally long normalized point lists. */
    internal fun score(a: List<Pair<Float, Float>>, b: List<Pair<Float, Float>>): Float {
        var sum = 0.0
        for (i in a.indices) sum += dist(a[i], b[i])
        return (1.0 - sum / a.size / SQRT2).toFloat()
    }

    /**
     * Step 5: number of corners, a corner being a run of consecutive points whose direction change
     * (chord from [CORNER_WINDOW] points before to chord to [CORNER_WINDOW] points after) is ≥ [CORNER_MIN_DEG].
     */
    internal fun cornerCount(points: List<Pair<Float, Float>>): Int {
        var corners = 0
        var inRun = false
        for (i in CORNER_WINDOW until points.size - CORNER_WINDOW) {
            val turn = turnDeg(points[i - CORNER_WINDOW], points[i], points[i + CORNER_WINDOW])
            val isCorner = turn >= CORNER_MIN_DEG
            if (isCorner && !inRun) corners++
            inRun = isCorner
        }
        return corners
    }

    private fun turnDeg(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>): Double {
        val ux = (b.first - a.first).toDouble()
        val uy = (b.second - a.second).toDouble()
        val vx = (c.first - b.first).toDouble()
        val vy = (c.second - b.second).toDouble()
        val norm = hypot(ux, uy) * hypot(vx, vy)
        if (norm == 0.0) return 0.0
        return Math.toDegrees(acos(((ux * vx + uy * vy) / norm).coerceIn(-1.0, 1.0)))
    }

    private fun dist(a: Pair<Float, Float>, b: Pair<Float, Float>): Double =
        hypot((a.first - b.first).toDouble(), (a.second - b.second).toDouble())
}
