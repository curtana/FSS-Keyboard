package kbd.app.view

import android.os.Looper
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.time.Duration
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.app.store.FloatSetting
import kbd.app.store.FloatingKeyboard
import kbd.app.store.KeyboardAlign
import kbd.app.store.KeyboardWidth
import kbd.app.store.PxRect
import kbd.app.store.WidthSetting
import kbd.graffiti.recognize.TemplateSet
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.TapOutput
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * `KeyboardRootView` as a floating keyboard (LLD-11 §7.7 FL-01~FL-16, SRS FR-CMN-15, HLD 5A "띄운 자판", LLD-08 §7.4,
 * FSS#86): off is the docked keyboard as it was, on is a plate with a move handle that a long press on the handle moves.
 *
 * The root is 800 dp wide and may be [MAX_H] dp high, with [NAV_DP] of system bar padding at the bottom: the area the
 * plate may be in is 800 × 976 dp. Key 40 dp: the keyboard rectangle is 440 dp wide, the keys 400 dp, a side margin
 * 20 dp; the plate is 440 × 312 dp (24 dp of handle, six 48 dp rows). The mode view is a QWERTY `TapKeyboardView`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class KeyboardFloatViewTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val events = ArrayList<String>()

    private val sink = object : TapSink {
        override fun onTapOutput(key: TapKeyDef, out: TapOutput) {
            events += "out:" + (out.text ?: out.jamo.toString())
        }

        override fun onFunctionDown(key: FunctionKey) {
            events += "down:$key"
        }

        override fun onFunctionUp(key: FunctionKey) {
            events += "up:$key"
        }
    }

    private val feedback = object : FeedbackPort {
        override fun key() {
            events += "key"
        }

        override fun error() {
            events += "error"
        }

        override fun grab() {
            events += "grab"
        }
    }

    private object NoState : GuardedState {
        override fun snapshot(): Any = Unit
        override fun restore(s: Any) {}
    }

    private val looper get() = Shadows.shadowOf(Looper.getMainLooper())
    private val longPressMs = ViewConfiguration.getLongPressTimeout().toLong()

    /** What the root told its owner to store: (landscape, fx, fy). */
    private val saved = ArrayList<Triple<Boolean, Float?, Float?>>()

    private lateinit var root: KeyboardRootView
    private lateinit var touch: Touches
    private var density = 1f

    @Before
    fun setUp() {
        root = newRoot()
        density = root.resources.displayMetrics.density
        touch = Touches(root)
    }

    private fun newRoot(rows: Int = KeyboardRootView.WINDOW_ROWS): KeyboardRootView {
        val v = KeyboardRootView(RuntimeEnvironment.getApplication())
        v.rowHeightDp = ROW
        v.portraitWidth = WidthSetting(40f, KeyboardAlign.CENTER)
        v.landscapeWidth = WidthSetting(40f, KeyboardAlign.CENTER)
        v.setPadding(0, 0, 0, Math.round(NAV_DP * v.resources.displayMetrics.density))
        v.moveFeedback = feedback
        v.onFloatMoved = { landscape, fx, fy -> saved += Triple(landscape, fx, fy) }
        v.setModeView(tapView(), rows)
        return v
    }

    private fun tapView(): TapKeyboardView {
        val guard = EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState)
        val v = TapKeyboardView(RuntimeEnvironment.getApplication(), sink, guard, feedback)
        v.setLayout(TapLayouts.QWERTY, ThresholdDefaults.VALUES, ROW)
        return v
    }

    private fun px(dp: Float): Int = Math.round(dp * density)

    /** One traversal: the parent allows [maxHeightDp] of height (the input method's window: wrap content, at most). */
    private fun traverse(v: KeyboardRootView = root, maxHeightDp: Float = MAX_H, widthDp: Float = 800f) {
        v.measure(
            View.MeasureSpec.makeMeasureSpec(px(widthDp), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(px(maxHeightDp), View.MeasureSpec.AT_MOST),
        )
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)
    }

    /** A setting changed: placed again, as the service does (`applySettings`, `onConfigurationChanged`). */
    private fun relayout(v: KeyboardRootView = root, maxHeightDp: Float = MAX_H) {
        v.relayout()
        traverse(v, maxHeightDp)
    }

    private fun float(v: KeyboardRootView = root, fx: Float? = null, fy: Float? = null) {
        v.portraitFloat = FloatSetting(true, fx, fy)
        relayout(v)
    }

    private val rowsPx: Int get() = px(KeyboardRootView.WINDOW_ROWS * ROW)
    private val handlePx: Int get() = px(FloatingKeyboard.HANDLE_DP)

    private fun children(v: ViewGroup = root): List<View> = (0 until v.childCount).map(v::getChildAt)
    private fun handle(v: ViewGroup = root): View? = children(v).firstOrNull { it.contentDescription == KeyboardRootView.HANDLE_NAME }
    private fun modeView(v: ViewGroup = root): View = children(v).first { it is TapKeyboardView }
    private fun rect(v: View) = PxRect(v.left, v.top, v.right, v.bottom)

    /** Centre of QWERTY key [id] in the root, px. */
    private fun keyX(id: String, v: KeyboardRootView = root): Float {
        val k = TapLayouts.QWERTY.keys.first { it.id == id }
        val m = modeView(v)
        return m.left + (k.x + k.w / 2f) * m.width / 360f
    }

    private fun keyY(id: String, v: KeyboardRootView = root): Float {
        val k = TapLayouts.QWERTY.keys.first { it.id == id }
        val m = modeView(v)
        return m.top + (k.row + 0.5f) * m.height / TapLayouts.QWERTY.rows
    }

    private val handleX: Float get() = root.plate.left + root.plate.width / 2f
    private val handleY: Float get() = root.plate.top + handlePx / 2f

    private fun idle(ms: Long) = looper.idleFor(Duration.ofMillis(ms))

    /** Finger [id] comes down on the handle and stays for the long press time: the move has begun. */
    private fun grab(id: Int = 0) {
        touch.down(id, handleX, handleY)
        idle(longPressMs)
    }

    // --- off, or no room: the docked keyboard ---

    @Test
    fun `FL-01 off - height, places, plate and children are the docked keyboard's, with no handle view`() {
        // The docked keyboard as it was: no floating setting, the height asked with no limit (as the tests before).
        val docked = newRoot()
        docked.relayout()
        docked.measure(View.MeasureSpec.makeMeasureSpec(px(800f), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        docked.layout(0, 0, docked.measuredWidth, docked.measuredHeight)
        assertEquals(rowsPx + px(NAV_DP), docked.height)
        assertEquals(PxRect(px(180f), 0, px(620f), rowsPx + px(NAV_DP)), docked.plate)

        // Off, a parent that allows much more; and on for the other orientation only.
        val off = newRoot()
        relayout(off)
        val otherOn = newRoot()
        otherOn.landscapeFloat = FloatSetting(true, 0.2f, 0.2f)
        relayout(otherOn)
        for (v in listOf(off, otherOn)) {
            assertFalse(v.floated)
            assertNull(v.floatPlace)
            assertEquals(docked.height, v.height)
            assertEquals(docked.plate, v.plate)
            assertEquals(docked.childCount, v.childCount)
            assertNull(handle(v))
            assertEquals(rect(modeView(docked)), rect(modeView(v)))
            val canvas = RectCanvas()
            v.draw(canvas)
            assertEquals(listOf(px(180f).toFloat(), 0f, px(620f).toFloat(), v.height.toFloat()) to KeyboardRootView.BACKGROUND_COLOR, canvas.rects.first())
        }
        // Switched on and off again: the handle view is gone, the rest as before.
        float(off)
        assertTrue(off.floated)
        off.portraitFloat = FloatSetting(false, null, null)
        relayout(off)
        assertFalse(off.floated)
        assertNull(handle(off))
        assertEquals(docked.height, off.height)
        assertEquals(docked.plate, off.plate)
        assertEquals(rect(modeView(docked)), rect(modeView(off)))
        // No move begins on a docked keyboard: where the handle would be is the app's, a key is a key.
        val t = Touches(off)
        t.down(0, keyX("q", off), keyY("q", off))
        idle(3 * longPressMs)
        t.move(0, keyX("q", off) + px(2f), keyY("q", off))
        t.up(0)
        assertEquals(listOf("key", "out:q"), events)
        assertEquals(emptyList<Triple<Boolean, Float?, Float?>>(), saved)
    }

    @Test
    fun `FL-02 on but the plate does not fit - docked in every way, the handle has no size`() {
        root.portraitFloat = FloatSetting(true, 0.5f, 0.5f)
        // 312 dp of plate and 24 dp of bar need 336 dp: one px less is docked, as is a parent that gives no limit.
        val docked = newRoot()
        relayout(docked)
        root.relayout()
        root.measure(View.MeasureSpec.makeMeasureSpec(px(800f), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(px(336f) - 1, View.MeasureSpec.AT_MOST))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        assertFalse(root.floated)
        assertEquals(docked.height, root.height)
        assertEquals(docked.plate, root.plate)
        assertEquals(rect(modeView(docked)), rect(modeView()))
        assertEquals(PxRect(0, 0, 0, 0), rect(handle()!!))
        touch.down(0, root.plate.left + 5f, 1f)   // where a handle would start: the top row's key, no move
        idle(2 * longPressMs)
        touch.up(0)
        assertFalse(events.contains("grab"))
        events.clear()
        root.measure(View.MeasureSpec.makeMeasureSpec(px(800f), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        assertFalse(root.floated)
        assertEquals(docked.plate, root.plate)
        // Exactly enough: floating, with no range up or down.
        traverse(maxHeightDp = 336f)
        assertTrue(root.floated)
        assertEquals(px(336f), root.height)
        assertEquals(0, root.floatPlace!!.rangeY)
        assertEquals(PxRect(px(180f), 0, px(620f), px(312f)), root.plate)
        // The stored place is kept for when there is room again.
        assertEquals(FloatSetting(true, 0.5f, 0.5f), root.portraitFloat)
    }

    // --- on: the plate ---

    @Test
    fun `FL-03 on - the view is as high as allowed, the plate is handle and rectangle at the bottom end, alignment across`() {
        for ((align, left) in listOf(KeyboardAlign.LEFT to 0f, KeyboardAlign.CENTER to 180f, KeyboardAlign.RIGHT to 360f)) {
            root.portraitWidth = WidthSetting(40f, align)
            float()
            assertTrue("$align", root.floated)
            assertEquals("$align", px(MAX_H), root.height)
            val plate = root.plate
            // The bar padding is not in the plate, though the plate is at the bottom end of the area.
            assertEquals("$align", PxRect(px(left), px(MAX_H - NAV_DP) - handlePx - rowsPx, px(left + 440f), px(MAX_H - NAV_DP)), plate)
            assertEquals("$align", PxRect(0, 0, px(800f), px(MAX_H - NAV_DP)), root.floatPlace!!.area)
            // The keyboard rectangle is where the docked one is: same left, its rows ending above the bar.
            assertEquals("$align", root.rectLeft, plate.left)
            assertEquals("$align", root.rectWidth, plate.width)
            val h = handle()!!
            assertEquals("$align", PxRect(plate.left, plate.top, plate.right, plate.top + handlePx), rect(h))
            val m = modeView()
            assertEquals("$align", PxRect(plate.left + root.sideMargin, plate.top + handlePx, plate.left + root.sideMargin + root.keyboardWidth, plate.bottom), rect(m))
            assertEquals("$align", px(400f), m.width)
            assertFalse("$align", root.plateFillsView)
        }
    }

    @Test
    fun `FL-04 on - the root paints the plate and nothing else`() {
        val bare = KeyboardRootView(RuntimeEnvironment.getApplication())
        bare.rowHeightDp = ROW
        bare.portraitWidth = WidthSetting(40f, KeyboardAlign.RIGHT)
        bare.setPadding(px(48f), 0, 0, px(NAV_DP))   // a side bar the plate is not set against, and the bottom bar
        bare.portraitFloat = FloatSetting(true, 1f, 0.5f)
        relayout(bare)
        assertNull(bare.background)
        val p = bare.plate
        assertEquals(PxRect(px(360f), px(332f), px(800f), px(644f)), p)   // (976 − 312) / 2 = 332
        val canvas = RectCanvas()
        bare.draw(canvas)
        val own = canvas.rects.filter { it.second == KeyboardRootView.BACKGROUND_COLOR }
        assertEquals(listOf(listOf(p.left.toFloat(), p.top.toFloat(), p.right.toFloat(), p.bottom.toFloat()) to KeyboardRootView.BACKGROUND_COLOR), own)
        // Set against the left side bar: the bar's padding is still not in the plate.
        bare.portraitFloat = FloatSetting(true, 0f, 1f)
        relayout(bare)
        assertEquals(PxRect(px(48f), px(664f), px(488f), px(976f)), bare.plate)
        // A full-width keyboard: the plate is as wide as the area, moved up and down only.
        bare.setPadding(0, 0, 0, px(NAV_DP))
        bare.portraitWidth = WidthSetting.DEFAULT
        bare.portraitFloat = FloatSetting(true, 0.7f, 0f)
        relayout(bare)
        assertEquals(PxRect(0, 0, px(800f), px(312f)), bare.plate)
        assertEquals(0, bare.floatPlace!!.rangeX)
    }

    @Test
    fun `FL-05 stored shares place the plate - 0, a half, 1 - and a new view is placed the same`() {
        float(fx = 0f, fy = 0f)
        assertEquals(PxRect(0, 0, px(440f), px(312f)), root.plate)
        float(fx = 0.5f, fy = 0.5f)
        assertEquals(PxRect(px(180f), px(332f), px(620f), px(644f)), root.plate)
        float(fx = 1f, fy = 1f)
        assertEquals(PxRect(px(360f), px(664f), px(800f), px(976f)), root.plate)
        // With a share across, the alignment no longer places it.
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.LEFT)
        relayout()
        assertEquals(PxRect(px(360f), px(664f), px(800f), px(976f)), root.plate)
        val again = newRoot()
        float(again, fx = 1f, fy = 1f)
        assertEquals(root.plate, again.plate)
    }

    // --- moving ---

    @Test
    fun `FL-06 a long press on the handle moves the plate with the finger - everything on it by the same way`() {
        float()
        val start = root.plate
        val before = children().map(::rect)
        touch.down(0, handleX, handleY)
        idle(longPressMs - 1)
        assertEquals(emptyList<String>(), events)   // not yet
        touch.move(0, handleX + px(3f), handleY)   // within the slop
        assertEquals(start, root.plate)
        idle(1)
        assertEquals(listOf("grab"), events)   // the move began: one vibration, no key feedback
        val x0 = handleX + px(3f)
        val y0 = handleY
        touch.move(0, x0 - px(100f), y0 - px(300f), steps = 5)
        val moved = PxRect(start.left - px(100f), start.top - px(300f), start.right - px(100f), start.bottom - px(300f))
        assertEquals(moved, root.plate)
        assertEquals(moved, root.floatPlace!!.plate)
        assertEquals(before.map { PxRect(it.left - px(100f), it.top - px(300f), it.right - px(100f), it.bottom - px(300f)) }, children().map(::rect))
        assertEquals(px(MAX_H), root.height)   // the view itself did not move or change
        // What is painted follows.
        val canvas = RectCanvas()
        root.draw(canvas)
        assertEquals(listOf(moved.left.toFloat(), moved.top.toFloat(), moved.right.toFloat(), moved.bottom.toFloat()) to KeyboardRootView.BACKGROUND_COLOR, canvas.rects.first())
        assertEquals(emptyList<Triple<Boolean, Float?, Float?>>(), saved)   // nothing is stored while it moves
        touch.up(0)
        assertEquals(listOf("grab"), events)   // no vibration when it is put
        assertEquals(listOf(Triple(false, 80f / 360, 364f / 664)), saved.map { Triple(it.first, it.second, it.third) })
    }

    @Test
    fun `FL-07 the plate stops at the ends of the area and follows again when the finger comes back`() {
        float()
        val start = root.plate
        grab()
        val x0 = handleX
        val y0 = handleY
        touch.move(0, x0 - px(700f), y0 - px(2000f))   // far past the left and the top end
        assertEquals(PxRect(0, 0, px(440f), px(312f)), root.plate)
        touch.move(0, x0 + px(700f), y0 + px(2000f))   // far past the right and the bottom end
        assertEquals(PxRect(px(360f), px(664f), px(800f), px(976f)), root.plate)   // above the bar padding, never on it
        touch.move(0, x0 - px(10f), y0 - px(20f))   // back: the start plus the finger's way now
        assertEquals(PxRect(start.left - px(10f), start.top - px(20f), start.right - px(10f), start.bottom - px(20f)), root.plate)
        touch.up(0)
        // A full-width keyboard moves up and down only, and stores only that.
        saved.clear()
        root.portraitWidth = WidthSetting.DEFAULT
        relayout()
        val full = root.plate
        assertEquals(px(800f), full.width)
        grab()
        touch.move(0, handleX + px(200f), handleY - px(100f))
        assertEquals(PxRect(0, full.top - px(100f), px(800f), full.bottom - px(100f)), root.plate)
        touch.up(0)
        assertEquals(1, saved.size)
        assertNull(saved[0].second)
        assertEquals(root.portraitFloat.fy, saved[0].third)
    }

    @Test
    fun `FL-08 put - the shares are told once and kept, the next layout and a new view give the same px`() {
        float()
        grab()
        touch.move(0, handleX - px(77f), handleY - px(123f))
        touch.up(0)
        val put = root.plate
        assertEquals(1, saved.size)
        val (landscape, fx, fy) = saved[0]
        assertFalse(landscape)
        assertEquals(FloatSetting(true, fx, fy), root.portraitFloat)   // the view has them before they come back
        assertEquals(FloatSetting.DEFAULT, root.landscapeFloat)
        assertTrue(root.isLayoutRequested)   // a traversal follows: the touch region is asked for again
        traverse()
        assertEquals(put, root.plate)
        assertEquals(PxRect(put.left, put.top, put.right, put.top + handlePx), rect(handle()!!))
        relayout()   // the stored value comes back through the settings flow
        assertEquals(put, root.plate)
        val again = newRoot()   // the keyboard went down and came up, or the view was made again
        float(again, fx, fy)
        assertEquals(put, again.plate)
        assertEquals(rect(modeView()), rect(modeView(again)))
        // Another size of area: the same shares, the plate inside.
        traverse(again, maxHeightDp = 700f)
        assertEquals(Math.round(fy!! * px(700f - NAV_DP - 312f)), again.plate.top)
        assertEquals(put.left, again.plate.left)
    }

    @Test
    fun `FL-09 only the axis that changed is stored - not moved stores nothing and the alignment still places it`() {
        float()
        val start = root.plate
        grab()
        touch.up(0)   // held and let go
        assertEquals(listOf("grab"), events)
        assertEquals(emptyList<Triple<Boolean, Float?, Float?>>(), saved)
        assertEquals(FloatSetting(true, null, null), root.portraitFloat)
        traverse()
        assertEquals(start, root.plate)
        // Up only: the share down is stored, none across.
        grab()
        touch.move(0, handleX, handleY - px(200f))
        touch.up(0)
        assertEquals(1, saved.size)
        assertNull(saved[0].second)
        assertEquals(464f / 664, saved[0].third)
        assertNull(root.portraitFloat.fx)
        // So the alignment still places it across.
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.RIGHT)
        relayout()
        assertEquals(PxRect(px(360f), px(464f), px(800f), px(776f)), root.plate)
        // Across only, from there: the share across, the one down stays as it is.
        saved.clear()
        grab()
        touch.move(0, handleX - px(360f), handleY)
        touch.up(0)
        assertEquals(listOf(Triple(false, 0f, null as Float?)), saved.toList())
        assertEquals(FloatSetting(true, 0f, 464f / 664), root.portraitFloat)
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.CENTER)
        relayout()
        assertEquals(PxRect(0, px(464f), px(440f), px(776f)), root.plate)   // the alignment no longer moves it
    }

    @Test
    @Config(qualifiers = "w800dp-h400dp-land")
    fun `FL-10 landscape has its own setting and place`() {
        root.portraitFloat = FloatSetting(true, 0f, 0f)   // the portrait screen's: not used here
        relayout()
        assertFalse(root.floated)
        assertNull(handle())
        root.landscapeFloat = FloatSetting(true, null, null)
        relayout()
        assertTrue(root.floated)
        assertEquals(PxRect(px(180f), px(664f), px(620f), px(976f)), root.plate)
        grab()
        touch.move(0, handleX + px(90f), handleY - px(166f))
        touch.up(0)
        assertEquals(listOf(Triple(true, 270f / 360, 498f / 664)), saved.toList())
        assertEquals(FloatSetting(true, 0f, 0f), root.portraitFloat)   // the other orientation's is as it was
        assertEquals(FloatSetting(true, 270f / 360, 498f / 664), root.landscapeFloat)
    }

    @Test
    fun `FL-11 turning the screen places the keyboard with that orientation's setting and gives up a move`() {
        root.portraitFloat = FloatSetting(true, 0f, 0f)
        root.landscapeFloat = FloatSetting(false, 1f, 1f)
        relayout()
        assertEquals(PxRect(0, 0, px(440f), px(312f)), root.plate)
        grab()
        touch.move(0, handleX + px(50f), handleY + px(50f))
        assertEquals(PxRect(px(50f), px(50f), px(490f), px(362f)), root.plate)
        RuntimeEnvironment.setQualifiers("+land")
        relayout()   // the service's onConfigurationChanged
        assertFalse(root.floated)   // landscape is docked
        assertNull(handle())
        assertEquals(rowsPx + px(NAV_DP), root.height)
        touch.move(0, handleX, handleY + px(80f))
        touch.up(0)
        assertEquals(emptyList<Triple<Boolean, Float?, Float?>>(), saved)
        RuntimeEnvironment.setQualifiers("+port")
        relayout()
        assertEquals(PxRect(0, 0, px(440f), px(312f)), root.plate)   // where it was before the move
    }

    // --- what does not change ---

    @Test
    fun `FL-12 the handle is not a key - a tap, a short press or a stroke on it gives no input and no feedback`() {
        float()
        val start = root.plate
        touch.tap(0, handleX, handleY)
        assertTrue(touch.lastHandled)
        touch.down(0, handleX, handleY)
        idle(longPressMs - 1)
        touch.up(0)   // up before the long press time
        idle(longPressMs)
        // A stroke along the handle: 8 dp or more before the long press time drops it, however long it then stays.
        touch.down(0, handleX, handleY)
        touch.move(0, handleX + px(FloatingKeyboard.GRAB_SLOP_DP), handleY)
        touch.move(0, handleX, handleY)
        idle(3 * longPressMs)
        touch.move(0, handleX + px(100f), handleY - px(100f))
        touch.up(0)
        assertEquals(emptyList<String>(), events)
        assertEquals(start, root.plate)
        assertEquals(emptyList<Triple<Boolean, Float?, Float?>>(), saved)
        // Just under 8 dp keeps the wait.
        touch.down(0, handleX, handleY)
        touch.move(0, handleX + px(FloatingKeyboard.GRAB_SLOP_DP) - 1f, handleY)
        idle(longPressMs)
        assertEquals(listOf("grab"), events)
        touch.up(0)
    }

    @Test
    fun `FL-13 while the plate moves a finger on a key gives nothing, also after the plate is put, until it is lifted`() {
        float()
        grab()
        events.clear()
        touch.down(1, keyX("q"), keyY("q"))
        touch.move(0, handleX - px(40f), handleY - px(40f))
        val moved = root.plate
        touch.move(1, keyX("q") + px(60f), keyY("q"))
        touch.up(0)   // put
        assertEquals(moved, root.plate)
        assertEquals(1, saved.size)
        touch.move(1, keyX("w"), keyY("w"))
        touch.up(1)
        assertEquals(emptyList<String>(), events)
        traverse()
        touch.tap(0, keyX("q"), keyY("q"))   // a new touch is a key again, where the keys now are
        assertEquals(listOf("key", "out:q"), events)
    }

    @Test
    fun `FL-14 while waiting a finger on a key is that key's input and the wait is dropped`() {
        float()
        val start = root.plate
        touch.down(0, handleX, handleY)
        idle(longPressMs / 2)
        touch.down(1, keyX("q"), keyY("q"))
        touch.up(1)
        assertEquals(listOf("key", "out:q"), events)
        idle(2 * longPressMs)
        touch.move(0, handleX + px(100f), handleY - px(100f))
        touch.up(0)
        assertEquals(listOf("key", "out:q"), events)   // no move began
        assertEquals(start, root.plate)
    }

    @Test
    fun `FL-15 a held key - its repeat goes on while a finger rests on the handle, and no move begins`() {
        float()
        val start = root.plate
        touch.down(0, keyX("backspace"), keyY("backspace"))
        assertEquals(listOf("key", "down:BACKSPACE"), events)
        touch.down(1, handleX, handleY)
        idle(ViewConfiguration.getKeyRepeatTimeout() + 3L * ViewConfiguration.getKeyRepeatDelay())
        assertTrue(events.count { it == "down:BACKSPACE" } >= 3)   // the repeat went on
        idle(2 * longPressMs)
        touch.move(1, handleX + px(100f), handleY - px(100f))
        assertEquals(start, root.plate)
        assertFalse(events.contains("grab"))
        assertEquals(1, events.count { it == "key" })
        touch.up(1)
        touch.up(0)
        assertEquals("up:BACKSPACE", events.last())
        // A key held long, alone: no move either.
        events.clear()
        touch.down(0, keyX("q"), keyY("q"))
        idle(3 * longPressMs)
        touch.up(0)
        assertEquals(listOf("key", "out:q"), events)
        assertEquals(start, root.plate)
    }

    @Test
    fun `FL-16 a stroke from a key up over the handle gives what it gives on the docked keyboard`() {
        val docked = newRoot()
        relayout(docked)
        float()
        // From a key of the number row and of the top row, straight up: over the top row, the handle and on.
        fun stroke(v: KeyboardRootView, id: String, upDp: Float): List<String> {
            events.clear()
            val t = Touches(v)
            t.down(0, keyX(id, v), keyY(id, v))
            t.move(0, keyX(id, v), keyY(id, v) - px(upDp), steps = 8)
            t.up(0)
            return events.toList()
        }
        for (id in listOf("1", "5", "esc", "backspace", "q")) for (up in listOf(30f, 80f, 130f, 400f)) {
            val want = stroke(docked, id, up)
            assertTrue("$id $up", want.isNotEmpty())
            assertEquals("$id up $up dp", want, stroke(root, id, up))
        }
        // The up stroke of the number row's key is its upper symbol on both.
        assertEquals(listOf("key", "out:!"), stroke(root, "1", 80f))
        assertFalse(events.contains("grab"))
    }

    @Test
    fun `FL-17 another mode view or the stroke guide leaves the plate, and the plate moves with the guide open`() {
        val v = newRoot(rows = 5)   // a five-row mode: a blank band under the handle
        v.portraitFloat = FloatSetting(true, 0.25f, 0.25f)
        relayout(v)
        val plate = v.plate
        assertEquals(PxRect(plate.left + v.sideMargin, plate.bottom - px(5 * ROW), plate.left + v.sideMargin + px(400f), plate.bottom), rect(modeView(v)))
        v.enableGuide(TemplateSet(emptyMap()), feedback)
        relayout(v)
        assertEquals(plate, v.plate)
        val button = children(v).first { it.width == px(ROW) && it.height == px(ROW) }
        // The guide button: the band's right end, under the handle.
        assertEquals(PxRect(modeView(v).right - px(ROW), plate.top + handlePx, modeView(v).right, plate.top + handlePx + px(ROW)), rect(button))
        v.openGuide()
        traverse(v)
        assertEquals(plate, v.plate)
        v.setModeView(tapView(), KeyboardRootView.WINDOW_ROWS)
        traverse(v)
        assertEquals(plate, v.plate)
        assertTrue(v.guideOpen)
        // Moved with the guide open: the guide stays open and goes along.
        val t = Touches(v)
        t.down(0, plate.left + plate.width / 2f, plate.top + handlePx / 2f)
        idle(longPressMs)
        t.move(0, plate.left + plate.width / 2f + px(30f), plate.top + handlePx / 2f + px(40f))
        t.up(0)
        assertTrue(v.guideOpen)
        assertEquals(PxRect(plate.left + px(30f), plate.top + px(40f), plate.right + px(30f), plate.bottom + px(40f)), v.plate)
        assertEquals(v.plate.top + handlePx, button.top)
    }

    // --- cancel, placing again ---

    @Test
    fun `FL-18 cancel - the plate goes back to where the move began and nothing is stored`() {
        float(fx = 0.5f, fy = 0.5f)
        val start = root.plate
        // The touch is cancelled.
        grab()
        touch.move(0, handleX + px(60f), handleY - px(60f))
        assertEquals(start.left + px(60f), root.plate.left)
        touch.cancel()
        assertEquals(start, root.plate)
        assertEquals(rect(handle()!!).top, start.top)
        assertTrue(root.isLayoutRequested)   // the touch region is asked for again
        traverse()
        assertEquals(start, root.plate)
        // The keyboard goes down (the service: cancelTouches).
        grab()
        touch.move(0, handleX - px(60f), handleY + px(60f))
        root.cancelTouches()
        assertEquals(start, root.plate)
        touch.move(0, handleX - px(90f), handleY + px(90f))   // the finger is still down: nothing follows it
        touch.up(0)
        assertEquals(start, root.plate)
        // A setting changed and the keyboard is placed again.
        grab()
        touch.move(0, handleX + px(33f), handleY + px(33f))
        relayout()
        assertEquals(start, root.plate)
        touch.move(0, handleX + px(66f), handleY + px(66f))
        touch.up(0)
        assertEquals(start, root.plate)
        // While waiting: nothing begins afterwards.
        events.clear()
        touch.down(0, handleX, handleY)
        root.cancelTouches()
        idle(2 * longPressMs)
        touch.up(0)
        touch.down(0, handleX, handleY)
        relayout()
        idle(2 * longPressMs)
        touch.up(0)
        assertEquals(emptyList<String>(), events)
        assertEquals(emptyList<Triple<Boolean, Float?, Float?>>(), saved)
        assertEquals(FloatSetting(true, 0.5f, 0.5f), root.portraitFloat)
    }

    @Test
    fun `FL-19 placed again after a setting - the switch, the key size, the row height - with the stored shares kept`() {
        float(fx = 0.5f, fy = 0.5f)
        root.portraitWidth = WidthSetting(50f, KeyboardAlign.CENTER)   // 550 dp wide: 250 dp of range
        relayout()
        assertEquals(PxRect(px(125f), px(332f), px(675f), px(644f)), root.plate)
        root.rowHeightDp = 40f   // 264 dp of plate: 712 dp of range
        relayout()
        assertEquals(PxRect(px(125f), px(356f), px(675f), px(620f)), root.plate)
        assertEquals(px(240f), modeView().height)
        root.portraitFloat = root.portraitFloat.copy(on = false)
        relayout()
        assertFalse(root.floated)
        assertEquals(px(240f) + px(NAV_DP), root.height)
        assertEquals(FloatSetting(false, 0.5f, 0.5f), root.portraitFloat)   // the place is kept while off
        root.portraitFloat = root.portraitFloat.copy(on = true)
        relayout()
        assertEquals(PxRect(px(125f), px(356f), px(675f), px(620f)), root.plate)
        // The parent allows less (another window size): the same shares in the new area.
        traverse(maxHeightDp = 500f)
        assertEquals(PxRect(px(125f), px(106f), px(675f), px(370f)), root.plate)
    }

    @Test
    fun `FL-20 the top of the area - a status bar or cutout inset given while floating, kept while docked`() {
        fun give(top: Int) = ViewCompat.dispatchApplyWindowInsets(
            root,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, top, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, px(NAV_DP)))
                .build(),
        )
        float(fx = 0.5f, fy = 0f)
        assertEquals(0, root.plate.top)
        give(px(60f))   // the high view reaches under the status bar
        assertTrue(root.isLayoutRequested)
        traverse()
        assertEquals(PxRect(0, px(60f), px(800f), px(976f)), root.floatPlace!!.area)
        assertEquals(px(60f), root.plate.top)   // the top end is under the status bar
        // The cutout, when larger, is the top.
        ViewCompat.dispatchApplyWindowInsets(
            root,
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, px(60f), 0, 0))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(0, px(90f), 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, px(NAV_DP)))
                .build(),
        )
        traverse()
        assertEquals(px(90f), root.plate.top)
        give(px(60f))
        traverse()
        assertEquals(px(60f), root.plate.top)   // while floating the inset is taken as given, smaller too

        // A parent that allows 380 dp: 356 dp of area without the status bar (the 312 dp plate fits), 296 dp with it.
        // The low docked view is given no top inset; the choice must not go back and forth.
        val low = 380f
        traverse(maxHeightDp = low)
        assertFalse(root.floated)   // the status bar's 60 dp is known: docked
        give(0)   // docked, the view is low: no status bar inset comes
        traverse(maxHeightDp = low)
        assertFalse(root.floated)   // still docked: the inset known from floating is kept
        traverse(maxHeightDp = low)
        assertFalse(root.floated)
        // A new view there starts with none known, floats once, is given the inset and settles docked.
        root = newRoot()
        root.portraitFloat = FloatSetting(true, 0.5f, 0f)
        relayout(maxHeightDp = low)
        assertTrue(root.floated)
        give(px(60f))
        traverse(maxHeightDp = low)
        assertFalse(root.floated)
        give(0)
        traverse(maxHeightDp = low)
        assertFalse(root.floated)
        // With room again it floats, under the status bar.
        traverse()
        assertTrue(root.floated)
        assertEquals(px(60f), root.plate.top)
    }

    private companion object {
        const val ROW = 48f
        const val NAV_DP = 24f
        const val MAX_H = 1000f
    }
}
