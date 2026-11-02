package kbd.app.swipe.impl

import java.io.File
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.ime.FieldClass
import kbd.app.ime.FieldInfo
import kbd.app.store.ErrorLog
import kbd.app.store.Settings
import kbd.app.swipe.SwipeEngineState
import kbd.app.swipe.SwipeHost
import kbd.gesture.input.SwipeInputMapper
import kbd.gesture.layout.NumberKeyTable
import kbd.gesture.layout.SwipeKeyDef
import kbd.gesture.progress.ProgressState
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.stroke.geom.Dir
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Issue #237 (owner, tablet 711 dp): the number keys' ↑ (! @ # …). The view's top edge is never an outer edge
 * (SRS 3.3, FR-SWP-45, `KeyGeom.build`), so a stroke from a top-row number key that goes above the view (y < 0) and
 * one from 8 / 9 / 0 that goes up into the next key both cross an edge and give the ↑ symbol. 711 and 360 dp.
 */
class SwipeNumberUpTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class Host(log: ErrorLog) : SwipeHost {
        val texts = ArrayList<String>()
        var registered: SwipeEngineState? = null
        override val thresholds: Thresholds = ThresholdDefaults.VALUES
        override val settings: Settings = Settings.DEFAULT
        override val fieldInfo = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = false, enterAction = null)
        override fun onSwipeEvents(events: List<JamoEvent>, errorVibe: Boolean) {}
        override fun onSwipeText(text: String, errorVibe: Boolean) { texts += text }
        val calls = ArrayList<String>()
        override fun onFunctionKey(key: FunctionKey) { calls += "fn:$key" }
        override fun onSettingsStroke() { calls += "settings" }
        override fun onFunctionKeyUp(key: FunctionKey) {}
        override val feedback = object : FeedbackPort {
            override fun key() {}
            override fun error() {}
        }
        override val guard = EngineGuard(log, object : GuardedState {
            override fun snapshot(): Any = registered?.snapshot() ?: Unit
            override fun restore(s: Any) { if (s != Unit) registered?.restore(s) }
        })
        override fun registerSwipeState(state: SwipeEngineState?) { registered = state }
    }

    private object NoGuide : StrokeGuide {
        override fun onDown(key: SwipeKeyDef, keyRect: RectDp, origin: StrokePoint) {}
        override fun onProgress(ps: ProgressState, finger: StrokePoint) {}
        override fun onUp() {}
    }

    private object NoRepeat : KeyRepeat {
        override fun start(pointerId: Int, key: FunctionKey) {}
        override fun stop(pointerId: Int) {}
        override fun stopAll() {}
    }

    private inner class Board(widthDp: Float) {
        val host = Host(ErrorLog(File(tmp.newFolder(), "errors.log")))
        val mapper = SwipeInputMapper(host.thresholds)
        val geo = SwipeGeometry.build(widthDp, 48f)
        val lines = ArrayList<String>()
        val logic = SwipeTouchLogic(host, mapper, NoGuide, NoRepeat, strokeLog = { lines += it }).also {
            host.registerSwipeState(SwipeMapperState(mapper))
            it.geometry = geo
        }
        private var t = 0L

        /** From the centre of [id] by (dx, dy) in 10 moves; the text of its release. */
        fun swipe(id: String, dx: Float, dy: Float): String? {
            val r = geo.geoms[geo.keys.indexOfFirst { it.id == id }].rect
            val x = (r.left + r.right) / 2
            val y = (r.top + r.bottom) / 2
            val before = host.texts.size
            logic.down(0, StrokePoint(x, y, t))
            logic.move((1..9).map { i -> StrokePoint(x + dx * i / 10, y + dy * i / 10, t + i * 8) })
            logic.up(0, StrokePoint(x + dx, y + dy, t + 100))
            t += 1000
            return host.texts.drop(before).singleOrNull()
        }
    }

    private val widths = listOf(711f, 360f)

    @Test
    fun `top-row number keys - an up stroke past the view's top gives the up symbol at both widths`() {
        for (w in widths) {
            val b = Board(w)
            for (d in "1234567") {
                val up = NumberKeyTable[d][Dir.UP]
                assertEquals("w=$w $d", up, b.swipe(d.toString(), 0f, -40f))   // 16 dp above the view top
                assertEquals("w=$w $d short", up, b.swipe(d.toString(), 0f, -33f))   // 9 dp above: past minCrossDp 8
            }
        }
    }

    @Test
    fun `8 9 0 - an up stroke into the key above gives the up symbol`() {
        for (w in widths) {
            val b = Board(w)
            for (d in "890") assertEquals("w=$w $d", NumberKeyTable[d][Dir.UP], b.swipe(d.toString(), 0f, -40f))
        }
    }

    /** [points] as given (absolute dp) on a fresh board; the text of its release. */
    private fun Board.path(points: List<Pair<Float, Float>>): String? {
        val before = host.texts.size
        val (x0, y0) = points.first()
        logic.down(0, StrokePoint(x0, y0, 0L))
        logic.move(points.drop(1).dropLast(1).mapIndexed { i, (x, y) -> StrokePoint(x, y, 8L * (i + 1)) })
        val (x1, y1) = points.last()
        logic.up(0, StrokePoint(x1, y1, 200L))
        return host.texts.drop(before).singleOrNull()
    }

    @Test
    fun `the owner's tablet strokes - the platform pins points above the window to y 0, still the up symbol (issue 237)`() {
        val b = Board(711f)
        // FSSStroke 2026-11-02: key=1 (68,39.6)→(68,0.0), bbox y 0.0–39.6, gave "1".
        assertEquals("!", b.path(listOf(68f to 39.6f, 68f to 25f, 68f to 10f, 68f to 0f, 68f to 0f)))
        for ((d, x) in listOf('2' to 150f, '3' to 255f, '5' to 455f)) {
            assertEquals("$d", NumberKeyTable[d][Dir.UP], b.path(listOf(x to 35f, x to 18f, x to 4f, x to 0f)))
        }
        // Up to the border and back down into the key: exit and return equals the exit (DCR-02).
        assertEquals("!", b.path(listOf(68f to 30f, 68f to 0f, 68f to 0f, 68f to 20f, 68f to 30f)))
        // A tap that stays above the border is the digit.
        assertEquals("1", b.path(listOf(68f to 20f, 68f to 6f, 68f to 6f)))
        val s = Board(360f)
        assertEquals("!", s.path(listOf(25f to 30f, 25f to 0f)))
    }

    @Test
    fun `number key strokes write an FSSStroke line with the exits`() {
        val b = Board(711f)
        b.swipe("1", 0f, -40f)
        val l = b.lines.last()
        assertTrue(l, l.startsWith("stroke key=1 view=711.0x288.0"))
        assertTrue(l, l.contains("shape=text \"!\""))
        assertTrue(l, l.contains("keyRect=(0.0,0.0,101.6,48.0) outer=[LEFT] crossings=[TOP OUT] e1=TOP"))
        assertTrue(l, l.contains("result=text \"!\""))
    }

    @Test
    fun `the left, right and bottom window borders are exits too - unassigned directions equal the tap (issue 237)`() {
        val b = Board(711f)
        // 1 ← to the left border (x 0): LEFT OUT recorded, nothing assigned = the digit.
        assertEquals("1", b.path(listOf(50f to 24f, 20f to 24f, 0f to 24f)))
        assertTrue(b.lines.last(), b.lines.last().contains("crossings=[LEFT OUT] e1=LEFT"))
        // 7 → to the right border: RIGHT OUT, the digit.
        assertEquals("7", b.path(listOf(660f to 24f, 690f to 24f, 711f to 24f)))
        assertTrue(b.lines.last(), b.lines.last().contains("crossings=[RIGHT OUT] e1=RIGHT"))
        // symbol2 → to the right border: RIGHT OUT, its tap ",".
        assertEquals(",", b.path(listOf(660f to 216f, 690f to 216f, 711f to 216f)))
        assertTrue(b.lines.last(), b.lines.last().contains("e1=RIGHT"))
        // 1 ← one dp short of the border: no crossing, the digit as before.
        assertEquals("1", b.path(listOf(50f to 24f, 20f to 24f, 1f to 24f)))
        assertTrue(b.lines.last(), b.lines.last().contains("crossings=[] e1=-"))
        // The mode key ↓ to the bottom border: an exit down = the mode change, as a tap (only ↑ opens the settings).
        b.logic.down(0, StrokePoint(100f, 264f, 0L))
        b.logic.move(listOf(StrokePoint(100f, 280f, 10L), StrokePoint(100f, 288f, 20L)))
        b.logic.up(0, StrokePoint(100f, 288f, 30L))
        assertEquals(listOf("fn:MODE"), b.host.calls)
    }

    @Test
    fun `1 up from near the left border brushes it - still the up symbol, a tap on the border stays the digit (SRS 0_60 FR-SWP-45)`() {
        val b = Board(711f)
        assertEquals("!", b.path(listOf(6f to 40f, 3f to 20f, 0f to 0f)))
        assertTrue(b.lines.last(), b.lines.last().contains("crossings=[TOP OUT] e1=TOP"))
        assertEquals("1", b.path(listOf(3f to 30f, 0f to 30f, 0f to 31f)))      // pressed 3 dp from the left border
    }
}
