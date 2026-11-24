package kbd.app.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Looper
import android.text.TextPaint
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import java.time.Duration
import kbd.app.feedback.FeedbackPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/**
 * `SuggestionBarView` and `SuggestionAccessHelper` (LLD-11 §7.12 (자) SB-01~SB-12, LLD-12 §9, SRS FR-SUG-03, FSS#73):
 * three equal cells, drawing, the press and pick with the press-time revision, and the screen reader nodes.
 * Real text widths ([GraphicsMode.Mode.NATIVE]) for the fitting of long words.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SuggestionBarViewTest {

    private val events = ArrayList<String>()

    private val feedback = object : FeedbackPort {
        override fun key() {
            events += "key"
        }

        override fun error() {
            events += "error"
        }
    }

    private lateinit var bar: SuggestionBarView
    private lateinit var touch: Touches
    private var density = 1f

    @Before
    fun setUp() {
        bar = SuggestionBarView(RuntimeEnvironment.getApplication(), feedback)
        bar.onPick = { i, r -> events += "pick:$i@$r" }
        density = bar.resources.displayMetrics.density
        layoutExactly(bar, W, H)
        touch = Touches(bar)
    }

    private fun cells(vararg s: String) = s.map { BarCell(it) }

    private fun bounds(i: Int) = Rect().also { bar.cellBounds(i, it) }

    private fun centreX(i: Int) = bounds(i).exactCenterX()

    /** What one draw of the bar asked for: texts (with x, y, size) and filled rectangles (with colour). */
    private class DrawCanvas : Canvas() {
        data class TextCall(val text: String, val x: Float, val y: Float, val size: Float)

        val texts = ArrayList<TextCall>()
        val rects = ArrayList<Pair<List<Float>, Int>>()

        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            texts += TextCall(text, x, y, paint.textSize)
        }

        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
            rects += listOf(left, top, right, bottom) to paint.color
        }
    }

    private fun draw(): DrawCanvas = DrawCanvas().also { bar.draw(it) }

    @Test
    fun `SB-01 three equal cells, the rounding at the ends of each`() {
        assertEquals(3, bar.cellCount)
        assertEquals(listOf(Rect(0, 0, 110, H), Rect(110, 0, 220, H), Rect(220, 0, 330, H)), (0..2).map(::bounds))
        layoutExactly(bar, 331, H)
        assertEquals(listOf(Rect(0, 0, 110, H), Rect(110, 0, 221, H), Rect(221, 0, 331, H)), (0..2).map(::bounds))
        assertEquals(listOf(0, 0, 1, 1, 2, 2), listOf(0f, 109.9f, 110f, 220.9f, 221f, 330.9f).map(bar::cellAt))
        assertEquals(-1, bar.cellAt(-0.1f))
        assertEquals(-1, bar.cellAt(331f))
    }

    @Test
    fun `SB-02 two cells - their texts at the centres of cells 0 and 1, none in cell 2, two separators`() {
        bar.show(cells("a", "b"), 1)
        val c = draw()
        assertEquals(listOf("a", "b"), c.texts.map { it.text })
        assertEquals(listOf(centreX(0), centreX(1)), c.texts.map { it.x })
        assertTrue(c.texts.all { it.size == H * SuggestionBarView.TEXT_RATIO })
        val seps = c.rects.filter { it.second == SuggestionBarView.SEPARATOR_COLOR }.map { it.first }
        assertEquals(2, seps.size)
        for ((s, x) in seps.zip(listOf(110f, 220f))) {
            assertEquals(x, (s[0] + s[2]) / 2, 0.01f)
            assertEquals(SuggestionBarView.SEPARATOR_DP * density, s[2] - s[0], 0.01f)   // 1 dp wide
            assertEquals(H * 0.25f, s[1], 0.01f)   // half the height, centred
            assertEquals(H * 0.75f, s[3], 0.01f)
        }
        assertTrue(c.rects.none { it.second == SuggestionBarView.PRESSED_COLOR })
        assertEquals(null, bar.background)   // the root paints the plate under it
    }

    @Test
    fun `SB-03 pressed and released in cell 1 - feedback on the press, one pick with the revision`() {
        bar.show(cells("a", "b"), 7)
        touch.down(0, centreX(1), H / 2f)
        assertEquals(listOf("key"), events)
        assertTrue(draw().rects.any { it.second == SuggestionBarView.PRESSED_COLOR && it.first == listOf(110f, 0f, 220f, H.toFloat()) })
        touch.up(0)
        assertEquals(listOf("key", "pick:1@7"), events)
        assertTrue(draw().rects.none { it.second == SuggestionBarView.PRESSED_COLOR })
    }

    @Test
    fun `SB-04 released in another cell, above or below the bar - no pick`() {
        bar.show(cells("a", "b", "c"), 1)
        touch.down(0, centreX(0), H / 2f)
        touch.move(0, centreX(1), H / 2f)
        touch.up(0)
        for (y in listOf(-1f, H.toFloat(), H + 10f)) {
            touch.down(0, centreX(0), H / 2f)
            touch.move(0, centreX(0), y)
            touch.up(0)
        }
        assertEquals(List(4) { "key" }, events)
    }

    @Test
    fun `SB-05 an empty cell, or an empty bar - no feedback, no pick, taken`() {
        bar.show(cells("a", "b"), 1)
        touch.down(0, centreX(2), H / 2f)
        assertTrue(touch.lastHandled)
        touch.up(0)
        assertTrue(touch.lastHandled)
        bar.show(emptyList(), 1)
        for (i in 0..2) {
            touch.down(0, centreX(i), H / 2f)
            assertTrue(touch.lastHandled)
            touch.up(0)
            assertTrue(touch.lastHandled)
        }
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `SB-06 the pick carries the revision of the press, not a later one`() {
        bar.show(cells("they", "there"), 3)
        touch.down(0, centreX(0), H / 2f)
        bar.show(cells("the", "then"), 4)
        touch.up(0)
        assertEquals(listOf("key", "pick:0@3"), events)
    }

    @Test
    fun `SB-07 a cancelled press picks nothing`() {
        bar.show(cells("a"), 1)
        touch.down(0, centreX(0), H / 2f)
        touch.cancel()
        assertEquals(listOf("key"), events)
        assertTrue(draw().rects.none { it.second == SuggestionBarView.PRESSED_COLOR })
    }

    @Test
    fun `SB-08 a long word is cut with an ellipsis at 0_70 of the size, a little long one shrinks to fit, a short one keeps the size`() {
        val base = H * SuggestionBarView.TEXT_RATIO
        val avail = bounds(0).width() - 2 * SuggestionBarView.TEXT_INSET_DP * density
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = base }
        // The shortest run of "m" wider than the cell at the base size: it fits between the base and 0.70 of it.
        val little = generateSequence(1) { it + 1 }.map { "m".repeat(it) }.first { paint.measureText(it) > avail }
        assertTrue(paint.measureText(little) * SuggestionBarView.MIN_SCALE < avail)
        val long = "abcdefghij".repeat(4)   // 40 letters
        bar.show(cells(long, little, "go"), 1)
        val (l, m, s) = draw().texts
        assertEquals(base * SuggestionBarView.MIN_SCALE, l.size, 0.01f)
        assertTrue(l.text, l.text.endsWith("…"))
        assertTrue(l.text.length < long.length)
        paint.textSize = l.size
        assertTrue(paint.measureText(l.text) <= avail + 0.5f)
        assertEquals(little, m.text)
        assertTrue("${m.size}", m.size < base && m.size > base * SuggestionBarView.MIN_SCALE)
        paint.textSize = m.size
        assertTrue(paint.measureText(little) <= avail + 0.5f)
        assertEquals("go", s.text)
        assertEquals(base, s.size, 0.001f)
    }

    @Test
    fun `SB-09 the same cells again - not drawn again but the new revision is kept, other cells - drawn again`() {
        val shadow = Shadows.shadowOf(bar)
        bar.show(cells("a", "b"), 3)
        shadow.clearWasInvalidated()
        bar.show(cells("a", "b"), 4)
        assertFalse(shadow.wasInvalidated())
        touch.tap(0, centreX(1), H / 2f)
        assertEquals(listOf("key", "pick:1@4"), events)
        shadow.clearWasInvalidated()
        bar.show(cells("a", "c"), 5)
        assertTrue(shadow.wasInvalidated())
    }

    @Test
    fun `SB-10 a long press neither repeats nor moves - one pick on release`() {
        bar.show(cells("a"), 2)
        touch.down(0, centreX(0), H / 2f)
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2000))
        touch.up(0)
        assertEquals(listOf("key", "pick:0@2"), events)
    }

    private val provider get() = bar.accessibilityNodeProvider!!

    private fun nodeCount(): Int = provider.createAccessibilityNodeInfo(View.NO_ID)!!.childCount

    @Test
    fun `SB-11 the screen reader - one button per cell with text, named and bounded, a click picks`() {
        bar.show(listOf(BarCell("they"), BarCell("there")), 5)
        assertEquals(2, nodeCount())
        for ((i, name) in listOf("they", "there").withIndex()) {
            val n = provider.createAccessibilityNodeInfo(i)!!
            assertEquals(name, n.contentDescription)
            assertEquals("android.widget.Button", n.className)
            @Suppress("DEPRECATION")
            assertEquals(bounds(i), Rect().also(n::getBoundsInParent))
            assertTrue(n.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK })
        }
        assertTrue(provider.performAction(0, AccessibilityNodeInfo.ACTION_CLICK, null))
        assertEquals(listOf("pick:0@5"), events)   // no key feedback: the screen reader gives its own
        assertFalse(provider.performAction(2, AccessibilityNodeInfo.ACTION_CLICK, null))
    }

    @Test
    fun `SB-12 the tree is made again when the cells change, with no announcement`() {
        val sent = ArrayList<Int>()
        val parent = object : FrameLayout(RuntimeEnvironment.getApplication()) {
            override fun requestSendAccessibilityEvent(child: View, event: AccessibilityEvent): Boolean {
                sent += event.eventType
                return true
            }
        }
        parent.addView(bar)
        layoutExactly(bar, W, H)
        val am = RuntimeEnvironment.getApplication().getSystemService(AccessibilityManager::class.java)
        Shadows.shadowOf(am).setEnabled(true)
        bar.show(cells("they", "there"), 5)
        assertEquals(2, nodeCount())
        bar.show(emptyList(), 6)
        assertEquals(0, nodeCount())
        bar.show(cells("x"), 7)
        assertEquals(1, nodeCount())
        assertEquals("x", provider.createAccessibilityNodeInfo(0)!!.contentDescription)
        assertTrue(sent.isNotEmpty())   // the tree was told it changed
        assertTrue("$sent", sent.none { it == AccessibilityEvent.TYPE_ANNOUNCEMENT })
    }

    private companion object {
        const val W = 330
        const val H = 48
    }
}
