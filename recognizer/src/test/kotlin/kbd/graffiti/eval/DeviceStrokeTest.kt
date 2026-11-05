package kbd.graffiti.eval

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GR-36 (FSS#47): real tablet strokes in `strokes/device/` must all be recognized. They are raw touches
 * read off the device while "갸" (ㄱ, ㅣ, ㆍ, ㆍ) was written over and over: 84 strokes, 23 of them ㅣ. The long
 * ㅣ strokes end in a short move back up as the finger lifts; seven ㅣ failed before the lift-off tail rule. FSS#52 added ㄴ and ㅂ strokes (`ko_n_*`, `ko_b_*`).
 */
class DeviceStrokeTest {

    @Test
    fun `GR-36 device strokes ga - every stroke recognized, lift-off tails included`() {
        val sets = GraffitiEval.load("device")
        for ((folder, list) in sets) {
            val s = GraffitiEval.score(folder, list)
            println(s)
            assertEquals("misses ${s.misses}", s.total, s.correct)
        }
    }

    /** FSS#52: a ㄴ with a long bottom stroke read as ㅃ. These strokes are not templates (`ko_n_*` in `strokes/device/`). */
    @Test
    fun `GR-38 wide n strokes are n`() {
        val wide = GraffitiEval.load("device").getValue("ko").filter { it.file.name.startsWith("ko_n_") }
        assertEquals(11, wide.size)
        val s = GraffitiEval.score("wide n", wide)
        assertEquals("misses ${s.misses}", s.total, s.correct)
    }

    /** FSS#64: real ㅍ and ㅎ strokes that are not templates (`strokes/device_held/`). Measured 72 / 74 when the owner's strokes went in. */
    @Test
    fun `GR-39 held-out owner strokes stay at 95 percent`() {
        for ((folder, list) in GraffitiEval.load("device_held")) {
            val s = GraffitiEval.score(folder, list)
            println(s)
            assertTrue("$s misses ${s.misses}", s.accuracy >= HELD_OUT_MIN)
        }
    }

    /** FSS#70: the owner's Q, whose tail leaves the loop's width, read as v. The V strokes must stay v. Neither is a template (`en_*` in `strokes/device/`). */
    @Test
    fun `GR-40 owner Q strokes with the tail outside the loop are q, V strokes stay v`() {
        val en = GraffitiEval.load("device").getValue("en")
        val q = en.filter { it.file.name.startsWith("en_q_") }
        val v = en.filter { it.file.name.startsWith("en_v_") }
        assertEquals(22, q.size)
        assertEquals(10, v.size)
        for (s in listOf(GraffitiEval.score("Q", q), GraffitiEval.score("V", v))) {
            println(s)
            assertEquals("${s.name} misses ${s.misses}", s.total, s.correct)
        }
    }

    private companion object {
        const val HELD_OUT_MIN = 95.0
    }
}
