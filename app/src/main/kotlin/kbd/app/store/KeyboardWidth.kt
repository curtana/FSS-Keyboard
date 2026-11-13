package kbd.app.store

import kotlin.math.ceil
import kotlin.math.roundToInt

/** Where a narrowed keyboard sits in the available width (SRS FR-CMN-14 ②): 왼손모드, 중앙모드, 오른손모드. */
enum class KeyboardAlign { LEFT, CENTER, RIGHT }

/**
 * One screen orientation's keyboard size setting (FR-SET-09, LLD-07 §6.1): [keyDp] is the width of one key cell, the
 * keyboard being [KeyboardWidth.UNITS] cells wide with half a cell of side margin on each side (FR-CMN-14 ②⑪).
 * null = the default: the full width on the portrait screen, the portrait key size on the landscape screen
 * (FR-CMN-14 ③).
 */
data class WidthSetting(val keyDp: Float?, val align: KeyboardAlign) {
    companion object {
        val DEFAULT = WidthSetting(null, KeyboardAlign.CENTER)
    }
}

/**
 * The keyboard rectangle's place in the available width, px (FR-CMN-14 ④, HLD 5A): [left] from the left end of that
 * width and [width] wide, side margins included. Each side margin is [margin] wide, so the keys are [keysWidth] wide
 * from [keysLeft]: margin + keys + margin is exactly [width].
 */
data class KeyboardSpan(val left: Int, val width: Int, val margin: Int) {
    /** Left end of the key area (the mode view). */
    val keysLeft: Int get() = left + margin

    /** Width of the key area, [KeyboardWidth.UNITS] key cells. */
    val keysWidth: Int get() = width - 2 * margin
}

/** Keyboard width from the key size, and alignment (SRS FR-CMN-14, HLD 5A, LLD-08 §7.2). No Android: unit tested. */
object KeyboardWidth {
    /** Key cells in a row: the layout unit 360 is ten 36 dp letter keys (FR-CMN-14 ②). */
    const val UNITS = 10

    /** Cells of the keyboard rectangle: the row and half a cell of side margin on each side (FR-CMN-14 ④⑪, FSS#84). */
    const val RECT_UNITS = UNITS + 1

    /** The smallest key, the keys then being 320 dp wide and the keyboard rectangle 352 dp (FR-CMN-14 ④). */
    const val MIN_KEY_DP = 32f

    /** Upper end for stored values (a 1600 dp wide screen); the keyboard stops at the available width anyway. */
    const val MAX_KEY_DP = 160f

    /** The keyboard rectangle is never narrower than this, unless the available width is (FR-CMN-14 ④). */
    const val MIN_DP = MIN_KEY_DP * RECT_UNITS

    /**
     * "꽉 채움" as a key size: the slider's top stop is stored as this mark, not as a number, so the keyboard still fills
     * after a rotation or a system bar change (FR-SET-09).
     */
    const val FULL = Float.POSITIVE_INFINITY

    /** A stored key size to whole dp inside [MIN_KEY_DP]..[MAX_KEY_DP] (1 dp steps, FR-SET-09; as D-33); [FULL] stays. */
    fun snap(k: Float): Float = if (k == FULL) FULL else k.roundToInt().toFloat().coerceIn(MIN_KEY_DP, MAX_KEY_DP)

    /**
     * The slider's top stop for an available width of about [availDp]: the first whole key size whose keyboard
     * rectangle ([RECT_UNITS] cells) fills it.
     */
    fun fullKeyDp(availDp: Float): Int = ceil(availDp / RECT_UNITS - 1e-3f).toInt().coerceIn(MIN_KEY_DP.toInt(), MAX_KEY_DP.toInt())

    /**
     * The keyboard rectangle in an available width of [availPx] (FR-CMN-14 ①③④⑤⑪). The orientation's key size gives a
     * rectangle of [RECT_UNITS] cells, [FULL] the whole of [availPx]; with no key size the portrait rectangle fills
     * [availPx] and the landscape one takes the portrait key size ([portrait]'s, or an eleventh of [portraitAvailPx],
     * the screen's short side, when that is the full width). A key size stored before the side margins is read as it
     * is: the keys keep their width and the rectangle is one cell wider.
     * The rectangle is at least [MIN_DP] and at most [availPx] wide; the alignment leaves the rest on the right (LEFT),
     * on both sides (CENTER) or on the left (RIGHT). Each side margin is the rectangle's width / 22 rounded to a px and
     * the keys get what is left, so the two margins are equal and margin + keys + margin is the rectangle to the px.
     */
    fun place(
        availPx: Int,
        portraitAvailPx: Int,
        density: Float,
        portrait: WidthSetting,
        landscape: WidthSetting,
        isLandscape: Boolean,
    ): KeyboardSpan {
        if (availPx <= 0) return KeyboardSpan(0, 0, 0)
        val s = if (isLandscape) landscape else portrait
        fun keys(keyDp: Float, full: Int) = if (keyDp == FULL) full else (keyDp * RECT_UNITS * density).roundToInt()
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
        return KeyboardSpan(left, width, (width / (2f * RECT_UNITS)).roundToInt())
    }
}

/** A rectangle in px: [left] and [top] are inside, [right] and [bottom] are the first px outside. */
data class PxRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val isEmpty: Boolean get() = right <= left || bottom <= top

    /** True when the px at ([x], [y]) is in the rectangle. */
    fun contains(x: Int, y: Int): Boolean = x >= left && x < right && y >= top && y < bottom

    companion object {
        val EMPTY = PxRect(0, 0, 0, 0)
    }
}

/**
 * The keyboard plate (SRS FR-CMN-14 ⑫, HLD 5A, LLD-08 §7.3, FSS#85): the one rectangle of the input view that the
 * keyboard paints and that takes touches. What the input view has outside it is left clear: not painted, and not in
 * the window's touch region, so that the app below can show and be touched there. Painting and the touch region
 * read the rectangle this gives and work out no place of their own, so the two cannot differ. No Android: unit
 * tested.
 */
object KeyboardPlate {
    /**
     * The plate in a view of [viewWidth] × [viewHeight] px whose system bar padding is [padLeft] and [padRight] wide
     * and whose keyboard rectangle is [span] (in the width between the two paddings, [KeyboardWidth.place]).
     *
     * Left and right are the keyboard rectangle's; a side where the rectangle reaches the end of the available width
     * (none of the rest on that side, in the px the rectangle is placed with) goes on over that side's padding to the
     * view's edge, so a keyboard set against a side bar is one plate with it. Top and bottom are the view's: the rows
     * and the bottom padding under them. A full-width keyboard's plate is the whole view.
     */
    fun rect(viewWidth: Int, viewHeight: Int, padLeft: Int, padRight: Int, span: KeyboardSpan): PxRect {
        if (viewWidth <= 0 || viewHeight <= 0) return PxRect.EMPTY
        val restLeft = span.left
        val restRight = viewWidth - padLeft - padRight - span.left - span.width
        val left = if (restLeft <= 0) 0 else padLeft + span.left
        val right = if (restRight <= 0) viewWidth else padLeft + span.left + span.width
        return PxRect(left, 0, right, viewHeight)
    }
}
