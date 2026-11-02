package kbd.app.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Looper
import android.view.View
import java.io.File
import java.time.Duration
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.graffiti.GraffitiGuideLayout
import kbd.app.graffiti.GraffitiGuidePanel
import kbd.app.graffiti.GraffitiInputHandler
import kbd.app.graffiti.GraffitiPadView
import kbd.app.graffiti.InputSink
import kbd.app.ime.ComposingController
import kbd.app.ime.EditorKey
import kbd.app.ime.EditorPort
import kbd.app.ime.FieldClass
import kbd.app.ime.FieldInfo
import kbd.app.store.ErrorLog
import kbd.graffiti.guide.GuideCatalog
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.TemplateSet
import kbd.graffiti.state.GraffitiState
import kbd.hangul.cheonjiin.CheonjiinAssembler
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.tap.TapOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * The stroke guide (LLD-11 §6.6 GD-01~GD-10, LLD-08 §7.1, LLD-05 §10, SRS FR-GRF-09~09d): the band button,
 * the opaque panel over a real `GraffitiPadView`, its ✕, scrolling, and that nothing under it gets a touch.
 *
 * Window 360 dp wide, 6 rows of 48 dp: band 0–48 (button 312–360), pad 48–240, auxiliary bar 240–288
 * (mode 0–72, space 72–168, ⌫ 168–264, ↵ 264–360). The panel covers 48–288; its ✕ is 320–360 × 48–88.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class GraffitiGuideTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val events = ArrayList<String>()

    private val feedback = object : FeedbackPort {
        override fun key() { events += "key" }
        override fun error() { events += "error" }
    }

    private val tapSink = object : TapSink {
        override fun onTapOutput(key: TapKeyDef, out: TapOutput) { events += "out" }
        override fun onFunctionDown(key: FunctionKey) { events += "down:$key" }
        override fun onFunctionUp(key: FunctionKey) { events += "up:$key" }
        override fun onSettingsStroke() { events += "settings" }
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

    private val inputSink = object : InputSink {
        override fun markInput() { events += "input" }
        override fun error() { events += "error" }
        override var hidesDisplay = true   // a password field: the guide is shown all the same (FR-GRF-09d)
        override val field = FieldInfo(FieldClass.TEXT, isPassword = true, noLearning = false, multiLine = false, enterAction = null)
    }

    private object NoState : GuardedState {
        override fun snapshot(): Any = Unit
        override fun restore(s: Any) {}
    }

    private val templates: TemplateSet =
        TemplateLoader::class.java.getResourceAsStream("/graffiti/templates.json")!!.use { TemplateLoader.load(it) }
    private val state = GraffitiState(templates)
    private lateinit var guard: EngineGuard
    private lateinit var handler: GraffitiInputHandler
    private lateinit var root: KeyboardRootView
    private lateinit var touch: Touches
    private var density = 1f

    @Before
    fun setUp() {
        val ctx = RuntimeEnvironment.getApplication()
        guard = EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState)
        val composer = HangulComposer(ComposePolicy.GRAFFITI)
        handler = GraffitiInputHandler(Recognizer(templates), state, CheonjiinAssembler(), { composer }, ComposingController(NoEditor), inputSink, {}, redraw = {})
        root = KeyboardRootView(ctx)
        root.rowHeightDp = ROW
        root.setModeView(pad(), 5)
        root.enableGuide(templates, feedback)
        handler.enterMode()
        density = root.resources.displayMetrics.density
        touch = Touches(root)
        layout()
    }

    private fun pad() = GraffitiPadView(RuntimeEnvironment.getApplication(), tapSink, handler, guard, feedback)

    private fun layout() {
        root.measure(
            View.MeasureSpec.makeMeasureSpec(px(WIDTH), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
    }

    private fun px(dp: Float): Int = Math.round(dp * density)
    private fun tap(x: Float, y: Float, id: Int = 0) = touch.tap(id, x * density, y * density)
    private fun button() = tap(336f, 24f)
    private fun panel(): GraffitiGuidePanel = (0 until root.childCount).map { root.getChildAt(it) }.filterIsInstance<GraffitiGuidePanel>().single()

    @Test
    fun `GD-01 the band button opens and closes the guide on press, with key feedback`() {
        assertFalse(root.guideOpen)
        touch.down(0, 336f * density, 24f * density)
        assertTrue(root.guideOpen)                       // on press (FR-CMN-11a ①)
        assertEquals(listOf("key"), events)
        touch.up(0)
        assertTrue(root.guideOpen)
        val p = panel()
        assertEquals(px(48f), p.top)                     // pad + auxiliary bar: 5 rows, the band stays free
        assertEquals(px(288f), p.bottom)
        assertEquals(px(WIDTH), p.width)
        button()
        assertFalse(root.guideOpen)
        assertEquals(listOf("key", "key"), events)
    }

    @Test
    fun `GD-02 while open no pad stroke, auxiliary key or mode key gets the touch, and there is no feedback`() {
        button()
        events.clear()
        touch.down(0, 120f * density, 200f * density)     // a pad ↑ (shift) stroke
        touch.move(0, 120f * density, 80f * density, steps = 10)
        touch.up(0)
        tap(36f, 264f)                                    // mode key
        touch.down(0, 36f * density, 264f * density)      // mode key ↑ (settings, issue #205)
        touch.move(0, 36f * density, 120f * density, steps = 5)
        touch.up(0)
        tap(120f, 264f)                                   // space
        tap(216f, 264f)                                   // ⌫
        tap(312f, 264f)                                   // ↵
        tap(150f, 150f)                                   // a tap elsewhere on the panel does not close it
        assertEquals(emptyList<String>(), events)
        assertFalse(state.snapshot().shiftPending)
        assertTrue(root.guideOpen)
    }

    @Test
    fun `GD-03 the ✕ closes on release inside it, feedback on press`() {
        button()
        events.clear()
        touch.down(0, 340f * density, 68f * density)
        assertEquals(listOf("key"), events)
        assertTrue(root.guideOpen)                       // not on press
        touch.up(0)
        assertFalse(root.guideOpen)
        assertEquals(listOf("key"), events)
    }

    @Test
    fun `GD-04 pressing the ✕ and dragging out neither closes nor scrolls`() {
        button()
        touch.down(0, 340f * density, 68f * density)
        touch.move(0, 200f * density, 250f * density, steps = 5)
        touch.up(0)
        assertTrue(root.guideOpen)
        assertEquals(0f, panel().scrollDp, 0f)
    }

    @Test
    fun `GD-05 a drag on the panel scrolls it within its content, and closing starts it at the top again`() {
        button()
        val p = panel()
        touch.down(0, 150f * density, 250f * density)
        touch.move(0, 150f * density, 100f * density, steps = 5)
        assertEquals(150f, p.scrollDp, 0.5f)
        touch.move(0, 150f * density, 280f * density, steps = 5)   // back past the top: clamped at 0
        assertEquals(0f, p.scrollDp, 0f)
        touch.up(0)
        repeat(30) {                                                // far down: clamped at the end
            touch.down(0, 150f * density, 280f * density)
            touch.move(0, 150f * density, 60f * density, steps = 5)
            touch.up(0)
        }
        val l = GraffitiGuideLayout(WIDTH, GuideCatalog.groups)
        assertEquals(l.maxScrollDp(240f), p.scrollDp, 1f)
        assertTrue(root.guideOpen)
        button()
        button()
        assertEquals(0f, p.scrollDp, 0f)
    }

    @Test
    fun `GD-06 opening drops the pad stroke in progress - no input, no error vibration`() {
        touch.down(0, 120f * density, 200f * density)     // pad ↑ started
        touch.move(0, 120f * density, 120f * density, steps = 5)
        touch.down(1, 336f * density, 24f * density)       // the button, second finger
        assertTrue(root.guideOpen)
        touch.move(0, 120f * density, 70f * density, steps = 5)
        touch.up(1)
        touch.up(0)
        assertEquals(listOf("key"), events)                // the button's feedback only
        assertFalse(state.snapshot().shiftPending)
    }

    @Test
    fun `GD-07 opening stops a held ⌫ repeat`() {
        touch.down(0, 216f * density, 264f * density)     // ⌫ held
        assertEquals(1, events.count { it == "down:BACKSPACE" })
        touch.down(1, 336f * density, 24f * density)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        assertEquals(1, events.count { it == "down:BACKSPACE" })
        touch.up(1)
        touch.up(0)
        assertEquals(1, events.count { it == "down:BACKSPACE" })
    }

    @Test
    fun `GD-08 a new mode view in graffiti keeps it open, leaving graffiti closes it, a new root starts closed`() {
        button()
        root.setModeView(pad(), 5)                         // field change, still graffiti
        layout()
        assertTrue(root.guideOpen)
        tap(120f, 150f)
        assertEquals(listOf("key"), events)                // the new pad is under the panel too
        root.disableGuide()                                // e.g. a numeric field: QWERTY (FR-CMN-04)
        assertFalse(root.guideOpen)
        val b = (0 until root.childCount).map { root.getChildAt(it) }.single { it !is GraffitiPadView && it !is GraffitiGuidePanel }
        assertEquals(View.GONE, b.visibility)
        events.clear()
        tap(336f, 24f)                                     // the band again: no key
        assertEquals(emptyList<String>(), events)
        assertFalse(KeyboardRootView(RuntimeEnvironment.getApplication()).guideOpen)   // view re-created (rotation)
    }

    @Test
    fun `GD-09 closing by the keyboard going down and the guide state does not touch the graffiti state`() {
        touch.down(0, 120f * density, 200f * density)     // pad ↑: shift pending
        touch.move(0, 120f * density, 80f * density, steps = 10)
        touch.up(0)
        val before = state.snapshot()
        assertTrue(before.shiftPending)
        button()
        root.cancelTouches()                               // onFinishInputView: still open (field change)
        assertTrue(root.guideOpen)
        root.closeGuide()                                  // onWindowHidden
        assertFalse(root.guideOpen)
        assertEquals(before, state.snapshot())
    }

    /** Draw calls the panel uses. */
    private class RecordingCanvas : Canvas() {
        val texts = ArrayList<String>()
        var paths = 0
        var dashed = 0
        override fun drawText(text: String, x: Float, y: Float, paint: Paint) { texts += text }
        override fun drawPath(path: Path, paint: Paint) {
            paths++
            if (paint.pathEffect != null) dashed++
        }
        override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {}
        override fun drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {}
        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {}
        override fun drawRect(rect: android.graphics.RectF, paint: Paint) {}
        override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float, paint: Paint) {}
    }

    @Test
    fun `GD-10 the panel draws every group and cell from the templates, dashed for interim, 미정 for no stroke`() {
        val p = GraffitiGuidePanel(RuntimeEnvironment.getApplication(), templates, feedback) {}
        val l = GraffitiGuideLayout(WIDTH, GuideCatalog.groups)
        layoutExactly(p, px(WIDTH), px(l.contentHeightDp))   // tall enough to show all of it
        val c = RecordingCanvas().also { p.draw(it) }
        for (g in GuideCatalog.groups) assertTrue(g.title, g.title in c.texts)
        val lineCells = GuideCatalog.groups.flatMap { it.cells }.flatMap { cell -> cell.marks.map { cell to it } }
            .filter { (_, m) -> GuideCatalog.shape(m, templates) is kbd.graffiti.guide.GuideShape.Line }
        assertEquals(lineCells.size, c.paths)
        assertEquals(lineCells.count { it.first.interim }, c.dashed)
        assertEquals(7, c.texts.count { it == GuideCatalog.UNSETTLED })
        assertTrue(c.texts.any { it.contains(GraffitiGuidePanel.INTERIM) })
        assertTrue(c.texts.none { it.contains("Graffiti") || it.contains("그래피티") })   // generic name only
    }

    @Test
    fun `GD-11 layout - columns fill the width, the first cells start under the ✕, the content scrolls in 5 rows`() {
        val l = GraffitiGuideLayout(WIDTH, GuideCatalog.groups)
        assertEquals(5, l.columns)
        assertEquals(GuideCatalog.groups.sumOf { it.cells.size }, l.cells.size)
        assertEquals(GuideCatalog.groups.flatMap { it.cells }, l.cells.map { it.cell })   // same order
        assertTrue(l.cells.first().rect.top >= GraffitiGuideLayout.CLOSE_DP)
        assertTrue(l.titles.all { it.rect.right <= WIDTH - GraffitiGuideLayout.CLOSE_DP })
        assertTrue(l.cells.all { it.rect.left >= 0f && it.rect.right <= WIDTH + 0.01f })
        assertTrue(l.maxScrollDp(240f) > 0f)
        assertEquals(9, GraffitiGuideLayout(640f, GuideCatalog.groups).columns)   // landscape: more per line
        assertEquals(1, GraffitiGuideLayout(40f, GuideCatalog.groups).columns)
    }

    private companion object {
        const val WIDTH = 360f
        const val ROW = 48f
    }
}
