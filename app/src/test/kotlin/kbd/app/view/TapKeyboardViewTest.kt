package kbd.app.view

import android.os.Looper
import android.view.ViewConfiguration
import java.io.File
import java.time.Duration
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.TapOutput
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * `TapKeyboardView` itself with synthetic `MotionEvent`s (LLD-11 §3 KH-03, §5.4 MQ-05, MQ-06, MQ-08,
 * FB-01, FB-02; LLD-05A §4, §6; gate G1). The ⌫ repeat runs on the view's real main-looper
 * `RepeatTimer`, driven by Robolectric's paused looper.
 *
 * View: 360 × 288 dp (QWERTY, 6 rows of 48 dp), so a key's dp rectangle is its layout units.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class TapKeyboardViewTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Every sink call and every key feedback, in one ordered list. */
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

    private lateinit var view: TapKeyboardView
    private lateinit var touch: Touches
    private var density = 1f

    private val looper get() = shadowOf(Looper.getMainLooper())
    private val repeatTimeoutMs = ViewConfiguration.getKeyRepeatTimeout().toLong()
    private val repeatDelayMs = ViewConfiguration.getKeyRepeatDelay().toLong()

    @Before
    fun setUp() {
        val guard = EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState)
        view = TapKeyboardView(RuntimeEnvironment.getApplication(), sink, guard, feedback)
        view.setLayout(TapLayouts.QWERTY, ThresholdDefaults.VALUES, ROW)
        density = view.resources.displayMetrics.density
        layoutExactly(view, px(WIDTH), px(ROWS * ROW))
        touch = Touches(view)
    }

    private fun px(dp: Float): Int = Math.round(dp * density)

    private fun key(id: String): TapKeyDef = TapLayouts.QWERTY.keys.first { it.id == id }

    /** Centre of key [id] in px. */
    private fun cx(id: String): Float = key(id).let { (it.x + it.w / 2f) * density }
    private fun cy(id: String): Float = key(id).let { (it.row + it.rowSpan / 2f) * ROW * density }

    private fun down(pointer: Int, id: String) = touch.down(pointer, cx(id), cy(id))

    private fun count(e: String) = events.count { it == e }

    /** Lets the first repeat come (`getKeyRepeatTimeout`), then [more] repeats `getKeyRepeatDelay` apart. */
    private fun idleRepeats(more: Int = 0) {
        looper.idleFor(Duration.ofMillis(repeatTimeoutMs + more * repeatDelayMs + repeatDelayMs / 2))
    }

    @Test
    fun `KH-03 no blanks since FSS#105 - where the indent blanks were are a, z and enter themselves`() {
        fun hit(x: Float, y: Float) = view.hit(StrokePoint(x, y, 0))?.id
        val row3 = 3 * ROW
        val row4 = 4 * ROW
        assertEquals("a", hit(5f, row3 + 1))
        assertEquals("a", hit(5f, row3 + ROW / 2))
        assertEquals("enter", hit(355f, row3 + 1))
        assertEquals("z", hit(5f, row4 + 1))
    }

    @Test
    fun `KH-03 a tap where the indent blanks were types a, enter and z`() {
        touch.tap(0, 5f * density, (3 * ROW + 1) * density)
        touch.tap(0, 355f * density, (3 * ROW + 1) * density)
        touch.tap(0, 5f * density, (4 * ROW + 1) * density)
        assertEquals(listOf("key", "out:a", "key", "down:ENTER", "up:ENTER", "key", "out:z"), events)
    }

    @Test
    fun `MQ-05 backspace pressed while a letter is held acts at once, the letter on its release`() {
        down(0, "q")
        down(1, "backspace")
        assertEquals(listOf("key", "key", "down:BACKSPACE"), events)
        touch.up(0)
        assertEquals(listOf("key", "key", "down:BACKSPACE", "out:q"), events)
        touch.up(1)
        assertEquals(listOf("key", "key", "down:BACKSPACE", "out:q", "up:BACKSPACE"), events)
    }

    @Test
    fun `MQ-06 a letter tapped while backspace is held does not wait for it`() {
        down(0, "backspace")
        down(1, "w")
        touch.up(1)
        assertEquals(listOf("key", "down:BACKSPACE", "key", "out:w"), events)
        touch.up(0)
        assertEquals("up:BACKSPACE", events.last())
    }

    @Test
    fun `MQ-08 cancel keeps the backspace presses, stops the repeat and drops the held letter`() {
        down(0, "backspace")
        idleRepeats()   // one repeat
        assertEquals(2, count("down:BACKSPACE"))
        down(1, "e")
        touch.cancel()
        looper.idleFor(Duration.ofMillis(2_000))
        assertEquals(listOf("key", "down:BACKSPACE", "down:BACKSPACE", "key", "up:BACKSPACE"), events)
        assertEquals(0, events.count { it.startsWith("out:") })
    }

    @Test
    fun `FB-01 key feedback on press only, function keys act on press`() {
        down(0, "q")
        assertEquals(listOf("key"), events)
        touch.up(0)
        assertEquals(listOf("key", "out:q"), events)
        down(0, "enter")
        assertEquals(listOf("key", "out:q", "key", "down:ENTER"), events)
        touch.up(0)
        assertEquals(listOf("key", "out:q", "key", "down:ENTER", "up:ENTER"), events)
    }

    @Test
    fun `FB-02 no key feedback on the repeats of a held backspace`() {
        down(0, "backspace")
        idleRepeats(more = 2)   // three repeats
        touch.up(0)
        assertEquals(1, count("key"))
        assertEquals(4, count("down:BACKSPACE"))
        assertEquals(listOf("key", "down:BACKSPACE"), events.take(2))
    }

    @Test
    fun `the repeat stops on release`() {
        down(0, "backspace")
        idleRepeats()
        touch.up(0)
        looper.idleFor(Duration.ofMillis(2_000))
        assertEquals(2, count("down:BACKSPACE"))
    }

    private companion object {
        const val WIDTH = 360f
        const val ROW = 48f
        const val ROWS = 6
    }
}
