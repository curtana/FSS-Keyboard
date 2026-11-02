package kbd.app.swipe.impl

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import kbd.gesture.layout.SwipeKeyDef
import kbd.gesture.layout.SwipeLayout
import kbd.gesture.progress.Candidate
import kbd.gesture.progress.ProgressState
import kbd.stroke.geom.Dir
import kbd.stroke.geom.StrokePoint
import kotlin.math.hypot

/**
 * The swipe guide popup (LLD-06 §3, HLD 6). It only reads engine state; timing decisions are
 * [GuideTimerLogic]'s. The popup covers the input view plus [extraTopPx] above it, so guides of
 * top-row keys can reach into the app area (SWIPE 9.1). [extraTopPx] = 2 × row height (the anchor is 5 rows).
 */
class GuideOverlay(
    private val anchor: View,
    private val logic: GuideTimerLogic,
    private val handler: Handler,
) {
    private val density = anchor.resources.displayMetrics.density
    private val canvasView = GuideCanvasView(anchor.context)
    private val popup = PopupWindow(canvasView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
        isTouchable = false
        isFocusable = false
        isClippingEnabled = false
        inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        setBackgroundDrawable(null)
    }
    private val token = Any()
    private val showDelay = Runnable { onTimer(TimerKind.SHOW_DELAY) }
    private var originX = 0f
    private var originY = 0f
    /** What is drawn now (read by tests). */
    var view: GuideView = HIDDEN
        private set
    private var ps: ProgressState? = null
    private val extraTopPx: Int get() = anchor.height * 2 / SwipeLayout.ROWS

    /** LLD-06 §3 `onDown(key, rect, t)`; [origin] (dp) is also kept for the moved distance of [onProgress]. */
    fun onDown(key: SwipeKeyDef, keyRectPx: Rect, origin: StrokePoint) {
        handler.removeCallbacksAndMessages(token)
        originX = origin.x
        originY = origin.y
        for (r in logic.onDown(origin.t)) {
            handler.postAtTime(showDelay, token, r.atMs) // eventTime is on the uptime clock
        }
        ps = null
        view = HIDDEN
        canvasView.set(key, keyRectPx, null, null, view)
    }

    fun onProgress(ps: ProgressState, finger: StrokePoint) {
        val moved = hypot(finger.x - originX, finger.y - originY)
        logic.onMove(moved, t = finger.t)?.let { view = it }
        this.ps = ps
        apply(finger)
    }

    fun onUp() {
        handler.removeCallbacksAndMessages(token)
        view = logic.onUp()
        ps = null
        canvasView.clear()
    }

    /** Also closes the window (`onFinishInputView`, `onWindowHidden`). */
    fun dismiss() {
        onUp()
        if (popup.isShowing) popup.dismiss()
    }

    private fun onTimer(kind: TimerKind) {
        val v = logic.onTimer(kind, SystemClock.uptimeMillis()) ?: return
        view = v
        apply(null)
    }

    /** The canvas invalidates only when the progress, the view or the finger changed (HLD 6). */
    private fun apply(finger: StrokePoint?) {
        if (view.showDirection || view.showCurrent || view.showNext) ensureShown()
        canvasView.update(ps, finger, view)
    }

    private fun ensureShown() {
        if (popup.isShowing || anchor.windowToken == null) return
        popup.width = anchor.width
        popup.height = anchor.height + extraTopPx
        val loc = IntArray(2)
        anchor.getLocationInWindow(loc)
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, loc[0], loc[1] - extraTopPx)
    }

    /** Draws with preallocated paints only (LLD-06 §3 performance). Coordinates: anchor px + [extraTopPx] down. */
    private inner class GuideCanvasView(context: Context) : View(context) {
        private val keyRect = Rect()
        private var key: SwipeKeyDef? = null
        private var ps: ProgressState? = null
        private var fingerX = 0f
        private var fingerY = 0f
        private var hasFinger = false
        private var view: GuideView = HIDDEN

        private val currentPaint = textPaint(Color.WHITE)
        private val candidatePaint = textPaint(Color.WHITE)
        private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC303030.toInt() }

        fun set(key: SwipeKeyDef, keyRectPx: Rect, ps: ProgressState?, finger: StrokePoint?, v: GuideView) {
            this.key = key
            keyRect.set(keyRectPx)
            currentPaint.textSize = keyRect.height() * CURRENT_SCALE // D-15
            candidatePaint.textSize = keyRect.height() * CANDIDATE_SCALE
            update(ps, finger, v)
        }

        private fun fingerMoved(finger: StrokePoint): Boolean =
            !hasFinger || finger.x * density != fingerX || finger.y * density != fingerY

        fun update(ps: ProgressState?, finger: StrokePoint?, v: GuideView) {
            if (ps == this.ps && v == view && (finger == null || !fingerMoved(finger))) return
            this.ps = ps
            this.view = v
            if (finger != null) {
                fingerX = finger.x * density
                fingerY = finger.y * density
                hasFinger = true
            }
            invalidate()
        }

        fun clear() {
            key = null
            ps = null
            hasFinger = false
            view = HIDDEN
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            if (key == null) return
            val top = extraTopPx.toFloat()
            val cx = keyRect.exactCenterX()
            val above = keyRect.top + top
            val s = ps
            if (view.showCurrent && s != null && s.current.isNotEmpty()) {
                val base = above - currentPaint.descent()
                val half = currentPaint.measureText(s.current) / 2 + PAD_DP * density
                canvas.drawRect(cx - half, above - currentPaint.textSize - PAD_DP * density, cx + half, above, bubblePaint)
                canvas.drawText(s.current, cx, base, currentPaint)
            }
            if (s == null || !hasFinger) return
            // Before the first exit: direction guide (RING/OUTER…); after it: next characters (FR-DSP-02, 04).
            val show = if (s.beforeExit) view.showDirection else view.showNext
            if (!show) return
            val list = s.candidates
            for (i in list.indices) drawCandidate(canvas, list[i], top)
        }

        private fun drawCandidate(canvas: Canvas, c: Candidate, top: Float) {
            val d = c.offsetDp * density // Dir.unit would box a Pair per call
            val x = fingerX + when (c.dir) { Dir.LEFT -> -d; Dir.RIGHT -> d; else -> 0f }
            val y = fingerY + top + when (c.dir) { Dir.UP -> -d; Dir.DOWN -> d; else -> 0f }
            val r = candidatePaint.textSize * 0.75f
            canvas.drawCircle(x, y, r, bubblePaint)
            canvas.drawText(c.label, x, y - (candidatePaint.ascent() + candidatePaint.descent()) / 2, candidatePaint)
        }

        private fun textPaint(c: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = c
            textAlign = Paint.Align.CENTER
        }
    }

    private companion object {
        val HIDDEN = GuideView(showDirection = false, showCurrent = false, showNext = false)

        /** Current character = 0.8 × key height (D-15). */
        const val CURRENT_SCALE = 0.8f
        const val CANDIDATE_SCALE = 0.4f
        const val PAD_DP = 4f
    }
}
