package kbd.app.store

import kotlin.math.ceil
import kotlin.math.roundToInt

/** Where a narrowed keyboard sits in the available width (SRS FR-CMN-14 ②): 왼손모드, 중앙모드, 오른손모드. */
enum class KeyboardAlign { LEFT, CENTER, RIGHT }

/**
 * One screen orientation's keyboard size setting (FR-SET-09, LLD-07 §6.1): [keyDp] is the width of one key cell, the
 * keyboard being [KeyboardWidth.UNITS] cells wide (FR-CMN-14 ②). null = the default: the full width on the portrait
 * screen, the portrait key size on the landscape screen (FR-CMN-14 ③).
 */
data class WidthSetting(val keyDp: Float?, val align: KeyboardAlign) {
    companion object {
        val DEFAULT = WidthSetting(null, KeyboardAlign.CENTER)
    }
}

/** The keyboard's place in the available width, in px from the left end of that width. */
data class KeyboardSpan(val left: Int, val width: Int)

/** Keyboard width from the key size, and alignment (SRS FR-CMN-14, HLD 5A, LLD-08 §7.2). No Android: unit tested. */
object KeyboardWidth {
    /** Key cells in a row: the layout unit 360 is ten 36 dp letter keys (FR-CMN-14 ②). */
    const val UNITS = 10

    /** The smallest key, the keyboard then being 320 dp (FR-CMN-14 ④). */
    const val MIN_KEY_DP = 32f

    /** Upper end for stored values (a 1600 dp wide screen); the keyboard stops at the available width anyway. */
    const val MAX_KEY_DP = 160f

    /** The keyboard is never narrower than this, unless the available width is (FR-CMN-14 ④). */
    const val MIN_DP = MIN_KEY_DP * UNITS

    /**
     * "꽉 채움" as a key size: the slider's top stop is stored as this mark, not as a number, so the keyboard still fills
     * after a rotation or a system bar change (FR-SET-09).
     */
    const val FULL = Float.POSITIVE_INFINITY

    /** A stored key size to whole dp inside [MIN_KEY_DP]..[MAX_KEY_DP] (1 dp steps, FR-SET-09; as D-33); [FULL] stays. */
    fun snap(k: Float): Float = if (k == FULL) FULL else k.roundToInt().toFloat().coerceIn(MIN_KEY_DP, MAX_KEY_DP)

    /** The slider's top stop for an available width of about [availDp]: the first whole key size that fills it. */
    fun fullKeyDp(availDp: Float): Int = ceil(availDp / UNITS - 1e-3f).toInt().coerceIn(MIN_KEY_DP.toInt(), MAX_KEY_DP.toInt())

    /**
     * The keyboard in an available width of [availPx] (FR-CMN-14 ①③④⑤). The orientation's key size gives a width of
     * [UNITS] keys, [FULL] the whole of [availPx]; with no key size the portrait keyboard fills [availPx] and the
     * landscape one takes the portrait key size ([portrait]'s, or a tenth of [portraitAvailPx], the screen's short side,
     * when that is the full width).
     * The width is at least [MIN_DP] and at most [availPx]; the alignment leaves the rest on the right (LEFT), on both
     * sides (CENTER) or on the left (RIGHT).
     */
    fun place(
        availPx: Int,
        portraitAvailPx: Int,
        density: Float,
        portrait: WidthSetting,
        landscape: WidthSetting,
        isLandscape: Boolean,
    ): KeyboardSpan {
        if (availPx <= 0) return KeyboardSpan(0, 0)
        val s = if (isLandscape) landscape else portrait
        fun keys(keyDp: Float, full: Int) = if (keyDp == FULL) full else (keyDp * UNITS * density).roundToInt()
        val wanted = when {
            s.keyDp != null -> keys(s.keyDp, availPx)
            !isLandscape -> availPx
            portrait.keyDp != null -> keys(portrait.keyDp, portraitAvailPx)
            else -> portraitAvailPx
        }
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
