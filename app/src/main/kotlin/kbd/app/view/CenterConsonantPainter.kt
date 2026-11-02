package kbd.app.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kbd.app.ime.FieldInfo
import kbd.hangul.jamo.Jamo

/**
 * Draws the center consonant (SWIPE 9.5, FR-DSP-10, issue #180 and its follow-up) on the swipe keyboard (the
 * input practice shows the same view, FSS#17): the consonant the next vowel attaches to, faint, in the place C at the top
 * of the consonant key area, centred across (`SwipeLayout.consonantBox`, issue #213; the same box the vowel strokes are judged
 * against). Called after the keys and before the trail (keys < center consonant < trail < pop-up).
 * Display only. What to draw and how big are pure functions ([textFor], [sizeFor]) for the unit tests.
 */
class CenterConsonantPainter {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR
        textAlign = Paint.Align.CENTER
    }

    /** Draws [text] (from [textFor]; null or empty = nothing) centred in the place [box] (px). */
    fun draw(canvas: Canvas, text: String?, box: RectF) {
        if (text.isNullOrEmpty() || box.width() <= 0f || box.height() <= 0f) return
        paint.textSize = 1f
        val unitWidth = paint.measureText(text)
        paint.textSize = sizeFor(box.width(), box.height(), unitWidth)
        canvas.drawText(text, box.centerX(), box.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
    }

    companion object {
        /** The label colour (white) at the trail's 40 % (0x66, SWIPE 9.5 "획 자국과 같은 반투명"). */
        const val COLOR = TrailPainter.COLOR

        /**
         * The text to draw: [consonant] (the composer's last consonant: the final's last one, else the initial;
         * a double consonant as itself), or null when none is composing or [field] is a password field
         * (FR-CMN-08 ①). A no-learning field draws it (②, owner 2026-11-01), and no guide setting turns it
         * off (FR-DSP-08). [field] null = no input field.
         */
        fun textFor(consonant: Jamo?, field: FieldInfo?): String? =
            consonant?.takeIf { field?.hidesDisplay != true }?.compat?.toString()

        /** Text size in px: the place's height, shrunk so text [unitWidth] px wide at size 1 fits its width. */
        fun sizeFor(boxWidth: Float, boxHeight: Float, unitWidth: Float): Float {
            var size = boxHeight
            if (unitWidth > 0f) size = minOf(size, boxWidth / unitWidth)
            return size
        }
    }
}

