package kbd.app.swipe.impl

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Looper
import android.view.ViewConfiguration
import java.io.File
import java.time.Duration
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.ime.FieldClass
import kbd.app.ime.FieldInfo
import kbd.app.store.ErrorLog
import kbd.app.store.Settings
import kbd.app.swipe.SwipeEngineState
import kbd.app.swipe.SwipeHost
import kbd.app.view.CenterConsonantPainter
import kbd.app.view.CenterText
import kbd.app.view.Touches
import kbd.app.view.ViewTestSdk
import kbd.app.view.layoutExactly
import kbd.gesture.input.SwipeInputMapper
import kbd.gesture.layout.SwipeLayout
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.hangul.stroke.DotPlace
import kbd.stroke.layout.FunctionKey
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
 * `SwipeKeyboardView` itself with synthetic `MotionEvent`s and a fake [SwipeHost]
 * (LLD-11 §4.5 SW-01..SW-04, LLD-04 §9, FR-CMN-11a; gate G1). The ⌫ repeat and the guide timers
 * run on the main looper, paused (Robolectric).
 *
 * View: 357 × 288 dp → 51 × 48 dp cells (6 rows, issue #168); empty cell r5c5, cheonjiin key r4c0, ㄱ = col 4 row 2, ㄴ = col 2 row 3, ⌫ = col 6 row 3.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class SwipeKeyboardViewTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Every host call and every key feedback, in one ordered list. */
    private class FakeHost(log: ErrorLog) : SwipeHost {
        val calls = ArrayList<String>()
        var registered: SwipeEngineState? = null
        override val thresholds: Thresholds = ThresholdDefaults.VALUES
        override var settings: Settings = Settings.DEFAULT
        override var fieldInfo = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = false, enterAction = null)
        override var centerConsonant: Jamo? = null
        /** The center text, when set; else the interface default ([centerConsonant] alone). */
        var center: CenterText? = null
        override val centerText: CenterText? get() = center ?: centerConsonant?.let { CenterText(it.compat.toString()) }
        /** What the next swipe events make the center consonant (the composer is not in this test). */
        var consonantAfterEvents: Jamo? = null
        override fun onSwipeEvents(events: List<JamoEvent>, errorVibe: Boolean) {
            calls += "events:${events.size}"
            consonantAfterEvents?.let { centerConsonant = it }
        }
        override fun onSwipeText(text: String, errorVibe: Boolean) { calls += "text:$text" }
        override fun onFunctionKey(key: FunctionKey) { calls += "fn:$key" }
        override fun onFunctionKeyUp(key: FunctionKey) { calls += "fnUp:$key" }
        override val feedback = object : FeedbackPort {
            override fun key() { calls += "key" }
            override fun error() { calls += "error" }
        }
        override val guard = EngineGuard(log, object : GuardedState {
            override fun snapshot(): Any = registered?.snapshot() ?: Unit
            override fun restore(s: Any) {
                if (s != Unit) registered?.restore(s)
            }
        })
        override fun registerSwipeState(state: SwipeEngineState?) { registered = state }
    }

    private lateinit var host: FakeHost
    private lateinit var view: SwipeKeyboardView
    private lateinit var touch: Touches
    private var density = 1f

    private val looper get() = shadowOf(Looper.getMainLooper())
    private val repeatTimeoutMs = ViewConfiguration.getKeyRepeatTimeout().toLong()
    private val repeatDelayMs = ViewConfiguration.getKeyRepeatDelay().toLong()

    @Before
    fun setUp() {
        host = FakeHost(ErrorLog(File(tmp.root, "errors.log")))
        view = SwipeKeyboardView(RuntimeEnvironment.getApplication(), host)
        density = view.resources.displayMetrics.density
        layoutExactly(view, Math.round(WIDTH * density), Math.round(SwipeLayout.ROWS * ROW * density))
        touch = Touches(view)
    }

    private fun cx(col: Int) = (col + 0.5f) * COL * density
    private fun cy(row: Int) = (row + 0.5f) * ROW * density
    private fun dp(v: Float) = v * density

    private fun count(c: String) = host.calls.count { it == c }
    private fun countPrefix(c: String) = host.calls.count { it.startsWith(c) }

    /** The guide overlay the view made, read for its timer state (LLD-06 §3). */
    private fun guideLogic(): GuideTimerLogic? {
        val g = SwipeKeyboardView::class.java.getDeclaredField("guide").apply { isAccessible = true }.get(view) ?: return null
        return GuideOverlay::class.java.getDeclaredField("logic").apply { isAccessible = true }.get(g) as GuideTimerLogic
    }

    /** Issue #237: the top-row number keys' ↑ — touch points above the view (y < 0) reach the touch logic; the top is no outer edge. */
    @Test
    fun `SW-18 a top-row number key's up stroke past the view's top types its up symbol`() {
        for ((col, up) in listOf(0 to "!", 1 to "@", 6 to "&")) {
            touch.down(0, cx(col), cy(0))
            touch.move(0, cx(col), cy(0) - dp(40f), steps = 8)   // ends 16 dp above the view
            touch.up(0)
            assertEquals(host.calls.toString(), 1, count("text:$up"))
        }
    }

    /** Owner #208: the symbol keys' exit symbols in the middle of their edges, like the number keys (no corners). */
    @Test
    fun `SW-19 symbol2's exit symbols sit in the middle of the up, left and down edges`() {
        val rc = RecordingCanvas().also { view.draw(it) }
        fun at(t: String) = rc.textAt.filter { it.first == t }
        val cx = cx(6)
        val cy = cy(4)
        val up = at("<").single()
        val down = at("'").single()
        val left = at("\"").single { it.second < cx + COL * density / 2 && it.second > cx - COL * density / 2 }
        assertEquals(cx, up.second, 0.5f)
        assertEquals(cx, down.second, 0.5f)
        assertTrue("$up above $down", up.third < cy && down.third > cy)
        assertTrue("$left left of the middle", left.second < cx)
        assertTrue("$left on the middle height", left.third > up.third && left.third < down.third)
    }

    /** #135: the real keyboard follows the three guide settings (FR-DSP-08); all off = nothing drawn. */
    @Test
    fun `SW-08 the current character setting and all three off`() {
        host.settings = Settings.DEFAULT.copy(showCurrentChar = false)
        touch.down(0, cx(N_COL), cy(N_ROW))
        touch.move(0, cx(N_COL), cy(N_ROW) - dp(30f), steps = 6)
        assertEquals(GuideView(showDirection = true, showCurrent = false, showNext = true), guideView())
        touch.up(0)
        host.settings = Settings.DEFAULT.copy(showDirectionGuide = false, showNextChar = false, showCurrentChar = false)
        touch.down(0, cx(N_COL), cy(N_ROW))
        touch.move(0, cx(N_COL), cy(N_ROW) - dp(30f), steps = 6)
        assertEquals(GuidePhase.SHOWING, guideLogic()?.phase)
        assertEquals(GuideView(showDirection = false, showCurrent = false, showNext = false), guideView())
        touch.up(0)
    }

    private fun guideView(): GuideView? =
        (SwipeKeyboardView::class.java.getDeclaredField("guide").apply { isAccessible = true }.get(view) as GuideOverlay?)?.view

    @Test
    fun `SW-01 the empty cell takes a press, a move and a release with no input, other fingers ignored meanwhile`() {
        val x = cx(EMPTY_COL)
        val y = cy(EMPTY_ROW)
        touch.down(0, x, y)
        touch.move(0, x + dp(45f), y, steps = 5)   // R45
        touch.down(1, cx(G_COL), cy(G_ROW))   // another finger on ㄱ
        touch.move(1, cx(G_COL) + dp(45f), cy(G_ROW), steps = 5)
        touch.up(1)
        touch.down(2, cx(BS_COL), cy(BS_ROW))   // and on ⌫
        touch.up(2)
        touch.up(0)
        looper.idleFor(Duration.ofMillis(2_000))
        assertEquals(emptyList<String>(), host.calls)   // no onSwipeEvents, onSwipeText, onFunctionKey, feedback.key
        // The view is not stuck: the next stroke works.
        touch.tap(0, cx(G_COL), cy(G_ROW))
        assertEquals(listOf("key", "events:1"), host.calls.filter { it == "key" || it.startsWith("events:") })   // Consonant(ㄱ)
    }

    @Test
    fun `SW-02 backspace acts on press, key feedback first`() {
        touch.down(0, cx(BS_COL), cy(BS_ROW))
        assertEquals(listOf("key", "fn:BACKSPACE"), host.calls)   // before UP
        touch.up(0)
        assertEquals(listOf("key", "fn:BACKSPACE", "fnUp:BACKSPACE"), host.calls)
    }

    @Test
    fun `SW-03 cancel mid-stroke gives no events, closes the guide and leaves the swipe mapper as it was`() {
        touch.tap(0, cx(CJ_COL), cy(CJ_ROW))   // the cheonjiin key: a pending ㆍ in the mapper (its only state, issue #168)
        assertEquals(1, countPrefix("events:"))
        val state = checkNotNull(host.registered)
        val before = state.snapshot()
        assertNotEquals(SwipeMapperState(SwipeInputMapper(host.thresholds)).snapshot(), before)   // not the empty state

        val x = cx(G_COL)
        val y = cy(G_ROW)
        touch.down(0, x, y)
        touch.move(0, x + dp(45f), y, steps = 9)   // R45 out of ㄱ
        assertEquals(GuidePhase.SHOWING, guideLogic()?.phase)   // the guide is up (moved past the slop)
        touch.cancel()

        assertEquals(1, countPrefix("events:"))   // nothing from the cancelled stroke
        assertEquals(GuidePhase.IDLE, guideLogic()?.phase)   // guide.onUp
        assertEquals(before, state.snapshot())
        looper.idleFor(Duration.ofMillis(2_000))   // guide timers removed: nothing comes later
        assertEquals(1, countPrefix("events:"))
        assertEquals(GuidePhase.IDLE, guideLogic()?.phase)
    }

    @Test
    fun `SW-04 cancel after one repeat keeps both backspaces and stops the repeat`() {
        touch.down(0, cx(BS_COL), cy(BS_ROW))
        looper.idleFor(Duration.ofMillis(repeatTimeoutMs + repeatDelayMs / 2))   // one repeat
        assertEquals(2, count("fn:BACKSPACE"))
        touch.cancel()
        looper.idleFor(Duration.ofMillis(2_000))
        assertEquals(2, count("fn:BACKSPACE"))   // both stay, no repeat after
        assertEquals(1, count("key"))   // none for the repeat
        assertEquals(listOf("key", "fn:BACKSPACE", "fn:BACKSPACE", "fnUp:BACKSPACE"), host.calls)
    }

    /** Text and path draws in order, with the text size and colour (the center consonant is the big one) and where. */
    private class RecordingCanvas : Canvas() {
        val ops = ArrayList<String>()
        val centerAt = ArrayList<Pair<Float, Float>>()
        val textAt = ArrayList<Triple<String, Float, Float>>()
        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            textAt += Triple(text, x, y)
            if (paint.textSize > ROW) {
                ops += "center:$text:${paint.color}"
                centerAt += x to y
            } else {
                ops += "label:$text"
            }
        }
        override fun drawPath(path: Path, paint: Paint) { ops += "path" }
        val circles = ArrayList<Triple<Float, Float, Int>>()
        override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) { circles += Triple(cx, cy, paint.color) }
        override fun drawRoundRect(rect: android.graphics.RectF, rx: Float, ry: Float, paint: Paint) {}
        override fun drawColor(color: Int) {}
    }

    private fun draw(): List<String> = RecordingCanvas().also { view.draw(it) }.ops
    private fun center(ops: List<String>) = ops.filter { it.startsWith("center:") }

    /** #180 follow-up SWIPE 9.5: the consonant faint in the place C, after the keys; nothing when none composes. */
    @Test
    fun `SW-14 the center consonant is drawn in C after the keys, none when no consonant composes`() {
        assertEquals(emptyList<String>(), center(draw()))
        host.centerConsonant = Jamo.GG
        val canvas = RecordingCanvas().also { view.draw(it) }
        assertEquals(listOf("center:ㄲ:${CenterConsonantPainter.COLOR}"), center(canvas.ops))
        assertEquals(canvas.ops.lastIndex, canvas.ops.indexOfFirst { it.startsWith("center:") })   // after every key label
        // Centred on C = consonantBox(357, 48) ≈ (137.7, 76)–(219.3, 172) (centred, issue #213, half a row down, FSS#37).
        val c = SwipeLayout.consonantBox(WIDTH, ROW)
        assertEquals((c.left + c.right) / 2 * density, canvas.centerAt.single().first, 0.5f)
    }

    /** #180: under the trail; a vowel stroke keeps it, the next consonant replaces it on release. */
    @Test
    fun `SW-15 the center consonant is under the trail, stays for vowel strokes and changes on release`() {
        host.centerConsonant = Jamo.G
        touch.down(0, cx(G_COL), cy(G_ROW))
        touch.move(0, cx(G_COL), cy(G_ROW) + dp(25f), steps = 8)   // a ㅣ stroke beside C being drawn (its middle right-up, SWIPE 0.43)
        val mid = draw()
        assertEquals(listOf("center:ㄱ:${CenterConsonantPainter.COLOR}"), center(mid))
        val c = mid.indexOfFirst { it.startsWith("center:") }
        val p = mid.indexOf("path")
        assertTrue("center $c before trail $p: $mid", c in 0 until p)
        touch.up(0)
        assertEquals(1, count("events:1"))   // taken as ㅣ against C
        assertEquals(listOf("center:ㄱ:${CenterConsonantPainter.COLOR}"), center(draw()))
        host.consonantAfterEvents = Jamo.N
        touch.down(0, cx(N_COL), cy(N_ROW))
        assertEquals(listOf("center:ㄱ:${CenterConsonantPainter.COLOR}"), center(draw()))   // not on press
        touch.up(0)
        assertEquals(listOf("center:ㄴ:${CenterConsonantPainter.COLOR}"), center(draw()))
    }

    /** FSS#36 SWIPE 9.5: the composing syllable big in C, a first stroke's other role small in brackets right above C, its dot placed. */
    @Test
    fun `SW-14b the center syllable with its bracket - the dot under the consonant, above C`() {
        host.center = CenterText("기", "ㄱ", DotPlace.UNDER)
        val canvas = RecordingCanvas().also { view.draw(it) }
        assertEquals(listOf("center:기:${CenterConsonantPainter.COLOR}"), center(canvas.ops))
        val c = SwipeLayout.consonantBox(WIDTH, ROW)
        val small = canvas.textAt.filter { it.first in listOf("(", "ㄱ", ")") }.takeLast(3)
        assertEquals(listOf("(", "ㄱ", ")"), small.map { it.first })
        for ((_, _, y) in small) assertTrue("$y above C.top ${c.top * density}", y <= c.top * density && y > (c.top - ROW / 2) * density)
        val dot = canvas.circles.single()
        assertEquals(CenterConsonantPainter.COLOR, dot.third)
        assertTrue("dot $dot under the bracket's ㄱ", dot.second > small[1].third - ROW / 2 * density && dot.first > small[1].second)
        host.center = CenterText("그", "ㄱ", DotPlace.RIGHT)
        val right = RecordingCanvas().also { view.draw(it) }
        val k = right.textAt.filter { it.first == "ㄱ" }.last()
        val close = right.textAt.last { it.first == ")" }
        assertTrue("dot right of ㄱ, before ')'", right.circles.single().first in k.second..close.second)
        host.center = CenterText("가")
        assertEquals(0, RecordingCanvas().also { view.draw(it) }.circles.size)   // no bracket once settled
    }

    /** #180 FR-CMN-08 ①: not in password fields; ② no-learning fields draw it; FR-DSP-08: the guide settings do not matter. */
    @Test
    fun `SW-16 no center consonant in password fields only, guide settings do not matter`() {
        host.centerConsonant = Jamo.G
        host.settings = Settings.DEFAULT.copy(showDirectionGuide = false, showNextChar = false, showCurrentChar = false)
        assertEquals(1, center(draw()).size)
        host.fieldInfo = host.fieldInfo.copy(isPassword = true)
        assertEquals(emptyList<String>(), center(draw()))
        host.fieldInfo = host.fieldInfo.copy(isPassword = false, noLearning = true)
        assertEquals(1, center(draw()).size)   // owner 2026-11-01: "학습 금지이지 암호처럼 출력 금지가 아니잖아!"
    }

    /** TBD-24: while a consonant composes, strokes are judged against C (SWIPE 0.43 two zones: a vertical left of C is a dot). */
    @Test
    fun `SW-17 the touch logic hands C to the mapper while a consonant composes`() {
        // From the ㅉ key (col 2, row 1): its middle (127.5, 107) is under-left of C's corner = a dot against C (SWIPE 0.43).
        host.centerConsonant = Jamo.G
        touch.down(0, cx(2), cy(1))
        touch.move(0, cx(2), cy(1) + dp(70f), steps = 8)
        touch.up(0)
        assertEquals(0, count("error"))
        assertEquals(1, count("events:0"))   // a dot: no composer event yet
        // Bare (no consonant composing): the same stroke is ㅣ.
        checkNotNull(host.registered).reset()   // the app closes the open dot (as a tap or cursor move would)
        host.centerConsonant = null
        touch.down(0, cx(2), cy(1))
        touch.move(0, cx(2), cy(1) + dp(70f), steps = 8)
        touch.up(0)
        assertEquals(1, count("events:1"))
    }

    @Test
    fun `the view registers its swipe mapper state`() {
        assertNotNull(host.registered)
        assertNull(guideLogic())   // no guide before the first stroke
    }

    private companion object {
        const val WIDTH = 357f
        const val ROW = 48f
        const val COL = WIDTH / SwipeLayout.COLS
        const val EMPTY_COL = 5
        const val EMPTY_ROW = 5
        const val G_COL = 4
        const val G_ROW = 2
        const val N_COL = 2
        const val N_ROW = 3
        const val BS_COL = 6
        const val BS_ROW = 2   // issue #173: the end columns moved up a cell
        const val CJ_COL = 0
        const val CJ_ROW = 3
    }
}
