package kbd.app.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kbd.app.feedback.FeedbackPort
import kbd.app.graffiti.GraffitiGuidePanel
import kbd.app.store.FloatPlace
import kbd.app.store.FloatSetting
import kbd.app.store.FloatingKeyboard
import kbd.app.store.KeyboardPlate
import kbd.app.store.KeyboardSpan
import kbd.app.store.KeyboardWidth
import kbd.app.store.PxRect
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
 * The mode view gets the width that is left. The padding inside the keyboard plate (below) is painted
 * [BACKGROUND_COLOR] and, like the blank band, takes touches with no input.
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
 * margins are this view's own place, like the band: [BACKGROUND_COLOR], touches taken with no input, and a finger
 * that comes down there is not given to the mode view even while another finger is on it ([OffKeyboardPointers]).
 * A stroke that began on the mode view stays with it when it goes out over a margin or the rest of the width.
 *
 * Keyboard plate (SRS FR-CMN-14 ⑫, HLD 5A, LLD-08 §7.3, FSS#85): this view has no background. It paints one rectangle,
 * [plate] (the keyboard rectangle's width, with the side padding it reaches, by the view's height), and the service
 * reports the same rectangle as the window's touchable region. The rest of the width and the padding under it are
 * left clear: nothing is drawn there, and a touch that begins there should not be sent to this window (the platform's
 * part, not confirmed on a device: LLD-08 §7.3). One that is sent all the same is, as before, off the children: taken
 * with no input.
 *
 * Floating keyboard (SRS FR-CMN-15, HLD 5A "띄운 자판", LLD-08 §7.4, FSS#86): with the orientation's floating setting
 * on ([portraitFloat], [landscapeFloat]) and room for it, the view measures itself as high as its parent allows and
 * the plate is a move handle and the keyboard rectangle somewhere in it ([FloatingKeyboard.place], [floatPlace]); the
 * children are placed on the plate, and nothing else is painted or touchable. A long press on the handle moves the
 * plate ([PlateMover]); the place it is put at is told to [onFloatMoved]. With the setting off, or no room, every
 * path here is the docked one above: same height, same places, same plate, no handle.
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

    /**
     * Floating settings (FR-CMN-15, FR-SET-09): the orientation's one is used. Call [relayout] after changing them.
     * The view itself changes the shares of the one in use when its plate is put somewhere ([onFloatMoved]).
     */
    var portraitFloat: FloatSetting = FloatSetting.DEFAULT
    var landscapeFloat: FloatSetting = FloatSetting.DEFAULT

    /** The one vibration when a move begins (FR-CMN-15 ⑦ 나); the port follows the vibration setting. */
    var moveFeedback: FeedbackPort? = null

    /**
     * The floating plate was put somewhere on the landscape (`landscape`) or portrait screen: the shares to store, null
     * on an axis that is not to be stored (FR-CMN-15 ⑨). Not called when neither is.
     */
    var onFloatMoved: ((landscape: Boolean, fx: Float?, fy: Float?) -> Unit)? = null

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

    /**
     * The keyboard plate in this view, px ([KeyboardPlate.rect]), set by [onLayout]: the one place this view paints
     * and the window takes touches. Empty until the first layout.
     */
    internal var plate: PxRect = PxRect.EMPTY
        private set

    /** True when [plate] is the whole view: nothing is left clear (a full-width keyboard). */
    internal val plateFillsView: Boolean get() = plate == PxRect(0, 0, width, height)

    /**
     * Where the floating keyboard is, set by [onLayout]; null = docked (the setting is off, or the plate does not fit).
     * Painting, the touch region and the insets read [plate] and [floated] as laid out, so they change together.
     */
    internal var floatPlace: FloatPlace? = null
        private set

    /** True when the keyboard is laid out floating (FR-CMN-15 ①④). */
    internal val floated: Boolean get() = floatPlace != null

    /** The move handle; a child only while the orientation's floating setting is on. Zero size while docked. */
    private var handle: MoveHandle? = null

    /** What [onMeasure] decided and with which orientation's setting; [onLayout] places with the same. */
    private var measuredFloat = false
    private var measuredLandscape = false

    /**
     * The top of the area a floating plate may be in: the larger of the status bar and display cutout insets this view
     * was given. Taken as given while the view is laid out floating; while it is docked (a low view may be given none)
     * only a larger value replaces it, so that the choice between docked and floating settles (LLD-08 §7.4).
     */
    private var topInset = 0

    private val mover = PlateMover(ViewConfiguration.getLongPressTimeout().toLong(), FloatingKeyboard.GRAB_SLOP_DP * density)
    private val moveHandler = Handler(Looper.getMainLooper())
    private val longPress = Runnable { onMoveEffects(mover.timeout(SystemClock.uptimeMillis(), floatPlace)) }

    private val platePainter = PlatePainter()

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
        setWillNotDraw(false)   // no background: only the plate is painted (onDraw, FR-CMN-14 ⑫)
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            // The union type gives, side by side, the larger of the two insets (OD-1 A, OD-3 A).
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.tappableElement())
            if (v.paddingLeft != nav.left || v.paddingRight != nav.right || v.paddingBottom != nav.bottom) {
                v.setPadding(nav.left, 0, nav.right, nav.bottom)
            }
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()).top
            if (top != topInset && (floated || top > topInset)) {
                topInset = top
                if (floatSetting.on) requestLayout()
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


    /** Same layout at the new width, row height or floating setting (FR-CMN-12, FR-CMN-15 ⑬); a move in progress is given up. */
    fun relayout() {
        dropMove()
        syncHandle()
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
     * The screen turned: placed again with that orientation's settings, a move in progress given up (FR-CMN-15 ⑩⑬).
     * The top inset of the other orientation is dropped and asked for again.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isLandscape != measuredLandscape) {
            topInset = 0   // the other orientation's; the insets are asked for again
            ViewCompat.requestApplyInsets(this)
        }
        relayout()
    }

    private val isLandscape: Boolean get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /** The floating setting of the screen's orientation now. */
    private val floatSetting: FloatSetting get() = if (isLandscape) landscapeFloat else portraitFloat

    /** The handle is a child while the orientation's floating setting is on, and no view at all while it is off. */
    private fun syncHandle() {
        val h = handle
        if (floatSetting.on) {
            if (h == null) handle = MoveHandle(context).also { addView(it, LayoutParams(LayoutParams.MATCH_PARENT, px(FloatingKeyboard.HANDLE_DP))) }
        } else if (h != null) {
            removeView(h)
            handle = null
        }
    }

    /**
     * The move's part of a touch event, before the children get it (FR-CMN-15 ⑦~⑩, [PlateMover]). Positions are this
     * view's: it stays where it is while the plate moves in it.
     */
    private fun onMoveEvent(ev: MotionEvent) {
        if (handle == null && mover.pointer == null) return   // docked: the machine stays at rest
        val t = ev.eventTime
        val i = ev.actionIndex
        val effects = when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN ->
                mover.down(ev.actionMasked == MotionEvent.ACTION_DOWN, ev.getPointerId(i), ev.getX(i), ev.getY(i), t, onHandle(ev.getX(i), ev.getY(i)), floatPlace)
            MotionEvent.ACTION_MOVE -> {
                val p = mover.pointer?.let(ev::findPointerIndex) ?: -1
                if (p >= 0) mover.move(ev.getX(p), ev.getY(p), t, floatPlace) else emptyList()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> mover.up(ev.getPointerId(i), t, floatPlace)
            MotionEvent.ACTION_CANCEL -> mover.cancel()
            else -> emptyList()
        }
        onMoveEffects(effects)
    }

    private fun onHandle(x: Float, y: Float): Boolean {
        val h = handle ?: return false
        return floated && x >= h.left && x < h.right && y >= h.top && y < h.bottom
    }

    private fun onMoveEffects(effects: List<PlateMover.Effect>) {
        for (e in effects) when (e) {
            PlateMover.Effect.Started -> {
                moveFeedback?.grab()
                handle?.held = true
                swallowGesture = true   // no key input while the plate moves, nor from this gesture's later fingers (⑧)
            }
            is PlateMover.Effect.Moved -> movePlate(e.plate)
            is PlateMover.Effect.Put -> {
                handle?.held = false
                movePlate(e.plate)
                val s = e.save
                if (s.fx != null || s.fy != null) {
                    val old = if (measuredLandscape) landscapeFloat else portraitFloat
                    val new = old.copy(fx = s.fx ?: old.fx, fy = s.fy ?: old.fy)
                    if (measuredLandscape) landscapeFloat = new else portraitFloat = new
                    onFloatMoved?.invoke(measuredLandscape, s.fx, s.fy)
                }
                requestLayout()   // the window is traversed and asks for the touch region again (LLD-08 §7.4)
            }
            is PlateMover.Effect.Cancelled -> {
                handle?.held = false
                movePlate(e.plate)
                requestLayout()
            }
        }
        armLongPress()
    }

    /** The timer for a finger that waits on the handle without moving; off when nothing waits. */
    private fun armLongPress() {
        moveHandler.removeCallbacks(longPress)
        val end = mover.waitEnd ?: return
        moveHandler.postDelayed(longPress, (end - SystemClock.uptimeMillis()).coerceAtLeast(0))
    }

    /** Gives up a wait or a move without moving anything: the layout that follows places the plate. */
    private fun dropMove() {
        mover.cancel()
        moveHandler.removeCallbacks(longPress)
        handle?.held = false
    }

    /** The plate and everything on it go to [to] with no measure or layout pass (FR-CMN-15 ⑧: one piece). */
    private fun movePlate(to: PxRect) {
        val fp = floatPlace ?: return
        val dx = to.left - plate.left
        val dy = to.top - plate.top
        if (dx == 0 && dy == 0) return
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            c.offsetLeftAndRight(dx)
            c.offsetTopAndBottom(dy)
        }
        plate = to
        floatPlace = fp.copy(plate = to)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        dropMove()
        super.onDetachedFromWindow()
    }

    /**
     * After the guide opened or closed, the rest of that gesture is taken here: its pointers already on the
     * mode view, the button or the panel get `ACTION_CANCEL` and then nothing (LLD-08 §7.1). The same takes the
     * gesture whose finger moves the floating keyboard (FR-CMN-15 ⑧).
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) swallowGesture = false   // a new gesture
        return swallowGesture
    }

    /** The children get the event without the pointers that came down off them (FR-CMN-14 ⑤, LLD-08 §7.2). */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        onMoveEvent(ev)
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
        if (mover.pointer != null) dropMove()   // laid out again: the move is given up (FR-CMN-15 ⑩)
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = px(WINDOW_ROWS * rowHeightDp) + paddingBottom
        val landscape = isLandscape
        val dm = resources.displayMetrics
        span = KeyboardWidth.place(w - paddingLeft - paddingRight, minOf(dm.widthPixels, dm.heightPixels), density,
            portraitWidth, landscapeWidth, landscape)
        // Floating (FR-CMN-15 ④): only with the handle (the setting is on) and when the plate fits the highest view
        // the parent allows. With no limit given there is no such height: docked.
        val maxH = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) 0 else MeasureSpec.getSize(heightMeasureSpec)
        measuredLandscape = landscape
        measuredFloat = handle != null && floatPlaceIn(w, maxH) != null
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c === handle) {
                c.measure(
                    MeasureSpec.makeMeasureSpec(if (measuredFloat) span.width else 0, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(if (measuredFloat) px(FloatingKeyboard.HANDLE_DP) else 0, MeasureSpec.EXACTLY),
                )
                continue
            }
            val lp = c.layoutParams
            val cw = if (lp.width >= 0) lp.width else span.keysWidth
            c.applySideMargin(span.margin)
            c.measure(
                MeasureSpec.makeMeasureSpec(cw, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(lp.height.coerceAtLeast(0), MeasureSpec.EXACTLY),
            )
        }
        setMeasuredDimension(w, if (measuredFloat) maxH else h)
    }

    /** The floating place in this view at [viewWidth] × [viewHeight] with the measured span, or null = docked. */
    private fun floatPlaceIn(viewWidth: Int, viewHeight: Int): FloatPlace? = FloatingKeyboard.place(
        viewWidth, viewHeight, paddingLeft, topInset, paddingRight, paddingBottom, span,
        px(FloatingKeyboard.HANDLE_DP) + px(WINDOW_ROWS * rowHeightDp),
        if (measuredLandscape) landscapeFloat else portraitFloat,
    )

    /**
     * The mode view and the guide panel at the bottom of the rows, at the keys' left end; the guide button at the
     * top, at the keys' right end (FR-CMN-14 ⑥, FR-GRF-09). The side margins stay empty. The plate is set here, from
     * the same span the children are placed with.
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val fp = if (measuredFloat) floatPlaceIn(right - left, bottom - top) else null
        floatPlace = fp
        val p = fp?.plate ?: KeyboardPlate.rect(right - left, bottom - top, paddingLeft, paddingRight, span)
        if (p != plate) {
            plate = p
            invalidate()
        }
        if (fp != null) {
            layoutOnPlate(p)
            return
        }
        val x = keyboardLeft
        val rowsBottom = bottom - top - paddingBottom
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == GONE) continue
            if (c === handle) {
                c.layout(0, 0, 0, 0)   // docked although the setting is on (no room): no handle to see or touch
            } else if (c === guideButton) {
                c.layout(x + span.keysWidth - c.measuredWidth, paddingTop, x + span.keysWidth, paddingTop + c.measuredHeight)
            } else {
                c.layout(x, rowsBottom - c.measuredHeight, x + c.measuredWidth, rowsBottom)
            }
        }
    }

    /**
     * A floating keyboard's children on its plate [p] (FR-CMN-15 ②, HLD 5A): the handle across the top, the rows under
     * it; in the rows the same places as docked, the keys one side margin in from the plate's left.
     */
    private fun layoutOnPlate(p: PxRect) {
        val rowsTop = p.top + px(FloatingKeyboard.HANDLE_DP)
        val x = p.left + span.margin
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == GONE) continue
            when {
                c === handle -> c.layout(p.left, p.top, p.right, rowsTop)
                c === guideButton -> c.layout(x + span.keysWidth - c.measuredWidth, rowsTop, x + span.keysWidth, rowsTop + c.measuredHeight)
                else -> c.layout(x, p.bottom - c.measuredHeight, x + c.measuredWidth, p.bottom)
            }
        }
    }

    /** The plate in the keyboard colour; outside it nothing is drawn (FR-CMN-14 ⑫). The children draw over it. */
    override fun onDraw(canvas: Canvas) {
        platePainter.draw(canvas, plate)
    }

    /**
     * Touches the children did not take (the blank band, the keyboard's side margins, the system bar padding, and any
     * touch outside the plate that the window was sent) are consumed with no input (SRS 3.3, FR-CMN-14 ⑤⑪⑫,
     * FR-CMN-11a ②).
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

    /**
     * The move handle above a floating keyboard's rows (FR-CMN-15 ②): not a key. It takes its touches so that they are
     * nobody's input, gives no key feedback, and shows a short bar, longer and lighter while the plate is held.
     */
    private inner class MoveHandle(context: Context) : View(context) {
        private val bar = Paint(Paint.ANTI_ALIAS_FLAG)

        var held = false
            set(v) {
                if (field != v) {
                    field = v
                    invalidate()
                }
            }

        init {
            contentDescription = HANDLE_NAME
        }

        override fun onDraw(canvas: Canvas) {
            val w = (if (held) HANDLE_BAR_HELD_DP else HANDLE_BAR_DP) * density
            val h = HANDLE_BAR_HEIGHT_DP * density
            bar.color = if (held) Color.WHITE else HANDLE_BAR_COLOR
            canvas.drawRoundRect((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2, h / 2, h / 2, bar)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean = true
    }

    companion object {
        /** The move handle's accessibility name (LLD-08 §7.4). */
        const val HANDLE_NAME = "자판 옮기기 손잡이"

        /** The handle's bar, dp: its length at rest and while held, its thickness; and its colour at rest. */
        private const val HANDLE_BAR_DP = 32f
        private const val HANDLE_BAR_HELD_DP = 48f
        private const val HANDLE_BAR_HEIGHT_DP = 4f
        private const val HANDLE_BAR_COLOR = 0xFF808080.toInt()

        /** QWERTY and Dubeolsik have 6 rows, the tallest layout (LLD-08 §7). */
        const val WINDOW_ROWS = 6

        /** Keyboard background; mode views and the keyboard plate share it (SRS 3.3 ②, OD-2 A, FR-CMN-14 ⑫). */
        const val BACKGROUND_COLOR = Color.BLACK

        /** The guide button's mark (LLD-08 §7.1). */
        const val BUTTON_LABEL = "?"
    }
}
