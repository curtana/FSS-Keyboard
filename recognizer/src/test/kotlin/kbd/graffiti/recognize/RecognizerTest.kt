package kbd.graffiti.recognize

import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.withSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hand-drawn test strokes for the test templates in `graffiti/test-templates.json` (written for these tests,
 * not real template content, CON-6). Shapes are 0..1 vertex lists drawn at [SCALE] dp with a small
 * deterministic wobble, so they never equal the templates point for point.
 */
internal object TestStrokes {
    val LETTER_RECT = RectF2(0f, 0f, 300f, 240f)
    val NUMBER_RECT = RectF2(300f, 0f, 450f, 240f)
    const val SCALE = 60f

    val templates: TemplateSet by lazy {
        TestStrokes::class.java.getResourceAsStream("/graffiti/test-templates.json")!!.use { TemplateLoader.load(it) }
    }

    /** Ellipse arc in 0..1 space: centre, radii, from [a0]° to [a1]° (screen angles, y down) in [steps]. */
    fun arc(cx: Double, cy: Double, rx: Double, ry: Double, a0: Double, a1: Double, steps: Int) =
        (0..steps).map { i ->
            val a = Math.toRadians(a0 + (a1 - a0) * i / steps)
            cx + rx * cos(a) to cy + ry * sin(a)
        }

    private fun alpha(x0: Double, w: Double) =
        listOf(1.0 to 0.0, 0.6 to 0.55, 0.3 to 0.95, 0.05 to 0.75, 0.05 to 0.25, 0.3 to 0.05, 0.6 to 0.45, 1.0 to 1.0)
            .map { (x, y) -> x0 + w * x to y }

    // ㄱ-like bend: right then down (also the shape of the 7 and en_hook test templates).
    val G = listOf(0.0 to 0.0, 1.0 to 0.05, 0.95 to 1.05)
    val CIRCLE = arc(0.5, 0.5, 0.5, 0.45, -95.0, -445.0, 20)
    val SQUARE = listOf(0.0 to 0.02, 0.02 to 1.0, 1.0 to 0.98, 0.98 to 0.0, 0.05 to 0.03)
    val SEVEN_SEVEN = listOf(0.0 to 0.0, 0.45 to 0.02, 0.48 to 1.0, 0.5 to 0.05, 1.0 to 0.03, 0.98 to 1.0)
    val CC = arc(0.26, 0.5, 0.24, 0.48, -55.0, -300.0, 10) + arc(0.76, 0.5, 0.24, 0.5, -60.0, -305.0, 10)
    val W = listOf(0.0 to 0.0, 0.22 to 1.0, 0.5 to 0.05, 0.78 to 1.0, 1.0 to 0.0)
    val M = listOf(0.0 to 1.0, 0.22 to 0.0, 0.5 to 0.95, 0.78 to 0.0, 1.0 to 1.0)
    val ALPHA_ALPHA = (alpha(0.0, 0.5) + alpha(0.5, 0.5)).map { (x, y) -> x * 0.95 to y }
    val QUESTION = listOf(
        0.02 to 0.28, 0.15 to 0.05, 0.5 to 0.02, 0.85 to 0.1, 0.98 to 0.32, 0.8 to 0.52, 0.52 to 0.62, 0.5 to 1.0,
    )
    val ZIGZAG = listOf(0.0 to 0.02, 0.97 to 0.0, 0.03 to 0.98, 1.0 to 1.0)
    val LAMBDA = listOf(0.0 to 1.0, 0.48 to 0.0, 1.0 to 0.98)
    val SCRIBBLE = listOf(
        0.0 to 0.0, 1.0 to 0.3, 0.1 to 0.5, 0.8 to 1.0, 0.5 to 0.0, 0.2 to 0.9, 1.0 to 0.6, 0.0 to 0.2, 0.6 to 0.7,
    )
    val STAR = listOf(0.5 to 0.0, 0.2 to 1.0, 1.0 to 0.35, 0.0 to 0.35, 0.8 to 1.0, 0.5 to 0.0)
    val HATCH = listOf(0.0 to 0.0, 1.0 to 0.1, 0.0 to 0.25, 1.0 to 0.4, 0.0 to 0.55, 1.0 to 0.7, 0.0 to 0.85, 1.0 to 1.0)
    val CURLS = (0..3).flatMap { k -> arc(0.15 + 0.23 * k, 0.5, 0.15, 0.4, 90.0, -270.0, 12) }
    val KNOT = listOf(
        0.3 to 0.0, 0.9 to 0.8, 0.0 to 0.6, 1.0 to 0.1, 0.6 to 1.0, 0.1 to 0.2, 0.7 to 0.3, 0.4 to 0.9, 0.95 to 0.5,
    )

    /** Strokes far from every template (GR-16, GR-26): no letter, digit or symbol shape. */
    val SCRIBBLES = listOf("scribble" to SCRIBBLE, "star" to STAR, "hatch" to HATCH, "curls" to CURLS, "knot" to KNOT)

    /** The production templates (T28), for checks that must hold on the real set as well. */
    val production: TemplateSet by lazy {
        TestStrokes::class.java.getResourceAsStream("/graffiti/templates.json")!!.use { TemplateLoader.load(it) }
    }

    /** The shipped model (LLD-05 7.1). */
    val model: kbd.graffiti.nn.GraffitiModel by lazy {
        TestStrokes::class.java.getResourceAsStream(kbd.graffiti.nn.GraffitiModelLoader.RESOURCE)!!.use { kbd.graffiti.nn.GraffitiModelLoader.load(it) }
    }

    /** The keyboard's recognizer: production templates and the shipped model (SRS FR-GRF-10). */
    val productionRecognizer: Recognizer by lazy { Recognizer(production, model) }

    /** Rounded square |x|^p + |y|^p = 1 traced from [a0]° (screen angles) counter-clockwise on screen. */
    fun superellipse(p: Double, a0: Double, steps: Int = 48) = (0..steps).map { i ->
        val a = Math.toRadians(a0 - 360.0 * i / steps)
        val c = cos(a)
        val s = sin(a)
        0.5 + 0.5 * abs(c).pow(2 / p).withSign(c) to 0.5 + 0.5 * abs(s).pow(2 / p).withSign(s)
    }

    /** [verts] scaled by [scale] at ([ox], [oy]), sampled every [step] dp with a ±[amp] dp wobble. */
    fun points(
        verts: List<Pair<Double, Double>>,
        ox: Float = 100f,
        oy: Float = 80f,
        scale: Float = SCALE,
        amp: Double = 1.0,
        step: Double = 2.0,
    ): List<InkPoint> {
        val v = verts.map { (x, y) -> ox + x * scale to oy + y * scale }
        val out = ArrayList<InkPoint>()
        var j = 0
        for (k in 1 until v.size) {
            val (ax, ay) = v[k - 1]
            val (bx, by) = v[k]
            val n = max(1, (hypot(bx - ax, by - ay) / step).toInt())
            for (s in 0 until n) {
                val t = s.toDouble() / n
                val x = ax + (bx - ax) * t + amp * sin(j * 0.7)
                val y = ay + (by - ay) * t + amp * cos(j * 0.9)
                out += InkPoint(x.toFloat(), y.toFloat(), j * 8L)
                j++
            }
        }
        out += InkPoint(v.last().first.toFloat(), v.last().second.toFloat(), j * 8L)
        return out
    }

    fun letter(points: List<InkPoint>) = InkStroke(points, PadArea.LETTER, LETTER_RECT)
    fun number(points: List<InkPoint>) = InkStroke(points, PadArea.NUMBER, NUMBER_RECT)

    /** Straight stroke from (x0, y0) by (dx, dy), a point every 2 dp, no wobble. */
    fun line(x0: Float, y0: Float, dx: Float, dy: Float, area: PadArea = PadArea.LETTER): InkStroke {
        val n = max(1, (hypot(dx, dy) / 2f).toInt())
        val pts = (0..n).map { i -> InkPoint(x0 + dx * i / n, y0 + dy * i / n, i * 8L) }
        return InkStroke(pts, area, if (area == PadArea.LETTER) LETTER_RECT else NUMBER_RECT)
    }
}

/** `Recognizer` (LLD-05 §3.1–3.2) acceptance tests GR-01..GR-19, GR-25, GR-26 (LLD-11 §6.1). */
class RecognizerTest {

    private companion object {
        /** How far below MATCH_MIN a scribble's best score must stay (GR-16, GR-26), so a small shift of D-11 keeps it Unknown. */
        const val SCRIBBLE_MARGIN = 0.02f
    }

    private val rec = Recognizer(TestStrokes.templates)

    private fun ko(s: InkStroke, expect: Expect = Expect.NORMAL) = rec.recognize(s, GraffitiLang.KO, expect)
    private fun en(s: InkStroke, expect: Expect = Expect.NORMAL) = rec.recognize(s, GraffitiLang.EN, expect)
    private fun sym(id: String) = StrokeToken.Symbol(id)

    private fun tap(x: Float, y: Float, area: PadArea): InkStroke {
        val rect = if (area == PadArea.LETTER) TestStrokes.LETTER_RECT else TestStrokes.NUMBER_RECT
        return InkStroke(listOf(InkPoint(x, y, 0), InkPoint(x + 2f, y + 1f, 30), InkPoint(x + 3f, y + 1f, 60)), area, rect)
    }

    @Test
    fun `GR-01 short stroke in the letter area centre is Tap`() {
        assertEquals(StrokeToken.Tap, ko(tap(150f, 120f, PadArea.LETTER)))
        // A single point is a tap as well (LLD-05 §3.2 errors).
        val one = InkStroke(listOf(InkPoint(150f, 120f, 0)), PadArea.LETTER, TestStrokes.LETTER_RECT)
        assertEquals(StrokeToken.Tap, en(one))
    }

    @Test
    fun `GR-02 tap at 10 percent of the letter area's top-left is CornerTap`() {
        // Letter area 300 x 240 dp: 10 % = (30, 24); the corner is the top-left 15 % = 45 x 36 dp.
        assertEquals(StrokeToken.CornerTap, ko(tap(30f, 24f, PadArea.LETTER)))
        assertEquals(StrokeToken.CornerTap, en(tap(0f, 0f, PadArea.LETTER)))
        // Start exactly on the 15 % line is outside the corner.
        assertEquals(StrokeToken.Tap, ko(tap(45f, 10f, PadArea.LETTER)))
        assertEquals(StrokeToken.Tap, ko(tap(10f, 36f, PadArea.LETTER)))
    }

    @Test
    fun `GR-03 tap at the number area's top-left is a plain Tap`() {
        assertEquals(StrokeToken.Tap, ko(tap(310f, 5f, PadArea.NUMBER)))
        assertEquals(StrokeToken.Tap, en(tap(300f, 0f, PadArea.NUMBER)))
    }

    @Test
    fun `GR-04 top to bottom 40 dp is LineDown`() {
        assertEquals(StrokeToken.LineDown, ko(TestStrokes.line(150f, 60f, 0f, 40f)))
        assertEquals(StrokeToken.LineDown, en(TestStrokes.line(350f, 60f, 0f, 40f, PadArea.NUMBER)))
    }

    @Test
    fun `GR-05 bottom to top is LineUp`() {
        assertEquals(StrokeToken.LineUp, ko(TestStrokes.line(150f, 160f, 0f, -40f)))
    }

    @Test
    fun `GR-06 left to right is LineRight`() {
        assertEquals(StrokeToken.LineRight, ko(TestStrokes.line(100f, 120f, 40f, 0f)))
    }

    @Test
    fun `GR-07 right to left is LineLeft`() {
        assertEquals(StrokeToken.LineLeft, ko(TestStrokes.line(200f, 120f, -40f, 0f)))
        // Slightly upwards: atan2 gives about −175°, still within 20° of 180°.
        assertEquals(StrokeToken.LineLeft, ko(TestStrokes.line(200f, 120f, -40f, -3f)))
    }

    @Test
    fun `GR-08 top-right to bottom-left is DiagDownLeft`() {
        assertEquals(StrokeToken.DiagDownLeft, ko(TestStrokes.line(200f, 60f, -40f, 40f)))
    }

    @Test
    fun `GR-09 top-left to bottom-right is DiagDownRight`() {
        assertEquals(StrokeToken.DiagDownRight, ko(TestStrokes.line(100f, 60f, 40f, 40f)))
    }

    @Test
    fun `GR-10 bent stroke in the letter area, KO, is Symbol ko_g`() {
        val s = TestStrokes.letter(TestStrokes.points(TestStrokes.G))
        assertNull("straightness < 0.92 goes to templates", StrokeRules.line(s))
        assertEquals(sym("ko_g"), ko(s))
    }

    @Test
    fun `GR-11 the same stroke in the number area is the 7 template`() {
        assertEquals(sym("num_7"), ko(TestStrokes.number(TestStrokes.points(TestStrokes.G, ox = 330f))))
        assertEquals(sym("num_7"), en(TestStrokes.number(TestStrokes.points(TestStrokes.G, ox = 330f))))
    }

    @Test
    fun `GR-12 the same stroke in the letter area, EN, is found in EN_LETTER`() {
        val enIds = TestStrokes.templates.sets.getValue(SetName.EN_LETTER).map { it.id }
        val token = en(TestStrokes.letter(TestStrokes.points(TestStrokes.G)))
        assertEquals(sym("en_hook"), token)
        assertTrue((token as StrokeToken.Symbol).id in enIds)
    }

    @Test
    fun `GR-13 circle is ㅇ, o or 0 by area and lang`() {
        val pts = TestStrokes.points(TestStrokes.CIRCLE)
        assertEquals(sym("ko_ieung"), ko(TestStrokes.letter(pts)))
        assertEquals(sym("en_o"), en(TestStrokes.letter(pts)))
        val numPts = TestStrokes.points(TestStrokes.CIRCLE, ox = 330f)
        assertEquals(sym("num_0"), ko(TestStrokes.number(numPts)))
        assertEquals(sym("num_0"), en(TestStrokes.number(numPts)))
    }

    @Test
    fun `GR-14 expect PUNCT searches the PUNCT set first`() {
        val s = TestStrokes.letter(TestStrokes.points(TestStrokes.QUESTION))
        assertEquals(sym("p_question"), ko(s, Expect.PUNCT))
        assertEquals(sym("p_question"), en(s, Expect.PUNCT))
        assertEquals(sym("p_question"), ko(TestStrokes.number(TestStrokes.points(TestStrokes.QUESTION, ox = 330f)), Expect.PUNCT))
        // Without the punctuation shift the PUNCT set is not searched.
        assertNotEquals(sym("p_question"), ko(s))
    }

    @Test
    fun `GR-15 expect EXTENDED searches the EXTENDED set`() {
        val s = TestStrokes.letter(TestStrokes.points(TestStrokes.ZIGZAG))
        assertEquals(sym("x_zigzag"), ko(s, Expect.EXTENDED))
        assertEquals(sym("x_zigzag"), en(s, Expect.EXTENDED))
        assertNotEquals(sym("x_zigzag"), ko(s))
        assertNotEquals(sym("x_zigzag"), en(s, Expect.PUNCT))
    }

    /**
     * Every scribble × context: [StrokeToken.Unknown], and the best score in each set [Recognizer.recognize] searches
     * stays at least [SCRIBBLE_MARGIN] below [Recognizer.MATCH_MIN], on the test and the production templates.
     */
    private fun assertScribblesUnknown(expect: Expect) {
        var closest = "" to 0f
        for (templates in listOf(TestStrokes.templates, TestStrokes.production)) {
            // The test templates have no model (their letters are not the model's): they go by the template score alone.
            val r = if (templates === TestStrokes.production) TestStrokes.productionRecognizer else Recognizer(templates)
            for ((name, shape) in TestStrokes.SCRIBBLES) for (lang in GraffitiLang.entries) for (area in PadArea.entries) {
                val s = if (area == PadArea.LETTER) TestStrokes.letter(TestStrokes.points(shape))
                else TestStrokes.number(TestStrokes.points(shape, ox = 330f))
                val where = "$name $lang $area $expect"
                assertEquals(where, StrokeToken.Unknown, r.recognize(s, lang, expect))
                for (e in setOf(expect, Expect.NORMAL)) {
                    val best = TemplateMatcher.best(s, templates.forContext(area, lang, e))?.second ?: continue
                    assertTrue("$where: best $best in $e", best <= Recognizer.MATCH_MIN - SCRIBBLE_MARGIN)
                    if (best > closest.second) closest = "$where in $e" to best
                }
            }
        }
        println("closest scribble score (expect $expect): ${closest.second} at ${closest.first}, MATCH_MIN ${Recognizer.MATCH_MIN}")
    }

    @Test
    fun `GR-16 scribbles far from every template are Unknown, well below the threshold`() {
        assertScribblesUnknown(Expect.NORMAL)
    }

    @Test
    fun `GR-17 the same stroke twice gives the same result`() {
        val other = Recognizer(TestStrokes.templates)
        val shapes = listOf(
            TestStrokes.G, TestStrokes.CIRCLE, TestStrokes.SQUARE, TestStrokes.SEVEN_SEVEN, TestStrokes.CC,
            TestStrokes.W, TestStrokes.M, TestStrokes.ALPHA_ALPHA, TestStrokes.QUESTION, TestStrokes.ZIGZAG,
            TestStrokes.SCRIBBLE, TestStrokes.STAR, TestStrokes.HATCH, TestStrokes.CURLS, TestStrokes.KNOT,
        )
        for (shape in shapes) for (lang in GraffitiLang.entries) for (expect in Expect.entries) {
            val s = TestStrokes.letter(TestStrokes.points(shape))
            val first = rec.recognize(s, lang, expect)
            assertEquals("$shape $lang $expect", first, rec.recognize(s, lang, expect))
            assertEquals("$shape $lang $expect", first, other.recognize(s.copy(points = s.points.toList()), lang, expect))
        }
    }

    @Test
    fun `GR-18 a stroke starting in the letter area and ending in the number area uses the letter set`() {
        // Starts at x = 260 in the letter area (0..300) and ends near x = 317 in the number area.
        val pts = TestStrokes.points(TestStrokes.G, ox = 260f)
        assertTrue(pts.first().x < 300f && pts.last().x > 300f)
        assertEquals(sym("ko_g"), ko(TestStrokes.letter(pts)))
    }

    @Test
    fun `GR-19 length 7,9 is Tap, 8,0 goes to the line rules`() {
        fun h(len: Float) = InkStroke(
            listOf(InkPoint(150f, 120f, 0), InkPoint(150f + len / 2, 120f, 20), InkPoint(150f + len, 120f, 40)),
            PadArea.LETTER, TestStrokes.LETTER_RECT,
        )
        assertEquals(StrokeToken.Tap, ko(h(7.9f)))
        assertEquals(StrokeToken.LineRight, ko(h(8.0f)))
        // Same boundary in the letter corner: 7.9 is CornerTap, 8.0 is a line.
        fun c(len: Float) = InkStroke(listOf(InkPoint(5f, 5f, 0), InkPoint(5f, 5f + len, 40)), PadArea.LETTER, TestStrokes.LETTER_RECT)
        assertEquals(StrokeToken.CornerTap, ko(c(7.9f)))
        assertEquals(StrokeToken.LineDown, ko(c(8.0f)))
    }

    @Test
    fun `GR-25 a stroke missing from the PUNCT or EXTENDED set falls back to the normal set`() {
        assertEquals(sym("en_a"), en(TestStrokes.letter(TestStrokes.points(TestStrokes.LAMBDA)), Expect.PUNCT))
        assertEquals(sym("ko_g"), ko(TestStrokes.letter(TestStrokes.points(TestStrokes.G)), Expect.EXTENDED))
    }

    @Test
    fun `GR-26 expect PUNCT or EXTENDED with a scribble is Unknown, in the symbol set and the normal set`() {
        assertScribblesUnknown(Expect.PUNCT)
        assertScribblesUnknown(Expect.EXTENDED)
    }

    @Test
    fun `a template set without strokes gives Unknown for a non-rule stroke`() {
        val empty = Recognizer(TemplateSet(emptyMap()))
        assertEquals(StrokeToken.Unknown, empty.recognize(TestStrokes.letter(TestStrokes.points(TestStrokes.G)), GraffitiLang.KO, Expect.PUNCT))
        assertEquals(StrokeToken.LineDown, empty.recognize(TestStrokes.line(150f, 60f, 0f, 40f), GraffitiLang.KO, Expect.NORMAL))
    }
}
