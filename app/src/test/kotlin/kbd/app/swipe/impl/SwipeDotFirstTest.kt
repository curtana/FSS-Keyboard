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
import kbd.gesture.layout.SwipeKeyDef
import kbd.gesture.progress.ProgressState
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.hangul.stroke.ConsonantBox
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.FunctionKey
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Issue #217 (owner, tablet 711 dp wide): ㄱ tap, then ㅗ / ㅛ with the dot(s) first. The whole touch flow at
 * 711 dp and 360 dp with the default 48 dp rows and t = 20 dp, natural places relative to the place C
 * (SWIPE 3.6.3, 9.5). C is 81.6 × 96 dp at both widths (two rows high, × 0.85 wide); only its left moves
 * (centred across the keyboard since #213), and t is the fixed 20 dp (SRS 1.3), so b = C.B − t/2 = 138 dp at both.
 *
 * Findings pinned here (the touch flow follows SWIPE 0.43 3.6.3 and the next-candidate table):
 * - a dot under C is ㅗ / ㅛ at both widths, centre or left;
 * - a dot that starts inside C's lower part is a dot since issue #217 (owner: strokes may overlap C; the role goes
 *   by the stroke's middle point, under-left of the 45° line through C's bottom-right corner); SWIPE 0.41 ignored
 *   it (T < b) — the device's 노;
 * - a ㅡ that starts left of C on the cheonjiin key (column 0) is a cheonjiin element (FR-SWP-62): it closes
 *   the vowel assembly, drops the dot (its trail goes while the ㅡ is drawn) and types ㅡ (그) — layout rule.
 *   With C centred (#213) column 0 ends 213 dp left of C at 711 dp and 88 dp at 360 dp (before: 25 and 13 dp),
 *   so a ㅡ under C hardly starts there any more; the rule itself is unchanged.
 * The `FSSStroke` debug line (personal debug builds) is checked at the end.
 */
class SwipeDotFirstTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class Host(log: ErrorLog, password: Boolean = false, th: Thresholds = ThresholdDefaults.VALUES) : SwipeHost {
        val events = ArrayList<List<JamoEvent>>()
        var errors = 0
        private var registered: SwipeEngineState? = null
        override val thresholds: Thresholds = th
        override val settings: Settings = Settings.DEFAULT
        override val fieldInfo = FieldInfo(FieldClass.TEXT, isPassword = password, noLearning = false, multiLine = false, enterAction = null)
        override var centerConsonant: Jamo? = null
        override fun onSwipeEvents(events: List<JamoEvent>, errorVibe: Boolean) {
            this.events += events
            if (errorVibe) errors++
            events.filterIsInstance<JamoEvent.Consonant>().lastOrNull()?.let { centerConsonant = it.c }
        }
        override fun onSwipeText(text: String, errorVibe: Boolean) {}
        override fun onFunctionKey(key: FunctionKey) {}
        override fun onFunctionKeyUp(key: FunctionKey) {}
        override val feedback = object : FeedbackPort {
            override fun key() {}
            override fun error() { errors++ }
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

    /** One keyboard [widthDp] wide after a ㄱ tap; [stroke] draws a straight line in 10 steps. */
    private inner class Board(widthDp: Float, password: Boolean = false, th: Thresholds = ThresholdDefaults.VALUES) {
        val host = Host(ErrorLog(File(tmp.newFolder(), "errors.log")), password, th)
        val mapper = SwipeInputMapper(host.thresholds)
        val geo = SwipeGeometry.build(widthDp, 48f)
        val lines = ArrayList<String>()
        val logic = SwipeTouchLogic(host, mapper, NoGuide, NoRepeat, strokeLog = { lines += it }).also { it.geometry = geo }
        val c: ConsonantBox = geo.consonantBox
        val cx = (c.left + c.right) / 2
        private var t = 0L

        init {
            host.registerSwipeState(SwipeMapperState(mapper))
            val g = geo.geoms[geo.keys.indexOfFirst { it.id == "ㄱ" }].rect
            val p = StrokePoint((g.left + g.right) / 2, (g.top + g.bottom) / 2, 0L)
            logic.down(0, p)
            logic.up(0, p.copy(t = 50))
            t = 100
        }

        fun keyAt(x: Float, y: Float): String = geo.keys[geo.indexAt(x, y)].id

        /** Draws (x0, y0) → (x1, y1); returns the composer events of its release. */
        fun stroke(x0: Float, y0: Float, x1: Float, y1: Float): List<JamoEvent> {
            val before = host.events.size
            logic.down(0, StrokePoint(x0, y0, t))
            val pts = (1..10).map { i -> StrokePoint(x0 + (x1 - x0) * i / 10, y0 + (y1 - y0) * i / 10, t + i) }
            logic.move(pts.dropLast(1))
            logic.up(0, pts.last())
            t += 100
            return host.events.drop(before).flatten()
        }
    }

    private val widths = listOf(711f, 360f)

    @Test
    fun `C and b are the same size at 711 and 360 dp, only C's left moves`() {
        val wide = SwipeGeometry.build(711f, 48f).consonantBox
        val phone = SwipeGeometry.build(360f, 48f).consonantBox
        assertEquals(phone.right - phone.left, wide.right - wide.left, 0.01f)
        assertEquals(phone.top, wide.top, 0f)
        assertEquals(phone.bottom, wide.bottom, 0f)
        assertEquals(314.7f, wide.left, 0.1f)   // 711 / 2 − 81.6 / 2 (#213)
        assertEquals(139.2f, phone.left, 0.1f)  // 360 / 2 − 81.6 / 2
    }

    @Test
    fun `a dot under C then a line under it is ㅗ, centre or left, at both widths`() {
        for (w in widths) {
            for (dx in listOf(0f, -25f)) {
                val b = Board(w)
                val x = b.cx + dx
                assertEquals("w=$w dx=$dx", emptyList<JamoEvent>(), b.stroke(x, b.c.bottom + 2, x, b.c.bottom + 22))
                assertEquals("ㆍ", b.mapper.pendingGlyph())
                assertEquals(listOf<JamoEvent>(JamoEvent.Vowel(Jamo.O)), b.stroke(b.c.left - 10, b.c.bottom + 32, b.c.right, b.c.bottom + 32))
                assertEquals(2, b.logic.trail.paths.size)
                assertEquals(0, b.host.errors)
            }
        }
    }

    @Test
    fun `two dots under C then a line under them is ㅛ at both widths`() {
        for (w in widths) {
            val b = Board(w)
            b.stroke(b.cx - 15, b.c.bottom + 2, b.cx - 15, b.c.bottom + 20)
            b.stroke(b.cx + 15, b.c.bottom + 2, b.cx + 15, b.c.bottom + 20)
            assertEquals("‥", b.mapper.pendingGlyph())
            assertEquals("w=$w", listOf<JamoEvent>(JamoEvent.Vowel(Jamo.YO)), b.stroke(b.c.left, b.c.bottom + 30, b.c.right, b.c.bottom + 30))
            assertEquals(0, b.host.errors)
        }
    }

    @Test
    fun `a dot under C's middle starting inside C (its middle point under-left) is a dot`() {
        for (w in widths) {
            val b = Board(w)
            val base = b.c.bottom - b.host.thresholds.posTolDp / 2
            b.stroke(b.cx, base, b.cx, base + 20)   // P (cx, C.B), d = C.R − cx > 0
            assertEquals("w=$w", "ㆍ", b.mapper.pendingGlyph())
        }
    }

    @Test
    fun `a dot that starts inside C's lower part is a dot by its middle (issue 217) - ㅗ`() {
        for (w in widths) {
            val b = Board(w)
            assertEquals(emptyList<JamoEvent>(), b.stroke(b.cx, b.c.bottom - 15, b.cx, b.c.bottom + 10))   // T 133 < b, M 145.5 ≥ b
            assertEquals("w=$w", 0, b.host.errors)
            assertEquals("ㆍ", b.mapper.pendingGlyph())
            assertEquals(1, b.logic.trail.paths.size)
            assertEquals(listOf<JamoEvent>(JamoEvent.Vowel(Jamo.O)), b.stroke(b.c.left, b.c.bottom + 25, b.c.right, b.c.bottom + 25))
            assertEquals(2, b.logic.trail.paths.size)
        }
    }

    @Test
    fun `ㅛ with both dots starting inside C's lower part is ㅛ`() {
        for (w in widths) {
            val b = Board(w)
            b.stroke(b.cx - 15, b.c.bottom - 12, b.cx - 15, b.c.bottom + 8)
            b.stroke(b.cx + 15, b.c.bottom - 12, b.cx + 15, b.c.bottom + 8)
            assertEquals("w=$w", 0, b.host.errors)
            assertEquals(listOf<JamoEvent>(JamoEvent.Vowel(Jamo.YO)), b.stroke(b.c.left, b.c.bottom + 20, b.c.right, b.c.bottom + 20))
        }
    }

    /** A tap on the key labelled [id]. */
    private fun Board.tap(id: String) {
        val g = geo.geoms[geo.keys.indexOfFirst { it.id == id }].rect
        stroke((g.left + g.right) / 2, (g.top + g.bottom) / 2, (g.left + g.right) / 2, (g.top + g.bottom) / 2)
    }

    @Test
    fun `the owner's tablet strokes (FSSStroke 2026-11-02, 711 dp) - 노, 뚜, two long ㅣ (SWIPE 0_43 two zones)`() {
        // The records were made with C 24 dp higher (before FSS#37); each stroke is moved down with C (SWIPE 3.6.5: only the difference to C counts).
        val b = Board(711f)
        b.tap("ㄴ")
        assertEquals(listOf<JamoEvent>(JamoEvent.Consonant(Jamo.N)), b.host.events.last())
        assertEquals(emptyList<JamoEvent>(), b.stroke(362f, 157f, 358f, 192f))              // P (360, 174.5), d 38.8: under-left = a dot
        assertEquals("ㆍ", b.mapper.pendingGlyph())
        assertEquals(listOf<JamoEvent>(JamoEvent.Vowel(Jamo.O)), b.stroke(320f, 209f, 400f, 209f))   // 노
        assertEquals(0, b.host.errors)
        val dd = Board(711f)
        dd.tap("ㄸ")
        assertEquals(listOf<JamoEvent>(JamoEvent.Vowel(Jamo.EU)), dd.stroke(304.9f, 163.1f, 401.3f, 158.7f))   // P (353.1, 160.9), d 32.1: ㅡ
        assertTrue(dd.lines.last(), dd.lines.last().contains("role=first horizontal under-left: shows ㅡ, both roles kept (P (353.1,160.9), d 32.1;"))
        assertEquals(listOf<JamoEvent>(JamoEvent.ReplaceVowel(Jamo.U)), dd.stroke(355f, 174f, 355f, 199f))   // 뚜
        val g = Board(711f)                                                                   // after its ㄱ tap
        assertEquals(listOf<JamoEvent>(JamoEvent.Vowel(Jamo.I)), g.stroke(409f, 82f, 407f, 170f))     // P (408, 126), d −57.7: ㅣ
        val long = Board(711f)
        assertEquals(listOf<JamoEvent>(JamoEvent.Vowel(Jamo.I)), long.stroke(445f, 82f, 443f, 194f))  // P (444, 138), d −81.7: ㅣ
        val longer = Board(711f)
        assertEquals(listOf<JamoEvent>(JamoEvent.Vowel(Jamo.I)), longer.stroke(409f, 81f, 409f, 244f)) // P (409, 162.5), d −22.2: ㅣ
        assertEquals(0, dd.host.errors + g.host.errors + long.host.errors + longer.host.errors)
    }

    @Test
    fun `a line that starts left of C on the cheonjiin key closes the assembly and drops the dot (layout rule FR-SWP-62)`() {
        for (w in widths) {
            val b = Board(w)
            b.stroke(b.cx, b.c.bottom + 2, b.cx, b.c.bottom + 22)
            assertEquals(1, b.logic.trail.paths.size)
            val colW = w / 7
            val x0 = colW - 5                                  // 5 dp inside column 0
            assertEquals(if (w == 711f) 213.1f else 87.8f, b.c.left - colW, 0.1f)   // column 0 ends this far left of C (#213)
            assertEquals("cheonjiin", b.keyAt(x0, b.c.bottom + 10))   // the cheonjiin row (C.B + 32 is the symbol3 row since FSS#37)
            assertEquals("w=$w", listOf<JamoEvent>(JamoEvent.Vowel(Jamo.EU)), b.stroke(x0, b.c.bottom + 10, b.c.right, b.c.bottom + 10))
            assertEquals(1, b.host.errors)                     // the dropped dot (SWIPE 3.3 "조립 끝")
            assertEquals(0, b.mapper.strokes.size)
            assertEquals(0, b.logic.trail.paths.size)          // the dot's trail went with the ㅡ
        }
    }

    @Test
    fun `FSSStroke lines - the consonant tap, the role by C with its numbers and the assembly`() {
        val b = Board(711f)
        assertTrue(b.lines[0], b.lines[0].startsWith("tap key=ㄱ view=711.0x288.0"))
        assertTrue(b.lines[0], b.lines[0].contains("C=(314.7,76.0,396.3,172.0)"))
        b.stroke(b.cx - 20, 134f, b.cx - 20, 174f)                          // over C: its middle is under-left = a dot (SWIPE 0.43)
        val first = b.lines[1]
        assertTrue(first, first.startsWith("stroke key=ㄷ view=711.0x288.0 from=(335.5,134.0) to=(335.5,174.0) bbox=(335.5,134.0)-(335.5,174.0)"))
        assertTrue(first, first.contains("C=(314.7,76.0,396.3,172.0) t=20.0 center=ㄱ shape=vertical"))
        assertTrue(first, first.contains("role=first vertical under-left: shows a dot, both roles kept (P (335.5,154.0), d 42.8; C.R 396.3, C.B 172.0) result=jamo[]"))
        assertTrue(first, first.endsWith("assembly=V1[ㅣㅏㅑㅐㅒㅗㅛㅚㅘㅙ] vowel=- pending=ㆍ"))
        b.stroke(320f, 174f, 320f, 194f)                                     // left of the first: not beside it
        assertTrue(b.lines[2], b.lines[2].contains("role=vertical under-left = dot (P (320.0,184.0), d 88.3; C.R 396.3, C.B 172.0) result=Ignored ignored=no vertical zone of the next candidates fits"))
        b.stroke(20f, b.c.bottom + 10, b.c.right, b.c.bottom + 10)            // from the cheonjiin key
        assertTrue(b.lines[3], b.lines[3].contains("key=cheonjiin"))
        assertTrue(b.lines[3], b.lines[3].contains("closed: dropped 1 open stroke(s) assembly=empty"))
    }

    @Test
    fun `FSSStroke lines in a password field mask every typed character, places and roles stay (FR-CMN-08)`() {
        val b = Board(711f, password = true)
        b.stroke(b.cx, b.c.bottom + 2, b.cx, b.c.bottom + 22)
        b.stroke(b.c.left, b.c.bottom + 32, b.c.right, b.c.bottom + 32)   // ㅗ
        val g = b.geo.geoms[b.geo.keys.indexOfFirst { it.id == "1" }].rect
        b.stroke(g.left + 20, g.top + 20, g.left + 21, g.top + 21)          // number key tap: text "1"
        for (l in b.lines) {
            assertTrue(l, l.contains("key=* "))
            for (secret in listOf("ㄱ", "ㄷ", "ㅇ", "\"1\"", "Consonant", "Vowel", "vowel=", "pending=")) assertFalse("$secret in $l", l.contains(secret))
        }
        assertTrue(b.lines[0], b.lines[0].contains("result=jamo[1 × *]"))
        assertTrue(b.lines[1], b.lines[1].contains("center=* shape=vertical len=20.0 minCross=8.0 role=first vertical under-left: shows a dot"))
        assertTrue(b.lines[1], b.lines[1].endsWith("assembly=V1[ㅣㅏㅑㅐㅒㅗㅛㅚㅘㅙ]"))  // candidates are the state, not input
        assertTrue(b.lines[3], b.lines[3].contains("result=text \"*\""))
    }

    // --- short dots (issue #217 owner: "길이와 상관없이 자음 아래 점이 두 개여도 인식해야 한다", "점(탭)보다는 길어야겠지만") ---
    // The only length threshold on a consonant key is minCrossDp (tap / stroke, classifyConsonant ①); the assembler has none.

    /** ㅗ ㅛ ㅜ ㅠ ㅘ ㅝ with every dot [len] dp long, written under / beside C as by hand; the vowel each makes. */
    private fun shortDotVowels(w: Float, th: Thresholds, len: Float): List<Jamo?> {
        fun vowel(write: Board.() -> Unit): Jamo? {
            val b = Board(w, th = th)
            b.write()
            if (b.host.errors != 0) return null   // a stroke was ignored or a tap closed the assembly
            return when (val e = b.host.events.flatten().lastOrNull()) {
                is JamoEvent.Vowel -> e.v
                is JamoEvent.ReplaceVowel -> e.v
                else -> null
            }
        }
        val o = vowel { stroke(cx, c.bottom + 2, cx, c.bottom + 2 + len); stroke(c.left - 10, c.bottom + 14 + len, c.right, c.bottom + 14 + len) }
        val yo = vowel {
            stroke(cx - 15, c.bottom + 2, cx - 15, c.bottom + 2 + len); stroke(cx + 15, c.bottom + 2, cx + 15, c.bottom + 2 + len)
            stroke(c.left - 10, c.bottom + 12 + len, c.right, c.bottom + 12 + len)
        }
        val u = vowel { stroke(c.left, c.bottom + 15, c.right, c.bottom + 15); stroke(cx, c.bottom + 20, cx, c.bottom + 20 + len) }
        val yu = vowel {
            stroke(c.left, c.bottom + 15, c.right, c.bottom + 15); stroke(cx - 10, c.bottom + 20, cx - 10, c.bottom + 20 + len)
            stroke(cx + 15, c.bottom + 20, cx + 15, c.bottom + 20 + len)
        }
        val wa = vowel {
            stroke(cx, c.bottom + 2, cx, c.bottom + 2 + len); stroke(c.left - 10, c.bottom + 14 + len, c.right, c.bottom + 14 + len)
            stroke(c.right + 15, c.top + 10, c.right + 15, c.bottom + 40); stroke(c.right + 18, 100f, c.right + 18 + len, 100f)
        }
        val wo = vowel {
            stroke(c.left, c.bottom + 15, c.right, c.bottom + 15); stroke(cx, c.bottom + 20, cx, c.bottom + 20 + len)
            stroke(cx + 5, c.bottom + 30, cx + 5 + len, c.bottom + 30); stroke(c.right + 15, c.top + 10, c.right + 15, c.bottom + 50)
        }
        return listOf(o, yo, u, yu, wa, wo)
    }

    private val shortDotWant = listOf(Jamo.O, Jamo.YO, Jamo.U, Jamo.YU, Jamo.WA, Jamo.WEO)

    @Test
    fun `dots of 8 to 15 dp make ㅗ ㅛ ㅜ ㅠ ㅘ ㅝ at 711 and 360 dp with the fixed minCrossDp 8 - nothing else measures length`() {
        assertEquals(8f, ThresholdDefaults.VALUES.minCrossDp)   // owner decision, issue #217
        for (w in widths) for (len in listOf(8f, 10f, 12f, 15f, 30f)) {
            assertEquals("w=$w len=$len", shortDotWant, shortDotVowels(w, ThresholdDefaults.VALUES, len))
        }
    }

    @Test
    fun `a 7 dp dot is under minCrossDp - a tap of the key under it, no trail`() {
        for (w in widths) {
            val b = Board(w)
            assertEquals(1, b.stroke(b.cx, b.c.bottom + 2, b.cx, b.c.bottom + 9).filterIsInstance<JamoEvent.Consonant>().size)
            assertEquals(0, b.logic.trail.paths.size)
        }
    }

    @Test
    fun `a tap that moves less than minCrossDp stays a tap of its consonant key, never a vowel stroke`() {
        for ((th, wobble) in listOf(ThresholdDefaults.VALUES to 7f, ThresholdDefaults.VALUES to 7.9f)) {
            for (w in widths) {
                val b = Board(w, th = th)
                val g = b.geo.geoms[b.geo.keys.indexOfFirst { it.id == "ㄴ" }].rect
                val x = (g.left + g.right) / 2; val y = (g.top + g.bottom) / 2
                for ((dx, dy) in listOf(wobble to 0f, 0f to wobble, wobble to wobble, -wobble to 0f, 0f to -wobble)) {
                    assertEquals("w=$w min=${th.minCrossDp} ($dx,$dy)", listOf<JamoEvent>(JamoEvent.Consonant(Jamo.N)), b.stroke(x, y, x + dx, y + dy))
                }
                assertEquals(0, b.logic.trail.paths.size)
            }
        }
    }

    @Test
    fun `without a log sink (release builds) the touch flow is unchanged`() {
        val host = Host(ErrorLog(File(tmp.newFolder(), "errors.log")))
        val mapper = SwipeInputMapper(host.thresholds)
        host.registerSwipeState(SwipeMapperState(mapper))
        val logic = SwipeTouchLogic(host, mapper, NoGuide, NoRepeat).also { it.geometry = SwipeGeometry.build(711f, 48f) }
        val p = StrokePoint(450f, 120f, 0L)
        logic.down(0, p)
        logic.up(0, p.copy(t = 50))
        assertEquals(listOf<JamoEvent>(JamoEvent.Consonant(Jamo.G)), host.events.flatten())
    }
}
