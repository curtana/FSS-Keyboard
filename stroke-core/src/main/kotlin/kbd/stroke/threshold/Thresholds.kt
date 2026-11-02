package kbd.stroke.threshold

/**
 * Judgement thresholds (LLD-03 §3.4, HLD 7.2). All are fixed code constants ([ThresholdDefaults], FSS#17: nothing is
 * measured or stored). Distances are absolute dp, never scaled by key size (FR-CMN-12) — except [posTolDp], which the
 * swipe keyboard sets at run time from its drawn key width ([ThresholdDefaults.posTolFor], PM decision, FSS#17).
 * [longDp], [keySizeWDp] and [keySizeHDp] are kept for the debug log and tests only; nothing judges with them.
 *
 * Swipe input has no time threshold (issue #168: double consonants have their own keys, so the
 * double-tap window `doubleTapWindowMs` of formats 3 and before is gone). Issue #173: the one-stroke
 * compound-vowel thresholds `addIDp`, `returnLongDp`, `turnAngleDeg` went with the edge-crossing vowel
 * rules (format 5).
 */
data class Thresholds(
    val minCrossDp: Float, val longDp: Float,
    val guideDelayMs: Long,
    /** Position tolerance t of the vowel strokes' relations (SWIPE 3.6, SRS 1.3): key width × 0.4 at run time, this value until then. */
    val posTolDp: Float,
    val keySizeWDp: Float, val keySizeHDp: Float,
)

/** HLD 7.2 fixed values (FSS#17: the input practice measures nothing, there is no stored file). */
object ThresholdDefaults {
    /**
     * The tap / stroke boundary (owner decision 2026-11-02, issue #217): fixed at the platform touch slop, never
     * measured nor read from a stored file. A tap in place stays a tap; anything drawn further is a
     * stroke whatever its length (a vowel stroke on the swipe consonant keys; an edge crossing elsewhere).
     */
    const val MIN_CROSS_DP = 8f

    val VALUES: Thresholds = Thresholds(
        minCrossDp = MIN_CROSS_DP, longDp = 51f,
        guideDelayMs = 120L,
        posTolDp = 20f,   // key width 51 dp × 0.4, until the swipe keyboard knows its own (posTolFor)
        keySizeWDp = 51f, keySizeHDp = 48f,
    )

    /** t = key width × 0.4 (SRS 1.3, HLD 7.2). */
    const val POS_TOL_RATIO = 0.4f

    /** The position tolerance for swipe keys [keyWidthDp] wide, as drawn now (PM decision 2026-11-02, FSS#17). */
    fun posTolFor(keyWidthDp: Float): Float = keyWidthDp * POS_TOL_RATIO
}
