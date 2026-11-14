package kbd.app.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import java.io.File
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayout
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.TapOutput
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * LLD-11 §7.5 BV-01..BV-04: the two-row ↵ on `TapKeyboardView` (FSS#105, SRS FR-TAP-03d ②⑤⑦, LLD-05A §4, §7) — one
 * rectangle drawn over both rows with the label in its middle at the size of the other labels, pressed as a whole,
 * one accessibility node over both rows, listed after l.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class TallEnterViewTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val events = ArrayList<String>()
    private lateinit var view: TapKeyboardView

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

    private object NoFeedback : FeedbackPort {
        override fun key() = Unit
        override fun error() = Unit
    }

    private object NoState : GuardedState {
        override fun snapshot(): Any = Unit
        override fun restore(s: Any) {}
    }

    private var density = 1f
    private lateinit var touch: Touches

    private fun show(layout: TapLayout = TapLayouts.QWERTY) {
        val guard = EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState)
        view = TapKeyboardView(RuntimeEnvironment.getApplication(), sink, guard, NoFeedback)
        view.setLayout(layout, ThresholdDefaults.VALUES, ROW)
        density = view.resources.displayMetrics.density
        layoutExactly(view, Math.round(360f * density), Math.round(6 * ROW * density))
        touch = Touches(view)
    }

    private fun index(id: String) = TapLayouts.QWERTY.keys.indexOfFirst { it.id == id }

    /** The keys as drawn, in drawing order: each rounded rectangle with its colour, each label with its place and size. */
    private class Drawn : Canvas() {
        val rects = ArrayList<Pair<RectF, Int>>()
        val labels = ArrayList<Triple<String, Pair<Float, Float>, Float>>()

        override fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint) {
            rects += RectF(rect) to paint.color
        }

        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            labels += Triple(text, x to y, paint.textSize)
        }
    }

    private fun draw() = Drawn().also { view.draw(it) }

    @Test
    fun `BV-01 enter is drawn as one rectangle over rows 3 and 4 with its label in the middle, as big as the other labels`() {
        for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            show(layout)
            val d = draw()
            assertEquals(layout.name, layout.keys.size, d.rects.size)   // one rectangle per key, ↵ once
            val (r, _) = d.rects[index("enter")]
            val inset = 2f * density
            assertEquals(layout.name, 324f * density + inset, r.left, 0.5f)
            assertEquals(layout.name, 360f * density - inset, r.right, 0.5f)
            assertEquals(layout.name, 3 * ROW * density + inset, r.top, 0.5f)
            assertEquals(layout.name, 5 * ROW * density - inset, r.bottom, 0.5f)
            val (text, at, size) = d.labels.single { it.first == "↵" }
            assertEquals(layout.name, "↵", text)
            assertEquals(layout.name, r.centerX(), at.first, 0.01f)
            // Vertically centred on the rectangle (= the boundary of its two rows), as the view centres every label.
            val p = Paint().apply { textSize = size }
            assertEquals(layout.name, r.centerY() - (p.ascent() + p.descent()) / 2, at.second, 1f)
            assertEquals(layout.name, 4 * ROW * density, r.centerY(), 0.5f)
            assertEquals(layout.name, view.labelTextSizePx, size, 0f)
            assertEquals(layout.name, 0.4f * ROW * density, size, 0.01f)
            assertEquals(layout.name, d.labels.first { it.first == layout.keys[index("q")].label }.third, size, 0f)
        }
    }

    @Test
    fun `BV-02 a held enter is pressed as a whole, whichever row it was pressed on`() {
        show()
        for (y in listOf(3.2f, 4.8f)) {
            touch.down(0, 342f * density, y * ROW * density)
            val d = draw()
            assertEquals(PRESSED_COLOR, d.rects[index("enter")].second)
            assertEquals(1, d.rects.count { it.second == PRESSED_COLOR })
            touch.up(0)
        }
        assertEquals(listOf("down:ENTER", "up:ENTER", "down:ENTER", "up:ENTER"), events)
    }

    @Test
    fun `BV-03 one accessibility node for enter - bounds over both rows, both rows lead to it, after l and before z`() {
        show()
        val i = index("enter")
        val b = Rect()
        view.keyBoundsPx(i, b)
        assertEquals(Rect(px(324f), px(3 * ROW), px(360f), px(5 * ROW)), b)
        @Suppress("DEPRECATION")
        val nodeBounds = Rect().also { view.accessibilityNodeProvider.createAccessibilityNodeInfo(i)!!.getBoundsInParent(it) }
        assertEquals(b, nodeBounds)
        assertEquals(i, view.keyIndexAtPx(342f * density, 3.2f * ROW * density))
        assertEquals(i, view.keyIndexAtPx(342f * density, 4.8f * ROW * density))
        assertEquals(index("l") + 1, i)
        assertEquals(index("z") - 1, i)
        assertEquals(TapLayouts.QWERTY.keys.size, view.keyCount)
        assertEquals(1, TapLayouts.QWERTY.keys.count { it.function == FunctionKey.ENTER })
    }

    @Test
    fun `BV-04 the moved slash key is drawn in row 4 left of enter and the space bar 216 wide`() {
        show()
        val d = draw()
        val inset = 2f * density
        val slash = d.rects[index("slash")].first
        assertEquals(288f * density + inset, slash.left, 0.5f)
        assertEquals(4 * ROW * density + inset, slash.top, 0.5f)
        val space = d.rects[index("space")].first
        assertEquals(216f * density - 2 * inset, space.width(), 0.5f)
    }

    private fun px(dp: Float): Int = Math.round(dp * density)

    private companion object {
        const val ROW = 48f
        const val PRESSED_COLOR = 0xFF5A5A5A.toInt()
    }
}
