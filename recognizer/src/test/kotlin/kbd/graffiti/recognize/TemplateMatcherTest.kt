package kbd.graffiti.recognize

import java.io.ByteArrayInputStream
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.hangul.jamo.Jamo
import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** `TemplateMatcher` acceptance tests GR-21..GR-24 (LLD-05 §3.2, LLD-11 §6.1) and `TemplateLoader` (§7, no ID). */
class TemplateMatcherTest {

    private val templates = TestStrokes.templates
    private val ko = templates.sets.getValue(SetName.KO_LETTER)

    private fun stroke(points: List<InkPoint>) = TestStrokes.letter(points)

    private fun template(id: String, corners: Int, vararg pts: Pair<Float, Float>) =
        Template(id, TemplateOutput.Text(id), corners, listOf(pts.toList()))

    private fun scoreOf(s: InkStroke, t: Template): Float = TemplateMatcher.best(s, listOf(t))!!.second
    // --- GR-21..GR-24 -------------------------------------------------------------------------------------

    @Test
    fun `GR-21 resampling gives 32 evenly spaced points whatever the input count`() {
        val inputs = listOf(
            listOf(0f to 0f, 100f to 0f),
            listOf(0f to 0f, 10f to 0f, 10f to 10f, 30f to 10f, 30f to 50f, 0f to 50f, 0f to 20f),
            TestStrokes.points(TestStrokes.CIRCLE).map { it.x to it.y },   // ~ 100 points
            List(500) { i -> i * 0.3f to (i % 7).toFloat() },
        )
        for (input in inputs) {
            val out = TemplateMatcher.resample(input)
            assertEquals("${input.size} points", TemplateMatcher.N, out.size)
            assertEquals(input.first(), out.first())
            assertEquals(input.last(), out.last())
            val gaps = (1 until out.size).map { hypot(out[it].first - out[it - 1].first, out[it].second - out[it - 1].second) }
            // Along the path the gaps are equal; a chord across a bend can only be shorter.
            val pathGap = (1 until input.size).sumOf {
                hypot(input[it].first - input[it - 1].first, input[it].second - input[it - 1].second).toDouble()
            } / (TemplateMatcher.N - 1)
            assertTrue(gaps.all { it <= pathGap * 1.0001 + 1e-4 })
        }
        // On a straight line every gap is the path interval.
        assertTrue(TemplateMatcher.resample(inputs[0]).zipWithNext().all { (a, b) -> abs(b.first - a.first - 100f / 31) < 1e-3f })
        // A single point (or no movement) repeats it.
        assertEquals(List(TemplateMatcher.N) { 5f to 5f }, TemplateMatcher.resample(listOf(5f to 5f, 5f to 5f)))
        // A single-point stroke still matches (all 32 points at the origin after normalisation).
        assertEquals(List(TemplateMatcher.N) { 0f to 0f }, TemplateMatcher.normalize(TemplateMatcher.resample(listOf(1f to 1f))))
        assertTrue(TemplateMatcher.best(stroke(listOf(InkPoint(1f, 1f, 0))), ko) != null)
    }

    @Test
    fun `GR-22 the same shape at twice the size scores the same`() {
        for (shape in listOf(TestStrokes.G, TestStrokes.CIRCLE, TestStrokes.W, TestStrokes.QUESTION)) {
            val small = stroke(TestStrokes.points(shape, amp = 0.0))
            val big = stroke(TestStrokes.points(shape, ox = 20f, oy = 10f, scale = TestStrokes.SCALE * 2, amp = 0.0))
            val a = TemplateMatcher.best(small, ko + templates.sets.getValue(SetName.PUNCT))!!
            val b = TemplateMatcher.best(big, ko + templates.sets.getValue(SetName.PUNCT))!!
            assertEquals(a.first, b.first)
            assertEquals(a.second, b.second, 1e-3f)
        }
        // Aspect ratio is kept: a flat ㅡ-like bend is not stretched to a square.
        val flat = stroke(TestStrokes.points(listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 0.2), amp = 0.0))
        val square = template("square_bend", 1, 0f to 0f, 1f to 0f, 1f to 1f)
        val flatT = template("flat_bend", 1, 0f to 0f, 1f to 0f, 1f to 0.2f)
        assertEquals("flat_bend", TemplateMatcher.best(flat, listOf(square, flatT))!!.first)
    }

    @Test
    fun `GR-23 the same shape rotated 90 degrees matches a different template`() {
        // A: right then down (ㄱ). B: A turned 90° clockwise on screen, i.e. down then left.
        val a = template("A", 1, 0f to 0f, 1f to 0f, 1f to 1f)
        val b = template("B", 1, 1f to 0f, 1f to 1f, 0f to 1f)
        val shape = TestStrokes.G
        val rotated = shape.map { (x, y) -> 1.0 - y to x }   // (x, y) → (−y, x), shifted back into 0..1
        assertEquals("A", TemplateMatcher.best(stroke(TestStrokes.points(shape)), listOf(a, b))!!.first)
        assertEquals("B", TemplateMatcher.best(stroke(TestStrokes.points(rotated)), listOf(a, b))!!.first)
        // With rotation normalisation the rotated stroke would still match A; here it does not.
        assertTrue(scoreOf(stroke(TestStrokes.points(rotated)), a) < Recognizer.MATCH_MIN)
    }

    @Test
    fun `GR-24 the corner aid tells ㅁ and ㅇ apart`() {
        val ieung = ko.single { it.id == "ko_ieung" }
        val mieum = ko.single { it.id == "ko_m" }
        val square = stroke(TestStrokes.points(TestStrokes.SQUARE))
        val circle = stroke(TestStrokes.points(TestStrokes.CIRCLE))
        assertEquals(3, TemplateMatcher.cornerCount(TemplateMatcher.normalize(TemplateMatcher.resample(square.points.map { it.x to it.y }))))
        assertEquals(0, TemplateMatcher.cornerCount(TemplateMatcher.normalize(TemplateMatcher.resample(circle.points.map { it.x to it.y }))))
        assertEquals("ko_m", TemplateMatcher.best(square, listOf(ieung, mieum))!!.first)
        assertEquals("ko_ieung", TemplateMatcher.best(circle, listOf(ieung, mieum))!!.first)

        // A rounded square started near the top: closer to ㅇ point by point, but it has corners,
        // so ㅇ (corners 0) loses 0.1 and ㅁ wins.
        val rounded = stroke(TestStrokes.points(TestStrokes.superellipse(6.0, -94.0), amp = 0.0))
        val roundedPts = TemplateMatcher.normalize(TemplateMatcher.resample(rounded.points.map { it.x to it.y }))
        assertTrue(TemplateMatcher.cornerCount(roundedPts) >= 2)
        val noAid = { t: Template -> TemplateMatcher.best(rounded, listOf(t.copy(corners = TemplateMatcher.cornerCount(roundedPts))))!!.second }
        assertTrue("raw ㅇ > raw ㅁ", noAid(ieung) > noAid(mieum))
        // Its corners also keep it off the loop comparison (FSS#33, step 4a): the raw ㅇ score is the plain one.
        assertEquals(noAid(ieung) - TemplateMatcher.CORNER_PENALTY, scoreOf(rounded, ieung), 1e-6f)
        assertEquals("ko_m", TemplateMatcher.best(rounded, listOf(ieung, mieum))!!.first)
    }

    // --- other matcher rules ----------------------------------------------------------------------------

    @Test
    fun `best is null for an empty set or templates without strokes, ties go to the earlier template`() {
        val s = stroke(TestStrokes.points(TestStrokes.G))
        assertNull(TemplateMatcher.best(s, emptyList()))
        assertNull(TemplateMatcher.best(s, listOf(Template("none", TemplateOutput.Control("SPACE_V"), 1, emptyList()))))
        val first = template("first", 1, 0f to 0f, 1f to 0f, 1f to 1f)
        val second = template("second", 1, 0f to 0f, 1f to 0f, 1f to 1f)
        assertEquals("first", TemplateMatcher.best(s, listOf(first, second))!!.first)
        assertEquals("second", TemplateMatcher.best(s, listOf(second, first))!!.first)
    }

    @Test
    fun `an id scores its best alternative stroke`() {
        val s = stroke(TestStrokes.points(TestStrokes.G))
        val gOnly = template("g", 1, 0f to 0f, 1f to 0f, 1f to 1f)
        val withAlt = Template("g", TemplateOutput.Text("g"), 1, listOf(listOf(0f to 1f, 0.5f to 0f, 1f to 1f)) + gOnly.strokes)
        assertEquals(scoreOf(s, gOnly), scoreOf(s, withAlt), 0f)
    }

    @Test
    fun `each test template's corners field matches its own corner count`() {
        for ((name, list) in templates.sets) for (t in list) for (pts in t.strokes) {
            assertEquals("$name ${t.id}", t.corners, TemplateMatcher.cornerCount(TemplateMatcher.normalize(TemplateMatcher.resample(pts))))
        }
    }

    // --- TemplateLoader (LLD-05 §7) ---------------------------------------------------------------------

    private fun load(json: String) = TemplateLoader.load(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))

    private fun entry(id: String, type: String = "text", value: String = "x") =
        """{ "id": "$id", "output": { "type": "$type", "value": "$value" }, "corners": 1, "strokes": [ [[0,0],[1,1]] ] }"""

    @Test
    fun `loader reads every set of the test file`() {
        assertEquals(SetName.entries.toSet(), templates.sets.keys)
        assertEquals(listOf("ko_g", "ko_m", "ko_ieung", "ko_gg", "ko_dd", "ko_bb", "ko_ss", "ko_jj"), ko.map { it.id })
        assertEquals(TemplateOutput.Jamo(Jamo.GG), templates.outputOf("ko_gg"))
        assertEquals(TemplateOutput.Text("7"), templates.outputOf("num_7"))
        assertEquals(listOf(0f to 0f, 1f to 0f, 1f to 1f), ko.first().strokes.single())
    }

    @Test
    fun `loader reads the LLD-05 section 7 example`() {
        val set = load(
            """
            { "version": 1, "sets": {
                "KO_LETTER": [
                  { "id": "ko_g", "output": { "type": "jamo", "value": "ㄱ" }, "corners": 1, "strokes": [ [[0,0],[1,0],[1,1]] ] },
                  { "id": "ko_space_v", "output": { "type": "control", "value": "SPACE_V" }, "corners": 1, "strokes": [] }
                ],
                "EN_LETTER": [ { "id": "en_a", "output": { "type": "text", "value": "a" }, "corners": 1, "strokes": [] } ],
                "NUMBER": [], "PUNCT": [], "EXTENDED": [] } }
            """,
        )
        assertEquals(TemplateOutput.Jamo(Jamo.G), set.outputOf("ko_g"))
        assertEquals(TemplateOutput.Control("SPACE_V"), set.outputOf("ko_space_v"))
        assertEquals(TemplateOutput.Text("a"), set.outputOf("en_a"))
        assertTrue(set.forContext(PadArea.NUMBER, GraffitiLang.KO, Expect.NORMAL).isEmpty())
    }

    @Test
    fun `loader rejects version other than 1`() {
        assertThrows(IllegalStateException::class.java) { load("""{ "version": 2, "sets": {} }""") }
        assertThrows(IllegalStateException::class.java) { load("""{ "sets": {} }""") }
    }

    @Test
    fun `loader rejects a duplicate id within a set or across sets`() {
        assertThrows(IllegalStateException::class.java) {
            load("""{ "version": 1, "sets": { "KO_LETTER": [ ${entry("a")}, ${entry("a")} ] } }""")
        }
        assertThrows(IllegalStateException::class.java) {
            load("""{ "version": 1, "sets": { "PUNCT": [ ${entry("a")} ], "EXTENDED": [ ${entry("a")} ] } }""")
        }
    }

    @Test
    fun `loader rejects malformed content with IllegalStateException`() {
        val bad = listOf(
            "not json",
            """{ "version": 1, "sets": { "LETTERS": [] } }""",
            """{ "version": 1, "sets": { "KO_LETTER": [ ${entry("a", type = "jamo", value = "ㅏ")} ] } }""",
            """{ "version": 1, "sets": { "KO_LETTER": [ ${entry("a", type = "glyph")} ] } }""",
            """{ "version": 1, "sets": { "KO_LETTER": [ { "id": "a", "output": { "type": "text", "value": "a" }, "corners": 1, "strokes": [ [[0,0,0]] ] } ] } }""",
            """{ "version": 1, "sets": { "KO_LETTER": [ { "id": "a", "output": { "type": "text", "value": "a" }, "corners": "x", "strokes": [] } ] } }""",
        )
        for (json in bad) assertThrows(json, IllegalStateException::class.java) { load(json) }
    }

    @Test
    fun `loader rejects negative corners, coordinates outside 0 to 1 and unknown control values`() {
        fun one(corners: String = "1", stroke: String = "[[0,0],[1,1]]", type: String = "text", value: String = "a") =
            """{ "version": 1, "sets": { "KO_LETTER": [ { "id": "a", "output": { "type": "$type", "value": "$value" }, "corners": $corners, "strokes": [ $stroke ] } ] } }"""
        val bad = listOf(
            one(corners = "-1"),
            one(stroke = "[[0,0],[1.01,1]]"),
            one(stroke = "[[-0.01,0],[1,1]]"),
            one(stroke = "[[0,0],[1,1.5]]"),
            one(stroke = "[[0,-2],[1,1]]"),
            one(stroke = "[[NaN,0],[1,1]]"),
            one(stroke = "[[0,\"NaN\"],[1,1]]"),
            one(type = "control", value = "SPACE"),
            one(type = "control", value = "space_v"),
            one(type = "control", value = ""),
        )
        for (json in bad) assertThrows(json, IllegalStateException::class.java) { load(json) }
        // Boundaries stay valid: 0 corners, coordinates exactly 0 and 1, both control values.
        val ok = load(one(corners = "0", stroke = "[[0,1],[1,0]]"))
        assertEquals(0, ok.sets.getValue(SetName.KO_LETTER).single().corners)
        for (v in TemplateLoader.CONTROL_VALUES) {
            assertEquals(TemplateOutput.Control(v), load(one(type = "control", value = v)).outputOf("a"))
        }
    }
}
