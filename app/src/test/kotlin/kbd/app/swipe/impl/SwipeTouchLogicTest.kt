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
import kbd.app.swipe.SwipeModuleProvider
import kbd.gesture.classify.SwipeKeyKind
import kbd.gesture.input.SwipeInputMapper
import kbd.gesture.input.SwipeMapperSnapshot
import kbd.gesture.layout.SwipeKeyDef
import kbd.gesture.layout.SwipeLayout
import kbd.gesture.progress.ProgressState
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.stroke.geom.Edge
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * T30 self-tests of the swipe view's Android-free parts: the LLD-04 §9.3 touch flow
 * ([SwipeTouchLogic]), the key geometry (§8.1, §9.2), [SwipeMapperState] and the personal module
 * (LLD-03 §2.2). Issue #168: a tap types its consonant, a swipe the key's consonant + the vowel, every
 * consonant key (the double keys too) shows the guide. LLD-11 SW-05 (press-state guide, LLD-04 §9.3) is here; view-level SW-01..SW-04 come with T36.
 *
 * Geometry: width 357 dp, row 48 dp → 51 × 48 dp keys; ㄱ = (204, 96)–(255, 144) (6 rows, issue #168).
 */
class SwipeTouchLogicTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class FakeFeedback : FeedbackPort {
        var keys = 0
        var errors = 0
        override fun key() { keys++ }
        override fun error() { errors++ }
    }

    private class FakeHost(log: ErrorLog) : SwipeHost {
        val calls = ArrayList<String>()
        val events = ArrayList<List<JamoEvent>>()
        val texts = ArrayList<Pair<String, Boolean>>()
        var registered: SwipeEngineState? = null
        var restores = 0
        var failEvents = false
        override val thresholds: Thresholds = ThresholdDefaults.VALUES
        override val settings: Settings = Settings.DEFAULT
        override val fieldInfo = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = false, enterAction = null)
        /** As the real host (the composer's last consonant, SWIPE 9.5): the last tapped consonant, so C is judged against. */
        override var centerConsonant: Jamo? = null
        override fun onSwipeEvents(events: List<JamoEvent>, errorVibe: Boolean) {
            if (failEvents) error("composer failure")
            this.events += events
            events.filterIsInstance<JamoEvent.Consonant>().lastOrNull()?.let { centerConsonant = it.c }
        }
        override fun onSwipeText(text: String, errorVibe: Boolean) { texts += text to errorVibe }
        override fun onFunctionKey(key: FunctionKey) { calls += "down:$key" }
        override fun onFunctionKeyUp(key: FunctionKey) { calls += "up:$key" }
        override fun onSettingsStroke() { calls += "settings" }
        override val feedback = FakeFeedback()
        override val guard = EngineGuard(log, object : GuardedState {
            override fun snapshot(): Any = registered?.snapshot() ?: Unit
            override fun restore(s: Any) {
                restores++
                if (s != Unit) registered?.restore(s)
            }
        })
        override fun registerSwipeState(state: SwipeEngineState?) { registered = state }
    }

    private class FakeGuide : StrokeGuide {
        val calls = ArrayList<String>()
        /** All three callbacks in order, onProgress included (SW-05). */
        val order = ArrayList<String>()
        var progress = 0
        var lastCurrent: String? = null
        var downRect: RectDp? = null
        var downOrigin: StrokePoint? = null
        var firstFinger: StrokePoint? = null
        override fun onDown(key: SwipeKeyDef, keyRect: RectDp, origin: StrokePoint) {
            calls += "down:${key.id}"
            order += "down"
            downRect = keyRect
            downOrigin = origin
        }
        override fun onProgress(ps: ProgressState, finger: StrokePoint) {
            if (progress == 0) firstFinger = finger
            progress++
            lastCurrent = ps.current
            order += "progress"
        }
        override fun onUp() {
            calls += "up"
            order += "up"
        }
    }

    private class FakeRepeat : KeyRepeat {
        val calls = ArrayList<String>()
        override fun start(pointerId: Int, key: FunctionKey) { calls += "start:$pointerId:$key" }
        override fun stop(pointerId: Int) { calls += "stop:$pointerId" }
        override fun stopAll() { calls += "stopAll" }
    }

    private val host by lazy { FakeHost(ErrorLog(File(tmp.root, "errors.log"))) }
    private val mapper by lazy { SwipeInputMapper(host.thresholds) }
    private val guide = FakeGuide()
    private val repeat = FakeRepeat()
    private val geometry = SwipeGeometry.build(widthDp = 357f, rowHeightDp = 48f)
    private val logic by lazy {
        host.registerSwipeState(SwipeMapperState(mapper))
        SwipeTouchLogic(host, mapper, guide, repeat).also { it.geometry = geometry }
    }

    private fun centre(id: String): StrokePoint {
        val r = geometry.geoms[geometry.keys.indexOfFirst { it.id == id }].rect
        return StrokePoint((r.left + r.right) / 2, (r.top + r.bottom) / 2, 0L)
    }

    private fun at(p: StrokePoint, dx: Float, dy: Float, t: Long) = StrokePoint(p.x + dx, p.y + dy, t)

    /** Points every 2 dp from [from] along (dx, dy) over [steps] steps, one ms apart after [t0]. */
    private fun path(from: StrokePoint, dx: Float, dy: Float, steps: Int, t0: Long): List<StrokePoint> =
        (1..steps).map { i -> at(from, dx * i / steps, dy * i / steps, t0 + i) }

    private fun tap(id: String, t: Long, pointer: Int = 0) {
        val p = centre(id).copy(t = t)
        assertTrue(logic.down(pointer, p))
        logic.up(pointer, p.copy(t = t + 50))
    }

    // --- geometry (LLD-04 §8.1, §9.2; LLD-08 §7) ---

    @Test
    fun `geometry has every layout key with 51 x 48 dp cells`() {
        assertEquals(SwipeLayout.KEYS, geometry.keys)
        assertEquals(288f, geometry.heightDp)   // 6 rows (issue #168)
        assertEquals(RectDp(204f, 96f, 255f, 144f), geometry.geoms[geometry.keys.indexOfFirst { it.id == "ㄱ" }].rect)
        assertEquals(RectDp(204f, 48f, 255f, 96f), geometry.geoms[geometry.keys.indexOfFirst { it.id == "ㄲ" }].rect)   // right above ㄱ
        assertEquals(RectDp(102f, 240f, 255f, 288f), geometry.geoms[geometry.keys.indexOfFirst { it.id == "space" }].rect)
    }

    @Test
    fun `geometry outer edges are the view's left right and bottom, never top`() {
        fun outer(id: String) = geometry.geoms[geometry.keys.indexOfFirst { it.id == id }].outerEdges
        assertEquals(setOf(Edge.LEFT), outer("1"))
        assertEquals(setOf(Edge.RIGHT), outer("7"))
        assertEquals(setOf(Edge.RIGHT), outer("backspace"))
        assertEquals(setOf(Edge.LEFT, Edge.BOTTOM), outer("mode"))
        assertEquals(setOf(Edge.BOTTOM), outer("space"))
        // ㅋ: left neighbour is the cheonjiin key, not the window border (KH-06); the cheonjiin key's left is the window's
        assertEquals(emptySet<Edge>(), outer("ㅋ"))
        assertEquals(setOf(Edge.LEFT), outer("cheonjiin"))
        assertEquals(setOf(Edge.LEFT), outer("symbol3"))   // above the mode key, the ⚙ cell (issues #205, #208)
        assertEquals(setOf(Edge.LEFT), outer("8"))
        assertEquals(emptySet<Edge>(), outer("ㅃ"))
        for (k in geometry.keys.filter { it.kind == SwipeKeyKind.CONSONANT }) assertEquals(k.id, emptySet<Edge>(), outer(k.id))
    }

    @Test
    fun `geometry hit finds the key under the point and the empty cells`() {
        assertEquals("ㄱ", geometry.keys[geometry.indexAt(230f, 120f)].id)
        assertEquals("ㄲ", geometry.keys[geometry.indexAt(230f, 70f)].id)
        assertEquals("cheonjiin", geometry.keys[geometry.indexAt(10f, 160f)].id)   // row 3 (issue #173)
        assertEquals("symbol3", geometry.keys[geometry.indexAt(10f, 210f)].id)   // was ⚙ (issues #205, #208)
        assertEquals("8", geometry.keys[geometry.indexAt(10f, 70f)].id)
        assertEquals("symbol2", geometry.keys[geometry.indexAt(340f, 210f)].id)   // was an empty cell (issue #208)
        assertEquals("empty_r5c5", geometry.keys[geometry.indexAt(300f, 270f)].id)
        // below the view: nearest key
        assertEquals("space", geometry.keys[geometry.indexAt(200f, 300f)].id)
    }

    // --- one stroke (LLD-04 §9.3) ---

    @Test
    fun `SW-05 consonant tap gives Consonant, key feedback on press, guide down then press-state progress then up on every key`() {
        tap("ㄱ", 0)                                            // the guide shows on every consonant key (issue #168)
        assertEquals(listOf(listOf(JamoEvent.Consonant(Jamo.G))), host.events)
        assertEquals(1, host.feedback.keys)
        assertEquals(listOf("down:ㄱ", "up"), guide.calls)
        guide.calls.clear()
        guide.order.clear()
        guide.progress = 0
        tap("ㄱ", 1000)                                         // two taps: two consonants, whatever the speed
        assertEquals(listOf(listOf(JamoEvent.Consonant(Jamo.G)), listOf(JamoEvent.Consonant(Jamo.G))), host.events)
        assertEquals(listOf("down:ㄱ", "up"), guide.calls)
        assertEquals(listOf("down", "progress", "up"), guide.order)
        assertEquals(RectDp(204f, 96f, 255f, 144f), guide.downRect)
        assertEquals(centre("ㄱ").copy(t = 1000), guide.downOrigin)
        assertEquals(centre("ㄱ").copy(t = 1000), guide.firstFinger)
        assertEquals(1, guide.progress) // press state, for a guide shown after the delay without moving
        assertEquals("ㄱ", guide.lastCurrent)
        assertEquals(-1, logic.activePointerId)
    }

    @Test
    fun `vowel strokes from any consonant key write the vowel, with the stroke guide, and leave a trail`() {
        tap("ㄱ", 0)
        // Any consonant key (issue #173) right of C at C's height: C = consonantBox(357, 48) ≈ (137.7, 76)–(219.3, 172)
        // (centred, issue #213, FSS#37), so ㅣ needs x ≥ C.R − t/2 ≈ 209 and a start above b = 162 (SWIPE 3.6.3, 9.5) — the ㄲ
        // key (x 229.5, y 72).
        val c = centre("ㄲ").copy(t = 500)
        assertTrue(logic.down(0, c))
        val down = path(c, 0f, 120f, 20, 500)                    // ↓ long: ㅣ
        logic.move(down)
        assertEquals("ㅣ", guide.lastCurrent)
        assertEquals(1, logic.trail.paths.size)
        logic.up(0, down.last().copy(t = 530))
        assertEquals(listOf<JamoEvent>(JamoEvent.Vowel(Jamo.I)), host.events.last())
        assertEquals(1, logic.trail.paths.size)                  // kept after release (SWIPE 9.4)
        val mid = at(c, 0f, 60f, 900)                            // ㅏ's dot from the ㅣ's middle, on the key under it
        assertTrue(logic.down(0, mid))
        val dot = path(mid, 25f, 0f, 10, 900)
        logic.move(dot)
        logic.up(0, dot.last().copy(t = 930))
        assertEquals(listOf<JamoEvent>(JamoEvent.ReplaceVowel(Jamo.A)), host.events.last())
        assertEquals(2, logic.trail.paths.size)
        assertEquals(0, host.feedback.errors)
        tap("ㄴ", 1000)                                          // a consonant tap closes the vowel: the trail goes
        assertEquals(0, logic.trail.paths.size)
    }

    @Test
    fun `a reversed stroke is ignored with the error vibration and leaves no trail`() {
        tap("ㄱ", 0)
        val c = centre("ㄱ").copy(t = 500)
        logic.down(0, c)
        val upward = path(c, 0f, -60f, 20, 500)
        logic.move(upward)
        logic.up(0, upward.last().copy(t = 530))
        assertEquals(1, host.events.size)                         // only the tap
        assertEquals(1, host.feedback.errors)
        assertEquals(0, logic.trail.paths.size)
    }

    @Test
    fun `result comes from the first-pressed key even over another key`() {
        val c = centre("1").copy(t = 500)                         // a number key: its table, even ending over a consonant key
        logic.down(0, c)
        val pts = path(c, 0f, 100f, 25, 500)
        logic.move(pts)
        logic.up(0, pts.last().copy(t = 530))
        assertEquals("·", host.texts.last().first)
    }

    @Test
    fun `SW-08b backspace in an open vowel takes one stroke and its trail`() {
        tap("ㄱ", 0)
        val c = centre("ㅆ").copy(t = 500)                       // ㅣ right of C at C's height (above)
        logic.down(0, c)
        val down = path(c, 0f, 120f, 20, 500)
        logic.move(down)
        logic.up(0, down.last().copy(t = 530))
        assertEquals(1, logic.trail.paths.size)
        mapper.popElement()                                      // what the dispatcher's ⌫ does (the fake host only records the key)
        tap("backspace", 1000)
        assertEquals(0, logic.trail.paths.size)                  // synced to the assembly on the function key
        assertEquals(1, host.calls.count { it == "down:BACKSPACE" })
    }

    @Test
    fun `second pointer is ignored while a stroke is in progress`() {
        val c = centre("ㄱ")
        assertTrue(logic.down(0, c))
        assertFalse(logic.down(1, centre("ㅅ").copy(t = 10)))
        logic.up(1, centre("ㅅ").copy(t = 20)) // not the active pointer
        assertTrue(host.events.isEmpty())
        logic.up(0, c.copy(t = 40))
        assertEquals(listOf(listOf(JamoEvent.Consonant(Jamo.G))), host.events)
        assertEquals(1, host.feedback.keys)
        // after release the next press is taken again
        assertTrue(logic.down(1, centre("ㅅ").copy(t = 1000)))
    }

    @Test
    fun `empty cell takes the touch without feedback or input, wherever the finger goes`() {
        val e = centre("empty_r5c5")   // the mode row's empty cell (the double row has two more, issue #168)
        assertTrue(logic.down(0, e))
        assertEquals(0, logic.activePointerId)
        val pts = path(e, -100f, -60f, 30, 0) // over ㅍ and beyond
        logic.move(pts)
        logic.up(0, pts.last().copy(t = 40))
        assertEquals(0, host.feedback.keys)
        assertTrue(host.events.isEmpty())
        assertTrue(host.texts.isEmpty())
        assertTrue(host.calls.isEmpty())
        assertEquals(listOf("up"), guide.calls) // no guide down
    }

    // --- cheonjiin key (SWIPE 5.4, FR-SWP-60~65, issue #160) ---

    @Test
    fun `SW-10 cheonjiin key - feedback, guide, one element per stroke`() {
        val cj = centre("cheonjiin")
        // Nothing before: the guide shows (FR-SWP-65) and the vowel is a lone vowel (FR-SWP-63).
        assertTrue(logic.down(0, cj))
        assertEquals("down:cheonjiin", guide.calls.first())
        val up = path(cj, 0f, -40f, 20, 0)                 // ↑ out of the top edge
        logic.move(up)
        logic.up(0, up.last().copy(t = 30))
        assertEquals(listOf(listOf<JamoEvent>(JamoEvent.Vowel(Jamo.I))), host.events)
        assertEquals(1, host.feedback.keys)
        tap("cheonjiin", 100)                              // ㆍ: ㅣ → ㅏ
        assertEquals(listOf<JamoEvent>(JamoEvent.ReplaceVowel(Jamo.A)), host.events.last())
        tap("cheonjiin", 200)                              // ㆍ: ㅏ → ㅑ
        tap("cheonjiin", 300)                              // ㅑ + ㆍ cannot extend: ignored, error vibration
        assertEquals(3, host.events.size)
        assertEquals(1, host.feedback.errors)
    }

    @Test
    fun `SW-11 a pending dot shows no event, a vowel stroke then closes the assembly`() {
        tap("ㄱ", 0)
        tap("cheonjiin", 100)                              // ㆍ pending: no composer event
        assertEquals(emptyList<JamoEvent>(), host.events.last())
        assertEquals("ㆍ", mapper.pendingGlyph())
        val g = centre("ㄱ")
        val down = path(g, 0f, 30f, 15, 200)   // right of C: its middle is right-up = ㅣ (SWIPE 0.43)
        logic.down(0, g.copy(t = 200))
        logic.move(down)
        logic.up(0, down.last().copy(t = 230))
        assertEquals(listOf<JamoEvent>(JamoEvent.Vowel(Jamo.I)), host.events.last())   // the stroke closes the cheonjiin assembly
        assertEquals("", mapper.pendingGlyph())
    }

    @Test
    fun `backspace acts on press, starts repeat, stops it on release`() {
        val b = centre("backspace")
        logic.down(3, b)
        assertEquals(listOf("down:BACKSPACE"), host.calls)
        assertEquals(listOf("start:3:BACKSPACE"), repeat.calls)
        assertEquals(1, host.feedback.keys)
        logic.up(3, b.copy(t = 900))
        assertEquals(listOf("down:BACKSPACE", "up:BACKSPACE"), host.calls)
        assertEquals(listOf("start:3:BACKSPACE", "stop:3"), repeat.calls)
        assertTrue(guide.calls.none { it.startsWith("down") })
    }

    @Test
    fun `space and enter act on press without repeat, a mode key tap on release`() {
        tap("space", 0)
        tap("enter", 100)
        assertEquals(listOf("down:SPACE", "up:SPACE", "down:ENTER", "up:ENTER"), host.calls)
        host.calls.clear()
        val m = centre("mode")
        logic.down(0, m.copy(t = 200))
        assertTrue(host.calls.isEmpty())                       // nothing on press (issue #205)
        logic.up(0, m.copy(t = 250))
        assertEquals(listOf("down:MODE", "up:MODE"), host.calls)
        assertEquals(listOf("stop:0", "stop:0"), repeat.calls)
        assertEquals(3, host.feedback.keys)
    }

    /** Issue #205: an up exit on the mode key opens the settings and closes the open vowel assembly; no mode change. */
    @Test
    fun `MK-06 an up exit on the swipe mode key opens the settings`() {
        val m = centre("mode")
        logic.down(0, m)
        logic.move(path(m, 0f, -50f, 25, 0))
        logic.up(0, at(m, 0f, -50f, 30))
        assertEquals(listOf("settings"), host.calls)
        assertEquals(1, host.feedback.keys)
        assertTrue(guide.calls.none { it.startsWith("down") })
        assertEquals(-1, logic.activePointerId)
        // A right exit (onto the space key) is still the mode change; the result comes from the first-pressed key.
        host.calls.clear()
        logic.down(0, m.copy(t = 100))
        logic.move(path(m, 80f, 0f, 20, 100))
        logic.up(0, at(m, 80f, 0f, 130))
        assertEquals(listOf("down:MODE", "up:MODE"), host.calls)
    }

    /** Issue #205: an unreleased mode key gives nothing on cancel. */
    @Test
    fun `MK-07 cancel with the swipe mode key held gives nothing`() {
        val m = centre("mode")
        logic.down(0, m)
        logic.move(path(m, 0f, -50f, 25, 0))
        logic.cancel()
        assertTrue(host.calls.isEmpty())
        assertEquals(-1, logic.activePointerId)
    }

    @Test
    fun `cancel drops the unreleased stroke and frees the pointer`() {
        val c = centre("ㄱ")
        logic.down(0, c)
        logic.move(path(c, 40f, 0f, 20, 0))
        logic.cancel()
        assertTrue(host.events.isEmpty())
        assertEquals("up", guide.calls.last())
        assertEquals(-1, logic.activePointerId)
        logic.up(0, c.copy(t = 50)) // a late up after cancel does nothing
        assertTrue(host.events.isEmpty())
        tap("ㄱ", 1000)
        assertEquals(listOf(listOf(JamoEvent.Consonant(Jamo.G))), host.events)
    }

    @Test
    fun `cancel on backspace keeps the press input and only stops the repeat`() {
        logic.down(0, centre("backspace"))
        logic.cancel()
        assertEquals(listOf("down:BACKSPACE", "up:BACKSPACE"), host.calls)
        assertEquals(listOf("start:0:BACKSPACE", "stop:0"), repeat.calls)
        assertEquals(-1, logic.activePointerId)
    }

    @Test
    fun `number and symbol keys give text and close the cheonjiin assembly`() {
        tap("cheonjiin", 0)
        assertEquals("ㆍ", mapper.pendingGlyph())
        tap("5", 100)
        assertEquals("", mapper.pendingGlyph())
        val s = centre("symbol").copy(t = 200)
        logic.down(0, s)
        val pts = path(s, 40f, 0f, 20, 200) // → exit: ) (issue #208)
        logic.move(pts)
        logic.up(0, pts.last().copy(t = 230))
        assertEquals(listOf("5" to false, ")" to false), host.texts)
    }

    @Test
    fun `two fast taps on the same key are two consonants, a double key taps its double (issue 168)`() {
        tap("ㄱ", 0)   // up at 50
        tap("ㄱ", 100) // 50 ms later: still a plain ㄱ
        assertEquals(listOf(JamoEvent.Consonant(Jamo.G)), host.events.last())
        tap("ㄲ", 200)
        assertEquals(listOf(JamoEvent.Consonant(Jamo.GG)), host.events.last())
    }

    @Test
    fun `a failing release is rolled back and writes nothing`() {
        tap("cheonjiin", 0)
        val before = host.registered!!.snapshot()
        host.failEvents = true
        tap("cheonjiin", 100) // would be ‥
        assertEquals(1, host.events.size)
        assertEquals(1, host.restores)
        assertEquals(before, host.registered!!.snapshot())   // the cheonjiin assembly is back to ㆍ
        assertTrue(File(tmp.root, "errors.log").readText().contains(SwipeTouchLogic.SITE_RELEASE))
        assertEquals(-1, logic.activePointerId)
    }

    @Test
    fun `SW-07 a long press then lift is a plain tap (no hold, issue #142)`() {
        tap("ㄱ", 0)
        val p = centre("ㄱ").copy(t = 1000)
        assertTrue(logic.down(0, p))
        logic.move(listOf(p.copy(t = 1900)))
        assertEquals("ㄱ", guide.lastCurrent)
        logic.up(0, p.copy(t = 1950))
        assertEquals(List(2) { listOf(JamoEvent.Consonant(Jamo.G)) }, host.events)
    }

    @Test
    fun `touches before the first layout are ignored`() {
        val l = SwipeTouchLogic(host, mapper, guide, repeat)
        assertFalse(l.down(0, StrokePoint(10f, 10f, 0)))
        assertEquals(-1, l.activePointerId)
        assertEquals(0, host.feedback.keys)
    }

    // --- SwipeMapperState and the personal module (LLD-03 §2.2) ---

    @Test
    fun `SwipeMapperState delegates reset snapshot restore and the cheonjiin calls`() {
        val m = SwipeInputMapper(ThresholdDefaults.VALUES)
        val st = SwipeMapperState(m)
        assertEquals(SwipeMapperSnapshot(), st.snapshot())
        m.accept(kbd.gesture.classify.StrokeResult("cheonjiin", SwipeKeyKind.CHEONJIIN,
            kbd.gesture.classify.StrokeOutcome.Element(kbd.hangul.cheonjiin.CjElement.DOT), 0, null, 50))
        assertEquals("ㆍ", st.pendingGlyph())
        val one = st.snapshot()
        st.reset()
        assertEquals("", st.pendingGlyph())
        st.restore(one)
        assertEquals(one, m.snapshot())
        assertEquals(kbd.hangul.stroke.StrokePop.Popped(undoComposer = false), st.popElement())   // the pending dot
        assertEquals(kbd.hangul.stroke.StrokePop.Empty, st.popElement())
    }

    @Test
    fun `the provider gives the swipe engine module`() {
        assertSame(SwipeEngineModule, SwipeModuleProvider.get())
    }
}
