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
internal object LoopStrokes {
    val LETTER = RectF2(0f, 0f, 300f, 240f)
    val NUMBER = RectF2(300f, 0f, 450f, 240f)

    /** One loop of the battery: where it starts, how far and which way it turns ([sweep] < 0 = counter-clockwise on screen), its shape. */
    class Loop(val start: Int, val sweep: Int, val kx: Float, val ky: Float) {
        override fun toString() = "$start/$sweep/${kx}x$ky"
    }

    /** An ellipse arc from [startDeg] over [sweepDeg] (screen angles, + = clockwise on screen), a point every 4 degrees. */
    fun arc(cx: Float, cy: Float, rx: Float, ry: Float, startDeg: Int, sweepDeg: Int, area: PadArea, rect: RectF2): InkStroke {
        val n = Math.abs(sweepDeg) / 4
        val pts = (0..n).map { k ->
            val a = Math.toRadians(startDeg + sweepDeg * k.toDouble() / n)
            InkPoint((cx + rx * cos(a)).toFloat(), (cy + ry * sin(a)).toFloat(), k * 8L)
        }
        return InkStroke(pts, area, rect)
    }

    private val starts = (0 until 360 step 30).toList()
    private val sweeps = listOf(320, 340, 360, 380, 400, 420)   // ends 40 degrees short of closing to 60 past it
    private val shapes = listOf(1f to 1f, 0.75f to 1f, 1f to 0.75f)   // circle, tall and wide ellipses

    /** Every synthetic loop of GR-33 in [area]: 12 starts x 6 sweeps x 3 shapes x [dirs], radius 70 dp. */
    fun all(area: PadArea, dirs: List<Int> = listOf(1, -1)): List<Pair<Loop, InkStroke>> {
        val rect = if (area == PadArea.LETTER) LETTER else NUMBER
        val cx = (rect.left + rect.right) / 2
        val cy = (rect.top + rect.bottom) / 2
        val r = 70f
        val out = ArrayList<Pair<Loop, InkStroke>>()
        for (s in starts) for (w in sweeps) for ((kx, ky) in shapes) for (dir in dirs) {
            out += Loop(s, dir * w, kx, ky) to arc(cx, cy, r * kx, r * ky, s, dir * w, area, rect)
        }
        return out
    }
}

class LoopRecognitionTest {

    private val templates: TemplateSet = TestStrokes.production

    /** Every synthetic loop in [area]; returns the misreads as "start/sweep/shape: token". */
    private fun misreads(
        area: PadArea, lang: GraffitiLang, expect: Expect, want: String, dirs: List<Int> = listOf(1, -1),
        known: Map<String, TemplateOutput> = emptyMap(),
    ): List<String> {
        val rec = TestStrokes.productionRecognizer
        val bad = ArrayList<String>()
        for ((loop, stroke) in LoopStrokes.all(area, dirs)) {
            val t = rec.recognize(stroke, lang, expect)
            // The owner's own strokes (FSS#64) are other templates with the same output.
            if (t is StrokeToken.Symbol && templates.outputOf(t.id) == templates.outputOf(want)) continue
            // A known limit of the shipped model (LLD-11 6.7): only that loop, only that reading.
            if (t is StrokeToken.Symbol && known[loop.toString()]?.let { it == templates.outputOf(t.id) } == true) {
                println("GR-33 known limit: $loop reads as ${templates.outputOf(t.id)}")
                continue
            }
            // D (en_d2) starts at its bottom-left corner going up and round clockwise: a tall loop begun there the same
            // way is a D-like stroke, so D is an accepted reading of it (LLD-05 3.2 step 4a). The letter's best-scoring
            // built-in template is what comes back (LLD-05 3.2a), so the reading is told by its output.
            val dLike = want == "en_o" && loop.kx < loop.ky && loop.sweep > 0 && loop.start in 90..150 &&
                t is StrokeToken.Symbol && templates.outputOf(t.id) == templates.outputOf("en_d2")
            if (!dLike) bad += "$loop: $t"
        }
        return bad
    }

    @Test
    fun `GR-33 loops read as the circle of their area at any start, short or past closing`() {
        assertEquals(emptyList<String>(), misreads(PadArea.LETTER, GraffitiLang.KO, Expect.NORMAL, "ko_ieung", known = KO_KNOWN))
        assertEquals(emptyList<String>(), misreads(PadArea.LETTER, GraffitiLang.EN, Expect.NORMAL, "en_o"))
        assertEquals(emptyList<String>(), misreads(PadArea.NUMBER, GraffitiLang.EN, Expect.NORMAL, "num_0"))
    }

    /**
     * GR-33's one known limit (FSS#96, LLD-11 6.7): the Korean circle begun at 210° going counter-clockwise 320° (40°
     * short of closing) reads as ㅁ under the weighted sum — ㅁ template 0.823 / NN 0.966 against ㅇ 0.985 / 0.723. The
     * training side gets the same answer and scores from the shipped model (not a port defect); the templates alone
     * read it as ㅇ. Every other loop must still read as its circle.
     */
    private val KO_KNOWN: Map<String, TemplateOutput> by lazy { mapOf("210/-320/1.0x1.0" to checkNotNull(templates.outputOf("ko_m"))) }

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
        val circle = LoopStrokes.arc(150f, 120f, 70f, 70f, 45, 400, PadArea.LETTER, LoopStrokes.LETTER).points.map { it.x to it.y }
        val lap = checkNotNull(TemplateMatcher.loopOf(circle))
        assertEquals(lap.first(), lap.last())   // cut at one lap and closed
    }
}
