package kbd.app.store

import kotlin.math.roundToInt

/** Where a narrowed keyboard sits in the available width (SRS FR-CMN-14 ②): 왼손모드, 중앙모드, 오른손모드. */
enum class KeyboardAlign { LEFT, CENTER, RIGHT }

/**
 * One screen orientation's keyboard width setting (FR-SET-09, LLD-07 §6.1). [ratio] is the share of the available width
 * (0.50–1.00 in 0.05 steps); null = the same key size as the portrait screen (the landscape default, FR-CMN-14 ③).
 */
data class WidthSetting(val ratio: Float?, val align: KeyboardAlign) {
    companion object {
        /** Portrait default: the full width (FR-CMN-14 ③). */
        val PORTRAIT_DEFAULT = WidthSetting(1f, KeyboardAlign.CENTER)

        /** Landscape default: the portrait key size, centred (FR-CMN-14 ③). */
        val LANDSCAPE_DEFAULT = WidthSetting(null, KeyboardAlign.CENTER)
    }
}

/** The keyboard's place in the available width, in px from the left end of that width. */
data class KeyboardSpan(val left: Int, val width: Int)

/** Keyboard width and alignment (SRS FR-CMN-14, HLD 5A, LLD-08 §7.2). No Android: unit tested. */
object KeyboardWidth {
    const val MIN_PERCENT = 50
    const val MAX_PERCENT = 100
    const val STEP_PERCENT = 5

    /** The keyboard is never narrower than this, unless the available width is (FR-CMN-14 ④). */
    const val MIN_DP = 320f

    /** The selectable ratios, 0.50 to 1.00 (FR-SET-09). */
    val RATIOS: List<Float> = (MIN_PERCENT..MAX_PERCENT step STEP_PERCENT).map { it / 100f }

    /** A stored ratio to the nearest selectable one; out-of-range values go to the nearest end (as D-33). */
    fun snap(r: Float): Float {
        val pct = (r * 100).coerceIn(MIN_PERCENT.toFloat(), MAX_PERCENT.toFloat())
        return ((pct / STEP_PERCENT).roundToInt() * STEP_PERCENT) / 100f
    }

    /**
     * The landscape width choices (FR-SET-09): [RATIOS] with the default (null, the portrait key size) put where its
     * approximate share [approx] of the landscape width falls, first when that is below 50 %.
     */
    fun landscapeChoices(approx: Float): List<Float?> {
        val i = RATIOS.count { it < approx }
        return RATIOS.subList(0, i) + listOf(null) + RATIOS.subList(i, RATIOS.size)
    }

    /**
     * The keyboard in an available width of [availPx] (FR-CMN-14 ①③④⑤). [portraitPx] is the portrait available width
     * (the screen's short side), used when the ratio is null. The width is at least [MIN_DP] and at most [availPx];
     * the alignment leaves the rest on the right (LEFT), on both sides (CENTER) or on the left (RIGHT).
     */
    fun place(availPx: Int, portraitPx: Int, density: Float, s: WidthSetting): KeyboardSpan {
        if (availPx <= 0) return KeyboardSpan(0, 0)
        val wanted = s.ratio?.let { (availPx * it).roundToInt() } ?: portraitPx
        val width = maxOf(wanted, (MIN_DP * density).roundToInt()).coerceAtMost(availPx)
        val rest = availPx - width
        val left = when (s.align) {
            KeyboardAlign.LEFT -> 0
            KeyboardAlign.CENTER -> rest / 2
            KeyboardAlign.RIGHT -> rest
        }
        return KeyboardSpan(left, width)
    }
}
