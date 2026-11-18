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
        stroke(l, "x", 0f, -ROW)         // ↑ from ㅌ onto ㄴ (s): ㅌ (FSS#109; ㄾ in FSS#107)
        assertEquals(
            listOf("n:ㅠ:Exit(dir=DOWN)", "h:ㅛ:Exit(dir=UP)", "j:ㅕ:Exit(dir=LEFT)", "k:ㅑ:Exit(dir=RIGHT)", "x:ㅌ:Exit(dir=UP)"),
            sink.events,
        )
    }

    @Test
    fun `GJ-01 Dubeolsik ㄹ is a 4-way key since FSS 124 - a diagonal is the edge crossed first, as on every consonant key (FSS 109, 124)`() {
        val l = logic(TapLayouts.DUBEOLSIK)
        stroke(l, "f", -60f, -30f)      // up-left, the left edge first: ←
        stroke(l, "f", 0f, -ROW)         // ↑ onto ㄱ (r)
        stroke(l, "f", 36f, 0f)          // → onto ㅎ (g)
        stroke(l, "f", 0f, ROW)          // ↓ onto ㅍ (v)
        stroke(l, "r", -36f, -36f)       // a diagonal on ㄱ: the edge crossed first (←)
        // The sink shows the consonant; its vowel (ㄹ ← = 러, FR-TAP-02h) is the resolver's, TA-26.
        assertEquals(
            listOf("f:ㄹ:Exit(dir=LEFT)", "f:ㄹ:Exit(dir=UP)", "f:ㄹ:Exit(dir=RIGHT)", "f:ㄹ:Exit(dir=DOWN)", "r:ㄱ:Exit(dir=LEFT)"),
            sink.events,
        )
        assertTrue(geometry(TapLayouts.DUBEOLSIK).keys.none { it.jamo != null && it.jamo in 'ㄱ'..'ㅎ' && it.eightWay })
        // QWERTY f is a 4-way key too: the same diagonal is the edge crossed first.
        sink.events.clear()
        val q = logic(TapLayouts.QWERTY)
        stroke(q, "f", -36f, -36f)
        assertEquals(listOf("f:f:Exit(dir=LEFT)"), sink.events)
    }

    /** From the centre of [from] straight to the centre of [to], released there. */
    private fun strokeTo(l: TapTouchLogic, from: String, to: String) {
        val a = centre(from)
        val b = centre(to)
        stroke(l, from, b.x - a.x, b.y - a.y)
    }

    @Test
    fun `GJ-03 the key under the release changes nothing - the pressed key and its direction decide (FSS 124 removed FR-TAP-02f)`() {
        val l = logic(TapLayouts.DUBEOLSIK)
        // The 11 pairs that gave a compound final by the release key in FSS#109..#122: now the pressed consonant's own
        // stroke, the same as released anywhere else in that direction.
        val pairs = listOf("r" to "t", "s" to "w", "s" to "g", "f" to "r", "f" to "a", "f" to "q", "f" to "t", "f" to "x",
            "f" to "v", "f" to "g", "q" to "t")
        val base = TapLayouts.DUBEOLSIK.keys.filter { it.jamo != null }.associate { it.id to it.jamo!! }
        for ((from, to) in pairs) {
            sink.events.clear()
            strokeTo(l, from, to)
            val e = sink.events.single().split(":")
            assertEquals("$from to $to", listOf(from, base.getValue(from).toString()), e.take(2))
            assertTrue("$from to $to", e[2].startsWith("Exit("))
        }
        // A rub released over another key: the rub (up = the plain ㄹ).
        sink.events.clear()
        val c = centre("f")
        val up = centre("r").y - c.y
        l.down(0, c)
        l.move(0, listOf(StrokePoint(c.x, c.y + up, 1), StrokePoint(c.x, c.y, 2), StrokePoint(c.x, c.y + up, 3)))
        l.up(0, StrokePoint(c.x, c.y + up, 4))
        assertEquals(listOf("f:ㄹ:Rub(dir=UP)"), sink.events)
        // QWERTY as before.
        val ql = logic(TapLayouts.QWERTY)
        sink.events.clear()
        strokeTo(ql, "f", "q")
        assertEquals(listOf("f:f:Exit(dir=LEFT)"), sink.events)
    }

    private companion object {
        const val WIDTH = 360f
        const val ROW = 48f
        const val SIDE = 16f
        val MIN = ThresholdDefaults.MIN_CROSS_DP
    }

    /** From the centre of [id] at [deg] degrees (0 = right, 90 = up) for [len] dp, released there. */
    private fun strokeAt(l: TapTouchLogic, id: String, deg: Double, len: Float = 40f) {
        val r = Math.toRadians(deg)
        stroke(l, id, (len * Math.cos(r)).toFloat(), (-len * Math.sin(r)).toFloat())
    }

    @Test
    fun `GV-01 Dubeolsik ㅗ ㅜ are judged in eight ways from their data, ㅡ in four, sector edges as ㄹ (FSS 110, FR-TAP-02g ②③)`() {
        val l = logic(TapLayouts.DUBEOLSIK)
        fun out(id: String, deg: Double): String {
            sink.events.clear()
            strokeAt(l, id, deg)
            return sink.events.single()
        }
        // One point in each sector (→ 0, ↗ 45, ↑ 90, ↖ 135, ← 180, ↙ 225, ↓ 270, ↘ 315).
        val o = listOf("h:ㅘ:Exit(dir=RIGHT)", "h:ㅗ:Exit(dir=UP_RIGHT)", "h:ㅛ:Exit(dir=UP)", "h:ㅗ:Exit(dir=UP_LEFT)",
            "h:ㅗ:Exit(dir=LEFT)", "h:ㅗ:Exit(dir=DOWN_LEFT)", "h:ㅚ:Exit(dir=DOWN)", "h:ㅙ:Exit(dir=DOWN_RIGHT)")
        val u = listOf("n:ㅜ:Exit(dir=RIGHT)", "n:ㅜ:Exit(dir=UP_RIGHT)", "n:ㅟ:Exit(dir=UP)", "n:ㅜ:Exit(dir=UP_LEFT)",
            "n:ㅝ:Exit(dir=LEFT)", "n:ㅞ:Exit(dir=DOWN_LEFT)", "n:ㅠ:Exit(dir=DOWN)", "n:ㅜ:Exit(dir=DOWN_RIGHT)")
        for (i in 0 until 8) {
            assertEquals(o[i], out("h", i * 45.0))
            assertEquals(u[i], out("n", i * 45.0))
        }
        // Sector edges: → and ↓ are 60° wide, the diagonals 30° (FR-TAP-02e ②), just inside and outside each edge.
        assertEquals("h:ㅘ:Exit(dir=RIGHT)", out("h", -29.0))
        assertEquals("h:ㅙ:Exit(dir=DOWN_RIGHT)", out("h", -31.0))
        assertEquals("h:ㅙ:Exit(dir=DOWN_RIGHT)", out("h", -59.0))
        assertEquals("h:ㅚ:Exit(dir=DOWN)", out("h", -61.0))
        assertEquals("h:ㅛ:Exit(dir=UP)", out("h", 61.0))
        assertEquals("h:ㅗ:Exit(dir=UP_RIGHT)", out("h", 59.0))
        assertEquals("n:ㅝ:Exit(dir=LEFT)", out("n", 209.0))
        assertEquals("n:ㅞ:Exit(dir=DOWN_LEFT)", out("n", 211.0))
        assertEquals("n:ㅞ:Exit(dir=DOWN_LEFT)", out("n", 239.0))
        assertEquals("n:ㅠ:Exit(dir=DOWN)", out("n", 241.0))
        assertEquals("n:ㅟ:Exit(dir=UP)", out("n", 119.0))
        // ㅡ is a 4-way key: a diagonal is the edge crossed first, ↓ ㅢ, the rest the plain ㅡ.
        assertEquals("m:ㅢ:Exit(dir=DOWN)", out("m", 270.0))
        assertEquals("m:ㅡ:Exit(dir=UP)", out("m", 90.0))
        assertEquals("m:ㅡ:Exit(dir=LEFT)", out("m", 180.0))
        sink.events.clear()
        stroke(l, "m", 20f, 40f)                                       // a diagonal crossing the bottom edge first
        assertEquals(listOf("m:ㅢ:Exit(dir=DOWN)"), sink.events)
        // ㄹ 4-way since FSS#124; ㅓ ㅏ 8-way since FSS#126 (GV-04).
        assertEquals(listOf("h", "j", "k", "n"), geometry(TapLayouts.DUBEOLSIK).keys.filter { it.eightWay }.map { it.id })
    }

    @Test
    fun `GV-04 Dubeolsik ㅓ ㅏ are judged in eight ways from their data, sector edges as ㅗ ㅜ (FSS 126, FR-TAP-02j)`() {
        val l = logic(TapLayouts.DUBEOLSIK)
        fun out(id: String, deg: Double): String {
            sink.events.clear()
            strokeAt(l, id, deg)
            return sink.events.single()
        }
        // One point in each sector (→ 0, ↗ 45, ↑ 90, ↖ 135, ← 180, ↙ 225, ↓ 270, ↘ 315).
        val eo = listOf("j:ㅓ:Exit(dir=RIGHT)", "j:ㅓ:Exit(dir=UP_RIGHT)", "j:ㅓ:Exit(dir=UP)", "j:ㅖ:Exit(dir=UP_LEFT)",
            "j:ㅕ:Exit(dir=LEFT)", "j:ㅔ:Exit(dir=DOWN_LEFT)", "j:ㅓ:Exit(dir=DOWN)", "j:ㅓ:Exit(dir=DOWN_RIGHT)")
        val a = listOf("k:ㅑ:Exit(dir=RIGHT)", "k:ㅒ:Exit(dir=UP_RIGHT)", "k:ㅏ:Exit(dir=UP)", "k:ㅏ:Exit(dir=UP_LEFT)",
            "k:ㅏ:Exit(dir=LEFT)", "k:ㅏ:Exit(dir=DOWN_LEFT)", "k:ㅏ:Exit(dir=DOWN)", "k:ㅐ:Exit(dir=DOWN_RIGHT)")
        for (i in 0 until 8) {
            assertEquals(eo[i], out("j", i * 45.0))
            assertEquals(a[i], out("k", i * 45.0))
        }
        // Sector edges (FR-TAP-02e ②③): ← and → 60° wide, the diagonals 30°, just inside and outside each edge.
        assertEquals("j:ㅕ:Exit(dir=LEFT)", out("j", 151.0))
        assertEquals("j:ㅖ:Exit(dir=UP_LEFT)", out("j", 149.0))
        assertEquals("j:ㅖ:Exit(dir=UP_LEFT)", out("j", 121.0))
        assertEquals("j:ㅓ:Exit(dir=UP)", out("j", 119.0))
        assertEquals("j:ㅕ:Exit(dir=LEFT)", out("j", 209.0))
        assertEquals("j:ㅔ:Exit(dir=DOWN_LEFT)", out("j", 211.0))
        assertEquals("j:ㅔ:Exit(dir=DOWN_LEFT)", out("j", 239.0))
        assertEquals("j:ㅓ:Exit(dir=DOWN)", out("j", 241.0))
        assertEquals("k:ㅑ:Exit(dir=RIGHT)", out("k", 29.0))
        assertEquals("k:ㅒ:Exit(dir=UP_RIGHT)", out("k", 31.0))
        assertEquals("k:ㅒ:Exit(dir=UP_RIGHT)", out("k", 59.0))
        assertEquals("k:ㅏ:Exit(dir=UP)", out("k", 61.0))
        assertEquals("k:ㅑ:Exit(dir=RIGHT)", out("k", -29.0))
        assertEquals("k:ㅐ:Exit(dir=DOWN_RIGHT)", out("k", -31.0))
        assertEquals("k:ㅐ:Exit(dir=DOWN_RIGHT)", out("k", -59.0))
        assertEquals("k:ㅏ:Exit(dir=DOWN)", out("k", -61.0))
        // ㅕ ㅑ stay 4-way (↓ ㅖ ㅒ, no diagonal): a diagonal is the edge crossed first (FSS#126 addition).
        assertEquals("u:ㅖ:Exit(dir=DOWN)", out("u", 270.0))
        assertEquals("i:ㅒ:Exit(dir=DOWN)", out("i", 270.0))
        sink.events.clear()
        stroke(l, "u", 20f, 40f)                                       // crossing the bottom edge first
        assertEquals(listOf("u:ㅖ:Exit(dir=DOWN)"), sink.events)
        assertEquals("i:[:Exit(dir=LEFT)", out("i", 180.0))
    }

    @Test
    fun `GV-02 a vowel key has no release-key rule - the stroke's direction decides wherever it is released (FSS 110, FR-TAP-02g ⑤)`() {
        val l = logic(TapLayouts.DUBEOLSIK)
        fun out(from: String, to: String): String {
            sink.events.clear()
            strokeTo(l, from, to)
            return sink.events.single()
        }
        assertEquals("h:ㅘ:Exit(dir=RIGHT)", out("h", "k"))    // ㅗ → released over ㅏ: the direction's ㅘ
        assertEquals("h:ㅘ:Exit(dir=RIGHT)", out("h", "j"))    // over ㅓ
        assertEquals("h:ㅗ:Exit(dir=LEFT)", out("h", "g"))     // ← over the consonant ㅎ: the empty way, ㅗ
        assertEquals("h:ㅛ:Exit(dir=UP)", out("h", "y"))       // ↑ over the ㅛ key
        assertEquals("n:ㅝ:Exit(dir=LEFT)", out("n", "b"))     // ㅜ ← over ㅠ
        sink.events.clear()
        stroke(l, "m", 0f, ROW)                               // ㅡ ↓ into the space bar: ㅢ, not a space
        assertEquals(listOf("m:ㅢ:Exit(dir=DOWN)"), sink.events)
        assertEquals("n:ㅟ:Exit(dir=UP)", out("n", "h"))       // ㅜ ↑ onto ㅗ: no (ㅜ, ㅗ) anything, the direction
    }
}
