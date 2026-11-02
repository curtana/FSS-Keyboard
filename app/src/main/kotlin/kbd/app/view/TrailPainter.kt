package kbd.app.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kbd.stroke.geom.StrokePoint

/**
 * Draws the vowel strokes' trail (SWIPE 9.4, FR-DSP-09, issue #173) on the swipe keyboard and the
 * tutor's practice keyboard: the finger's own path, about 4 dp wide, round ends, the label colour at
 * about 40 % opacity. Called after the keys (above them); the guide pop-up is a window above the view.
 */
class TrailPainter(private val density: Float) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = WIDTH_DP * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = COLOR
    }
    private val path = Path()

    fun draw(canvas: Canvas, paths: List<List<StrokePoint>>) {
        for (pts in paths) {
            if (pts.size < 2) continue
            path.reset()
            path.moveTo(pts[0].x * density, pts[0].y * density)
            for (i in 1 until pts.size) path.lineTo(pts[i].x * density, pts[i].y * density)
            canvas.drawPath(path, paint)
        }
    }

    companion object {
        const val WIDTH_DP = 4f

        /** White (the label colour) at 40 % (0x66). */
        const val COLOR = 0x66FFFFFF
    }
}
