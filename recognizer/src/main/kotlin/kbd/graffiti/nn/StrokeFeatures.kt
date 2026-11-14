package kbd.graffiti.nn

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot

/**
 * The network's input for one stroke (LLD-05 §3.4, SRS FR-GRF-12): an 8 × 8 grid over the stroke's own bounding
 * box, 8 direction channels and one "no movement" channel, [SIZE] = 576 values laid out `[channel][row][column]`.
 *
 * The arithmetic follows the training script (`tools/graffiti-nn/features.py`) step by step, down to the order of
 * the additions, so that the app and the script give the same values: the shipped weights were trained on them.
 * Position and size are gone, so strokes told apart by place or length stay with the rules (FR-GRF-12 ④).
 * Deterministic: no clock, no randomness (NFR-04).
 */
object StrokeFeatures {
    const val GRID = 8
    const val DIRECTIONS = 8
    const val CHANNELS = DIRECTIONS + 1
    const val SIZE = CHANNELS * GRID * GRID

    /** The path is resampled to this many points before the direction channels are filled (12 per grid cell). */
    const val RESAMPLE = 12 * GRID

    /** The bounding box's long side covers this share of the grid; the rest is an even margin. */
    const val FILL = 0.875

    /** Centre channel: a raw step shorter than this (dp) counts as "no movement" … */
    const val CENTRE_STEP_DP = 1.0

    /** … and adds this much to its cell, so four such steps fill it. */
    const val CENTRE_GAIN = 0.25f

    /** [xs], [ys]: the raw samples of the stroke in dp, in order; at least one point. */
    fun compute(xs: DoubleArray, ys: DoubleArray): FloatArray {
        require(xs.isNotEmpty() && xs.size == ys.size) { "empty stroke" }
        val n = xs.size
        var minX = xs[0]
        var maxX = xs[0]
        var minY = ys[0]
        var maxY = ys[0]
        for (i in 1 until n) {
            if (xs[i] < minX) minX = xs[i]
            if (xs[i] > maxX) maxX = xs[i]
            if (ys[i] < minY) minY = ys[i]
            if (ys[i] > maxY) maxY = ys[i]
        }
        val long = maxOf(maxX - minX, maxY - minY)
        val side = if (long > 0.0) long else 1.0
        val cx = (maxX + minX) / 2
        val cy = (maxY + minY) / 2
        val scale = FILL * GRID
        val half = GRID / 2.0
        val out = FloatArray(SIZE)
        val plane = GRID * GRID

        // Direction channels: each step of the resampled path adds its length to its cell, split between the two
        // neighbouring directions by angle. All first shares are added before the second ones, as the script does.
        val qx = DoubleArray(RESAMPLE)
        val qy = DoubleArray(RESAMPLE)
        resample(xs, ys, qx, qy)
        for (i in 0 until RESAMPLE) {
            qx[i] = (qx[i] - cx) / side * scale + half
            qy[i] = (qy[i] - cy) / side * scale + half
        }
        val steps = RESAMPLE - 1
        val cell = IntArray(steps)
        val first = IntArray(steps)
        val len = DoubleArray(steps)
        val frac = DoubleArray(steps)
        val sector = 2 * PI / DIRECTIONS
        for (i in 0 until steps) {
            val dx = qx[i + 1] - qx[i]
            val dy = qy[i + 1] - qy[i]
            len[i] = hypot(dx, dy)
            cell[i] = cellOf((qx[i + 1] + qx[i]) / 2, (qy[i + 1] + qy[i]) / 2)
            var a = atan2(dy, dx)
            if (a < 0) a += 2 * PI
            a /= sector
            val fl = floor(a)
            first[i] = fl.toInt() % DIRECTIONS
            frac[i] = a - fl
        }
        for (i in 0 until steps) out[first[i] * plane + cell[i]] += (len[i] * (1 - frac[i])).toFloat()
        for (i in 0 until steps) out[(first[i] + 1) % DIRECTIONS * plane + cell[i]] += (len[i] * frac[i]).toFloat()
        for (i in 0 until DIRECTIONS * plane) if (out[i] > 1f) out[i] = 1f

        // Centre channel: the raw samples (resampling leaves no short step), a step shorter than CENTRE_STEP_DP in its cell.
        val centre = DIRECTIONS * plane
        for (i in 0 until n - 1) {
            if (hypot(xs[i + 1] - xs[i], ys[i + 1] - ys[i]) >= CENTRE_STEP_DP) continue
            val ax = (xs[i] - cx) / side * scale + half
            val ay = (ys[i] - cy) / side * scale + half
            val bx = (xs[i + 1] - cx) / side * scale + half
            val by = (ys[i + 1] - cy) / side * scale + half
            out[centre + cellOf((bx + ax) / 2, (by + ay) / 2)] += CENTRE_GAIN
        }
        for (i in centre until SIZE) if (out[i] > 1f) out[i] = 1f
        return out
    }

    /** Row-major cell of a grid position: truncated, kept inside the grid. */
    private fun cellOf(x: Double, y: Double): Int =
        y.toInt().coerceIn(0, GRID - 1) * GRID + x.toInt().coerceIn(0, GRID - 1)

    /**
     * [RESAMPLE] points evenly spaced along the path by piecewise linear interpolation over the cumulative length
     * (the script's `numpy.interp` over `numpy.linspace`): position `i × step`, the last one the whole length.
     */
    private fun resample(xs: DoubleArray, ys: DoubleArray, ox: DoubleArray, oy: DoubleArray) {
        val m = xs.size
        val cum = DoubleArray(m)
        for (i in 1 until m) cum[i] = cum[i - 1] + hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1])
        val total = cum[m - 1]
        if (total == 0.0) {
            ox.fill(xs[0])
            oy.fill(ys[0])
            return
        }
        val step = total / (RESAMPLE - 1)
        var j = 0
        for (i in 0 until RESAMPLE) {
            val t = if (i == RESAMPLE - 1) total else i * step
            if (t >= total) {
                ox[i] = xs[m - 1]
                oy[i] = ys[m - 1]
                continue
            }
            while (j < m - 2 && cum[j + 1] <= t) j++   // the last knot at or before t
            val d = cum[j + 1] - cum[j]
            ox[i] = (xs[j + 1] - xs[j]) / d * (t - cum[j]) + xs[j]
            oy[i] = (ys[j + 1] - ys[j]) / d * (t - cum[j]) + ys[j]
        }
    }
}
