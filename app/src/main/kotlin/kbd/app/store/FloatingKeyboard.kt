package kbd.app.store

import kotlin.math.roundToInt

/**
 * One screen orientation's floating setting (SRS FR-CMN-15 ①⑤, FR-SET-09, LLD-07 §6.1, FSS#86): [on] floats the
 * keyboard, [fx] and [fy] are where it was last put, as shares 0–1 of the range it can move in (0 = left or top end,
 * 1 = right or bottom end). null = never put on that axis: the alignment's place across, the bottom end down (⑥).
 */
data class FloatSetting(val on: Boolean, val fx: Float?, val fy: Float?) {
    companion object {
        val DEFAULT = FloatSetting(false, null, null)
    }
}

/**
 * A floating keyboard's place in the input view, px (FR-CMN-15 ③④): [area] is the rectangle S the plate may be in,
 * [plate] the keyboard plate (move handle + keyboard rectangle), wholly inside [area].
 */
data class FloatPlace(val area: PxRect, val plate: PxRect) {
    /** How far the plate can move across and down, px (FR-CMN-15 ⑤); 0 = it does not move on that axis. */
    val rangeX: Int get() = area.width - plate.width
    val rangeY: Int get() = area.height - plate.height
}

/** The shares of a place (FR-CMN-15 ⑤); null on an axis whose share is not to be stored. */
data class FloatRatio(val fx: Float?, val fy: Float?)

/**
 * Where a floating keyboard is (SRS FR-CMN-15 ④~⑨, HLD 5A "띄운 자판", LLD-08 §7.4, FSS#86). No Android: unit tested.
 * The docked keyboard's plate is [KeyboardPlate.rect]'s; this gives the plate only when the keyboard floats.
 */
object FloatingKeyboard {
    /** Height of the move handle above the keyboard rectangle (SRS 1.3); not the row height setting's. */
    const val HANDLE_DP = 24f

    /** A finger on the handle that goes this far across or down before the long press time is dropped (SRS 1.3). */
    const val GRAB_SLOP_DP = 8f

    /** A stored share cut to 0–1 (FR-CMN-15 ⑤); a value that is not a number reads as none. */
    fun ratio(v: Float?): Float? = v?.takeIf { !it.isNaN() }?.coerceIn(0f, 1f)

    /**
     * The floating place in a view of [viewWidth] × [viewHeight] px, or null when the keyboard is docked there: the
     * setting [s] is off, or the area is lower than the plate (FR-CMN-15 ①④).
     *
     * The area is the view less [left], [top], [right] and [bottom] (the system bar padding at the sides and the
     * bottom, the status bar or cutout at the top), so it is as wide as the available width [span] was placed in. The
     * plate is [span]'s width by [plateHeight]. Across it is at the stored share of the range, or with none stored at
     * [span]'s own left (the alignment's place, to the px of the docked keyboard); down at the stored share, or with
     * none stored at the bottom end (⑥). A share times the range is rounded to a px and kept inside the range, so the
     * whole plate is in the area.
     */
    fun place(
        viewWidth: Int,
        viewHeight: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        span: KeyboardSpan,
        plateHeight: Int,
        s: FloatSetting,
    ): FloatPlace? {
        if (!s.on || span.width <= 0 || plateHeight <= 0) return null
        val area = PxRect(left, top, viewWidth - right, viewHeight - bottom)
        val rangeX = area.width - span.width
        val rangeY = area.height - plateHeight
        if (rangeX < 0 || rangeY < 0) return null
        val x = ratio(s.fx)?.let { at(it, rangeX) } ?: span.left.coerceIn(0, rangeX)
        val y = at(ratio(s.fy) ?: 1f, rangeY)
        return FloatPlace(area, PxRect(left + x, top + y, left + x + span.width, top + y + plateHeight))
    }

    /**
     * The plate that was at [start] when the move began, the finger now ([dx], [dy]) px from where it was then, cut
     * into [area] on each axis by itself (FR-CMN-15 ⑧). The cut is of the sum, so a finger that went past an end and
     * comes back takes the plate along from where the finger is; an axis with no range stays.
     */
    fun drag(start: PxRect, dx: Int, dy: Int, area: PxRect): PxRect {
        val x = (start.left + dx).coerceIn(area.left, maxOf(area.left, area.right - start.width))
        val y = (start.top + dy).coerceIn(area.top, maxOf(area.top, area.bottom - start.height))
        return PxRect(x, y, x + start.width, y + start.height)
    }

    /**
     * The shares of [plate] in [area] (FR-CMN-15 ⑤): null on an axis with no range. [place] with these shares gives
     * [plate] back to the px: a share is off by less than a part in ten million, far under half a px of any range a
     * screen has.
     */
    fun ratios(plate: PxRect, area: PxRect): FloatRatio {
        val rangeX = area.width - plate.width
        val rangeY = area.height - plate.height
        return FloatRatio(
            fx = if (rangeX > 0) ((plate.left - area.left).toFloat() / rangeX).coerceIn(0f, 1f) else null,
            fy = if (rangeY > 0) ((plate.top - area.top).toFloat() / rangeY).coerceIn(0f, 1f) else null,
        )
    }

    private fun at(f: Float, range: Int): Int = (f.toDouble() * range).roundToInt().coerceIn(0, range)
}
