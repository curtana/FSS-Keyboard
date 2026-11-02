package kbd.graffiti.recognize

import kbd.graffiti.ink.InkStroke
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Template matching (LLD-05 §3.2), written from scratch (no GPL code): resample to [N] points,
 * centre on the centroid, scale the bounding box's long side to 1 keeping the aspect ratio,
 * **no rotation normalisation** (direction tells shapes apart, GRAFFITI 7), mean point distance,
 * corner-count aid. Deterministic: no clock, no randomness (NFR-04).
 *
 * Closed loops (FSS#33, LLD-05 §3.2 step 4a): a hand-drawn circle seldom starts where the template does and seldom
 * closes exactly. Against a **loop template** (a closed stroke with no corners: ㅇ, o, 0, @) an input that is a
 * near-closed loop ([loopOf]) is also scored as one exact lap, its start shifted round to where it fits best
 * ([cyclicScore]); the better of that and the plain score counts. Direction still tells (the templates carry both).
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

    /** Points the loop test resamples the input to before summing its turning (finer than [N] so a lap's ends cut cleanly). */
    internal const val LOOP_SAMPLES = 64

    /** A template stroke is closed when its ends are this close (normalized 0..1 template coordinates). */
    internal const val LOOP_TEMPLATE_GAP = 0.05f

    /** An input is a near-closed loop when its lap (cut at one turn, or the whole path) ends within this share of its long side of its start … */
    internal const val LOOP_GAP_MAX = 0.35

    /** … and it turns this far in total (one lap = 360°): from 60° short of closing to 1.5 laps. */
    internal const val LOOP_TURN_MIN_DEG = 300.0
    internal const val LOOP_TURN_MAX_DEG = 540.0

    /**
     * Past one lap, every point after the cut must lie within this share of the long side of the lap itself: a circle
     * drawn on past its start keeps to its own path, while the tail of g, q, 6, 9 leaves it.
     */
    internal const val LOOP_TAIL_MAX = 0.15

    private val SQRT2 = sqrt(2.0)

    /**
     * Best (id, score) in [set], score = 1 − d / √2 (best alternative stroke of the id), minus
     * [CORNER_PENALTY] when the stroke's corner count differs from the template's `corners` by
     * [CORNER_MISMATCH] or more. Ties go to the earlier template. Null when [set] has no strokes to match.
     */
    fun best(stroke: InkStroke, set: List<Template>): Pair<String, Float>? {
        if (stroke.points.isEmpty()) return null
        val raw = stroke.points.map { it.x to it.y }
        val input = normalize(resample(raw))
        val inputCorners = cornerCount(input)
        // null: not a near-closed loop, or one with corners (a rounded ㅁ keeps the plain comparison and its corner aid)
        val lap = if (inputCorners >= CORNER_MISMATCH) null else loopOf(raw)?.let { normalize(resample(it)) }
        var bestId: String? = null
        var bestScore = 0f
        for (t in set) {
            var s = t.strokes.maxOfOrNull { ts ->
                val tn = normalize(resample(ts))
                val plain = score(input, tn)
                if (lap != null && t.corners == 0 && isClosed(ts)) maxOf(plain, cyclicScore(lap, tn)) else plain
            } ?: continue
            if (abs(inputCorners - t.corners) >= CORNER_MISMATCH) s -= CORNER_PENALTY
            if (bestId == null || s > bestScore) {
                bestId = t.id
                bestScore = s
            }
        }
        return bestId?.let { it to bestScore }
    }

    /** A closed template stroke: its ends meet ([LOOP_TEMPLATE_GAP]). */
    internal fun isClosed(points: List<Pair<Float, Float>>): Boolean =
        points.size > 2 && dist(points.first(), points.last()) <= LOOP_TEMPLATE_GAP

    /**
     * Step 4a: [points] as exactly one closed lap, or null when they are not a near-closed loop: the path turns
     * [LOOP_TURN_MIN_DEG]..[LOOP_TURN_MAX_DEG] in one direction overall. A path past one lap is cut where it has turned
     * 360° (what follows must stay on the lap, [LOOP_TAIL_MAX], else it is a tail and the input no loop). The lap must
     * end within [LOOP_GAP_MAX] of the long side of where it began (a 6 or 9 turns a lap but does not come back); then the
     * start point is added at the end, so the lap is closed (a short gap becomes a straight bridge).
     */
    internal fun loopOf(points: List<Pair<Float, Float>>): List<Pair<Float, Float>>? {
        if (points.size < 3) return null
        val w = points.maxOf { it.first } - points.minOf { it.first }
        val h = points.maxOf { it.second } - points.minOf { it.second }
        val side = maxOf(w, h).toDouble()
        if (side <= 0.0) return null
        val r = resample(points, LOOP_SAMPLES)
        var turn = 0.0
        val cum = DoubleArray(r.size)
        for (i in 1 until r.size - 1) {
            turn += signedTurnDeg(r[i - 1], r[i], r[i + 1])
            cum[i + 1] = turn
        }
        val total = kotlin.math.abs(turn)
        if (total < LOOP_TURN_MIN_DEG || total > LOOP_TURN_MAX_DEG) return null
        val end = if (total > 360.0) (1 until r.size).first { kotlin.math.abs(cum[it]) >= 360.0 } else r.size - 1
        val lap = r.subList(0, end + 1)
        if (dist(lap.first(), lap.last()) > LOOP_GAP_MAX * side) return null   // the lap does not come back: 6, 9, e …
        for (j in end + 1 until r.size) {
            if (lap.minOf { dist(it, r[j]) } > LOOP_TAIL_MAX * side) return null   // a tail, not more of the loop
        }
        return lap + r[0]
    }

    /**
     * Step 4a: the best [score] of closed lap [a] against closed template [b] over every start of [a] (both [N] points,
     * first = last): the lap's [N] − 1 distinct points are shifted round, the template keeps its start.
     */
    internal fun cyclicScore(a: List<Pair<Float, Float>>, b: List<Pair<Float, Float>>): Float {
        val m = a.size - 1
        var best = -Double.MAX_VALUE
        for (k in 0 until m) {
            var sum = 0.0
            for (i in 0 until m) sum += dist(a[(i + k) % m], b[i])
            best = maxOf(best, 1.0 - sum / m / SQRT2)
        }
        return best.toFloat()
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

    /** Signed direction change at [b] in degrees (−180..180), screen axes. */
    private fun signedTurnDeg(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>): Double {
        val a1 = atan2((b.second - a.second).toDouble(), (b.first - a.first).toDouble())
        val a2 = atan2((c.second - b.second).toDouble(), (c.first - b.first).toDouble())
        var d = Math.toDegrees(a2 - a1)
        while (d > 180.0) d -= 360.0
        while (d < -180.0) d += 360.0
        return d
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
