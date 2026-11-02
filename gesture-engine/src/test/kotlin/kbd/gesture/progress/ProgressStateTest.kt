package kbd.gesture.progress

import kbd.gesture.classify.ClassifierTestSupport
import kbd.gesture.classify.StrokeClassifier
import kbd.stroke.geom.Dir
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import kbd.stroke.testing.Leg
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §4.3 PS-01..PS-18 and §4.5 PF-01 (LLD-04 §5). */
class ProgressStateTest {

    // LLD-11 §1.1 default test key: ㄱ, (0,0)-(51,48), no outer edges, start at the centre.
    private val gGeom = KeyGeom("ㄱ", RectDp(0f, 0f, 51f, 48f), emptySet())
    private val th = ThresholdDefaults.VALUES

    /** Progress after pressing the centre of [geom] and moving along [legs]. */
    private fun progress(c: StrokeClassifier, geom: KeyGeom, legs: List<Leg>): ProgressState {
        val pts = ClassifierTestSupport.stroke(geom, legs)
        c.begin(pts.first())
        var ps = ProgressState.of(c)
        for (p in pts.drop(1)) ps = c.update(p)
        return ps
    }

    private fun layoutKey(id: String, vararg legs: Leg): ProgressState {
        val k = ClassifierTestSupport.keyDef(id)
        val g = ClassifierTestSupport.layoutGeom(k)
        return progress(StrokeClassifier(k, th, g), g, legs.toList())
    }

    /** Candidates as "dir label" strings, order ignored. */
    private fun set(ps: ProgressState) = ps.candidates.map { "${it.dir} ${it.label}" }.sorted()

    private fun expect(ps: ProgressState, current: String, vararg cands: String) {
        assertEquals(current, ps.current)
        assertEquals(cands.sorted(), set(ps))
    }

    @Test fun `PS-01 consonant key - before the stroke one hint down and one right, labels from the real assembler`() {
        val preview = kbd.gesture.input.SwipeInputMapper(th)::preview   // the real assembler (bare: no consonant composing)
        val k = ClassifierTestSupport.keyDef("ㄱ")
        val ps = progress(StrokeClassifier(k, th, gGeom, preview), gGeom, emptyList())
        assertEquals("ㄱ", ps.current)
        assertTrue(ps.beforeExit)
        assertEquals(listOf("DOWN ㅣ", "RIGHT ㅡ"), set(ps))   // the dot and long probes give the same glyph: one hint each (SWIPE 9.1)
        assertEquals((th.minCrossDp + th.longDp) / 2, ps.candidates.single { it.dir == Dir.DOWN }.offsetDp, 1e-3f)   // the nearer (dot) probe stays
        assertTrue(ps.candidates.all { it.kind == ProbeKind.DOT })
        // No up or left: strokes go down and right only (SWIPE 3.2).
        assertFalse(ps.candidates.any { it.dir == Dir.UP || it.dir == Dir.LEFT })
    }

    /** Review PR #183 round 2: with the real assembler (no length) the dot and long probes give the same glyph; one hint per (direction, glyph). */
    @Test fun `PS-01b consonant key with the real assembler preview - one hint per direction and glyph, no duplicates`() {
        val k = ClassifierTestSupport.keyDef("ㄱ")
        val g = ClassifierTestSupport.layoutGeom(k)
        fun hints(mapper: kbd.gesture.input.SwipeInputMapper): List<String> =
            progress(StrokeClassifier(k, th, g, mapper::preview), g, emptyList()).candidates.map { "${it.dir} ${it.label}" }
        // A consonant composing: C of the layout the test geometry uses (SwipeLayout.consonantBox).
        val withC = kbd.gesture.input.SwipeInputMapper(th).also { m ->
            m.consonant = kbd.gesture.layout.SwipeLayout.consonantBox(kbd.gesture.layout.SwipeLayout.COLS * ClassifierTestSupport.COL_W, ClassifierTestSupport.ROW_H)
        }
        val c = hints(withC)
        assertEquals(c.distinct(), c)
        // SWIPE 0.43 (issue #217): from ㄱ (right of C) every ↓ probe's middle is right-up = ㅣ: one ↓ hint.
        assertEquals(listOf("DOWN ㅣ"), c.filter { it.startsWith("DOWN ") })
        assertEquals(1, c.count { it.startsWith("RIGHT ") })
        // Bare: ↓ = ㅣ, → = ㅡ, once each.
        assertEquals(listOf("DOWN ㅣ", "RIGHT ㅡ"), hints(kbd.gesture.input.SwipeInputMapper(th)).sorted())
    }

    @Test fun `PS-02 consonant key - a stroke the assembler ignores offers nothing`() {
        val k = ClassifierTestSupport.keyDef("ㄱ")
        val ps = progress(StrokeClassifier(k, th, gGeom) { null }, gGeom, emptyList())
        assertEquals(emptyList<String>(), set(ps))
    }

    @Test fun `PS-13 number key 5 press shows the table ring and the scripts`() {
        val ps = layoutKey("5")
        assertEquals("5", ps.current)
        fun of(kind: ProbeKind) = ps.candidates.filter { it.kind == kind }.map { "${it.dir} ${it.label}" }.sorted()
        assertEquals(listOf("DOWN ×", "LEFT _", "RIGHT +", "UP %"), of(ProbeKind.RING))
        assertEquals(listOf("DOWN ₅", "UP ⁵"), of(ProbeKind.RUB))
        assertEquals(6, ps.candidates.size)
    }

    @Test fun `PS-14 symbol key press shows one symbol per direction, no stages (issue #208)`() {
        val ps = layoutKey("symbol")
        assertEquals(".", ps.current)
        assertEquals(listOf("DOWN -", "LEFT (", "RIGHT )", "UP >"), ps.candidates.map { "${it.dir} ${it.label}" }.sorted())
        assertTrue(ps.candidates.all { it.kind == ProbeKind.RING })
        val ps2 = layoutKey("symbol2")
        assertEquals(",", ps2.current)
        assertEquals(listOf("DOWN '", "LEFT \"", "UP <"), ps2.candidates.map { "${it.dir} ${it.label}" }.sorted())   // → is the screen edge
        val ps3 = layoutKey("symbol3")
        assertEquals("/", ps3.current)
        assertEquals(listOf("RIGHT =", "UP ?"), ps3.candidates.map { "${it.dir} ${it.label}" }.sorted())   // ← screen edge, ↓ = the tap
        // After the exit nothing changes the result: no next-character hint (SWIPE 9.2).
        assertTrue(layoutKey("symbol", Leg.U(42.0)).candidates.isEmpty())
    }

    @Test fun `PS-16 number key 3 U42 offers only the superscript, R42 offers nothing`() {
        val up = layoutKey("3", Leg.U(42.0))
        assertEquals("#", up.current)
        assertEquals(1, up.candidates.size)
        assertEquals("³", up.candidates.single().label)
        assertEquals(ProbeKind.RUB, up.candidates.single().kind)
        expect(layoutKey("3", Leg.R(42.0)), "}")
    }

    @Test fun `PS-17 RUB points back while outside, onward once the finger is back inside (number key 5)`() {
        val out = layoutKey("5", Leg.U(42.0))
        val back = out.candidates.single { it.kind == ProbeKind.RUB }
        assertEquals(Dir.DOWN, back.dir)
        val inside = layoutKey("5", Leg.U(42.0), Leg.D(42.0))
        assertEquals(Dir.UP, inside.candidates.single { it.kind == ProbeKind.RUB }.dir)
    }

    @Test fun `PS-18 press only offsets are measured from the finger, the ring outside at OUTER's place`() {
        val m = th.minCrossDp
        val long = th.longDp
        /** Distance from the key centre (the press point) to the edge in direction [d]. */
        fun toEdge(g: KeyGeom, d: Dir): Float = when (d) {
            Dir.LEFT, Dir.RIGHT -> (g.rect.right - g.rect.left) / 2
            Dir.UP, Dir.DOWN -> (g.rect.bottom - g.rect.top) / 2
        }
        fun check(ps: ProgressState, g: KeyGeom, kind: ProbeKind, dirs: List<Dir>, past: Float) {
            for (d in dirs) {
                val c = ps.candidates.single { it.kind == kind && it.dir == d }
                assertEquals("$kind $d", toEdge(g, d) + past, c.offsetDp, 1e-3f)
            }
        }
        val all = listOf(Dir.UP, Dir.DOWN, Dir.LEFT, Dir.RIGHT)

        val five = layoutKey("5")
        val fiveGeom = ClassifierTestSupport.layoutGeom(ClassifierTestSupport.keyDef("5"))
        check(five, fiveGeom, ProbeKind.RING, all, m)
        check(five, fiveGeom, ProbeKind.RUB, listOf(Dir.UP, Dir.DOWN), long)

        val sym = layoutKey("symbol")
        val symGeom = ClassifierTestSupport.layoutGeom(ClassifierTestSupport.keyDef("symbol"))
        check(sym, symGeom, ProbeKind.RING, all, m)
    }

    /** PF-01 (NFR-02 proxy): mean of `update` + `of` over 10 000 samples after warm-up ≤ 0.1 ms on the dev PC JVM. */
    @Test fun `PF-01 update with probes averages at most 0_1 ms`() {
        val legs = listOf(listOf(Leg.R(45.0)), listOf(Leg.D(80.0)), listOf(Leg.U(42.0), Leg.D(42.0), Leg.U(42.0)), listOf(Leg.L(45.0), Leg.R(30.0)))
        val keys = listOf("ㄱ", "5", "symbol", "cheonjiin")
        val strokes = keys.flatMap { id ->
            val k = ClassifierTestSupport.keyDef(id)
            val g = ClassifierTestSupport.layoutGeom(k)
            legs.map { l -> ClassifierTestSupport.stroke(g, l) to StrokeClassifier(k, th, g) { "ㅏ" } }
        }
        fun drive(samples: Int): Long {
            var n = 0
            var sink = 0
            val t0 = System.nanoTime()
            while (n < samples) {
                for ((pts, c) in strokes) {
                    c.begin(pts.first())
                    for (i in 1 until pts.size) {
                        sink += c.update(pts[i]).candidates.size
                        if (++n == samples) break
                    }
                    if (n == samples) break
                }
            }
            val elapsed = System.nanoTime() - t0
            assertTrue(sink >= 0)
            return elapsed
        }
        drive(10_000) // warm-up
        val meanMs = drive(10_000) / 10_000.0 / 1_000_000.0
        assertTrue("mean $meanMs ms", meanMs <= 0.1)
    }
}
