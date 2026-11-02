package kbd.app.swipe.impl

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
import kbd.app.swipe.SwipeHost
import kbd.app.view.RepeatSchedule
import kbd.app.view.RepeatTimer
import kbd.gesture.classify.SwipeKeyKind
import kbd.gesture.input.SwipeInputMapper
import kbd.gesture.layout.NumberKeyTable
import kbd.gesture.layout.SwipeKeyDef
import kbd.gesture.layout.SwipeLayout
import kbd.gesture.layout.SymbolKeyTable
import kbd.gesture.progress.ProgressState
import kbd.stroke.geom.Dir
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey

/**
 * The swipe keyboard, 7 × 5 keys (LLD-04 §9.2). Touch handling is
 * [SwipeTouchLogic] (LLD-04 §9.3); this view converts events, draws the keys and owns the
 * [SwipeInputMapper], ⌫ repeat and the guide popup.
 */
class SwipeKeyboardView(context: Context, private val host: SwipeHost) : View(context) {

    private val density = resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())
    private val tracker = TouchTracker(density)
    private val mapper = SwipeInputMapper(host.thresholds)
    /** The app's resets (cursor move, new field…) close the assembly: redraw so the trail goes at once (SWIPE 9.4). */
    private val engineState = object : kbd.app.swipe.SwipeEngineState by SwipeMapperState(mapper) {
        override fun reset(): Boolean = mapper.reset().also { postInvalidate() }
    }
    private val repeatTimer = RepeatTimer(handler, repeatSchedule()) { key ->
        host.guard.stroke(SwipeTouchLogic.SITE_FN) { host.onFunctionKey(key) } // no feedback on repeats (FR-CMN-11a ②)
        invalidate()   // a repeated ⌫ changes the center consonant without a touch event (SWIPE 9.5)
    }
    private var guide: GuideOverlay? = null
    private var guideFlags: GuideFlags? = null

    private val logic = SwipeTouchLogic(host, mapper, object : StrokeGuide {
        override fun onDown(key: SwipeKeyDef, keyRect: RectDp, origin: StrokePoint) {
            val rect = Rect(px(keyRect.left), px(keyRect.top), px(keyRect.right), px(keyRect.bottom))
            guideFor().onDown(key, rect, origin)
        }

        override fun onProgress(ps: ProgressState, finger: StrokePoint) {
            guide?.onProgress(ps, finger)
        }

        override fun onUp() {
            guide?.onUp()
        }
    }, object : KeyRepeat {
        override fun start(pointerId: Int, key: FunctionKey) = repeatTimer.start(pointerId, key)
        override fun stop(pointerId: Int) = repeatTimer.stop(pointerId)
        override fun stopAll() = repeatTimer.stopAll()
    }, strokeLog = StrokeDebugLog.run { init(context); sink })

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3C3C3C.toInt() }
    private val functionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A2A2A.toInt() }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val sidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB0B0B0.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val keyRect = RectF()
    private val trailPainter = kbd.app.view.TrailPainter(density)
    private val centerPainter = kbd.app.view.CenterConsonantPainter()
    private val centerBox = RectF()

    init {
        setBackgroundColor(Color.BLACK)
        host.registerSwipeState(engineState) // main sees only SwipeEngineState (LLD-03 §2.2)
    }

    private val rowHeightDp: Float get() = host.settings.rowHeightDp

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, px(SwipeLayout.ROWS * rowHeightDp)) // SRS 3.3
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        rebuildGeometry()
    }

    /** Key geometry in dp from [SwipeLayout.KEYS] and the current size (FR-SWP-45, FR-CMN-12). */
    fun rebuildGeometry() {
        if (width <= 0) return
        val g = logic.geometry
        val wDp = width / density
        if (g != null && g.widthDp == wDp && g.rowHeightDp == rowHeightDp) return
        logic.geometry = SwipeGeometry.build(wDp, rowHeightDp)
        labelPaint.textSize = rowHeightDp * density * LABEL_SCALE
        sidePaint.textSize = rowHeightDp * density * SIDE_SCALE
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        host.registerSwipeState(engineState)
    }

    override fun onDetachedFromWindow() {
        logic.cancel()
        repeatTimer.stopAll()
        guide?.dismiss()
        host.registerSwipeState(null)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val g = logic.geometry ?: return
        val inset = KEY_INSET_DP * density
        for (i in g.keys.indices) {
            val k = g.keys[i]
            if (k.kind == SwipeKeyKind.EMPTY) continue
            val r = g.geoms[i].rect
            keyRect.set(r.left * density + inset, r.top * density + inset, r.right * density - inset, r.bottom * density - inset)
            canvas.drawRoundRect(keyRect, inset * 2, inset * 2, if (k.kind == SwipeKeyKind.FUNCTION) functionPaint else keyPaint)
            // Owner #198: every swiped result in the middle of its edge; the cheonjiin key shows its tap ㆍ in the centre then.
            drawCentered(canvas, if (k.kind == SwipeKeyKind.CHEONJIIN) CHEONJIIN_TAP else k.label, labelPaint)
            when (k.kind) {
                SwipeKeyKind.NUMBER -> drawSides(canvas, NumberKeyTable[checkNotNull(k.digit)])
                SwipeKeyKind.SYMBOL -> drawSides(canvas, SymbolKeyTable.of(k.id).exits)   // the four edge middles, as the numbers (#208)
                SwipeKeyKind.CHEONJIIN -> drawSides(canvas, kbd.gesture.classify.cheonjiinEdgeHints())
                else -> Unit   // consonant keys show no edge hints (their strokes are vowels)
            }
        }
        // SWIPE 9.5: the center consonant, faint, in the place C between the keys and the trail. It changes only when the
        // composer does (dispatch on release, ⌫ repeat, app resets), never while a stroke is drawn.
        val c = g.consonantBox
        centerBox.set(c.left * density, c.top * density, c.right * density, c.bottom * density)
        centerPainter.draw(canvas, kbd.app.view.CenterConsonantPainter.textFor(host.centerConsonant, host.fieldInfo), centerBox)
        // SWIPE 9.4: the vowel strokes' trail above the keys; it follows the open assembly (a cursor move or a new field closed it too).
        logic.trail.sync(mapper.strokes.size)
        trailPainter.draw(canvas, logic.trail.paths)
    }

    /**
     * Perimeter glyphs as in the layout tables (FR-SWP-50, LLD-04 §8.2·8.3) on the number and symbol keys: each direction's
     * glyph in the middle of its edge — ↑ top, ↓ bottom, ← left, → right (owner #208: no corner places).
     */
    private fun drawSides(canvas: Canvas, sides: Map<Dir, String>) {
        val w = keyRect.width()
        val mid = keyRect.centerY() - (sidePaint.ascent() + sidePaint.descent()) / 2
        sides[Dir.UP]?.let { canvas.drawText(it, keyRect.centerX(), keyRect.top - sidePaint.ascent(), sidePaint) }
        sides[Dir.DOWN]?.let { canvas.drawText(it, keyRect.centerX(), keyRect.bottom - sidePaint.descent(), sidePaint) }
        sides[Dir.LEFT]?.let { canvas.drawText(it, keyRect.left + w * 0.15f, mid, sidePaint) }
        sides[Dir.RIGHT]?.let { canvas.drawText(it, keyRect.right - w * 0.15f, mid, sidePaint) }
    }

    /** Centred in [keyRect]; a label wider than the key (the cheonjiin key's "ㆍ ㅡ ㅣ", issue #160) is drawn smaller to fit. */
    private fun drawCentered(canvas: Canvas, text: String, paint: Paint) {
        val size = paint.textSize
        val room = keyRect.width() * LABEL_FIT
        val w = paint.measureText(text)
        if (w > room) paint.textSize = size * room / w
        canvas.drawText(text, keyRect.centerX(), keyRect.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
        paint.textSize = size
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        invalidate()   // the trail follows the finger (SWIPE 9.4)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (tracker.activePointerId != TouchTracker.NONE) return true // FR-CMN-11
                val idx = e.actionIndex
                val p = tracker.onDown(e, idx)
                if (!logic.down(tracker.activePointerId, p)) tracker.activePointerId = TouchTracker.NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (tracker.activePointerId != TouchTracker.NONE) logic.move(tracker.pointsOf(e))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val idx = e.actionIndex
                val id = e.getPointerId(idx)
                if (id == tracker.activePointerId) {
                    logic.up(id, tracker.pointAt(e, idx))
                    tracker.activePointerId = TouchTracker.NONE
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                logic.cancel()
                tracker.activePointerId = TouchTracker.NONE
            }
        }
        return true
    }

    /** The overlay for the current guide flags, rebuilt when settings or field restrictions change. */
    private fun guideFor(): GuideOverlay {
        val flags = GuideFlags.of(host.settings, host.fieldInfo) // FR-DSP-08, FR-CMN-08
        guide?.let { if (flags == guideFlags) return it }
        guide?.dismiss()
        val slopDp = ViewConfiguration.get(context).scaledTouchSlop / density // D-17
        return GuideOverlay(this, GuideTimerLogic(host.thresholds, flags, slopDp), handler).also {
            guide = it
            guideFlags = flags
        }
    }

    private fun px(dp: Float): Int = Math.round(dp * density)

    private companion object {
        /** The cheonjiin key's centre label while its directions are drawn on the edges (owner #198). */
        val CHEONJIIN_TAP = kbd.gesture.classify.elementText(kbd.hangul.cheonjiin.CjElement.DOT)

        const val LABEL_SCALE = 0.4f
        const val SIDE_SCALE = 0.22f
        /** Share of the key width a centred label may take before it is drawn smaller. */
        const val LABEL_FIT = 0.8f
        const val KEY_INSET_DP = 2f

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
