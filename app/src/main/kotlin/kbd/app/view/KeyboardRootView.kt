package kbd.app.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kbd.app.feedback.FeedbackPort
import kbd.app.graffiti.GraffitiGuidePanel
import kbd.app.store.KeyboardSpan
import kbd.app.store.KeyboardWidth
import kbd.app.store.RowHeight
import kbd.app.store.WidthSetting
import kbd.graffiti.recognize.TemplateSet

/**
 * Fixed-height, bottom-aligned keyboard window (LLD-08 §7, SRS 3.3, HLD 5A). The window is always
 * [WINDOW_ROWS] rows high; a mode view of fewer rows sits at the bottom with a blank band above it.
 *
 * System bar margin (SRS 3.3 window rule "system bars", FR-CMN-12, HLD 5A, owner decision #93 OD-1·2·3 A):
 * the IME window is edge-to-edge (target SDK 35+), so the navigation bar and the system's tappable row
 * under the keyboard (hide-keyboard and IME switcher buttons), the larger of the two on each side, are
 * padding below the rows and, when the bar is at the side (landscape 3-button navigation), beside them.
 * The mode view gets the width that is left. The padding is painted [BACKGROUND_COLOR] and, like the
 * blank band, takes touches with no input.
 *
 * Stroke guide (SRS FR-GRF-09~09a, HLD 5장, LLD-08 §7.1): in graffiti mode ([enableGuide]) the band has a
 * 1 × 1 row guide button at its right end. It toggles, on press, an opaque [GraffitiGuidePanel] over the
 * mode view; while it is open the mode view gets no touch (the touch in progress is cancelled when it opens).
 * The open state is view state only: a new root starts closed.
 *
 * Keyboard width (SRS FR-CMN-14, HLD 5A, LLD-08 §7.2, FSS#25, FSS#84): [KeyboardWidth.place] gives the keyboard
 * rectangle in the width left by the system bar padding, with the setting of the screen's orientation. The rectangle
 * is a side margin, the keys and a side margin; the mode view, the guide panel and the band's guide button sit on the
 * keys ([keyboardLeft], [keyboardWidth]), and the mode view is told the margin's width ([SideMarginView]). The side
 * margins and the rest of the width are this view's own place, like the band: [BACKGROUND_COLOR], touches taken with
 * no input, and a finger that comes down there is not given to the mode view even while another finger is on it
 * ([OffKeyboardPointers]). A stroke that began on the mode view stays with it when it goes out over a margin.
 */
class KeyboardRootView(context: Context) : FrameLayout(context) {

    private val density = resources.displayMetrics.density

    /** Row height setting (FR-SET-06, 40–64 dp). Call [relayout] after changing it. */
    var rowHeightDp: Float = RowHeight.DEFAULT_DP

    /**
     * Key size settings (FR-SET-09): the orientation's one is used, the landscape default being the portrait key size.
     * Call [relayout] after changing them.
     */
    var portraitWidth: WidthSetting = WidthSetting.DEFAULT
    var landscapeWidth: WidthSetting = WidthSetting.DEFAULT

    /** The keyboard rectangle's span in the available width, set by [onMeasure]. */
    private var span = KeyboardSpan(0, 0, 0)

    /**
     * Left end of the keyboard rectangle (side margins included) in this view, px. What lies outside
     * [rectLeft] until [rectLeft] + [rectWidth] is the rest of the width (FR-CMN-14 ⑤).
     */
    internal val rectLeft: Int get() = paddingLeft + span.left

    /** Width of the keyboard rectangle, px. */
    internal val rectWidth: Int get() = span.width

    /** Width of one side margin, px. */
    internal val sideMargin: Int get() = span.margin

    /** Left end of the keys (the mode view) in this view, px. */
    internal val keyboardLeft: Int get() = paddingLeft + span.keysLeft

    /** Width of the keys, px. */
    internal val keyboardWidth: Int get() = span.keysWidth

    private val offKeyboard = OffKeyboardPointers(::onChild)

    private var rows = WINDOW_ROWS
    private var modeView: View? = null

    private var guideFeedback: FeedbackPort? = null
    private var guideButton: GuideButton? = null
    private var guidePanel: GraffitiGuidePanel? = null

    /** The rest of the gesture in which the guide opened or closed goes to nobody (LLD-08 §7.1). */
    private var swallowGesture = false

    /** True while the stroke guide panel is shown. */
    val guideOpen: Boolean get() = guidePanel?.visibility == VISIBLE

    init {
        setBackgroundColor(BACKGROUND_COLOR)   // the padding shows this (OD-2 A)
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            // The union type gives, side by side, the larger of the two insets (OD-1 A, OD-3 A).
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.tappableElement())
            if (v.paddingLeft != nav.left || v.paddingRight != nav.right || v.paddingBottom != nav.bottom) {
                v.setPadding(nav.left, 0, nav.right, nav.bottom)
            }
            insets
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ViewCompat.requestApplyInsets(this)   // the padding must not depend on an earlier dispatch
    }

    /** The mode view: [v] is [rows] rows high, at the bottom, under the guide button and panel. */
    fun setModeView(v: View, rows: Int) {
        this.rows = rows
        modeView?.let { removeView(it) }
        modeView = v
        addView(v, 0, LayoutParams(LayoutParams.MATCH_PARENT, px(rows * rowHeightDp), Gravity.BOTTOM))
        guidePanel?.layoutParams?.height = px(rows * rowHeightDp)
    }

    /**
     * Graffiti mode: shows the guide button; the panel draws [templates] (FR-GRF-09c). The open state is
     * kept when the mode view is replaced in the same mode (field change, FR-GRF-09). The panel is made on the
     * first call and keeps that call's [templates] and [feedback]: the service passes the same objects for
     * its whole life (read once at start, LLD-05 §7; one `Feedback`, LLD-08 §8).
     */
    fun enableGuide(templates: TemplateSet, feedback: FeedbackPort) {
        guideFeedback = feedback
        if (guidePanel == null) {
            // Made once and kept laid out: INVISIBLE (closed) takes no touch but has its size when it opens.
            val p = GraffitiGuidePanel(context, templates, feedback) { closeGuide() }
            p.visibility = INVISIBLE
            guidePanel = p
            addView(p, LayoutParams(LayoutParams.MATCH_PARENT, px(rows * rowHeightDp), Gravity.BOTTOM))
        }
        val b = guideButton ?: GuideButton(context).also {
            guideButton = it
            addView(it, LayoutParams(px(rowHeightDp), px(rowHeightDp), Gravity.TOP or Gravity.END))
        }
        b.visibility = VISIBLE
    }

    /** Leaving graffiti mode: the guide closes and the button goes (FR-GRF-09). */
    fun disableGuide() {
        closeGuide()
        guideButton?.visibility = GONE
    }

    /** Opens the panel over the mode view and cancels the mode view's touch in progress (FR-GRF-09, HLD 5장). */
    fun openGuide() {
        val p = guidePanel ?: return
        if (guideOpen || guideButton?.visibility != VISIBLE) return
        cancelModeViewTouch()
        p.visibility = VISIBLE
        swallowGesture = true
        guideButton?.invalidate()
    }

    /** Closes the panel; it just goes (FR-GRF-09). The next open starts at the top again. */
    fun closeGuide() {
        val p = guidePanel ?: return
        if (!guideOpen) return
        p.visibility = INVISIBLE   // not removed: this may run inside the panel's own touch dispatch (the ✕)
        p.rewind()   // the next open starts at the top with no touch state left
        swallowGesture = true
        guideButton?.invalidate()
    }

    /** Draws the mode view again, for a display setting it reads when it draws (the stroke trail, FSS#57). */
    fun redrawModeView() {
        modeView?.invalidate()
    }

    /** Same layout at the new width or row height (FR-CMN-12). */
    fun relayout() {
        modeView?.layoutParams?.height = px(rows * rowHeightDp)
        guidePanel?.layoutParams?.height = px(rows * rowHeightDp)
        guideButton?.layoutParams?.let {
            it.width = px(rowHeightDp)
            it.height = px(rowHeightDp)
        }
        requestLayout()
        invalidate()
    }

    /** Ends the touch in progress as `ACTION_CANCEL`: no input from it, repeats stop (D-27). */
    fun cancelTouches() {
        val t = SystemClock.uptimeMillis()
        val e = MotionEvent.obtain(t, t, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        dispatchTouchEvent(e)
        e.recycle()
    }

    private fun cancelModeViewTouch() {
        val v = modeView ?: return
        val t = SystemClock.uptimeMillis()
        val e = MotionEvent.obtain(t, t, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        v.dispatchTouchEvent(e)   // stroke dropped, no error vibration, ⌫ repeat stops (FR-CMN-11a ③)
        e.recycle()
    }

    /**
     * After the guide opened or closed, the rest of that gesture is taken here: its pointers already on the
     * mode view, the button or the panel get `ACTION_CANCEL` and then nothing (LLD-08 §7.1).
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) swallowGesture = false   // a new gesture
        return swallowGesture
    }

    /** The children get the event without the pointers that came down off them (FR-CMN-14 ⑤, LLD-08 §7.2). */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val e = offKeyboard.filter(ev) ?: return true   // only off-keyboard pointers: taken, no input
        val handled = super.dispatchTouchEvent(e)
        if (e !== ev) e.recycle()
        return handled || offKeyboard.active
    }

    /** True when ([x], [y]) is on a shown child: the mode view, the open guide panel or the guide button. */
    private fun onChild(x: Float, y: Float): Boolean {
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == VISIBLE && x >= c.left && x < c.right && y >= c.top && y < c.bottom) return true
        }
        return false
    }

    /**
     * Height = [WINDOW_ROWS] rows + the bottom padding. Children of `MATCH_PARENT` width (the mode view and the guide
     * panel) get the width of the keys of the orientation's setting (FR-CMN-14 ④); the available width is the measured
     * width less the side padding (system bars), the portrait width is the screen's short side (③). The mode view is
     * told the side margin before it is measured (⑧).
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = px(WINDOW_ROWS * rowHeightDp) + paddingBottom
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val dm = resources.displayMetrics
        span = KeyboardWidth.place(w - paddingLeft - paddingRight, minOf(dm.widthPixels, dm.heightPixels), density,
            portraitWidth, landscapeWidth, landscape)
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            val lp = c.layoutParams
            val cw = if (lp.width >= 0) lp.width else span.keysWidth
            c.applySideMargin(span.margin)
            c.measure(
                MeasureSpec.makeMeasureSpec(cw, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(lp.height.coerceAtLeast(0), MeasureSpec.EXACTLY),
            )
        }
        setMeasuredDimension(w, h)
    }

    /**
     * The mode view and the guide panel at the bottom of the rows, at the keys' left end; the guide button at the
     * top, at the keys' right end (FR-CMN-14 ⑥, FR-GRF-09). The side margins stay empty.
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val x = keyboardLeft
        val rowsBottom = bottom - top - paddingBottom
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == GONE) continue
            if (c === guideButton) {
                c.layout(x + span.keysWidth - c.measuredWidth, paddingTop, x + span.keysWidth, paddingTop + c.measuredHeight)
            } else {
                c.layout(x, rowsBottom - c.measuredHeight, x + c.measuredWidth, rowsBottom)
            }
        }
    }

    /**
     * Touches the children did not take (the blank band, the keyboard's side margins, the rest of the width, the system
     * bar padding) are consumed with no input (SRS 3.3, FR-CMN-14 ⑤⑪, FR-CMN-11a ②).
     */
    override fun onTouchEvent(event: MotionEvent): Boolean = true

    private fun px(dp: Float): Int = Math.round(dp * density)

    /**
     * The guide button at the band's right end (FR-GRF-09): a key, so key feedback and the toggle on press
     * (FR-CMN-11a ①②); lit while the guide is open.
     */
    private inner class GuideButton(context: Context) : View(context) {
        private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

        init {
            contentDescription = "획 안내표"
        }

        override fun onDraw(canvas: Canvas) {
            val inset = 3 * density
            bg.color = if (guideOpen) 0xFF505050.toInt() else 0xFF2A2A2A.toInt()
            canvas.drawRoundRect(inset, inset, width - inset, height - inset, inset * 2, inset * 2, bg)
            text.color = if (guideOpen) Color.WHITE else 0xFFB0B0B0.toInt()
            text.textSize = height * 0.42f
            canvas.drawText(BUTTON_LABEL, width / 2f, height / 2f - (text.ascent() + text.descent()) / 2, text)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                guideFeedback?.key()
                if (guideOpen) closeGuide() else openGuide()
            }
            return true
        }
    }

    companion object {
        /** QWERTY and Dubeolsik have 6 rows, the tallest layout (LLD-08 §7). */
        const val WINDOW_ROWS = 6

        /** Keyboard background; mode views and the system bar padding share it (SRS 3.3 ②, OD-2 A). */
        const val BACKGROUND_COLOR = Color.BLACK

        /** The guide button's mark (LLD-08 §7.1). */
        const val BUTTON_LABEL = "?"
    }
}
