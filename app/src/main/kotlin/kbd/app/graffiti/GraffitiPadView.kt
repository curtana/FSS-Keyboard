package kbd.app.graffiti

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kbd.app.error.EngineGuard
import kbd.app.feedback.FeedbackPort
import kbd.app.store.Settings
import kbd.app.view.KeyboardRootView
import kbd.app.view.RepeatSchedule
import kbd.app.view.RepeatTimer
import kbd.app.view.SideMarginView
import kbd.app.view.TapSink
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.RectF2
import kbd.graffiti.state.GraffitiStateSnapshot
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey
import kbd.stroke.tap.ModeKeyGesture
import kbd.stroke.tap.TapGestureClassifier
import kbd.stroke.threshold.ThresholdDefaults

/**
 * The graffiti pad (LLD-05 §8): 4-row pad with letter and number areas over a 1-row auxiliary bar
 * ([GraffitiPadGeometry]). Pad strokes go to [handler] through [guard]; auxiliary bar buttons are
 * function keys for [sink] (InputDispatcher, the same boundary as the tap keyboards, LLD-05A §4).
 * One pointer at a time, pad or bar (FR-CMN-11). The view is [GraffitiPadGeometry.ROWS] rows high.
 * The mode key is judged on release like on the tap keyboards (issue #205): an up exit (into the pad)
 * opens the settings ([TapSink.onSettingsStroke]), anything else is the mode change.
 * The pad status display (FR-GRF-05d, LLD-05 §8.1) is drawn from the handler's [GraffitiState][kbd.graffiti.state.GraffitiState]
 * on every frame, in password fields too, inside the area outlines (FR-GRF-01a, FSS#48).
 */
class GraffitiPadView(
    context: Context,
    private val sink: TapSink,
    private val handler: GraffitiInputHandler,
    private val guard: EngineGuard,
    private val feedback: FeedbackPort,
) : View(context), SideMarginView {

    private val density = resources.displayMetrics.density
    private val looper = Handler(Looper.getMainLooper())
    private val ink = InkCollector(density)
    private val repeatTimer = RepeatTimer(looper, repeatSchedule()) { key ->
        guard.stroke(SITE_FN) { sink.onFunctionDown(key) }   // no feedback on repeats (FR-CMN-11a ②)
    }

    /** Letter share of the pad width (FR-SET-05, `Settings.graffitiLetterRatio`). */
    var letterRatio: Float = Settings.DEFAULT.graffitiLetterRatio
        set(v) {
            field = v
            rebuildGeometry()
        }

    private var geometry: GraffitiPadGeometry? = null

    /** The keyboard's side margin, set by the parent (FR-CMN-14 ⑧, LLD-08 §7.2): read when the mode key is pressed. */
    override var sideMarginPx: Int = 0

    /** The mode key's exit threshold, the tap keyboards' (FR-TAP-02b); set by the service from the thresholds. */
    var minCrossDp: Float = ThresholdDefaults.VALUES.minCrossDp

    /** The mode key's stroke while it is held, judged on release (issue #205). */
    private var modeStroke: TapGestureClassifier? = null

    /** The auxiliary bar button held down and its pointer; [NONE] when none. */
    private var keyPointer = NONE
    private var heldKey: FunctionKey? = null

    private var flashText: String? = null
    private val clearFlash = Runnable {
        flashText = null
        invalidate()
    }

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A2A2A.toInt() }
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF505050.toInt() }
    private val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF505050.toInt()   // faint area outline, same grey as "123" (FR-GRF-01a)
        style = Paint.Style.STROKE
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF505050.toInt()   // faint "123" over the number area (GRAFFITI 6.3)
        textAlign = Paint.Align.CENTER
    }
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF5A5A5A.toInt()   // faint corner dot and language glyph (FR-GRF-05d ①)
        textAlign = Paint.Align.CENTER
    }
    private val pendingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB0B0B0.toInt()
        textAlign = Paint.Align.RIGHT
    }
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4FC3F7.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val inkPath = Path()
    private val keyRect = RectF()

    init {
        setBackgroundColor(KeyboardRootView.BACKGROUND_COLOR)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildGeometry()
    }

    /** Geometry for the current size and ratio (FR-CMN-12); the stroke in progress keeps its start area. */
    private fun rebuildGeometry() {
        if (width <= 0 || height <= 0) return
        val g = GraffitiPadGeometry(width / density, height / density, letterRatio)
        geometry = g
        val rowPx = g.rowHeightDp * density
        keyTextPaint.textSize = rowPx * LABEL_SCALE
        hintPaint.textSize = rowPx * HINT_SCALE
        pendingPaint.textSize = rowPx * LABEL_SCALE
        cornerPaint.textSize = minOf(rowPx * LABEL_SCALE, (g.cornerRect.bottom - g.cornerRect.top) * density * CORNER_TEXT_SCALE)
        flashPaint.textSize = rowPx * FLASH_SCALE
        inkPaint.strokeWidth = INK_WIDTH_DP * density
        areaPaint.strokeWidth = AREA_STROKE_DP * density
        invalidate()
    }

    /**
     * Shows the recognised glyph over the pad for [FLASH_MS] (FR-GRF-07, D-13). The caller
     * (`GraffitiInputHandler.onStroke`) does not call it for strokes without a glyph or in
     * password fields (FR-CMN-08 ①, DCR-19); no-learning fields flash as usual (②).
     */
    fun flash(label: String) {
        flashText = label
        removeCallbacks(clearFlash)
        postDelayed(clearFlash, FLASH_MS)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val g = geometry ?: return
        val padBottom = g.padBottomDp * density
        // Area outlines (FR-GRF-01a, LLD-05 §8.1): drawn only, the areas themselves stay as they are.
        // Their gaps take the place of the old divider lines.
        val boxInset = AREA_INSET_DP * density + areaPaint.strokeWidth / 2
        val boxRadius = AREA_RADIUS_DP * density
        for (r in arrayOf(g.letterRect, g.numberRect)) {
            setPx(keyRect, r)
            keyRect.inset(boxInset, boxInset)
            canvas.drawRoundRect(keyRect, boxRadius, boxRadius, areaPaint)
        }
        setPx(keyRect, g.numberRect)
        drawCentered(canvas, NUMBER_HINT, keyRect, hintPaint)

        // Pad status (FR-GRF-05d, LLD-05 §8.1): not input text, so drawn in password fields too.
        val state = handler.snapshot().state
        setPx(keyRect, g.cornerRect)
        val dot = CORNER_DOT_DP * density
        val dotOffset = AREA_INSET_DP * density + dot * 3   // inside the outline and its round corner
        canvas.drawCircle(keyRect.left + dotOffset, keyRect.top + dotOffset, dot, cornerPaint)
        keyRect.left += dotOffset + dot * 2   // the glyph sits right of the dot, inside the zone
        drawCentered(canvas, langLabel(state.lang), keyRect, cornerPaint)
        val pending = pendingLabel(state)
        if (pending.isNotEmpty()) {
            val inset = (AREA_INSET_DP + KEY_INSET_DP * 3) * density   // inside the number area outline
            canvas.drawText(pending, width - inset, inset - pendingPaint.ascent(), pendingPaint)
        }

        val inset = KEY_INSET_DP * density
        for (k in g.keys) {
            keyRect.set(k.rect.left * density + inset, k.rect.top * density + inset, k.rect.right * density - inset, k.rect.bottom * density - inset)
            canvas.drawRoundRect(keyRect, inset * 2, inset * 2, if (k.key == heldKey) pressedPaint else keyPaint)
            drawCentered(canvas, k.label, keyRect, keyTextPaint)
        }

        flashText?.let { keyRect.set(0f, 0f, width.toFloat(), padBottom); drawCentered(canvas, it, keyRect, flashPaint) }

        val pts = ink.points
        if (pts.isNotEmpty()) {
            inkPath.rewind()
            inkPath.moveTo(pts[0].x * density, pts[0].y * density)
            for (i in 1 until pts.size) inkPath.lineTo(pts[i].x * density, pts[i].y * density)
            if (pts.size == 1) canvas.drawPoint(pts[0].x * density, pts[0].y * density, inkPaint)
            else canvas.drawPath(inkPath, inkPaint)
        }
    }

    private fun setPx(out: RectF, r: RectF2) = out.set(r.left * density, r.top * density, r.right * density, r.bottom * density)

    private fun drawCentered(canvas: Canvas, text: String, r: RectF, paint: Paint) {
        canvas.drawText(text, r.centerX(), r.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val g = geometry ?: return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (ink.active || keyPointer != NONE) return true   // one pointer at a time (FR-CMN-11)
                val idx = e.actionIndex
                val x = e.getX(idx) / density
                val y = e.getY(idx) / density
                val k = g.keyAt(x, y)
                if (k != null && k.key == FunctionKey.MODE) {
                    keyPointer = e.getPointerId(idx)
                    heldKey = k.key
                    feedback.key()   // key feedback on press (FR-CMN-11a ②)
                    val r = k.rect
                    val geom = KeyGeom.build(k.key.name, RectDp(r.left, r.top, r.right, r.bottom), g.widthDp, g.heightDp, sideMarginPx / density)
                    modeStroke = TapGestureClassifier(geom, minCrossDp).also { it.begin(StrokePoint(x, y, e.eventTime)) }   // nothing on press (issue #205)
                } else if (k != null) {
                    keyPointer = e.getPointerId(idx)
                    heldKey = k.key
                    feedback.key()   // key feedback on press (FR-CMN-11a ②)
                    guard.stroke(SITE_FN) { sink.onFunctionDown(k.key) }   // handled on press (FR-CMN-11a ①)
                    if (k.key == FunctionKey.BACKSPACE && heldKey == k.key) repeatTimer.start(keyPointer, k.key)
                } else {
                    // The pad is not a key: no key feedback (FR-CMN-11a ②, GRAFFITI 6.2-9).
                    ink.onDown(e, idx, g::areaAt, g::rectOf)
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val m = modeStroke
                val mi = if (m != null) e.findPointerIndex(keyPointer) else -1
                if (m != null && mi >= 0) {
                    for (h in 0 until e.historySize) {
                        m.add(StrokePoint(e.getHistoricalX(mi, h) / density, e.getHistoricalY(mi, h) / density, e.getHistoricalEventTime(h)))
                    }
                    m.add(StrokePoint(e.getX(mi) / density, e.getY(mi) / density, e.eventTime))
                }
                if (ink.active) {
                    ink.onMove(e)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val id = e.getPointerId(e.actionIndex)
                if (id == keyPointer) {
                    val k = heldKey
                    val m = modeStroke
                    val idx = e.actionIndex
                    releaseKey()
                    if (m != null) modeUp(m, StrokePoint(e.getX(idx) / density, e.getY(idx) / density, e.eventTime))
                    else if (k != null) sink.onFunctionUp(k)
                } else {
                    val stroke = ink.onUp(e)
                    if (stroke != null) {
                        guard.stroke(SITE_STROKE) { handler.onStroke(stroke) }   // on failure: rolled back, no input (LLD-09 §2)
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelTouch()
                invalidate()
            }
        }
        return true
    }

    /** Drops the stroke in progress and stops the repeat; keys already handled stay done (FR-CMN-11a ③, HLD 4.1). */
    private fun cancelTouch() {
        ink.cancel()
        releaseKey()
    }

    private fun releaseKey() {
        repeatTimer.stopAll()
        keyPointer = NONE
        heldKey = null
        modeStroke = null   // an unreleased mode key gives nothing (issue #205)
    }

    /** Mode key release (issue #205): first edge up = the settings, else the mode change (FR-CMN-13); a failed judgement gives nothing (LLD-09 §2). */
    private fun modeUp(m: TapGestureClassifier, p: StrokePoint) {
        val settings = guard.run(SITE_FN) { ModeKeyGesture.of(m.finish(p)) == ModeKeyGesture.OPEN_SETTINGS } ?: return
        if (settings) {
            guard.stroke(SITE_FN) { sink.onSettingsStroke() }
        } else {
            guard.stroke(SITE_FN) { sink.onFunctionDown(FunctionKey.MODE) }
            sink.onFunctionUp(FunctionKey.MODE)
        }
    }

    override fun onDetachedFromWindow() {
        cancelTouch()
        removeCallbacks(clearFlash)
        flashText = null
        super.onDetachedFromWindow()
    }

    companion object {
        /** Recognised glyph display time (FR-GRF-07, D-13). */
        const val FLASH_MS = 600L

        private const val SITE_FN = "Graffiti.fn"
        private const val SITE_STROKE = "Graffiti.onStroke"
        private const val NONE = -1

        private const val NUMBER_HINT = "123"

        /** Current graffiti language in the corner-tap zone (FR-GRF-05d ①). */
        internal fun langLabel(lang: GraffitiLang): String = if (lang == GraffitiLang.KO) "한" else "A"

        /**
         * Pending indicators that are on, caps lock first since it stays (FR-GRF-05d ②, GRAFFITI 4.2):
         * ⇪ caps lock, ⇧ shift, • punctuation shift, ↘ extended shift; "" when none.
         */
        internal fun pendingLabel(s: GraffitiStateSnapshot): String = listOfNotNull(
            CAPS_LOCK.takeIf { s.capsLock },
            SHIFT.takeIf { s.shiftPending },
            PUNCT_PENDING.takeIf { s.punctPending },
            EXT_PENDING.takeIf { s.extPending },
        ).joinToString(" ")

        internal const val CAPS_LOCK = "⇪"
        internal const val SHIFT = "⇧"
        internal const val PUNCT_PENDING = "•"
        internal const val EXT_PENDING = "↘"

        private const val LABEL_SCALE = 0.4f
        private const val HINT_SCALE = 0.5f
        private const val FLASH_SCALE = 1.5f
        private const val KEY_INSET_DP = 2f
        internal const val CORNER_DOT_DP = 2f
        private const val CORNER_TEXT_SCALE = 0.6f
        private const val INK_WIDTH_DP = 3f

        /** Area outline (FR-GRF-01a, LLD-05 §8.1): inset from each area, corner radius, line width. */
        internal const val AREA_INSET_DP = 4f
        internal const val AREA_RADIUS_DP = 6f
        internal const val AREA_STROKE_DP = 1f

        /** Platform repeat timing (D-16), as in the swipe keyboard; before API 28 the values were not public. */
        private fun repeatSchedule(): RepeatSchedule =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                RepeatSchedule(ViewConfiguration.getKeyRepeatTimeout().toLong(), ViewConfiguration.getKeyRepeatDelay().toLong())
            } else {
                RepeatSchedule(ViewConfiguration.getLongPressTimeout().toLong(), LEGACY_REPEAT_DELAY_MS)
            }

        /** `ViewConfiguration.KEY_REPEAT_DELAY` of the platform before it became public (API 28). */
        private const val LEGACY_REPEAT_DELAY_MS = 50L
    }
}
