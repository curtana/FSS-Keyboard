package kbd.app.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Looper
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import java.time.Duration
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.stroke.geom.Dir
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayout
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.ChordModifier
import kbd.stroke.tap.TapGesture
import kbd.stroke.tap.TapOutput
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/**
 * LLD-11 §7.5 CV-01..CV-09: Ctrl and Alt on `TapKeyboardView` (LLD-05A §4, §7; SRS FR-TAP-03c, 15, 18; FSS#98) —
 * the touch path, the lit keys, the word labels and the accessibility nodes. The sink stands in for
 * `InputDispatcher`: it toggles its chord as the dispatcher does and tells the view.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class TapKeyboardChordViewTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val events = ArrayList<String>()
    private lateinit var view: TapKeyboardView

    private val sink = object : TapSink {
        override var chord: Set<ChordModifier> = emptySet()

        override fun onTapOutput(key: TapKeyDef, out: TapOutput) {
            events += "out:" + (out.text ?: out.jamo.toString())
        }

        override fun onTapResult(key: TapKeyDef, out: TapOutput, gesture: TapGesture) {
            events += "result:${key.id}:" + (out.text ?: out.jamo.toString()) + ":$gesture"
        }

        override fun onFunctionDown(key: FunctionKey) {
            events += "down:$key"
            val m = when (key) {
                FunctionKey.CTRL -> ChordModifier.CTRL
                FunctionKey.ALT -> ChordModifier.ALT
                else -> return
            }
            chord = if (m in chord) chord - m else chord + m
            view.chordChanged()
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

    private var density = 1f
    private lateinit var touch: Touches

    private fun show(layout: TapLayout = TapLayouts.QWERTY, widthDp: Float = 360f, rowDp: Float = 48f) {
        val guard = EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState)
        view = TapKeyboardView(RuntimeEnvironment.getApplication(), sink, guard, feedback)
        view.setLayout(layout, ThresholdDefaults.VALUES, rowDp)
        density = view.resources.displayMetrics.density
        layoutExactly(view, Math.round(widthDp * density), Math.round(6 * rowDp * density))
        touch = Touches(view)
    }

    private fun index(id: String) = TapLayouts.QWERTY.keys.indexOfFirst { it.id == id }
    private fun key(id: String) = TapLayouts.QWERTY.keys[index(id)]
    private fun cx(id: String) = key(id).let { (it.x + it.w / 2f) * density }
    private fun cy(id: String) = key(id).let { (it.row + it.rowSpan / 2f) * 48f * density }

    /** The keys as drawn: background colour and label text size by label. */
    private class Drawn : Canvas() {
        val colors = ArrayList<Int>()
        val textSizes = LinkedHashMap<String, Float>()

        override fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint) {
            colors += paint.color
        }

        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            textSizes.putIfAbsent(text, paint.textSize)
        }
    }

    private fun draw() = Drawn().also { view.draw(it) }

    private fun node(id: String): AccessibilityNodeInfo =
        view.accessibilityNodeProvider.createAccessibilityNodeInfo(index(id))!!

    @Test
    fun `CV-01 Ctrl and Alt act on press like the other function keys and do not repeat`() {
        show()
        touch.down(0, cx("ctrl"), cy("ctrl"))
        assertEquals(listOf("key", "down:CTRL"), events)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getKeyRepeatTimeout() * 3L))
        assertEquals(listOf("key", "down:CTRL"), events)   // held: no repeat (FR-TAP-15 ①)
        touch.up(0)
        assertEquals(listOf("key", "down:CTRL", "up:CTRL"), events)
        events.clear()
        touch.tap(0, cx("alt"), cy("alt"))
        assertEquals(listOf("key", "down:ALT", "up:ALT"), events)
        assertEquals(setOf(ChordModifier.CTRL, ChordModifier.ALT), sink.chord)
        assertFalse(FunctionKey.CTRL in TapTouchLogic.REPEATING || FunctionKey.ALT in TapTouchLogic.REPEATING)
    }

    @Test
    fun `CV-02 a character key reaches the sink with its judged gesture, in press order`() {
        show()
        touch.tap(0, cx("c"), cy("c"))
        touch.down(0, cx("a"), cy("a"))
        touch.move(0, cx("a"), cy("a") - 40f * density, steps = 5)   // out of the top edge
        touch.down(1, cx("i"), cy("i"))
        touch.move(1, cx("i") - 30f * density, cy("i"), steps = 5)   // out of the left edge
        touch.up(1)                                                   // waits for a
        touch.up(0)
        assertEquals(
            listOf("result:c:c:${TapGesture.Tap}", "result:a:A:${TapGesture.Exit(Dir.UP)}", "result:i:[:${TapGesture.Exit(Dir.LEFT)}"),
            events.filter { it.startsWith("result") },
        )
        assertTrue(events.none { it.startsWith("out:") })   // the view calls the new entry point only
    }

    @Test
    fun `CV-03 a key that is on is drawn lit - unlike a function key, unlike a pressed key - and plain again when off`() {
        show()
        val ctrl = index("ctrl")
        val alt = index("alt")
        val esc = index("esc")
        val off = draw().colors
        assertEquals(off[esc], off[ctrl])
        assertEquals(off[esc], off[alt])

        touch.down(0, cx("ctrl"), cy("ctrl"))          // on from the press, under the finger too
        touch.down(1, cx("esc"), cy("esc"))            // a pressed key, to compare
        val held = draw().colors
        assertNotEquals(off[esc], held[ctrl])          // not a plain function key
        assertNotEquals(held[esc], held[ctrl])         // not a pressed key
        assertNotEquals(off[esc], held[esc])
        assertEquals(off[alt], held[alt])
        touch.up(1)
        touch.up(0)
        val on = draw().colors
        assertEquals(held[ctrl], on[ctrl])             // stays lit after the release
        assertEquals(off[esc], on[esc])
        assertTrue(view.isChordOn(key("ctrl")))
        assertFalse(view.isChordOn(key("alt")))
        assertFalse(view.isChordOn(key("esc")))

        sink.chord = setOf(ChordModifier.ALT)          // Ctrl rode on a key; the dispatcher tells the view
        view.chordChanged()
        val moved = draw().colors
        assertEquals(off[ctrl], moved[ctrl])
        assertEquals(on[ctrl], moved[alt])
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)   // real text widths
    fun `CV-04 the four words share one text size, no larger than the other labels, and fit their key`() {
        for ((widthDp, rowDp) in listOf(360f to 48f, 327.3f to 48f, 320f to 48f, 320f to 40f, 800f to 48f)) {
            show(widthDp = widthDp, rowDp = rowDp)
            val d = draw()
            val words = listOf("Esc", "Ctrl", "Alt", "Del").map { d.textSizes.getValue(it) }
            assertEquals("$widthDp", 1, words.toSet().size)
            assertEquals(view.wordTextSizePx, words[0], 0f)
            assertTrue("$widthDp", view.wordTextSizePx <= view.labelTextSizePx)
            assertEquals(view.labelTextSizePx, d.textSizes.getValue("⇥"), 0f)   // the other edit row labels keep their size
            assertEquals(view.labelTextSizePx, d.textSizes.getValue("q"), 0f)
            val inner = (widthDp / 10f - 2 * (2f + 2f)) * density               // key − 2 × (inset 2 dp + pad 2 dp)
            val paint = Paint().apply { textSize = view.wordTextSizePx }
            for (w in listOf("Esc", "Ctrl", "Alt", "Del")) assertTrue("$w at $widthDp", paint.measureText(w) <= inner)
            // Shrunk only as far as needed: on a wide key the words keep the label size.
            if (widthDp >= 800f) assertEquals(view.labelTextSizePx, view.wordTextSizePx, 0f)
            if (widthDp <= 360f) assertTrue("$widthDp", view.wordTextSizePx < view.labelTextSizePx)
        }
    }

    @Test
    fun `CV-05 WordLabelFit keeps the size when the word fits and scales it down to fit when it does not`() {
        assertEquals(20f, WordLabelFit.size(20f, widestAtBase = 24f, innerWidth = 24f), 0f)
        assertEquals(20f, WordLabelFit.size(20f, widestAtBase = 10f, innerWidth = 24f), 0f)
        assertEquals(16f, WordLabelFit.size(20f, widestAtBase = 30f, innerWidth = 24f), 1e-4f)
        assertEquals(10f, WordLabelFit.size(20f, widestAtBase = 48f, innerWidth = 24f), 1e-4f)
        assertEquals(0f, WordLabelFit.size(20f, widestAtBase = 30f, innerWidth = -3f), 0f)
        assertEquals(20f, WordLabelFit.size(20f, widestAtBase = 0f, innerWidth = 24f), 0f)   // nothing measured
    }

    @Test
    fun `CV-06 the Ctrl and Alt nodes are toggles that tell their state, the other keys are not`() {
        show()
        for (id in listOf("ctrl", "alt")) {
            val n = node(id)
            assertTrue(id, n.isCheckable)
            assertFalse(id, n.isChecked)
            assertEquals("android.widget.ToggleButton", n.className.toString())
        }
        assertEquals("Ctrl", node("ctrl").contentDescription.toString())
        assertEquals("Alt", node("alt").contentDescription.toString())
        for (id in listOf("esc", "tab", "a", "mode")) assertFalse(id, node(id).isCheckable)

        touch.tap(0, cx("ctrl"), cy("ctrl"))
        assertTrue(node("ctrl").isChecked)
        assertFalse(node("alt").isChecked)
        sink.chord = emptySet()                 // rode on a key
        view.chordChanged()
        assertFalse(node("ctrl").isChecked)
    }

    @Test
    fun `CV-07 the click action on a node toggles like a touch, and a clicked letter is a tap`() {
        show()
        val provider = view.accessibilityNodeProvider
        assertTrue(provider.performAction(index("ctrl"), AccessibilityNodeInfo.ACTION_CLICK, null))
        assertEquals(listOf("down:CTRL", "up:CTRL"), events)
        assertTrue(node("ctrl").isChecked)
        events.clear()
        assertTrue(provider.performAction(index("c"), AccessibilityNodeInfo.ACTION_CLICK, null))
        assertEquals(listOf("result:c:c:${TapGesture.Tap}"), events)
        events.clear()
        assertTrue(provider.performAction(index("ctrl"), AccessibilityNodeInfo.ACTION_CLICK, null))
        assertFalse(node("ctrl").isChecked)
    }

    @Test
    fun `CV-08 the Dubeolsik view has the same two keys and the same display`() {
        show(TapLayouts.DUBEOLSIK)
        assertEquals(TapLayouts.QWERTY.keys.map { it.id }, TapLayouts.DUBEOLSIK.keys.map { it.id })
        touch.tap(0, cx("alt"), cy("alt"))
        assertEquals(listOf("key", "down:ALT", "up:ALT"), events)
        assertTrue(view.isChordOn(TapLayouts.DUBEOLSIK.keys[index("alt")]))
        val c = draw().colors
        assertNotEquals(c[index("esc")], c[index("alt")])
        assertTrue(node("alt").isChecked)
    }

    @Test
    fun `CV-09 a sink without chords shows nothing on (the practice keyboard)`() {
        val plain = object : TapSink {
            override fun onTapOutput(key: TapKeyDef, out: TapOutput) = Unit
            override fun onFunctionDown(key: FunctionKey) = Unit
            override fun onFunctionUp(key: FunctionKey) = Unit
        }
        val guard = EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState)
        view = TapKeyboardView(RuntimeEnvironment.getApplication(), plain, guard, feedback)
        view.setLayout(TapLayouts.QWERTY, ThresholdDefaults.VALUES, 48f)
        density = view.resources.displayMetrics.density
        layoutExactly(view, Math.round(360f * density), Math.round(288f * density))
        Touches(view).tap(0, cx("ctrl"), cy("ctrl"))
        assertEquals(emptySet<ChordModifier>(), plain.chord)
        assertFalse(view.isChordOn(key("ctrl")))
        val c = draw().colors
        assertEquals(c[index("esc")], c[index("ctrl")])
        assertFalse(node("ctrl").isChecked)
    }
}
