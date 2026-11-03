package kbd.app.graffiti

import android.graphics.Canvas
import android.graphics.Paint
import java.io.File
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.ime.ComposingController
import kbd.app.ime.EditorKey
import kbd.app.ime.EditorPort
import kbd.app.ime.FieldClass
import kbd.app.ime.FieldInfo
import kbd.app.store.ErrorLog
import kbd.app.view.Touches
import kbd.app.view.TapSink
import kbd.app.view.ViewTestSdk
import kbd.app.view.layoutExactly
import kbd.hangul.cheonjiin.CheonjiinAssembler
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.TemplateSet
import kbd.graffiti.state.GraffitiState
import kbd.graffiti.state.GraffitiStateSnapshot
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.tap.TapOutput
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
 * Pad status display and area outlines of `GraffitiPadView` (LLD-11 §6.5 PI-01~PI-07, PI-09·PI-10, LLD-05 §8.1, SRS FR-GRF-05d, FR-GRF-01a). Strokes go in
 * as synthetic touches through the real recognizer rules and [GraffitiState] (no templates: only taps and
 * straight lines are used); what the view draws is captured with a recording [Canvas].
 *
 * View: 360 × 240 dp (5 rows of 48 dp), letter ratio 2/3, so the letter area is 240 × 192 dp and the corner-tap
 * zone 36 × 28.8 dp.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class GraffitiPadViewTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Draw calls the status display uses; everything else is dropped. */
    private class RecordingCanvas : Canvas() {
        val texts = ArrayList<Triple<String, Float, Float>>()
        val circles = ArrayList<Triple<Float, Float, Float>>()
        /** Outlined round rects (the area outlines); the filled ones are the auxiliary bar buttons. */
        val outlines = ArrayList<Pair<android.graphics.RectF, Float>>()
        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            texts += Triple(text, x, y)
        }
        override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
            circles += Triple(cx, cy, radius)
        }
        override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {}
        override fun drawRoundRect(rect: android.graphics.RectF, rx: Float, ry: Float, paint: Paint) {
            if (paint.style == Paint.Style.STROKE) outlines += android.graphics.RectF(rect) to rx
        }
        override fun drawColor(color: Int) {}
    }

    private object NoEditor : EditorPort {
        override fun commitText(text: String) {}
        override fun setComposingText(text: String) {}
        override fun finishComposingText() {}
        override fun deleteBefore() {}
        override fun sendKey(key: EditorKey) {}
        override fun performEditorAction(actionId: Int) {}
        override fun setSelection(start: Int, end: Int) {}
        override fun beginBatch() {}
        override fun endBatch() {}
    }

    private val sink = object : InputSink {
        override fun markInput() {}
        override fun error() {}
        override var hidesDisplay = false
        override val field = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = false, enterAction = null)
    }

    private object NoFeedback : FeedbackPort {
        override fun key() {}
        override fun error() {}
    }

    private object NoState : GuardedState {
        override fun snapshot(): Any = Unit
        override fun restore(s: Any) {}
    }

    private val templates = TemplateSet(emptyMap())
    private val state = GraffitiState(templates)
    private val composer = HangulComposer(ComposePolicy.GRAFFITI)
    private var redraws = 0
    private lateinit var handler: GraffitiInputHandler
    private lateinit var view: GraffitiPadView
    private lateinit var touch: Touches
    private var density = 1f

    /** The auxiliary bar as InputDispatcher wires it: ⌫ goes to the graffiti handler (LLD-08 §6). */
    private val keyEvents = ArrayList<String>()
    private val tapSink = object : TapSink {
        override fun onTapOutput(key: TapKeyDef, out: TapOutput) {}
        override fun onFunctionDown(key: FunctionKey) {
            keyEvents += "down:$key"
            if (key == FunctionKey.BACKSPACE) handler.backspace()
        }
        override fun onFunctionUp(key: FunctionKey) { keyEvents += "up:$key" }
        override fun onSettingsStroke() { keyEvents += "settings" }
    }

    @Before
    fun setUp() {
        handler = GraffitiInputHandler(
            Recognizer(templates), state, CheonjiinAssembler(), { composer }, ComposingController(NoEditor), sink, {},
            redraw = { redraws++ },
        )
        val guard = EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState)
        view = GraffitiPadView(RuntimeEnvironment.getApplication(), tapSink, handler, guard, NoFeedback)
        view.letterRatio = 2f / 3f
        density = view.resources.displayMetrics.density
        layoutExactly(view, px(360f), px(240f))
        touch = Touches(view)
        handler.enterMode()
    }

    private fun px(dp: Float): Int = Math.round(dp * density)

    private fun draw(): RecordingCanvas = RecordingCanvas().also { view.draw(it) }

    /** The status texts drawn this frame: the corner glyph (in the corner-tap zone) and the pending label (top right). */
    private fun cornerText(c: RecordingCanvas): String? {
        val zoneRight = 36f * density
        val zoneBottom = 28.8f * density
        return c.texts.singleOrNull { (t, x, y) -> x < zoneRight && y < zoneBottom + 1 && t != "123" }?.first
    }

    private fun pendingText(c: RecordingCanvas): String? =
        c.texts.singleOrNull { (_, x, y) -> x > 324f * density && y < 48f * density }?.first   // right 10% of the pad, top row

    private fun status(): Pair<String?, String?> = draw().let { cornerText(it) to pendingText(it) }

    // Strokes in dp.
    private fun stroke(x0: Float, y0: Float, x1: Float, y1: Float) {
        touch.down(0, x0 * density, y0 * density)
        touch.move(0, x1 * density, y1 * density, steps = 10)
        touch.up(0)
    }

    private fun tap(x: Float, y: Float) = touch.tap(0, x * density, y * density)
    private fun cornerTap() = tap(10f, 10f)
    private fun up() = stroke(120f, 170f, 120f, 60f)          // ↑ shift / caps lock
    private fun down() = stroke(120f, 60f, 120f, 170f)        // ↓ ㅣ / i
    private fun diag() = stroke(60f, 40f, 160f, 140f)         // ↘ extended shift
    private fun letterTap() = tap(120f, 100f)                 // EN: punctuation shift
    private fun backspaceKey() = tap(200f, 216f)              // auxiliary bar ⌫ (168–264 dp)

    @Test
    fun `PI-01 entering graffiti shows the corner dot and 한, no pending indicator`() {
        val c = draw()
        assertEquals("한", cornerText(c))
        assertEquals(null, pendingText(c))
        val dot = c.circles.single()
        assertEquals(10f * density, dot.first, 0.01f)    // inside the area outline (FSS#48)
        assertEquals(10f * density, dot.second, 0.01f)
        assertEquals(2f * density, dot.third, 0.01f)
        assertTrue("dot inside the corner-tap zone", dot.first + dot.third < 36f * density && dot.second + dot.third < 28.8f * density)
    }

    @Test
    fun `PI-02 a corner tap switches the glyph to A at once and back to 한`() {
        cornerTap()
        assertEquals(GraffitiLang.EN, state.lang)
        assertEquals("A", status().first)
        cornerTap()
        assertEquals("한", status().first)
    }

    @Test
    fun `PI-03 shift pending shows ⇧ until it is used or cancelled`() {
        up()
        assertEquals("⇧", status().second)
        up()                                        // KO: shift twice = off (FR-GRF-05c)
        assertEquals(null, status().second)
        cornerTap()
        up()
        assertEquals("A" to "⇧", status())
        down()                                      // "I": the shift is used
        assertEquals(null, status().second)
    }

    @Test
    fun `PI-04 caps lock ⇪ stays, and is shown with punctuation or extended shift`() {
        cornerTap()
        up()
        up()                                        // EN: shift then up = caps lock
        assertEquals("⇪", status().second)
        down()
        assertEquals("⇪", status().second)          // still on after a letter
        letterTap()
        assertEquals("⇪ •", status().second)
        diag()                                      // one-shot pendings replace each other
        assertEquals("⇪ ↘", status().second)
        backspaceKey()
        assertEquals("⇪", status().second)
        up()                                        // caps lock off
        assertEquals(null, status().second)
    }

    @Test
    fun `PI-05 punctuation and extended shift show • and ↘, ⌫ clears them`() {
        cornerTap()
        letterTap()
        assertEquals("•", status().second)
        backspaceKey()
        assertEquals(null, status().second)
        diag()
        assertEquals("↘", status().second)
        backspaceKey()
        assertEquals(null, status().second)
    }

    @Test
    fun `PI-06 the status display is drawn in password fields too`() {
        sink.hidesDisplay = true
        assertEquals("한", status().first)
        cornerTap()
        up()
        assertEquals("A" to "⇧", status())
    }

    @Test
    fun `PI-07 changes from outside the pad ask for a redraw and clear the pending indicator`() {
        diag()
        assertEquals("↘", status().second)
        val before = redraws
        handler.reset(ResetReason.CURSOR_MOVED)
        assertEquals(before + 1, redraws)
        assertEquals(null, status().second)
        val snap = handler.snapshot()
        handler.restore(snap)
        handler.enterMode()
        assertEquals(before + 3, redraws)
    }

    @Test
    fun `PI-09 both areas get an outline inside them, the status display sits inside, in password fields too`() {
        for (hides in listOf(false, true)) {
            sink.hidesDisplay = hides
            val c = draw()
            assertEquals(2, c.outlines.size)
            val (letter, letterR) = c.outlines[0]
            val (number, numberR) = c.outlines[1]
            assertRect(4.5f, 4.5f, 235.5f, 187.5f, letter)
            assertRect(244.5f, 4.5f, 355.5f, 187.5f, number)
            assertEquals(6f * density, letterR, 0.01f)
            assertEquals(6f * density, numberR, 0.01f)
            val dot = c.circles.single()
            assertTrue("dot clear of the outline", dot.first - dot.third > letter.left && dot.second - dot.third > letter.top)
            val glyph = c.texts.single { it.first == "한" }
            assertTrue("glyph inside the letter outline", glyph.second > letter.left && glyph.second < letter.right)
        }
        cornerTap()
        up()
        val c = draw()
        val number = c.outlines[1].first
        val pending = c.texts.single { it.first == "⇧" }
        assertTrue("pending label left of the number outline's right edge", pending.second < number.right)
        assertTrue("pending label below the outline's top", pending.third > number.top)
    }

    @Test
    fun `PI-10 the outlines follow the letter ratio`() {
        for ((ratio, split) in listOf(0.5f to 180f, 0.75f to 270f)) {
            view.letterRatio = ratio
            val c = draw()
            assertEquals((split - 4.5f) * density, c.outlines[0].first.right, 0.01f)
            assertEquals((split + 4.5f) * density, c.outlines[1].first.left, 0.01f)
        }
    }

    private fun assertRect(l: Float, t: Float, r: Float, b: Float, actual: android.graphics.RectF) {
        assertEquals(l * density, actual.left, 0.01f)
        assertEquals(t * density, actual.top, 0.01f)
        assertEquals(r * density, actual.right, 0.01f)
        assertEquals(b * density, actual.bottom, 0.01f)
    }

    /** Issue #205: the auxiliary bar's mode key (0–72 dp) acts on release; an up exit into the pad opens the settings. */
    @Test
    fun `MK-08 graffiti mode key - a tap changes the mode on release, an up exit opens the settings`() {
        touch.down(0, 36f * density, 216f * density)
        assertTrue(keyEvents.isEmpty())                         // nothing on press
        touch.up(0)
        assertEquals(listOf("down:MODE", "up:MODE"), keyEvents)
        keyEvents.clear()
        stroke(36f, 216f, 36f, 150f)                            // up, into the pad: not a pad stroke
        assertEquals(listOf("settings"), keyEvents)
        assertEquals("한" to null, status())                     // the pad state is untouched (no stroke went to the handler)
        keyEvents.clear()
        touch.down(0, 36f * density, 216f * density)
        touch.move(0, 36f * density, 150f * density, steps = 5)
        touch.cancel()
        assertTrue(keyEvents.isEmpty())                         // an unreleased mode key gives nothing
    }

    @Test
    fun `pendingLabel orders caps lock first and joins with a space`() {
        val none = GraffitiStateSnapshot(GraffitiLang.KO, capsLock = false, shiftPending = false, extPending = false, punctPending = false)
        assertEquals("", GraffitiPadView.pendingLabel(none))
        assertEquals("⇪ ⇧ • ↘", GraffitiPadView.pendingLabel(none.copy(capsLock = true, shiftPending = true, extPending = true, punctPending = true)))
        assertEquals("한", GraffitiPadView.langLabel(GraffitiLang.KO))
        assertEquals("A", GraffitiPadView.langLabel(GraffitiLang.EN))
    }
}
