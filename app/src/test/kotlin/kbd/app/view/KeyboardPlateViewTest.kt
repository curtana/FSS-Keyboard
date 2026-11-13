package kbd.app.view

import android.view.View
import java.io.File
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.app.store.KeyboardAlign
import kbd.app.store.KeyboardWidth
import kbd.app.store.PxRect
import kbd.app.store.WidthSetting
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * `KeyboardRootView`'s keyboard plate (LLD-11 §7.7 PL-01~PL-08, SRS FR-CMN-14 ⑫, HLD 5A, LLD-08 §7.3, FSS#85): the
 * root paints the plate and nothing else, and the plate follows the layout.
 *
 * The mode view is a `TapKeyboardView` of [CHILD_ROWS] rows, as in `KeyboardRootViewTest`. Key 40 dp in 800 dp: the
 * keyboard rectangle is 440 dp, the keys 400 dp.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class KeyboardPlateViewTest {

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
    }

    private object NoState : GuardedState {
        override fun snapshot(): Any = Unit
        override fun restore(s: Any) {}
    }

    private lateinit var root: KeyboardRootView
    private lateinit var touch: Touches
    private var density = 1f

    @Before
    fun setUp() {
        root = KeyboardRootView(RuntimeEnvironment.getApplication())
        root.rowHeightDp = ROW
        root.setModeView(tapView(), CHILD_ROWS)
        density = root.resources.displayMetrics.density
        touch = Touches(root)
    }

    private fun tapView(): TapKeyboardView {
        val guard = EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState)
        val v = TapKeyboardView(RuntimeEnvironment.getApplication(), sink, guard, feedback)
        v.setLayout(TapLayouts.QWERTY, ThresholdDefaults.VALUES, ROW)
        return v
    }

    private fun px(dp: Float): Int = Math.round(dp * density)

    private fun layoutAt(v: KeyboardRootView, widthDp: Float) {
        v.relayout()   // as the service does after changing a setting
        v.measure(
            View.MeasureSpec.makeMeasureSpec(px(widthDp), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)
    }

    private fun layoutAt(widthDp: Float) = layoutAt(root, widthDp)

    private val rowsPx: Int get() = px(KeyboardRootView.WINDOW_ROWS * ROW)

    @Test
    fun `PL-01 a narrowed keyboard's plate is the keyboard rectangle by the view's height - left, centre, right`() {
        assertTrue(root.plate.isEmpty)   // before the first layout
        root.setPadding(0, 0, 0, px(NAV_DP))
        for ((align, left) in listOf(KeyboardAlign.LEFT to 0f, KeyboardAlign.CENTER to 180f, KeyboardAlign.RIGHT to 360f)) {
            root.portraitWidth = WidthSetting(40f, align)
            layoutAt(800f)
            assertEquals("$align", PxRect(px(left), 0, px(left + 440f), rowsPx + px(NAV_DP)), root.plate)
            assertEquals("$align", PxRect(root.rectLeft, 0, root.rectLeft + root.rectWidth, root.height), root.plate)
            assertFalse("$align", root.plateFillsView)
        }
    }

    @Test
    fun `PL-02 the root has no background and paints the plate only`() {
        // A root with no child: what is drawn is the root's own.
        val bare = KeyboardRootView(RuntimeEnvironment.getApplication())
        bare.rowHeightDp = ROW
        bare.setPadding(0, 0, 0, px(NAV_DP))
        assertNull(bare.background)
        assertFalse(bare.willNotDraw())   // or the platform would skip onDraw
        for (align in KeyboardAlign.values()) {
            bare.portraitWidth = WidthSetting(40f, align)
            layoutAt(bare, 800f)
            val canvas = RectCanvas()
            bare.draw(canvas)
            val p = bare.plate
            val want = listOf(p.left.toFloat(), 0f, p.right.toFloat(), bare.height.toFloat()) to KeyboardRootView.BACKGROUND_COLOR
            assertEquals("$align", listOf(want), canvas.rects)   // one rectangle: nothing on the rest of the width
            assertTrue("$align", p.right - p.left == px(440f) && p.right - p.left < bare.width)
        }
        // With the mode view: still no background, and the first thing drawn is the plate (the children draw over it).
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.CENTER)
        layoutAt(800f)
        assertNull(root.background)
        val canvas = RectCanvas()
        root.draw(canvas)
        assertEquals(listOf(px(180f).toFloat(), 0f, px(620f).toFloat(), root.height.toFloat()) to KeyboardRootView.BACKGROUND_COLOR, canvas.rects.first())
    }

    @Test
    fun `PL-03 full width - the plate is the whole view and all of it is painted`() {
        for (setting in listOf(WidthSetting.DEFAULT, WidthSetting(KeyboardWidth.FULL, KeyboardAlign.LEFT), WidthSetting(90f, KeyboardAlign.RIGHT))) {
            val bare = KeyboardRootView(RuntimeEnvironment.getApplication())
            bare.rowHeightDp = ROW
            bare.setPadding(px(SIDE_DP), 0, 0, px(NAV_DP))   // a side bar and the bottom bar
            bare.portraitWidth = setting
            layoutAt(bare, 800f)
            assertEquals("$setting", PxRect(0, 0, px(800f), rowsPx + px(NAV_DP)), bare.plate)
            assertTrue("$setting", bare.plateFillsView)
            val canvas = RectCanvas()
            bare.draw(canvas)
            assertEquals("$setting", listOf(listOf(0f, 0f, bare.width.toFloat(), bare.height.toFloat()) to KeyboardRootView.BACKGROUND_COLOR), canvas.rects)
        }
    }

    @Test
    fun `PL-04 a side bar padding is in the plate only on the side the keyboard is set against`() {
        // Bar on the left, 48 dp: the available width is 752 dp from 48 dp.
        root.setPadding(px(SIDE_DP), 0, 0, 0)
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.LEFT)
        layoutAt(800f)
        assertEquals(PxRect(0, 0, px(SIDE_DP + 440f), rowsPx), root.plate)
        assertEquals(px(SIDE_DP), root.rectLeft)   // the keyboard rectangle itself starts after the bar
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.CENTER)
        layoutAt(800f)
        assertEquals(PxRect(px(SIDE_DP + 156f), 0, px(SIDE_DP + 596f), rowsPx), root.plate)
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.RIGHT)
        layoutAt(800f)
        assertEquals(PxRect(px(360f), 0, px(800f), rowsPx), root.plate)
        // Bar on the right.
        root.setPadding(0, 0, px(SIDE_DP), 0)
        layoutAt(800f)
        assertEquals(PxRect(px(312f), 0, px(800f), rowsPx), root.plate)
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.LEFT)
        layoutAt(800f)
        assertEquals(PxRect(0, 0, px(440f), rowsPx), root.plate)
    }

    @Test
    fun `PL-05 a new width, alignment, key size or padding gives a new plate at the next layout`() {
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.CENTER)
        layoutAt(800f)
        assertEquals(PxRect(px(180f), 0, px(620f), rowsPx), root.plate)
        layoutAt(600f)   // the window got narrower
        assertEquals(PxRect(px(80f), 0, px(520f), rowsPx), root.plate)
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.RIGHT)
        layoutAt(600f)
        assertEquals(PxRect(px(160f), 0, px(600f), rowsPx), root.plate)
        root.portraitWidth = WidthSetting(50f, KeyboardAlign.RIGHT)
        layoutAt(600f)
        assertEquals(PxRect(px(50f), 0, px(600f), rowsPx), root.plate)
        root.portraitWidth = WidthSetting(KeyboardWidth.FULL, KeyboardAlign.RIGHT)
        layoutAt(600f)
        assertTrue(root.plateFillsView)
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.LEFT)
        root.rowHeightDp = 40f
        layoutAt(600f)
        assertEquals(PxRect(0, 0, px(440f), px(KeyboardRootView.WINDOW_ROWS * 40f)), root.plate)
        root.setPadding(0, 0, 0, px(NAV_DP))   // the system bar came
        layoutAt(600f)
        assertEquals(PxRect(0, 0, px(440f), px(KeyboardRootView.WINDOW_ROWS * 40f) + px(NAV_DP)), root.plate)
    }

    @Test
    @Config(qualifiers = "w800dp-h400dp-land")
    fun `PL-06 landscape - the plate follows the landscape setting`() {
        layoutAt(800f)
        assertEquals(PxRect(px(200f), 0, px(600f), rowsPx), root.plate)   // default: the portrait width, centred
        root.landscapeWidth = WidthSetting(40f, KeyboardAlign.RIGHT)
        layoutAt(800f)
        assertEquals(PxRect(px(360f), 0, px(800f), rowsPx), root.plate)
        root.landscapeWidth = WidthSetting(KeyboardWidth.FULL, KeyboardAlign.CENTER)
        layoutAt(800f)
        assertTrue(root.plateFillsView)
    }

    @Test
    fun `PL-07 another mode view or the guide leaves the plate as it is`() {
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.CENTER)
        layoutAt(800f)
        val before = root.plate
        root.setModeView(tapView(), KeyboardRootView.WINDOW_ROWS)   // a 6-row mode
        layoutAt(800f)
        assertEquals(before, root.plate)
        root.enableGuide(kbd.graffiti.recognize.TemplateSet(emptyMap()), feedback)
        layoutAt(800f)
        root.openGuide()
        layoutAt(800f)
        assertEquals(before, root.plate)
    }

    @Test
    fun `PL-08 a stroke from a key out over the rest stays with that key, a finger that comes down off the plate gives nothing`() {
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.CENTER)
        layoutAt(800f)
        val child = root.getChildAt(0)
        val plate = root.plate
        val y = child.top + child.height * 2.5f / 6   // the layout's row 2 (edit row, numbers, then q–p)
        // q, the left end key: out over the side margin and on over the rest, off the plate.
        touch.down(0, child.left + 10 * density, y)
        touch.move(0, plate.left - 60 * density, y, steps = 6)
        assertFalse(plate.contains((plate.left - 60 * density).toInt(), y.toInt()))
        touch.up(0)
        assertEquals(listOf("key", "out:q"), events)   // as on a full-width keyboard (KR-10)
        events.clear()
        touch.down(0, child.right - 10 * density, y)   // p, the right end key
        touch.move(0, plate.right + 60 * density, y, steps = 6)
        touch.up(0)
        assertEquals(listOf("key", "out:p"), events)
        events.clear()
        // A finger that comes down off the plate, if the window is sent it: no input, alone or while a key is held.
        for (x in listOf(plate.left - 1f, plate.right + 0f, 0f, root.width - 1f)) {
            touch.down(0, x, y)
            assertTrue("x $x", touch.lastHandled)
            touch.move(0, root.width / 2f, y)
            touch.up(0)
        }
        assertEquals(emptyList<String>(), events)
        touch.down(0, root.width / 2f, y)
        touch.down(1, plate.right + 20 * density, y)
        touch.move(1, root.width / 2f, y)
        touch.up(1)
        touch.up(0)
        assertEquals(1, events.count { it == "key" })
        assertEquals(1, events.count { it.startsWith("out:") || it.startsWith("down:") })
    }

    private companion object {
        const val ROW = 48f
        const val CHILD_ROWS = 5
        const val NAV_DP = 24f
        const val SIDE_DP = 48f
    }
}
