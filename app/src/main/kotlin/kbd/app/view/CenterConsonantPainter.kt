package kbd.app.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import kbd.app.ime.FieldInfo
import kbd.hangul.stroke.DotPlace

/**
 * What the place C shows (SWIPE 9.5, FR-DSP-10, owner decisions 2026-11-02, FSS#36): [text] = the one composing syllable
 * ("ㄱ", "가", "각"); [dots] = vowel strokes that make no vowel yet, drawn as dots in place beside it (ㄱ and one dot
 * under it on the way to 고, two side by side for 교; one dot right of it for 거, two one above the other for 겨).
 * No candidates are shown.
 */
data class CenterText(val text: String, val dots: List<DotPlace> = emptyList())

/**
 * Draws the center syllable (SWIPE 9.5, FR-DSP-10, issue #180 and its follow-up, FSS#36) on the swipe keyboard (the
 * input practice shows the same view, FSS#17): the composing syllable, faint, in the place C at the top of the
 * consonant key area half a row down, centred across (`SwipeLayout.consonantBox`, issue #213, FSS#37; the same box the
 * vowel strokes are judged against), with its pending dots in place. Called after the keys and before the trail
 * (keys < center syllable < trail < pop-up). The class keeps its 0.21 name.
 * Display only. What to draw and where are pure functions ([textFor], [sizeFor], [textBox], [dotCenters]) for the unit tests.
 */
class CenterConsonantPainter {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR
        textAlign = Paint.Align.CENTER
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR }
    private val textRect = RectF()
    private val dotAt = ArrayList<PointF>()

    /** Draws [t] (from [textFor]; null = nothing) in the place [box] (px): the syllable, then its dots. */
    fun draw(canvas: Canvas, t: CenterText?, box: RectF) {
        if (t == null || t.text.isEmpty() || box.width() <= 0f || box.height() <= 0f) return
        textBox(box, t.dots, textRect)
        paint.textSize = 1f
        paint.textSize = sizeFor(textRect.width(), textRect.height(), paint.measureText(t.text))
        canvas.drawText(t.text, textRect.centerX(), textRect.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
        dotCenters(box, t.dots, dotAt)
        val r = minOf(box.width(), box.height()) * DOT_RADIUS
        for (p in dotAt) canvas.drawCircle(p.x, p.y, r, dotPaint)
    }

    companion object {
        /** The label colour (white) at the trail's 40 % (0x66, SWIPE 9.5 "획 자국과 같은 반투명"). */
        const val COLOR = TrailPainter.COLOR

        /** Share of C the syllable keeps beside its dots, like a consonant beside its vowel: the top part (dots under) or the left part (dots right). */
        const val TEXT_SHARE = 0.7f

        /** Dot centres, as shares of C: under = this far down, right = this far across; two dots this far apart. */
        const val DOT_LINE = 0.85f
        const val DOT_GAP = 0.3f

        /** Dot radius = the smaller side of C × this (≈ 3 dp on the 82 × 96 dp place). */
        const val DOT_RADIUS = 0.04f

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

        /** Sets [out] to where the syllable goes in C = [box]: all of it, or its top / left [TEXT_SHARE] when dots stand under / right. */
        fun textBox(box: RectF, dots: List<DotPlace>, out: RectF) {
            out.set(box)
            when (dots.firstOrNull()) {
                DotPlace.UNDER -> out.bottom = box.top + box.height() * TEXT_SHARE
                DotPlace.RIGHT -> out.right = box.left + box.width() * TEXT_SHARE
                null -> Unit
            }
        }

        /**
         * Sets [out] to the dots' centres in C = [box] (owner 2026-11-02, FSS#36, as ㅛ·ㅕ's dots stand): under dots in a
         * row across the middle at [DOT_LINE] of the height, right dots in a column down the middle at [DOT_LINE] of the width.
         */
        fun dotCenters(box: RectF, dots: List<DotPlace>, out: MutableList<PointF>) {
            out.clear()
            val n = dots.size
            for (i in 0 until n) {
                val off = (i - (n - 1) / 2f) * DOT_GAP
                out += when (dots[i]) {
                    DotPlace.UNDER -> PointF(box.centerX() + off * box.width(), box.top + box.height() * DOT_LINE)
                    DotPlace.RIGHT -> PointF(box.left + box.width() * DOT_LINE, box.centerY() + off * box.height())
                }
            }
        }
    }
}
