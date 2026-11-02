package kbd.stroke.threshold

/**
 * Judgement thresholds (LLD-03 §3.4, HLD 7.2 format 5). Distances are absolute dp, never scaled
 * by key size (FR-CMN-12). [keySizeWDp]/[keySizeHDp] record the key size at measurement time and
 * only drive the re-measure suggestion, never judgement.
 *
 * Swipe input has no time threshold (issue #168: double consonants have their own keys, so the
 * double-tap window `doubleTapWindowMs` of formats 3 and before is gone). Issue #173: the one-stroke
 * compound-vowel thresholds `addIDp`, `returnLongDp`, `turnAngleDeg` went with the edge-crossing vowel
 * rules (format 5).
 */
data class Thresholds(
    val minCrossDp: Float, val longDp: Float,
    val guideDelayMs: Long,
    /** Position tolerance t of the vowel strokes' relations (SWIPE 3.6, SRS 1.3): fixed, not measured (issue #173). */
    val posTolDp: Float,
    val keySizeWDp: Float, val keySizeHDp: Float,
)

/** HLD 7.2 example values; replaced after tutor measurement (FR-TUT-05, TBD-6). */
object ThresholdDefaults {
    /**
     * The tap / stroke boundary (owner decision 2026-11-02, issue #217): fixed at the platform touch slop, never
     * measured by the tutor nor read from a stored file. A tap in place stays a tap; anything drawn further is a
     * stroke whatever its length (a vowel stroke on the swipe consonant keys; an edge crossing elsewhere).
     */
    const val MIN_CROSS_DP = 8f

    val VALUES: Thresholds = Thresholds(
        minCrossDp = MIN_CROSS_DP, longDp = 51f,
        guideDelayMs = 120L,
        posTolDp = 20f,   // key width 51 dp × 0.4
        keySizeWDp = 51f, keySizeHDp = 48f,
    )
}
