package kbd.app.view

import android.view.View
import java.io.File
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.app.store.KeyboardAlign
import kbd.app.store.KeyboardWidth
import kbd.app.store.WidthSetting
import kbd.graffiti.recognize.TemplateSet
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.TapOutput
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
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
 * `KeyboardRootView`'s blank band and system bar padding (LLD-11 §5.4 FB-03, LLD-08 §7, SRS 3.3, gate G1).
 *
 * The mode view is a `TapKeyboardView` given [CHILD_ROWS] = 5 rows of the 6-row window (as a 5-row
 * mode view is placed), so the top row of the window is the blank band.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class KeyboardRootViewTest {

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
        val ctx = RuntimeEnvironment.getApplication()
        val guard = EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState)
        val child = TapKeyboardView(ctx, sink, guard, feedback)
        child.setLayout(TapLayouts.QWERTY, ThresholdDefaults.VALUES, ROW)
        root = KeyboardRootView(ctx)
        root.rowHeightDp = ROW
        root.setModeView(child, CHILD_ROWS)
        density = root.resources.displayMetrics.density
        touch = Touches(root)
    }

    private fun px(dp: Float): Int = Math.round(dp * density)

    private fun layout(bottomPaddingDp: Float = 0f) {
        root.setPadding(0, 0, 0, px(bottomPaddingDp))
        root.measure(
            View.MeasureSpec.makeMeasureSpec(px(WIDTH), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
    }

    @Test
    fun `FB-03 a touch on the blank band gives no key feedback and no input, and is taken`() {
        layout()
        assertEquals(px(KeyboardRootView.WINDOW_ROWS * ROW), root.height)
        assertEquals(px(ROW), root.getChildAt(0).top)   // the band is the top row
        val x = WIDTH / 2 * density
        val y = ROW / 2 * density
        touch.down(0, x, y)
        assertTrue(touch.lastHandled)   // consumed: it does not go to the app (SRS 3.3)
        touch.move(0, x + 30 * density, y)
        touch.up(0)
        assertTrue(touch.lastHandled)
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `the mode view below the band still gets its touches`() {
        layout()
        touch.tap(0, WIDTH / 2 * density, (ROW + ROW / 2) * density)   // child row 0
        assertEquals(1, events.count { it == "key" })
        assertEquals(1, events.count { it.startsWith("out:") || it.startsWith("down:") })
    }

    @Test
    fun `a touch on the system bar padding gives no key feedback and no input`() {
        layout(bottomPaddingDp = NAV_DP)
        assertEquals(px(KeyboardRootView.WINDOW_ROWS * ROW) + px(NAV_DP), root.height)
        val y = (KeyboardRootView.WINDOW_ROWS * ROW + NAV_DP / 2) * density
        touch.tap(0, WIDTH / 2 * density, y)
        assertTrue(touch.lastHandled)
        assertEquals(emptyList<String>(), events)
    }

    private fun layoutAt(widthDp: Float) {
        root.relayout()   // as the service does after changing a setting
        root.measure(
            View.MeasureSpec.makeMeasureSpec(px(widthDp), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
    }

    private val keyY: Float get() = (ROW + ROW / 2) * density   // child row 0

    @Test
    fun `KR-01 a narrowed keyboard is placed left, centre or right, the mode view at its width`() {
        val child = root.getChildAt(0)
        for ((align, left) in listOf(KeyboardAlign.LEFT to 0f, KeyboardAlign.CENTER to 200f, KeyboardAlign.RIGHT to 400f)) {
            root.portraitWidth = WidthSetting(40f, align)
            layoutAt(800f)
            assertEquals("$align", px(left), child.left)
            assertEquals("$align", px(400f), child.width)
            assertEquals(px(KeyboardRootView.WINDOW_ROWS * ROW), root.height)   // height unchanged (FR-CMN-14 ⑥)
        }
        root.portraitWidth = WidthSetting.DEFAULT
        layoutAt(800f)
        assertEquals(0, child.left)
        assertEquals(px(800f), child.width)
    }

    @Test
    fun `KR-02 a touch on the side margin is taken with no feedback and no input`() {
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.CENTER)
        layoutAt(800f)
        touch.down(0, 100 * density, keyY)
        assertTrue(touch.lastHandled)
        touch.move(0, 300 * density, keyY)   // onto the keyboard: still the margin's touch
        touch.up(0)
        assertTrue(touch.lastHandled)
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `KR-03 a second finger on the margin gives nothing while the first is on a key`() {
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.CENTER)
        layoutAt(800f)
        touch.down(0, 400 * density, keyY)
        touch.down(1, 700 * density, keyY)
        touch.move(1, 650 * density, keyY)
        touch.up(1)
        touch.up(0)
        assertEquals(1, events.count { it == "key" })
        assertEquals(1, events.count { it.startsWith("out:") || it.startsWith("down:") })
    }

    @Test
    fun `KR-04 a finger resting on the margin does not stop another finger's key`() {
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.CENTER)
        layoutAt(800f)
        touch.down(0, 50 * density, keyY)
        touch.down(1, 400 * density, keyY)
        touch.up(1)
        touch.up(0)
        assertEquals(1, events.count { it == "key" })
        assertEquals(1, events.count { it.startsWith("out:") || it.startsWith("down:") })
    }

    @Test
    fun `KR-05 the guide button sits at the narrowed keyboard's right end`() {
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.LEFT)
        root.enableGuide(TemplateSet(emptyMap()), feedback)
        layoutAt(800f)
        val button = root.getChildAt(root.childCount - 1)
        assertEquals(px(400f), button.right)
        assertEquals(px(400f - ROW), button.left)
        val panel = root.getChildAt(1)
        assertEquals(0, panel.left)
        assertEquals(px(400f), panel.width)
    }

    @Test
    @Config(qualifiers = "w800dp-h400dp-land")
    fun `KR-06 landscape uses the landscape setting, by default the portrait width centred`() {
        val child = root.getChildAt(0)
        layoutAt(800f)
        assertEquals(px(400f), child.width)   // the screen's short side
        assertEquals(px(200f), child.left)
        root.landscapeWidth = WidthSetting(KeyboardWidth.FULL, KeyboardAlign.CENTER)
        root.portraitWidth = WidthSetting(40f, KeyboardAlign.LEFT)   // not used in landscape
        layoutAt(800f)
        assertEquals(px(800f), child.width)
    }

    private companion object {
        const val WIDTH = 360f
        const val ROW = 48f
        const val CHILD_ROWS = 5
        const val NAV_DP = 24f
    }
}
