package kbd.app.tutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §7.5 TE-01..TE-10. */
class ThresholdEstimatorTest {

    private val eps = 1e-3f

    private fun range(from: Int, to: Int, step: Int = 1): List<Float> =
        (from..to step step).map { it.toFloat() }

    private fun okValue(r: EstimateResult): Float {
        assertTrue("expected Ok, got $r", r is EstimateResult.Ok)
        return (r as EstimateResult.Ok).value
    }

    @Test
    fun `TE-01 boundary of separated distributions is midpoint of short p90 and long p10`() {
        val short = range(1, 10)
        val long = range(20, 30)
        // p90([1..10]) = 9.1, p10([20..30]) = 21.0
        val expected = (ThresholdEstimator.percentile(short, 90f) + ThresholdEstimator.percentile(long, 10f)) / 2
        assertEquals(15.05f, expected, eps)
        assertEquals(expected, okValue(ThresholdEstimator.boundary(short, long)), eps)
    }

    @Test
    fun `TE-02 boundary is Overlap when short p90 reaches long p10`() {
        val short = range(1, 20)   // p90 = 18.1
        val long = range(10, 30)   // p10 = 12.0
        assertEquals(EstimateResult.Overlap, ThresholdEstimator.boundary(short, long))
        // equality also counts as overlap (s90 ≥ l10)
        val same = List(10) { 5f }
        assertEquals(EstimateResult.Overlap, ThresholdEstimator.boundary(same, same))
    }

    @Test
    fun `TE-03 boundary is NotEnough when either list has fewer than MIN_SAMPLES`() {
        assertEquals(10, ThresholdEstimator.MIN_SAMPLES)
        assertEquals(EstimateResult.NotEnough, ThresholdEstimator.boundary(range(1, 9), range(20, 30)))
        assertEquals(EstimateResult.NotEnough, ThresholdEstimator.boundary(range(1, 10), range(20, 28)))
        assertEquals(EstimateResult.NotEnough, ThresholdEstimator.boundary(emptyList(), emptyList()))
    }

    @Test
    fun `TE-04 percentile interpolates linearly`() {
        assertEquals(3.7f, ThresholdEstimator.percentile(listOf(1f, 2f, 3f, 4f), 90f), eps)
    }

    @Test
    fun `TE-05 percentile of a single value is that value`() {
        assertEquals(5f, ThresholdEstimator.percentile(listOf(5f), 10f), 0f)
    }

    @Test
    fun `TE-06 percentile sorts unsorted input first`() {
        val sorted = listOf(1f, 2f, 3f, 4f)
        val unsorted = listOf(3f, 1f, 4f, 2f)
        for (p in listOf(0f, 10f, 50f, 90f, 100f)) {
            assertEquals(ThresholdEstimator.percentile(sorted, p), ThresholdEstimator.percentile(unsorted, p), 0f)
        }
    }

    @Test
    fun `TE-07 oneSidedDistance is p99 of jitter plus margin`() {
        val jitter = range(1, 10)
        assertEquals(9.91f, ThresholdEstimator.percentile(jitter, 99f), eps)
        assertEquals(4f, ThresholdEstimator.JITTER_MARGIN_DP, 0f)
        assertEquals(13.91f, okValue(ThresholdEstimator.oneSidedDistance(jitter)), eps)
    }

    @Test
    fun `TE-08 the 2-1 double-tap window is the boundary of fast and normal gaps, stored rounded`() {
        val fast = range(60, 150, 10)       // p90 141
        val normal = range(240, 330, 10)    // p10 249
        assertEquals(195f, okValue(ThresholdEstimator.boundary(fast, normal)), eps)
        assertEquals(272L, ThresholdEstimator.toStoredMs(271.5f))
        assertEquals(EstimateResult.Overlap, ThresholdEstimator.boundary(normal.map { it - 150f }, fast.map { it + 10f }))
    }

    @Test
    fun `TE-09 one-sided estimates are NotEnough with 9 samples`() {
        val nine = range(1, 9)
        assertEquals(EstimateResult.NotEnough, ThresholdEstimator.oneSidedDistance(nine))
    }

    @Test
    fun `TE-10 oneSidedDistance has no overlap check for widely spread samples`() {
        val wide = listOf(0f, 1f, 1000f, 5f, 20000f, 3f, 70000f, 2f, 150000f, 9f)
        val r = ThresholdEstimator.oneSidedDistance(wide)
        assertTrue("expected Ok, got $r", r is EstimateResult.Ok)
        assertEquals(ThresholdEstimator.percentile(wide, 99f) + 4f, (r as EstimateResult.Ok).value, 1f)
    }
}
