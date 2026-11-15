package kbd.stroke.threshold

/**
 * Judgement thresholds (LLD-03 §3.4, HLD 7.2): fixed code constants ([ThresholdDefaults], FSS#17: nothing is
 * measured or stored). Distances are absolute dp, never scaled by key size (FR-CMN-12).
 */
data class Thresholds(
    /** How far past a key's edge a stroke must go to cross it (SRS 1.3, FR-SWP-03). */
    val minCrossDp: Float,
)

/** HLD 7.2 fixed values (FSS#17: the input practice measures nothing, there is no stored file). */
object ThresholdDefaults {
    /**
     * The minimum crossing distance (owner decision 2026-11-02, issue #217): fixed at the platform touch slop, never
     * measured nor read from a stored file.
     */
    const val MIN_CROSS_DP = 8f

    val VALUES: Thresholds = Thresholds(minCrossDp = MIN_CROSS_DP)
}
