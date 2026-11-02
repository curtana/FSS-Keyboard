package kbd.app.graffiti

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kbd.app.feedback.FeedbackPort
import kbd.graffiti.guide.GuideCatalog
import kbd.graffiti.guide.GuideGroup
import kbd.graffiti.guide.GuideShape
import kbd.graffiti.recognize.TemplateSet
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The stroke guide panel (SRS FR-GRF-09~09d, HLD 5장, LLD-05 §10.2): an opaque cover over the pad and the
 * auxiliary bar that draws [GuideCatalog] from [templates], the recognizer's own points (no copied figures,
 * CON-6). Every touch on it is consumed with no input. A drag scrolls; the fixed ✕ at the top-right closes
 * it on release inside the ✕ ([onClose]), with key feedback on press. Nothing is stored; drawn in password
 * fields too (FR-GRF-09d).
 *
 * The input practice (SWIPE 10.1 ③, LLD-07 §2.3) shows it always open above the pad, not over it: [groups] = only the
 * item's sections, [showClose] = false (no ✕), and [highlight] marks the target's cells after a ✗.
 */
class GraffitiGuidePanel(
    context: Context,
    private val templates: TemplateSet,
    private val feedback: FeedbackPort,
    private val groups: List<GuideGroup> = GuideCatalog.groups,
    private val showClose: Boolean = true,
    private val onClose: () -> Unit,
) : View(context) {

    private val density = resources.displayMetrics.density
    private var layout: GraffitiGuideLayout? = null

    /** Scroll offset in dp, 0..[GraffitiGuideLayout.maxScrollDp]. */
    var scrollDp: Float = 0f
        private set

    /** Labels of the cells drawn highlighted (the practice's ✗, SWIPE 10.1 ③); the first one is scrolled into view. */
    var highlight: Set<String> = emptySet()
        set(v) {
            field = v
            scrollToHighlight()
            invalidate()
        }

    /** The pointer this panel follows (the first one down); others are consumed and ignored. */
    private var pointer = NONE
    private var onClosePressed = false
    private var lastY = 0f

    private val bgPaint = Paint().apply { color = BACKGROUND }
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF262626.toInt() }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE0E0E0.toInt(); textSize = 13 * density }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); textSize = 13 * density; textAlign = Paint.Align.CENTER
    }
    private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9E9E9E.toInt(); textSize = 9.5f * density; textAlign = Paint.Align.CENTER
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8FC1FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2 * density
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val dashedPaint = Paint(strokePaint).apply { pathEffect = DashPathEffect(floatArrayOf(4 * density, 3 * density), 0f) }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6A6A6A.toInt(); style = Paint.Style.STROKE; strokeWidth = 1 * density
    }
    private val closePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2 * density; strokeCap = Paint.Cap.ROUND
    }
    private val closeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3A3A3A.toInt() }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = HIGHLIGHT; style = Paint.Style.STROKE; strokeWidth = 3 * density
    }
    private val rect = RectF()
    private val box = RectF()
    private val path = Path()

    init {
        contentDescription = "획 안내표"
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layout = GraffitiGuideLayout(w / density, groups)
        scrollDp = scrollDp.coerceIn(0f, maxScroll())
        scrollToHighlight()
    }

    private fun scrollToHighlight() {
        val l = layout ?: return
        val c = l.cells.firstOrNull { it.cell.label in highlight } ?: return
        scrollDp = (c.rect.top - GraffitiGuideLayout.PAD_DP).coerceIn(0f, maxScroll())
    }

    private fun maxScroll(): Float = layout?.maxScrollDp(height / density) ?: 0f

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)   // opaque (FR-GRF-09a)
        val l = layout ?: return
        val dy = -scrollDp * density
        val viewH = height.toFloat()
        for (t in l.titles) {
            val top = t.rect.top * density + dy
            if (top > viewH || t.rect.bottom * density + dy < 0) continue
            canvas.drawText(t.text, t.rect.left * density, top + (t.rect.bottom - t.rect.top) * density * 0.7f, titlePaint)
        }
        for (c in l.cells) {
            val r = c.rect
            rect.set(r.left * density, r.top * density + dy, r.right * density, r.bottom * density + dy)
            if (rect.top > viewH || rect.bottom < 0) continue
            drawCell(canvas, c.cell, rect)
        }
        if (!showClose) return
        // ✕ last: fixed at the top-right, over the scrolled content (FR-GRF-09).
        val x = l.closeRect(width / density)
        rect.set(x.left * density, x.top * density, x.right * density, x.bottom * density)
        canvas.drawCircle(rect.centerX(), rect.centerY(), rect.width() * 0.4f, closeBgPaint)
        val a = rect.width() * 0.18f
        canvas.drawLine(rect.centerX() - a, rect.centerY() - a, rect.centerX() + a, rect.centerY() + a, closePaint)
        canvas.drawLine(rect.centerX() + a, rect.centerY() - a, rect.centerX() - a, rect.centerY() + a, closePaint)
    }

    private fun drawCell(canvas: Canvas, cell: kbd.graffiti.guide.GuideCell, r: RectF) {
        val m = CELL_MARGIN_DP * density
        canvas.drawRoundRect(r.left + m, r.top + m, r.right - m, r.bottom - m, 3 * density, 3 * density, cellPaint)
        if (cell.label in highlight) canvas.drawRoundRect(r.left + m, r.top + m, r.right - m, r.bottom - m, 3 * density, 3 * density, highlightPaint)
        val label = cell.label + when (cell.alt) { 1 -> " ①"; 2 -> " ②"; else -> "" }
        val note = listOfNotNull(cell.note, if (cell.interim) INTERIM else null).joinToString(" · ").ifEmpty { null }
        if (cell.marks.isEmpty()) {   // text only: vowel order, unsettled symbols
            canvas.drawText(label, r.centerX(), r.top + r.height() * 0.45f, labelPaint)
            note?.let { canvas.drawText(it, r.centerX(), r.top + r.height() * 0.8f, notePaint) }
            return
        }
        val n = cell.marks.size
        val side = minOf((r.width() - 2 * m) / n, GLYPH_DP * density) - 2 * GLYPH_INSET_DP * density
        val gap = (r.width() - n * (side + 2 * GLYPH_INSET_DP * density)) / 2
        cell.marks.forEachIndexed { i, mark ->
            val left = r.left + gap + i * (side + 2 * GLYPH_INSET_DP * density) + GLYPH_INSET_DP * density
            box.set(left, r.top + GLYPH_TOP_DP * density, left + side, r.top + GLYPH_TOP_DP * density + side)
            GuideCatalog.shape(mark, templates)?.let { drawShape(canvas, it, box, if (cell.interim) dashedPaint else strokePaint) }
        }
        val textTop = r.top + (GLYPH_TOP_DP + GLYPH_DP) * density
        canvas.drawText(label, r.centerX(), textTop + 12 * density, labelPaint)
        note?.let { canvas.drawText(it, r.centerX(), textTop + 24 * density, notePaint) }
    }

    /** Stroke line, start dot ●, end arrow (GRAFFITI 6.6-5); a tap is one dot, a corner tap a dot in a small pad. */
    private fun drawShape(canvas: Canvas, s: GuideShape, b: RectF, paint: Paint) {
        val dot = DOT_DP * density
        when (s) {
            GuideShape.Tap -> canvas.drawCircle(b.centerX(), b.centerY(), dot * 1.3f, dotPaint)
            GuideShape.CornerTap -> {
                canvas.drawRect(b, boxPaint)
                canvas.drawCircle(b.left + dot * 2, b.top + dot * 2, dot * 1.3f, dotPaint)
            }
            is GuideShape.Line -> {
                val pts = s.points
                fun px(p: Pair<Float, Float>) = b.left + p.first * b.width()
                fun py(p: Pair<Float, Float>) = b.top + p.second * b.height()
                path.rewind()
                path.moveTo(px(pts[0]), py(pts[0]))
                for (i in 1 until pts.size) path.lineTo(px(pts[i]), py(pts[i]))
                canvas.drawPath(path, paint)
                canvas.drawCircle(px(pts[0]), py(pts[0]), dot, dotPaint)
                val ex = px(pts.last())
                val ey = py(pts.last())
                // Direction from the last point far enough back (round strokes end in short segments).
                var j = pts.size - 2
                while (j > 0 && hypot(ex - px(pts[j]), ey - py(pts[j])) < ARROW_DP * density) j--
                if (j >= 0) {
                    val ang = atan2(ey - py(pts[j]), ex - px(pts[j]))
                    val len = ARROW_DP * density
                    for (sgn in intArrayOf(-1, 1)) {
                        val a = ang + Math.PI - sgn * ARROW_ANGLE
                        canvas.drawLine(ex, ey, ex + (len * cos(a)).toFloat(), ey + (len * sin(a)).toFloat(), strokePaint)
                    }
                }
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointer != NONE) return true   // one pointer at a time
                val idx = e.actionIndex
                pointer = e.getPointerId(idx)
                lastY = e.getY(idx)
                onClosePressed = inClose(e.getX(idx), e.getY(idx))
                if (onClosePressed) feedback.key()   // the ✕ is a key: feedback on press (FR-CMN-11a ②)
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = e.findPointerIndex(pointer)
                if (idx >= 0 && !onClosePressed) {   // a press on the ✕ never scrolls (FR-GRF-09)
                    val y = e.getY(idx)
                    val next = (scrollDp - (y - lastY) / density).coerceIn(0f, maxScroll())
                    lastY = y
                    if (next != scrollDp) {
                        scrollDp = next
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val idx = e.actionIndex
                if (e.getPointerId(idx) == pointer) {
                    val close = onClosePressed && inClose(e.getX(idx), e.getY(idx))   // on release inside the ✕
                    reset()
                    if (close) onClose()
                }
            }
            MotionEvent.ACTION_CANCEL -> reset()
        }
        return true   // no touch reaches the pad below (FR-GRF-09a)
    }

    private fun inClose(xPx: Float, yPx: Float): Boolean {
        if (!showClose) return false
        val r = (layout ?: return false).closeRect(width / density)
        val x = xPx / density
        val y = yPx / density
        return x >= r.left && x <= r.right && y >= r.top && y <= r.bottom
    }

    /** Back to the top with no pointer followed (the root calls it on close, LLD-08 §7.1). */
    fun rewind() {
        reset()
        scrollDp = 0f
        invalidate()
    }

    private fun reset() {
        pointer = NONE
        onClosePressed = false
    }

    override fun onDetachedFromWindow() {
        reset()
        super.onDetachedFromWindow()
    }

    companion object {
        /** Opaque cover (owner correction #189: "입력기 위에 불투명으로"). */
        const val BACKGROUND = 0xFF161616.toInt()
        const val INTERIM = "임시"
        /** The practice's ✗ cell mark (SWIPE 10.1 ③): amber, readable on the dark panel. */
        private const val HIGHLIGHT = 0xFFFFB300.toInt()
        private const val CELL_MARGIN_DP = 2f
        private const val GLYPH_DP = 48f
        private const val GLYPH_TOP_DP = 4f
        private const val GLYPH_INSET_DP = 5f
        private const val DOT_DP = 2.5f
        private const val ARROW_DP = 6f
        private const val ARROW_ANGLE = 0.5   // rad, each side of the arrow head
        private const val NONE = -1
    }
}
