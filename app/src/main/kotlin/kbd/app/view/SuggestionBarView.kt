package kbd.app.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.text.TextUtils
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import kbd.app.feedback.FeedbackPort

/** One cell of the suggestion bar: the text drawn and the name a screen reader says (LLD-12 §9). */
data class BarCell(val text: String, val spoken: String = text)

/**
 * The suggestion bar (SRS FR-SUG-03, HLD 5B.4, LLD-12 §9, D-86·D-89): one row above the keys, three equal cells. It
 * knows nothing about languages or modes: it draws the [BarCell]s it is [show]n and tells [onPick] which cell was
 * pressed and released in place, with the revision it had when the finger came down. A pick made while a newer
 * revision came in is thrown away by the owner, not here (FR-SUG-03 ③).
 *
 * No background: the root paints the keyboard plate under it (LLD-08 §7.3). It takes only the first finger of a gesture;
 * the root drops later fingers that come down on it (LLD-12 §10).
 */
class SuggestionBarView(context: Context, private val feedback: FeedbackPort) : View(context) {

    private val density = resources.displayMetrics.density

    /** Cell [index] was pressed and released inside it; [revision] = the one shown when it was pressed. */
    var onPick: (index: Int, revision: Int) -> Unit = { _, _ -> }

    private var cells: List<BarCell> = emptyList()
    private var revision = 0

    /** The cell held down, or -1; and the revision it was pressed under. */
    private var pressed = -1
    private var pressedRevision = 0

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val separatorPaint = Paint().apply { color = SEPARATOR_COLOR }
    private val pressedPaint = Paint().apply { color = PRESSED_COLOR }
    private val cell = Rect()

    private val access = SuggestionAccessHelper(this)

    init {
        ViewCompat.setAccessibilityDelegate(this, access)
    }

    /**
     * The cells to draw (the first [cellCount], from the left) and their revision. The revision is always taken; when
     * the cells are the same as shown nothing is drawn again and the accessibility tree is kept (LLD-12 §9). A cell held
     * down stays held: its pick carries the revision it was pressed under.
     */
    fun show(cells: List<BarCell>, revision: Int) {
        this.revision = revision
        val c = cells.take(CELLS)
        if (c == this.cells) return
        this.cells = c
        invalidate()
        access.invalidateRoot()
    }

    internal val cellCount: Int get() = CELLS

    /** Number of cells with text, from the left. */
    internal val filledCount: Int get() = cells.size

    /** The text of cell [i], or null when it is empty. */
    internal fun cellText(i: Int): BarCell? = cells.getOrNull(i)

    /** The cell under [x] (px in this view), 0 to 2, or -1 outside the view's width. */
    internal fun cellAt(x: Float): Int {
        val w = width
        if (x < 0 || x >= w) return -1
        for (i in 0 until CELLS) if (x < edge(i + 1, w)) return i
        return -1
    }

    /** Cell [i] = `[round(i·w/3), round((i+1)·w/3))` by the view's height (FR-SUG-03 ①). */
    internal fun cellBounds(i: Int, out: Rect) {
        out.set(edge(i, width), 0, edge(i + 1, width), height)
    }

    /** The accessibility click on cell [i]: a pick with the revision shown now, when the cell has text. */
    internal fun click(i: Int) {
        if (i in cells.indices) onPick(i, revision)
    }

    private fun edge(i: Int, w: Int): Int = Math.round(i * w / CELLS.toFloat())

    override fun onDraw(canvas: Canvas) {
        val h = height
        if (pressed in cells.indices) {
            cellBounds(pressed, cell)
            canvas.drawRect(cell.left.toFloat(), cell.top.toFloat(), cell.right.toFloat(), cell.bottom.toFloat(), pressedPaint)
        }
        val sep = SEPARATOR_DP * density
        for (i in 1 until CELLS) {
            val x = edge(i, width).toFloat()
            canvas.drawRect(x - sep / 2, h * 0.25f, x + sep / 2, h * 0.75f, separatorPaint)
        }
        val base = h * TEXT_RATIO
        for (i in cells.indices) {
            cellBounds(i, cell)
            val avail = (cell.width() - 2 * TEXT_INSET_DP * density).coerceAtLeast(0f)
            val text = fit(cells[i].text, base, avail)
            canvas.drawText(text, cell.exactCenterX(), h / 2f - (textPaint.ascent() + textPaint.descent()) / 2, textPaint)
        }
    }

    /**
     * Sets the text size for [text] in [avail] px and gives what to draw: the base size [base] when it fits, else the size
     * that fits down to [MIN_SCALE] × [base], and below that [MIN_SCALE] × [base] with the end cut to `…` (LLD-12 §9).
     */
    private fun fit(text: String, base: Float, avail: Float): String {
        textPaint.textSize = base
        val w = textPaint.measureText(text)
        if (w <= avail) return text
        val size = base * avail / w
        if (size >= base * MIN_SCALE) {
            textPaint.textSize = size
            return text
        }
        textPaint.textSize = base * MIN_SCALE
        return TextUtils.ellipsize(text, textPaint, avail, TextUtils.TruncateAt.END).toString()
    }

    /** First finger only; always taken (LLD-12 §9). Feedback on the press of a cell with text (FR-SUG-03 ②). */
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val i = cellAt(e.x)
                if (i in cells.indices) {
                    pressed = i
                    pressedRevision = revision
                    feedback.key()
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val p = pressed
                if (p >= 0 && cellAt(e.x) == p && e.y >= 0 && e.y < height) onPick(p, pressedRevision)
                release()
            }
            MotionEvent.ACTION_CANCEL -> release()
        }
        return true
    }

    private fun release() {
        if (pressed < 0) return
        pressed = -1
        invalidate()
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean =
        access.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        access.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        access.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    internal companion object {
        const val CELLS = 3

        /** Text size = the view's height × this (px). */
        const val TEXT_RATIO = 0.40f

        /** The smallest text size, × the base, before the end is cut. */
        const val MIN_SCALE = 0.70f

        /** Room left on each side of a cell's text, dp. */
        const val TEXT_INSET_DP = 8f

        const val SEPARATOR_DP = 1f
        const val SEPARATOR_COLOR = 0xFF505050.toInt()

        /** White at 15 % alpha over a held cell. */
        const val PRESSED_COLOR = 0x26FFFFFF
    }
}
