package kbd.app.tutor

import kotlin.math.floor
import kotlin.math.roundToLong

/** Result of one tutor threshold estimate (LLD-07 §3). */
sealed interface EstimateResult {
    data class Ok(val value: Float) : EstimateResult
    /** Short p90 ≥ long p10: the two distributions overlap (FR-TUT-03). */
    data object Overlap : EstimateResult
    /** Fewer than [ThresholdEstimator.MIN_SAMPLES] samples in a list. */
    data object NotEnough : EstimateResult
}

/** Tutor threshold formulas, pure functions (LLD-07 §3, HLD 7.1). */
object ThresholdEstimator {
    /** Minimum samples per list (D-18, `minSamplesPerTarget`). */
    const val MIN_SAMPLES = 10
    /** Jitter margin initial value (HLD 7.1). */
    const val JITTER_MARGIN_DP = 4f

    /** 1-1: a touch more than this past the key rectangle gives no jitter sample (D-51). */
    const val JITTER_OUTLIER_DP = 10f

    /** Boundary between a short and a long distribution: `(p90(short) + p10(long)) / 2`. */
    fun boundary(short: List<Float>, long: List<Float>): EstimateResult {
        if (short.size < MIN_SAMPLES || long.size < MIN_SAMPLES) return EstimateResult.NotEnough
        val s90 = percentile(short, 90f)
        val l10 = percentile(long, 10f)
        if (s90 >= l10) return EstimateResult.Overlap
        return EstimateResult.Ok((s90 + l10) / 2)
    }

    /**
     * The boundary formula's value even when the two sides overlap (SWIPE 10: after the extra rounds the
     * formula's value is kept and the overlap recorded, issue #173). Needs both sides non-empty.
     */
    fun midpoint(short: List<Float>, long: List<Float>): Float = (percentile(short, 90f) + percentile(long, 10f)) / 2

    /** 1-1 minimum cross distance: `p99(jitter) + marginDp`. One distribution, no overlap check. */
    fun oneSidedDistance(jitter: List<Float>, marginDp: Float = JITTER_MARGIN_DP): EstimateResult {
        if (jitter.size < MIN_SAMPLES) return EstimateResult.NotEnough
        return EstimateResult.Ok(percentile(jitter, 99f) + marginDp)
    }

    /** Linear-interpolation percentile on sorted values (D-20). [p] is 0..100. */
    fun percentile(xs: List<Float>, p: Float): Float {
        require(xs.isNotEmpty()) { "percentile of empty list" }
        val sorted = xs.sorted()
        if (sorted.size == 1) return sorted[0]
        val h = (sorted.size - 1) * p.toDouble() / 100.0
        val lo = floor(h).toInt().coerceIn(0, sorted.size - 1)
        val hi = (lo + 1).coerceAtMost(sorted.size - 1)
        return (sorted[lo] + (h - lo) * (sorted[hi] - sorted[lo])).toFloat()
    }

    /** Millisecond values are stored rounded to `Long` (LLD-07 §3). */
    fun toStoredMs(ms: Float): Long = ms.roundToLong()
}
