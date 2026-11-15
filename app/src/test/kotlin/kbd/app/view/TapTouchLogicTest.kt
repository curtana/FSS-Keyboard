package kbd.app.view

import java.io.File
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.TapOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The tap keyboard touch logic behind `TapKeyboardView` (LLD-05A §4, §7), on the JVM with fakes.
 * KH-03, MQ-05, MQ-06, MQ-08, FB-01, FB-02 here exercise [TapTouchLogic] and [TapGeometry]; the
 * same IDs against the Android view itself are T36's (`TapKeyboardViewTest`, gate G1).
 */
class TapTouchLogicTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class FakeSink : TapSink {
        val events = ArrayList<String>()
        var failOutput = false

        override fun onTapOutput(key: TapKeyDef, out: TapOutput) {
            if (failOutput) throw IllegalStateException("sink")
            events += "out:" + (out.text ?: out.jamo.toString())
        }

        override fun onFunctionDown(key: FunctionKey) {
            events += "down:$key"
        }

        override fun onFunctionUp(key: FunctionKey) {
            events += "up:$key"
        }

        override fun onSettingsStroke() {
            events += "settings"
        }
    }

    private class FakeFeedback : FeedbackPort {
        var keys = 0
        var errors = 0
        override fun key() {
            keys++
        }

        override fun error() {
            errors++
        }
    }

    /** Stands in for `RepeatTimer`; [fire] does what the view's repeat callback does. */
    private inner class FakeRepeat : TapKeyRepeat {
        val running = HashMap<Int, FunctionKey>()
        override fun start(pointerId: Int, key: FunctionKey) {
            running[pointerId] = key
        }

        override fun stop(pointerId: Int) {
            running.remove(pointerId)
        }

        override fun stopAll() {
            running.clear()
        }

        fun fire() = running.values.toList().forEach { k -> guard.stroke(TapTouchLogic.SITE_REPEAT) { sink.onFunctionDown(k) } }
    }

    private object NoState : GuardedState {
        override fun snapshot(): Any = Unit
        override fun restore(s: Any) {}
    }

    private val sink = FakeSink()
    private val feedback = FakeFeedback()
    private val guard by lazy { EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState) }
    private val repeater = FakeRepeat()
    private val logic by lazy {
        TapTouchLogic(sink, guard, feedback, repeater).also {
            it.geometry = TapGeometry.build(TapLayouts.QWERTY, WIDTH, ROW)
            it.minCrossDp = 14f
        }
    }

    /** Centre of [id] in the 360 × 48 dp QWERTY geometry. */
    private fun at(id: String, dy: Float = 0f, t: Long = 0L): StrokePoint {
        val r = logic.geometry!!.geoms.first { it.id == id }.rect
        return StrokePoint((r.left + r.right) / 2, (r.top + r.bottom) / 2 + dy, t)
    }

    private fun tap(pointer: Int, id: String) {
        logic.down(pointer, at(id))
        logic.up(pointer, at(id))
    }

    private fun idAt(x: Float, y: Float): String {
        val g = TapGeometry.build(TapLayouts.QWERTY, WIDTH, ROW)
        return g.keys[g.indexAt(x, y)].id
    }

    @Test
    fun `KH-03 no blanks since FSS#105 - where the indent blanks were are a, z and enter themselves`() {
        val row3 = 3 * ROW
        val row4 = 4 * ROW
        assertEquals("a", idAt(5f, row3 + 1))
        assertEquals("a", idAt(5f, row3 + ROW / 2))
        assertEquals("enter", idAt(355f, row3 + 1))
        assertEquals("z", idAt(5f, row4 + 1))
        assertEquals("q", idAt(5f, 2 * ROW + 1))
    }

    @Test
    fun `KH-03b key positions of the #198 layout as moved by FSS#105`() {
        val row5 = 5 * ROW
        // Bottom row: , . with `.` at the right end; space 216.
        assertEquals("period", idAt(359f, row5 + 1))
        assertEquals("period", idAt(325f, row5 + ROW / 2))
        assertEquals("comma", idAt(290f, row5 + ROW / 2))
        assertEquals("space", idAt(287f, row5 + ROW / 2))
        assertEquals("space", idAt(250f, row5 + ROW / 2))
        assertEquals("enter", idAt(355f, row5 - 1))
        // Row 4 after m is the `-` key, then `/` (FSS#105); row 0 holds Esc … Del ⌫.
        assertEquals("minus", idAt(260f, 4 * ROW + 1))
        assertEquals("slash", idAt(300f, 4 * ROW + 1))
        assertEquals("esc", idAt(1f, 1f))
        assertEquals("delete", idAt(300f, 1f))
        assertEquals("backspace", idAt(359f, 1f))
    }

    @Test
    fun `ID-37 Esc and Del presses reach the sink, Del repeats like backspace and Esc does not (#198)`() {
        tap(0, "esc")
        tap(0, "delete")
        assertTrue(sink.events.containsAll(listOf("down:ESC", "down:DELETE")))
        sink.events.clear()
        val keysBefore = feedback.keys
        logic.down(1, at("delete"))
        assertEquals(mapOf(1 to FunctionKey.DELETE), repeater.running)
        repeat(2) { repeater.fire() }
        assertEquals(listOf("down:DELETE", "down:DELETE", "down:DELETE"), sink.events)
        assertEquals(keysBefore + 1, feedback.keys)   // feedback once, on the press
        logic.up(1, at("delete"))
        assertTrue(repeater.running.isEmpty())
        logic.down(2, at("esc"))
        assertTrue(repeater.running.isEmpty())
        logic.up(2, at("esc"))
    }

    @Test
    fun `MQ-05 backspace pressed while a letter is held acts at once, the letter on its release`() {
        logic.down(1, at("q"))
        logic.down(2, at("backspace"))
        assertEquals(listOf("down:BACKSPACE"), sink.events)
        logic.up(1, at("q"))
        assertEquals(listOf("down:BACKSPACE", "out:q"), sink.events)
        logic.up(2, at("backspace"))
        assertEquals(listOf("down:BACKSPACE", "out:q", "up:BACKSPACE"), sink.events)
    }

    @Test
    fun `MQ-06 a letter tapped while backspace is held does not wait for it`() {
        logic.down(1, at("backspace"))
        tap(2, "w")
        assertEquals(listOf("down:BACKSPACE", "out:w"), sink.events)
    }

    @Test
    fun `MQ-08 cancel keeps backspace presses, stops the repeat and drops the held letter`() {
        logic.down(1, at("backspace"))
        repeater.fire()
        logic.down(2, at("e"))
        logic.cancel()
        assertEquals(listOf("down:BACKSPACE", "down:BACKSPACE", "up:BACKSPACE"), sink.events)
        assertTrue(repeater.running.isEmpty())
        logic.up(2, at("e"))   // a late up after the cancel gives nothing
        assertEquals(3, sink.events.size)
    }

    @Test
    fun `cancel lets out a released result that waited for a held key, in press order`() {
        logic.down(1, at("q"))
        tap(2, "w")
        assertEquals(emptyList<String>(), sink.events)
        logic.cancel()
        assertEquals(listOf("out:w"), sink.events)
    }

    @Test
    fun `FB-01 key feedback on press only, function keys act on press`() {
        logic.down(1, at("q"))
        assertEquals(1, feedback.keys)
        logic.up(1, at("q"))
        assertEquals(1, feedback.keys)
        logic.down(2, at("enter"))
        assertEquals(2, feedback.keys)
        assertEquals(listOf("out:q", "down:ENTER"), sink.events)
        logic.up(2, at("enter"))
        assertEquals(2, feedback.keys)
    }

    @Test
    fun `FB-02 no key feedback on repeats`() {
        logic.down(1, at("backspace"))
        repeat(3) { repeater.fire() }
        logic.up(1, at("backspace"))
        assertEquals(1, feedback.keys)
        assertEquals(4, sink.events.count { it == "down:BACKSPACE" })
    }

    @Test
    fun `a press at the left end of row 3 gives key feedback and types a (the blank there is gone, FSS#105)`() {
        logic.down(1, StrokePoint(5f, 3 * ROW + 1, 0))
        logic.up(1, StrokePoint(5f, 3 * ROW + 1, 10))
        assertEquals(1, feedback.keys)
        assertEquals(listOf("out:a"), sink.events)
    }

    @Test
    fun `exit up gives the capital, decided by the first-pressed key`() {
        logic.down(1, at("q", t = 0))
        logic.move(1, listOf(at("q", dy = -20f, t = 10), at("q", dy = -50f, t = 20)))
        logic.up(1, at("q", dy = -50f, t = 30))
        assertEquals(listOf("out:Q"), sink.events)
    }

    @Test
    fun `arrows and backspace repeat, other function keys do not`() {
        logic.down(1, at("left"))
        logic.down(2, at("tab"))
        logic.down(3, at("backspace"))
        assertEquals(mapOf(1 to FunctionKey.ARROW_LEFT, 3 to FunctionKey.BACKSPACE), repeater.running)
        logic.up(1, at("left"))
        assertEquals(mapOf(3 to FunctionKey.BACKSPACE), repeater.running)
    }

    @Test
    fun `a failing output is dropped and later outputs still come`() {
        sink.failOutput = true
        tap(1, "q")
        sink.failOutput = false
        tap(2, "w")
        assertEquals(listOf("out:w"), sink.events)
    }

    @Test
    fun `detach lets out released waiting results like cancel, held keys give nothing`() {
        // PM decision on PR #114: the mode key replacing the view must not lose a released letter.
        logic.down(1, at("q"))
        tap(2, "w")
        logic.down(3, at("backspace"))
        logic.detach()
        assertEquals(listOf("down:BACKSPACE", "up:BACKSPACE", "out:w"), sink.events)
        logic.up(1, at("q"))   // the held key's late up gives nothing
        assertEquals(3, sink.events.size)
        assertTrue(repeater.running.isEmpty())
        assertTrue(logic.pressed.isEmpty())
    }

    @Test
    fun `a new gesture ends a stale one first`() {
        logic.down(1, at("q"))
        tap(2, "w")
        logic.down(0, at("e"), first = true)   // the cancel of the last gesture never came
        logic.up(0, at("e"))
        assertEquals(listOf("out:w", "out:e"), sink.events)
    }

    @Test
    fun `accessibility click gives the tap result, function keys down then up`() {
        val g = logic.geometry!!
        logic.click(g.keys.indexOfFirst { it.id == "p" })
        logic.click(g.keys.indexOfFirst { it.id == "space" })
        assertEquals(listOf("out:p", "down:SPACE", "up:SPACE"), sink.events)
        assertEquals(0, feedback.keys)
    }

    @Test
    fun `dubeolsik keys give jamo`() {
        logic.geometry = TapGeometry.build(TapLayouts.DUBEOLSIK, WIDTH, ROW)
        tap(1, "q")
        assertEquals(listOf("out:ㅂ"), sink.events)
    }

    @Test
    fun `pressed tracks held keys`() {
        logic.down(1, at("q"))
        logic.down(2, at("enter"))
        val g = logic.geometry!!
        assertEquals(setOf(g.keys.indexOfFirst { it.id == "q" }, g.keys.indexOfFirst { it.id == "enter" }), logic.pressed.toSet())
    }

    /** Issue #205: the mode key acts on release; a tap is the mode change (down then up at once). */
    @Test
    fun `MK-01 a mode key tap changes the mode on release, nothing on press`() {
        logic.down(1, at("mode"))
        assertEquals(1, feedback.keys)   // key feedback still on press (FR-CMN-11a ②)
        assertTrue(sink.events.isEmpty())
        assertEquals(setOf(logic.geometry!!.keys.indexOfFirst { it.id == "mode" }), logic.pressed.toSet())
        logic.up(1, at("mode", t = 50))
        assertEquals(listOf("down:MODE", "up:MODE"), sink.events)
        assertTrue(logic.pressed.isEmpty())
        assertTrue(repeater.running.isEmpty())
    }

    /** Issue #205: an up exit on the mode key opens the settings, in QWERTY and dubeolsik alike; no mode change. */
    @Test
    fun `MK-02 an up exit on the mode key opens the settings`() {
        for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            sink.events.clear()
            logic.geometry = TapGeometry.build(layout, WIDTH, ROW)
            logic.down(1, at("mode", t = 0))
            logic.move(1, listOf(at("mode", dy = -20f, t = 10), at("mode", dy = -50f, t = 20)))
            logic.up(1, at("mode", dy = -50f, t = 30))
            assertEquals(layout.name, listOf("settings"), sink.events)
        }
        // Out and back in is still an up exit (it resolves like one, LLD-05A §2.2 row 8).
        sink.events.clear()
        logic.down(1, at("mode", t = 0))
        logic.move(1, listOf(at("mode", dy = -50f, t = 10)))
        logic.up(1, at("mode", t = 20))
        assertEquals(listOf("settings"), sink.events)
    }

    /** FR-CMN-13: other first edges keep the tap's mode change; only the first edge counts, so an up rub opens the settings. */
    @Test
    fun `MK-03 a right exit is the mode change, an up rub opens the settings`() {
        val r = logic.geometry!!.geoms.first { it.id == "mode" }.rect
        val y = (r.top + r.bottom) / 2
        logic.down(1, at("mode", t = 0))
        logic.move(1, listOf(StrokePoint(r.right + 20f, y, 10)))
        logic.up(1, StrokePoint(r.right + 20f, y, 20))
        assertEquals(listOf("down:MODE", "up:MODE"), sink.events)
        sink.events.clear()
        // Up, back in, up again, back in: a rub; its first edge is the top one.
        logic.down(1, at("mode", t = 0))
        logic.move(1, listOf(at("mode", dy = -50f, t = 10), at("mode", t = 20), at("mode", dy = -50f, t = 30)))
        logic.up(1, at("mode", t = 40))
        assertEquals(listOf("settings"), sink.events)
        sink.events.clear()
        // Right first, then up (onto the row above): the first edge is the right one = the mode change.
        logic.down(1, at("mode", t = 0))
        logic.move(1, listOf(StrokePoint(r.right + 20f, y, 10), StrokePoint(r.right + 20f, y - 60f, 20)))
        logic.up(1, StrokePoint(r.right + 20f, y - 60f, 30))
        assertEquals(listOf("down:MODE", "up:MODE"), sink.events)
    }

    /** Issue #205: an unreleased mode key gives nothing on cancel; a held letter is dropped as before. */
    @Test
    fun `MK-04 cancel with the mode key held gives nothing`() {
        logic.down(1, at("mode"))
        logic.move(1, listOf(at("mode", dy = -50f, t = 10)))
        logic.cancel()
        assertTrue(sink.events.isEmpty())
        assertTrue(logic.pressed.isEmpty())
        logic.up(1, at("mode", dy = -50f, t = 20))   // the late up gives nothing
        assertTrue(sink.events.isEmpty())
    }

    /** Issue #205: the mode key works outside the letter queue: a held letter does not wait for it, nor it for the letter. */
    @Test
    fun `MK-05 the mode key leaves outside the queue`() {
        logic.down(1, at("q"))
        logic.down(2, at("mode"))
        logic.up(2, at("mode", t = 10))
        assertEquals(listOf("down:MODE", "up:MODE"), sink.events)
        logic.up(1, at("q", t = 20))
        assertEquals(listOf("down:MODE", "up:MODE", "out:q"), sink.events)
    }

    private companion object {
        const val WIDTH = 360f
        const val ROW = 48f
    }
}
