package kbd.app.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.view.ViewCompat
import kbd.app.R
import kbd.app.error.EngineGuard
import kbd.app.feedback.FeedbackPort
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey
import kbd.stroke.geom.Dir
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapKeyKind
import kbd.stroke.layout.TapLayout
import kbd.stroke.threshold.Thresholds

/**
 * The QWERTY and Dubeolsik keyboards, 6 rows (LLD-05A §4, SRS 3.3). Touch handling is
 * [TapTouchLogic]; this view converts events per pointer, draws the keys, corner hints (§3.4) and
 * pressed state, owns the ⌫·arrow repeat (§6) and the accessibility helper (§7, NFR-07).
 */
class TapKeyboardView(
    context: Context,
    private val sink: TapSink,
    private val guard: EngineGuard,
    private val feedback: FeedbackPort,
) : View(context), SideMarginView {

    private val density = resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())
    private val repeatTimer = RepeatTimer(handler, repeatSchedule()) { key ->
        guard.stroke(TapTouchLogic.SITE_REPEAT) { sink.onFunctionDown(key) } // no feedback on repeats (FR-CMN-11a ②)
    }
    private val logic = TapTouchLogic(sink, guard, feedback, object : TapKeyRepeat {
        override fun start(pointerId: Int, key: FunctionKey) = repeatTimer.start(pointerId, key)
        override fun stop(pointerId: Int) = repeatTimer.stop(pointerId)
        override fun stopAll() = repeatTimer.stopAll()
    })
    private val access = TapKeyAccessHelper(this)

    private var layout: TapLayout? = null
    private var rowHeightDp = 0f

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = KEY_COLOR }
    private val functionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FUNCTION_COLOR }
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PRESSED_COLOR }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = HINT_COLOR }
    private val keyRect = RectF()

    init {
        setBackgroundColor(KeyboardRootView.BACKGROUND_COLOR)   // one colour with the root's margin (OD-2 A)
        ViewCompat.setAccessibilityDelegate(this, access)
    }

    /** The keyboard's side margin, set by the parent before it measures this view (FR-CMN-14 ⑧, LLD-08 §7.2). */
    override var sideMarginPx: Int = 0
        set(v) {
            if (field == v) return
            field = v
            rebuildGeometry()
        }

    /** Layout, thresholds and row height; key geometry is rebuilt at the next size (FR-TAP-03). */
    fun setLayout(layout: TapLayout, th: Thresholds, rowHeightDp: Float) {
        this.layout = layout
        this.rowHeightDp = rowHeightDp
        logic.minCrossDp = th.minCrossDp   // FR-TAP-02b
        logic.geometry = null
        requestLayout()
        rebuildGeometry()
    }

    /** The key at [p] (view dp): indent blanks first, then the nearest key (LLD-05A §4 `hit`). */
    fun hit(p: StrokePoint): TapKeyDef? {
        val g = logic.geometry ?: return null
        return g.keys.getOrNull(g.indexAt(p.x, p.y))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val rows = layout?.rows ?: 0
        setMeasuredDimension(w, resolveSize(px(rows * rowHeightDp), heightMeasureSpec)) // 5A
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        rebuildGeometry()
    }

    /**
     * Key geometry in dp at the current size. The row height is taken from the size the root gave
     * (it follows the row height setting, LLD-08 §7); the layout stays the same at any width (FR-CMN-12).
     */
    private fun rebuildGeometry() {
        val l = layout ?: return
        if (width <= 0 || height <= 0) return
        val wDp = width / density
        val rowDp = height / density / l.rows
        val sideDp = sideMarginPx / density
        val g = logic.geometry
        if (g != null && g.layout === l && g.widthDp == wDp && g.rowHeightDp == rowDp && g.sideDp == sideDp) return
        logic.geometry = TapGeometry.build(l, wDp, rowDp, sideDp)
        labelPaint.textSize = rowDp * density * LABEL_SCALE
        hintPaint.textSize = labelPaint.textSize * HINT_SCALE   // D-15
        access.invalidateRoot()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        logic.detach()   // the mode key replaced this view mid-touch
        repeatTimer.stopAll()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val g = logic.geometry ?: return
        val pressed = logic.pressed
        val inset = KEY_INSET_DP * density
        for (i in g.keys.indices) {
            val k = g.keys[i]
            val r = g.geoms[i].rect
            keyRect.set(r.left * density + inset, r.top * density + inset, r.right * density - inset, r.bottom * density - inset)
            val paint = when {
                i in pressed -> pressedPaint
                k.kind == TapKeyKind.FUNCTION -> functionPaint
                else -> keyPaint
            }
            canvas.drawRoundRect(keyRect, inset * 2, inset * 2, paint)
            canvas.drawText(k.label, keyRect.centerX(), keyRect.centerY() - (labelPaint.ascent() + labelPaint.descent()) / 2, labelPaint)
            if (k.kind != TapKeyKind.FUNCTION) drawHints(canvas, k)
        }
    }

    /**
     * Edge hints (LLD-05A §3.4, FR-TAP-03b, owner #198): what a stroke out of each edge types where it differs from
     * the tap ([TapKeyResolver.edgeHints] — a letter's ↑ capital or shift jamo too), in the middle of that edge.
     */
    private fun drawHints(canvas: Canvas, k: TapKeyDef) {
        val h = kbd.stroke.tap.TapKeyResolver.edgeHints(k)
        val pad = HINT_PAD_DP * density
        // Each direction's symbol in the middle of its edge (owner #208): ↑ top, ↓ bottom, ← left, → right; no corners.
        val top = keyRect.top + pad - hintPaint.ascent()
        val bottom = keyRect.bottom - pad - hintPaint.descent()
        val mid = keyRect.centerY() - (hintPaint.ascent() + hintPaint.descent()) / 2
        h[Dir.UP]?.let { drawHint(canvas, it, keyRect.centerX(), top, Paint.Align.CENTER) }
        h[Dir.LEFT]?.let { drawHint(canvas, it, keyRect.left + pad, mid, Paint.Align.LEFT) }
        h[Dir.DOWN]?.let { drawHint(canvas, it, keyRect.centerX(), bottom, Paint.Align.CENTER) }
        h[Dir.RIGHT]?.let { drawHint(canvas, it, keyRect.right - pad, mid, Paint.Align.RIGHT) }
    }

    private fun drawHint(canvas: Canvas, s: String, x: Float, y: Float, align: Paint.Align) {
        hintPaint.textAlign = align
        canvas.drawText(s, x, y, hintPaint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = e.actionIndex
                logic.down(e.getPointerId(idx), pointAt(e, idx), first = e.actionMasked == MotionEvent.ACTION_DOWN)
            }
            MotionEvent.ACTION_MOVE -> {
                for (idx in 0 until e.pointerCount) logic.move(e.getPointerId(idx), pointsOf(e, idx))
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val idx = e.actionIndex
                logic.up(e.getPointerId(idx), pointAt(e, idx))
            }
            MotionEvent.ACTION_CANCEL -> logic.cancel()
        }
        invalidate()   // pressed state
        return true
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean =
        access.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        access.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    // For TapKeyAccessHelper: px coordinates, key indices into the geometry.

    internal val keyCount: Int get() = logic.geometry?.keys?.size ?: 0

    /** The side margin the key geometry was built with, dp; null before the first size (tests). */
    internal val geometrySideDp: Float? get() = logic.geometry?.sideDp

    internal fun keyIndexAtPx(x: Float, y: Float): Int = logic.geometry?.indexAt(x / density, y / density) ?: -1

    internal fun keyAt(index: Int): TapKeyDef? = logic.geometry?.keys?.getOrNull(index)

    internal fun keyBoundsPx(index: Int, out: Rect) {
        val r = logic.geometry?.geoms?.getOrNull(index)?.rect
        if (r == null) out.setEmpty() else out.set(px(r.left), px(r.top), px(r.right), px(r.bottom))
    }

    /** What a screen reader says: the key label, a name for function keys (LLD-05A §7). */
    internal fun describe(key: TapKeyDef): String = when (key.function) {
        null -> key.label
        FunctionKey.ESC -> context.getString(R.string.a11y_key_esc)
        FunctionKey.DELETE -> context.getString(R.string.a11y_key_delete)
        FunctionKey.TAB -> context.getString(R.string.a11y_key_tab)
        FunctionKey.ARROW_LEFT -> context.getString(R.string.a11y_key_left)
        FunctionKey.ARROW_UP -> context.getString(R.string.a11y_key_up)
        FunctionKey.ARROW_DOWN -> context.getString(R.string.a11y_key_down)
        FunctionKey.ARROW_RIGHT -> context.getString(R.string.a11y_key_right)
        FunctionKey.BACKSPACE -> context.getString(R.string.a11y_key_backspace)
        FunctionKey.SPACE -> context.getString(R.string.a11y_key_space)
        FunctionKey.ENTER -> context.getString(R.string.a11y_key_enter)
        FunctionKey.MODE -> context.getString(R.string.a11y_key_mode)
    }

    internal fun clickKey(index: Int) = logic.click(index)

    /** History points of the pointer at [idx], then its current point, in dp (HLD 4.1). */
    private fun pointsOf(e: MotionEvent, idx: Int): List<StrokePoint> {
        val out = ArrayList<StrokePoint>(e.historySize + 1)
        for (h in 0 until e.historySize) {
            out += StrokePoint(e.getHistoricalX(idx, h) / density, e.getHistoricalY(idx, h) / density, e.getHistoricalEventTime(h))
        }
        out += pointAt(e, idx)
        return out
    }

    private fun pointAt(e: MotionEvent, idx: Int): StrokePoint =
        StrokePoint(e.getX(idx) / density, e.getY(idx) / density, e.eventTime)

    private fun px(dp: Float): Int = Math.round(dp * density)

    private companion object {
        const val LABEL_SCALE = 0.4f
        const val HINT_SCALE = 0.4f   // edge hints, 40 % of the key label (D-15)
        const val KEY_INSET_DP = 2f
        const val HINT_PAD_DP = 2f
        const val KEY_COLOR = 0xFF3C3C3C.toInt()
        const val FUNCTION_COLOR = 0xFF2A2A2A.toInt()
        const val PRESSED_COLOR = 0xFF5A5A5A.toInt()
        const val HINT_COLOR = 0xFFB0B0B0.toInt()

        /** Platform repeat timing (D-16); before API 28 the same values were not public. */
        fun repeatSchedule(): RepeatSchedule =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                RepeatSchedule(ViewConfiguration.getKeyRepeatTimeout().toLong(), ViewConfiguration.getKeyRepeatDelay().toLong())
            } else {
                RepeatSchedule(ViewConfiguration.getLongPressTimeout().toLong(), LEGACY_REPEAT_DELAY_MS)
            }

        /** `ViewConfiguration.KEY_REPEAT_DELAY` of the platform before it became public (API 28). */
        const val LEGACY_REPEAT_DELAY_MS = 50L
    }
}
