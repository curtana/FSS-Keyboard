package kbd.app.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kbd.app.ime.FieldInfo
import kbd.hangul.stroke.DotPlace

/**
 * What the place C shows (SWIPE 9.5, FR-DSP-10, FSS#36): [text] = the composing syllable as the editor shows it
 * ("ㄱ", "가", "각", "ㄱㆍ"); [other] = while a lone first stroke keeps both roles, the syllable of its other role,
 * drawn small in brackets, with a dot at [otherDot] beside its consonant when that role is a dot ("그(ㄱ·)").
 */
data class CenterText(val text: String, val other: String? = null, val otherDot: DotPlace? = null)

/**
 * Draws the center syllable (SWIPE 9.5, FR-DSP-10, issue #180 and its follow-up, FSS#36) on the swipe keyboard (the
 * input practice shows the same view, FSS#17): the composing syllable, faint, in the place C at the top of the
 * consonant key area half a row down, centred across (`SwipeLayout.consonantBox`, issue #213, FSS#37; the same box the
 * vowel strokes are judged against), and the bracket of a first stroke's other role small just above C. Called after the
 * keys and before the trail (keys < center syllable < trail < pop-up). The class keeps its 0.21 name.
 * Display only. What to draw and how big are pure functions ([textFor], [sizeFor], [otherBox]) for the unit tests.
 */
class CenterConsonantPainter {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR
        textAlign = Paint.Align.CENTER
    }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR
        textAlign = Paint.Align.LEFT
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR }
    private val otherRect = RectF()

    /** Draws [t] (from [textFor]; null = nothing) centred in the place [box] (px), its bracket above it. */
    fun draw(canvas: Canvas, t: CenterText?, box: RectF) {
        if (t == null || t.text.isEmpty() || box.width() <= 0f || box.height() <= 0f) return
        paint.textSize = 1f
        paint.textSize = sizeFor(box.width(), box.height(), paint.measureText(t.text))
        canvas.drawText(t.text, box.centerX(), box.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
        val other = t.other ?: return
        otherBox(box, otherRect)
        drawOther(canvas, other, t.otherDot, otherRect)
    }

    /** "(" [other] ")" fitted in [r], a dot under or right of [other] when the other role is a dot (owner 2026-11-02, FSS#36). */
    private fun drawOther(canvas: Canvas, other: String, dot: DotPlace?, r: RectF) {
        small.textSize = 1f
        val gapUnit = if (dot == DotPlace.RIGHT) DOT_GAP else 0f
        val unit = small.measureText(OPEN) + small.measureText(other) + gapUnit + small.measureText(CLOSE)
        val size = sizeFor(r.width(), r.height(), unit)
        small.textSize = size
        val y = r.centerY() - (small.ascent() + small.descent()) / 2
        var x = r.centerX() - unit * size / 2
        canvas.drawText(OPEN, x, y, small)
        x += small.measureText(OPEN)
        canvas.drawText(other, x, y, small)
        val w = small.measureText(other)
        val radius = size * DOT_RADIUS
        when (dot) {
            DotPlace.UNDER -> canvas.drawCircle(x + w / 2, r.centerY() + size * DOT_UNDER, radius, dotPaint)
            DotPlace.RIGHT -> canvas.drawCircle(x + w + size * DOT_GAP / 2, r.centerY(), radius, dotPaint)
            null -> Unit
        }
        x += w + size * gapUnit
        canvas.drawText(CLOSE, x, y, small)
    }

    companion object {
        /** The label colour (white) at the trail's 40 % (0x66, SWIPE 9.5 "획 자국과 같은 반투명"). */
        const val COLOR = TrailPainter.COLOR

        private const val OPEN = "("
        private const val CLOSE = ")"

        /** The bracket's place: a quarter of C high (half a row, the room C was moved down by, FSS#37), right above C, 1.5 × C wide. */
        const val OTHER_HEIGHT = 0.25f
        const val OTHER_WIDTH = 1.5f

        /** The bracket's dot: radius, its gap right of the consonant (ㅓ·ㅕ's dot), its drop under the middle (ㅗ·ㅛ's dot), × text size. */
        const val DOT_RADIUS = 0.08f
        const val DOT_GAP = 0.3f
        const val DOT_UNDER = 0.5f

        /**
         * The text to draw: [t] (the host's `centerText`), or null when there is none or [field] is a password field
         * (FR-CMN-08 ①). A no-learning field draws it (②, owner 2026-11-01), and no guide setting turns it off
         * (FR-DSP-08). [field] null = no input field.
         */
        fun textFor(t: CenterText?, field: FieldInfo?): CenterText? = t?.takeIf { field?.hidesDisplay != true }

        /** Text size in px: the place's height, shrunk so text [unitWidth] px wide at size 1 fits its width. */
        fun sizeFor(boxWidth: Float, boxHeight: Float, unitWidth: Float): Float {
            var size = boxHeight
            if (unitWidth > 0f) size = minOf(size, boxWidth / unitWidth)
            return size
        }

        /** Sets [out] to the bracket's place for C = [box]: centred across, its bottom on C's top. */
        fun otherBox(box: RectF, out: RectF) {
            val h = box.height() * OTHER_HEIGHT
            val w = box.width() * OTHER_WIDTH
            out.set(box.centerX() - w / 2, box.top - h, box.centerX() + w / 2, box.top)
        }
    }
}
