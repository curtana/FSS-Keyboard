package kbd.app.view

import java.io.File
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayout
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.TapGesture
import kbd.stroke.tap.TapOutput
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * LLD-11 §5.7 BE-01..BE-07: the two-row ↵ and the rows without blanks (FSS#105, SRS FR-TAP-03d, LLD-05A §3.2), and
 * §5.8 PV-01: strokes of the Dubeolsik paired vowels onto their neighbours (FSS#104), on
 * [TapGeometry] and [TapTouchLogic], on the JVM with fakes. The geometry is 360 dp wide, 48 dp rows, 16 dp side margins.
 */
class TallEnterTouchTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class FakeSink : TapSink {
        val events = ArrayList<String>()

        override fun onTapOutput(key: TapKeyDef, out: TapOutput) = error("the view calls onTapResult")

        override fun onTapResult(key: TapKeyDef, out: TapOutput, gesture: TapGesture) {
            events += "${key.id}:" + (out.text ?: out.jamo.toString()) + ":$gesture"
        }

        override fun onFunctionDown(key: FunctionKey) {
            events += "down:$key"
        }

        override fun onFunctionUp(key: FunctionKey) {
            events += "up:$key"
        }
    }

    private class FakeRepeat : TapKeyRepeat {
        val started = ArrayList<FunctionKey>()
        override fun start(pointerId: Int, key: FunctionKey) {
            started += key
        }

        override fun stop(pointerId: Int) = Unit
        override fun stopAll() = Unit
    }

    private object NoState : GuardedState {
        override fun snapshot(): Any = Unit
        override fun restore(s: Any) {}
    }

    private object NoFeedback : FeedbackPort {
        override fun key() = Unit
        override fun error() = Unit
    }

    private val sink = FakeSink()
    private val repeat = FakeRepeat()

    private fun logic(layout: TapLayout = TapLayouts.QWERTY) =
        TapTouchLogic(sink, EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState), NoFeedback, repeat).also {
            it.geometry = TapGeometry.build(layout, WIDTH, ROW, SIDE)
            it.minCrossDp = MIN
        }

    private fun geometry(layout: TapLayout = TapLayouts.QWERTY) = TapGeometry.build(layout, WIDTH, ROW, SIDE)

    private fun idAt(x: Float, y: Float, layout: TapLayout = TapLayouts.QWERTY): String =
        geometry(layout).let { it.keys[it.indexAt(x, y)].id }

    private fun centre(id: String): StrokePoint {
        val r = geometry().geoms.first { it.id == id }.rect
        return StrokePoint((r.left + r.right) / 2, (r.top + r.bottom) / 2, 0)
    }

    /** From the centre of [id] by (dx, dy) in eight steps, then released there. */
    private fun stroke(l: TapTouchLogic, id: String, dx: Float, dy: Float) {
        val c = centre(id)
        l.down(0, c)
        l.move(0, (1..8).map { StrokePoint(c.x + dx * it / 8, c.y + dy * it / 8, it * 5L) })
        l.up(0, StrokePoint(c.x + dx, c.y + dy, 50))
    }

    @Test
    fun `BE-01 every point of rows 3 and 4 at x 324 or more is enter, the boundary and the four inside corners too`() {
        for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            val points = listOf(
                324f to 3 * ROW, 359.9f to 3 * ROW, 324f to 5 * ROW - 0.01f, 359.9f to 5 * ROW - 0.01f,   // corners
                342f to 4 * ROW - 0.01f, 342f to 4 * ROW, 342f to 4 * ROW + 0.01f,                       // the row boundary
                342f to 3.5f * ROW, 342f to 4.5f * ROW, 330f to 3.1f * ROW, 355f to 4.9f * ROW,
            )
            for ((x, y) in points) assertEquals("${layout.name} ($x, $y)", "enter", idAt(x, y, layout))
            // Its neighbours: p above, l and `/` left, `.` below.
            assertEquals("p", idAt(342f, 3 * ROW - 0.01f, layout))
            assertEquals("l", idAt(323.9f, 3.5f * ROW, layout))
            assertEquals("slash", idAt(323.9f, 4.5f * ROW, layout))
            assertEquals("period", idAt(342f, 5 * ROW, layout))
        }
    }

    @Test
    fun `BE-02 the keys of rows 3 and 4 moved half a key left - a at 0, z at 0, = at 252, slash at 288`() {
        "asdfghjkl".forEachIndexed { i, c -> assertEquals("$c", c.toString(), idAt(18f + 36f * i, 3.5f * ROW)) }
        "zxcvbnm".forEachIndexed { i, c -> assertEquals("$c", c.toString(), idAt(18f + 36f * i, 4.5f * ROW)) }
        assertEquals("minus", idAt(270f, 4.5f * ROW))
        assertEquals("slash", idAt(306f, 4.5f * ROW))
        // The old blanks are the keys themselves now (and ↵ on the right of row 3).
        assertEquals("a", idAt(1f, 3 * ROW + 1))
        assertEquals("z", idAt(1f, 4 * ROW + 1))
        assertEquals("enter", idAt(355f, 3 * ROW + 1))
        // Row 5: 72 · 216 · 36 · 36.
        assertEquals("mode", idAt(71f, 5.5f * ROW))
        assertEquals("space", idAt(73f, 5.5f * ROW))
        assertEquals("space", idAt(287f, 5.5f * ROW))
        assertEquals("comma", idAt(289f, 5.5f * ROW))
        assertEquals("period", idAt(359f, 5.5f * ROW))
    }

    @Test
    fun `BE-03 a press on either row of enter is one ENTER on press, moving between its rows is nothing, no repeat`() {
        for ((from, to) in listOf(3.2f to 4.8f, 4.8f to 3.2f)) {
            sink.events.clear()
            val l = logic()
            l.down(0, StrokePoint(342f, from * ROW, 0))
            assertEquals(listOf("down:ENTER"), sink.events)
            l.move(0, (1..8).map { StrokePoint(342f, (from + (to - from) * it / 8) * ROW, it * 5L) })
            assertEquals(listOf("down:ENTER"), sink.events)
            l.up(0, StrokePoint(342f, to * ROW, 50))
            assertEquals(listOf("down:ENTER", "up:ENTER"), sink.events)
        }
        assertTrue(repeat.started.isEmpty())
        // Out of ↵ onto `/` and released there: still nothing more.
        sink.events.clear()
        val l = logic()
        l.down(0, StrokePoint(342f, 4.5f * ROW, 0))
        l.move(0, listOf(StrokePoint(300f, 4.5f * ROW, 10)))
        l.up(0, StrokePoint(300f, 4.5f * ROW, 20))
        assertEquals(listOf("down:ENTER", "up:ENTER"), sink.events)
    }

    @Test
    fun `BE-04 strokes of the neighbours onto enter are the first-pressed key's result`() {
        val l = logic()
        stroke(l, "p", 0f, ROW)            // ↓ onto ↵
        stroke(l, "l", 36f, 0f)            // → onto ↵ (no assignment)
        stroke(l, "slash", 36f, 0f)        // → onto ↵ (no assignment)
        stroke(l, "period", 0f, -ROW)      // ↑ onto ↵
        assertEquals(
            listOf("p:|:Exit(dir=DOWN)", "l:l:Exit(dir=RIGHT)", "slash:/:Exit(dir=RIGHT)", "period:>:Exit(dir=UP)"),
            sink.events,
        )
    }

    @Test
    fun `BE-05 the moved slash key - = right onto it is =, slash up is ?, slash down is an ordinary crossing that types slash`() {
        val l = logic()
        stroke(l, "minus", 36f, 0f)
        stroke(l, "slash", 0f, -ROW)
        stroke(l, "slash", 0f, ROW)        // onto `,`: an ordinary edge now, not the bottom border
        assertEquals(listOf("minus:=:Exit(dir=RIGHT)", "slash:?:Exit(dir=UP)", "slash:/:Exit(dir=DOWN)"), sink.events)
    }

    @Test
    fun `BE-06 a and z left and period right into the side margin are ordinary crossings that type the key`() {
        val l = logic()
        stroke(l, "a", -30f, 0f)
        stroke(l, "z", -30f, 0f)
        stroke(l, "period", 30f, 0f)
        assertEquals(listOf("a:a:Exit(dir=LEFT)", "z:z:Exit(dir=LEFT)", "period:.:Exit(dir=RIGHT)"), sink.events)
    }

    @Test
    fun `BE-07 the Dubeolsik layout is the same - ㅣ is the l key, the same enter`() {
        val l = logic(TapLayouts.DUBEOLSIK)
        stroke(l, "l", 36f, 0f)
        l.down(1, StrokePoint(342f, 4.8f * ROW, 0))
        l.up(1, StrokePoint(342f, 3.2f * ROW, 10))
        assertEquals(listOf("l:ㅣ:Exit(dir=RIGHT)", "down:ENTER", "up:ENTER"), sink.events)
    }

    @Test
    fun `PV-01 Dubeolsik ㅜ down onto the space bar is ㅠ, the paired vowels of the first-pressed key (FSS#104)`() {
        val l = logic(TapLayouts.DUBEOLSIK)
        assertEquals("space", idAt(198f, 4.5f * ROW + ROW, TapLayouts.DUBEOLSIK))   // where the stroke ends
        stroke(l, "n", 0f, ROW)          // ↓ from ㅜ into the space bar
        stroke(l, "h", 0f, -ROW)         // ↑ from ㅗ onto ㅛ (y)
        stroke(l, "j", -36f, 0f)         // ← from ㅓ onto ㅗ (h)
        stroke(l, "k", 36f, 0f)          // → from ㅏ onto ㅣ (l)
        stroke(l, "x", 0f, -ROW)         // ↑ from ㅌ onto ㄴ (s): ㄾ (FSS#107)
        assertEquals(
            listOf("n:ㅠ:Exit(dir=DOWN)", "h:ㅛ:Exit(dir=UP)", "j:ㅕ:Exit(dir=LEFT)", "k:ㅑ:Exit(dir=RIGHT)", "x:ㄾ:Exit(dir=UP)"),
            sink.events,
        )
    }

    private companion object {
        const val WIDTH = 360f
        const val ROW = 48f
        const val SIDE = 16f
        val MIN = ThresholdDefaults.MIN_CROSS_DP
    }
}
