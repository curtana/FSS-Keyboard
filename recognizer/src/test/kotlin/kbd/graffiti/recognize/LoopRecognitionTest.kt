package kbd.graffiti.recognize

import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * LLD-11 GR-33..GR-35 (FSS#33, owner on the device): a hand-drawn circle seldom starts at the top or closes exactly.
 * Synthetic circles and ellipses on the production templates — every start angle, short of closing by up to 40° or
 * past it by up to 60°, both directions — must read as ㅇ / o / 0 (and @ while punctuation is pending), never as a
 * neighbouring shape.
 */
class LoopRecognitionTest {

    private val templates: TemplateSet by lazy {
        LoopRecognitionTest::class.java.getResourceAsStream("/graffiti/templates.json")!!.use { TemplateLoader.load(it) }
    }

    /** An ellipse arc from [startDeg] over [sweepDeg] (screen angles, + = clockwise on screen), a point every 4°. */
    private fun arc(cx: Float, cy: Float, rx: Float, ry: Float, startDeg: Int, sweepDeg: Int, area: PadArea, rect: RectF2): InkStroke {
        val n = Math.abs(sweepDeg) / 4
        val pts = (0..n).map { k ->
            val a = Math.toRadians(startDeg + sweepDeg * k.toDouble() / n)
            InkPoint((cx + rx * cos(a)).toFloat(), (cy + ry * sin(a)).toFloat(), k * 8L)
        }
        return InkStroke(pts, area, rect)
    }

    private val starts = (0 until 360 step 30).toList()
    private val sweeps = listOf(320, 340, 360, 380, 400, 420)   // end gap −40° … +60°
    private val shapes = listOf(1f to 1f, 0.75f to 1f, 1f to 0.75f)   // circle, tall and wide ellipses

    /** Every synthetic loop in [area]; returns the misreads as "start/sweep/shape/dir → token". */
    private fun misreads(area: PadArea, lang: GraffitiLang, expect: Expect, want: String, dirs: List<Int> = listOf(1, -1)): List<String> {
        val rect = if (area == PadArea.LETTER) LETTER else NUMBER
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val r = 70f
        val rec = Recognizer(templates)
        val bad = ArrayList<String>()
        for (s in starts) for (w in sweeps) for ((kx, ky) in shapes) for (dir in dirs) {
            val t = rec.recognize(arc(cx, cy, r * kx, r * ky, s, dir * w, area, rect), lang, expect)
            // The owner's own strokes (FSS#64) are other templates with the same output.
            if (t is StrokeToken.Symbol && templates.outputOf(t.id) == templates.outputOf(want)) continue
            // D (en_d2) starts at its bottom-left corner going up and round clockwise: a tall loop begun there the same
            // way is a D-like stroke, so D is an accepted reading of it (LLD-05 §3.2 step 4a).
            val dLike = want == "en_o" && kx < ky && dir == 1 && s in 90..150 && t == StrokeToken.Symbol("en_d2")
            if (!dLike) bad += "$s/${dir * w}/${kx}x$ky → $t"
        }
        return bad
    }

    @Test
    fun `GR-33 loops read as the circle of their area at any start, short or past closing`() {
        assertEquals(emptyList<String>(), misreads(PadArea.LETTER, GraffitiLang.KO, Expect.NORMAL, "ko_ieung"))
        assertEquals(emptyList<String>(), misreads(PadArea.LETTER, GraffitiLang.EN, Expect.NORMAL, "en_o"))
        assertEquals(emptyList<String>(), misreads(PadArea.NUMBER, GraffitiLang.EN, Expect.NORMAL, "num_0"))
    }

    @Test
    fun `GR-34 a loop while punctuation is pending reads as at, in the direction of its template`() {
        // The @ template has one stroke, counter-clockwise on screen (decreasing screen angle); the other way is not @.
        assertEquals(emptyList<String>(), misreads(PadArea.LETTER, GraffitiLang.EN, Expect.PUNCT, "p_at", dirs = listOf(-1)))
    }

    @Test
    fun `GR-35 loop and tail, open curves and a figure eight are no near-closed loop`() {
        fun points(id: String) = templates.sets.values.flatten().single { it.id == id }.strokes[0].map { (x, y) -> x * 100f to y * 100f }
        for (id in listOf("en_g", "en_q", "num_6", "num_9", "ko_h6", "en_c", "en_u", "en_e", "num_8")) {
            assertEquals(id, null, TemplateMatcher.loopOf(points(id)))
        }
        val circle = arc(150f, 120f, 70f, 70f, 45, 400, PadArea.LETTER, LETTER).points.map { it.x to it.y }
        val lap = checkNotNull(TemplateMatcher.loopOf(circle))
        assertEquals(lap.first(), lap.last())   // cut at one lap and closed
    }

    private companion object {
        val LETTER = RectF2(0f, 0f, 300f, 240f)
        val NUMBER = RectF2(300f, 0f, 450f, 240f)
    }
}
