package kbd.app.graffiti

import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
import kbd.graffiti.recognize.StrokeRules
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayouts

/** One auxiliary bar button; [rect] in view dp. */
data class AuxKey(val key: FunctionKey, val label: String, val rect: RectF2)

/**
 * Graffiti pad layout in dp, pure (LLD-05 §8.1, FR-GRF-01). The view is [ROWS] rows high: the pad
 * is the top [PAD_ROWS] rows, split letter : number by [letterRatio] (the letter share of the width,
 * FR-SET-05), and the auxiliary bar is the bottom row: mode key, space, ⌫, ↵.
 */
class GraffitiPadGeometry(val widthDp: Float, val heightDp: Float, val letterRatio: Float) {

    val rowHeightDp: Float = heightDp / ROWS

    /** Bottom edge of the pad = top edge of the auxiliary bar. */
    val padBottomDp: Float = rowHeightDp * PAD_ROWS

    val letterRect = RectF2(0f, 0f, widthDp * letterRatio, padBottomDp)
    val numberRect = RectF2(letterRect.right, 0f, widthDp, padBottomDp)

    /**
     * The language-switch corner-tap zone drawn on the pad (FR-GRF-05a, 05d, LLD-05 §8.1): the top-left
     * [StrokeRules.CORNER_RATIO] of [letterRect], the same rectangle and ratio the recognizer tests.
     */
    val cornerRect = RectF2(
        letterRect.left,
        letterRect.top,
        letterRect.left + (letterRect.right - letterRect.left) * StrokeRules.CORNER_RATIO,
        letterRect.top + (letterRect.bottom - letterRect.top) * StrokeRules.CORNER_RATIO,
    )

    /**
     * Mode key at the tap layout's width scaled to this width (72 / 360 × width), the other three share
     * what is left equally, so the mode key sits where it does on the tap keyboards (DCR-12).
     */
    val keys: List<AuxKey> = run {
        val modeRight = widthDp * MODE_KEY.w / LAYOUT_WIDTH_DP
        val rest = (widthDp - modeRight) / 3
        val top = padBottomDp
        val bottom = heightDp
        listOf(
            AuxKey(FunctionKey.MODE, MODE_KEY.label, RectF2(0f, top, modeRight, bottom)),
            AuxKey(FunctionKey.SPACE, label(FunctionKey.SPACE), RectF2(modeRight, top, modeRight + rest, bottom)),
            AuxKey(FunctionKey.BACKSPACE, label(FunctionKey.BACKSPACE), RectF2(modeRight + rest, top, modeRight + 2 * rest, bottom)),
            AuxKey(FunctionKey.ENTER, label(FunctionKey.ENTER), RectF2(modeRight + 2 * rest, top, widthDp, bottom)),
        )
    }

    fun inPad(yDp: Float): Boolean = yDp < padBottomDp

    /** The area a stroke starting at ([xDp], [yDp]) belongs to (GRAFFITI 6.3); the y is not used. */
    @Suppress("UNUSED_PARAMETER")
    fun areaAt(xDp: Float, yDp: Float): PadArea = if (xDp < letterRect.right) PadArea.LETTER else PadArea.NUMBER

    fun rectOf(area: PadArea): RectF2 = if (area == PadArea.LETTER) letterRect else numberRect

    /** The auxiliary bar button under [xDp]; null above the bar. Outside the width it is the nearest end button. */
    fun keyAt(xDp: Float, yDp: Float): AuxKey? {
        if (inPad(yDp)) return null
        return keys.firstOrNull { xDp < it.rect.right } ?: keys.last()
    }

    companion object {
        /** Pad and auxiliary bar together (FR-GRF-01). */
        const val ROWS = 5
        const val PAD_ROWS = 4

        /** Tap layout row width in dp units (LLD-05A §3.2). */
        private const val LAYOUT_WIDTH_DP = 360f

        private val MODE_KEY: TapKeyDef = TapLayouts.QWERTY.keys.first { it.function == FunctionKey.MODE }

        /** Same labels as the tap keyboards' function keys. */
        private fun label(f: FunctionKey): String = TapLayouts.QWERTY.keys.first { it.function == f }.label
    }
}
