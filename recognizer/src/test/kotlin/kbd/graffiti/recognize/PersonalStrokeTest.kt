package kbd.graffiti.recognize

import kbd.graffiti.eval.EvalStroke
import kbd.graffiti.eval.GraffitiEval
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §6.7 PS-01..PS-09 (SRS FR-GRF-14, HLD 11.1 나): personal strokes go into their letter on both sides, change
 * nothing else, and the two recorded cases of a taught stroke taking another letter that the repository's files can
 * rebuild are kept by the weighted sum. The third recorded case (ㅋ taught, ㄱ lost) is not here: the ㅋ strokes are
 * not in the repository.
 */
class PersonalStrokeTest {

    private val shipped = GraffitiEval.recognizer
    private val owner: Map<String, EvalStroke> by lazy { GraffitiEval.load("owner_en").values.flatten().associateBy { it.file.nameWithoutExtension } }

    private fun personal(name: String): PersonalStroke {
        val s = owner.getValue(name)
        return PersonalStroke(SetName.EN_LETTER, s.expect, s.stroke.points.map { it.x to it.y })
    }

    private fun right(rec: Recognizer, strokes: List<EvalStroke>) = strokes.count { GraffitiEval.label(rec.recognize(it.stroke, it.lang, it.shift), it) == it.expect }

    /** GR-33's English loops read as o (strictly: a D-like reading is a miss here). */
    private fun loopsRight(rec: Recognizer): Int =
        LoopStrokes.all(PadArea.LETTER).count { (_, s) -> (rec.recognize(s, GraffitiLang.EN, Expect.NORMAL) as? StrokeToken.Symbol)?.let { GraffitiEval.templates.outputOf(it.id) } == TemplateOutput.Text("o") }

    @Test
    fun `PS-01 recorded case - u taught, the device V strokes stay v`() {
        val v = GraffitiEval.load("device").getValue("en").filter { it.file.name.startsWith("en_v_") }
        assertEquals(10, v.size)
        val taught = listOf(personal("en_u_x1"), personal("en_u_x2"))
        val sumBefore = right(shipped, v)
        val sumAfter = right(shipped.withPersonal(taught), v)
        val aloneBefore = right(GraffitiEval.templateOnly, v)
        val aloneAfter = right(GraffitiEval.templateOnly.withPersonal(taught), v)
        println("PS-01 V of 10: templates alone $aloneBefore -> $aloneAfter, weighted sum $sumBefore -> $sumAfter")
        assertEquals(10, sumBefore)
        assertEquals(10, sumAfter)
        // The taught strokes themselves are u afterwards (they are what the user meant to teach).
        for (p in listOf("en_u_x1", "en_u_x2")) assertEquals(p, 1, right(shipped.withPersonal(taught), listOf(owner.getValue(p))))
    }

    @Test
    fun `PS-02 recorded case - d taught, the loops of GR-33 are not fewer than before`() {
        val taught = listOf("en_d_x1", "en_d_x3", "en_d_x4", "en_d_x7").map { personal(it) }
        val sumBefore = loopsRight(shipped)
        val sumAfter = loopsRight(shipped.withPersonal(taught))
        val aloneBefore = loopsRight(GraffitiEval.templateOnly)
        val aloneAfter = loopsRight(GraffitiEval.templateOnly.withPersonal(taught))
        println("PS-02 loops of ${LoopStrokes.all(PadArea.LETTER).size}: templates alone $aloneBefore -> $aloneAfter, weighted sum $sumBefore -> $sumAfter")
        assertEquals(LOOPS, LoopStrokes.all(PadArea.LETTER).size)
        assertTrue("before $sumBefore", sumBefore >= LOOPS_BEFORE_MIN)
        assertTrue("after $sumAfter, before $sumBefore", sumAfter >= sumBefore)
    }

    @Test
    fun `PS-03 no personal strokes is the shipped recognizer`() {
        val none = shipped.withPersonal(emptyList())
        val back = shipped.withPersonal(listOf(personal("en_u_x1"))).withPersonal(emptyList())
        for (split in listOf("device", "device_held")) for (s in GraffitiEval.load(split).values.flatten()) {
            val want = shipped.recognize(s.stroke, s.lang, s.shift)
            assertEquals(s.file.name, want, none.recognize(s.stroke, s.lang, s.shift))
            assertEquals(s.file.name, want, back.recognize(s.stroke, s.lang, s.shift))
        }
    }

    @Test
    fun `PS-04 a personal stroke changes its own set only`() {
        val taught = shipped.withPersonal(listOf(personal("en_u_x1"), personal("en_d_x1")))
        val eval = GraffitiEval.load("eval")
        for (folder in listOf("ko", "num", "punct", "ext")) for (s in eval.getValue(folder)) {
            // A pending stroke may fall back to its normal set: only the Korean and number-area ones never reach EN_LETTER.
            if (s.shift != Expect.NORMAL && s.lang == GraffitiLang.EN && s.stroke.area == PadArea.LETTER) continue
            assertEquals(s.file.name, shipped.recognize(s.stroke, s.lang, s.shift), taught.recognize(s.stroke, s.lang, s.shift))
        }
        for (set in SetName.entries - SetName.EN_LETTER) for (s in eval.getValue("en").take(40)) {
            val a = shipped.score(s.stroke, set)!!
            val b = taught.score(s.stroke, set)!!
            assertEquals("$set ${s.file.name}", a.letter, b.letter)
            assertEquals("$set ${s.file.name}", a.sum, b.sum, 0f)
        }
    }

    @Test
    fun `PS-05 a personal stroke raises both scores of its letter for that stroke`() {
        val s = owner.getValue("en_u_x1")
        val before = shipped.score(s.stroke, SetName.EN_LETTER)!!
        val after = shipped.withPersonal(listOf(personal("en_u_x1"))).score(s.stroke, SetName.EN_LETTER)!!
        println("PS-05 en_u_x1 before ${before.letter} t ${before.template} n ${before.network}, after ${after.letter} t ${after.template} n ${after.network}")
        assertEquals("u", after.letter)
        assertTrue(after.accepted)
        assertTrue("network ${after.network}", after.network > 0.9999f)   // its own exemplar
        // Its own template: stored as 32 resampled points and resampled again by the matcher, so not quite 1 (as for the built-in owner strokes).
        assertTrue("template ${after.template}", after.template > before.template && after.template > 0.97f)
        assertEquals(TemplateOutput.Text("u"), GraffitiEval.templates.outputOf(after.id))   // a built-in id of the letter, never the personal template's
        assertNotNull(GraffitiEval.templates.outputOf(after.id))
    }

    @Test
    fun `PS-06 strokes for other sets, unknown letters and empty strokes are left out`() {
        val pts = owner.getValue("en_u_x1").stroke.points.map { it.x to it.y }
        val odd = shipped.withPersonal(
            listOf(
                PersonalStroke(SetName.NUMBER, "3", pts), PersonalStroke(SetName.PUNCT, "?", pts), PersonalStroke(SetName.EXTENDED, "x", pts),
                PersonalStroke(SetName.EN_LETTER, "i", pts), PersonalStroke(SetName.EN_LETTER, "ㄱ", pts), PersonalStroke(SetName.KO_LETTER, "u", pts),
                PersonalStroke(SetName.EN_LETTER, "u", emptyList()),
            ),
        )
        val s = owner.getValue("en_u_x1").stroke
        for (set in SetName.entries) {
            val a = shipped.score(s, set)!!
            val b = odd.score(s, set)!!
            assertEquals("$set", a.letter, b.letter)
            assertEquals("$set", a.sum, b.sum, 0f)
        }
    }

    @Test
    fun `PS-07 the same strokes and personal strokes give the same result, in any recognizer built from them`() {
        val taught = listOf(personal("en_u_x1"), personal("en_d_x3"))
        val a = shipped.withPersonal(taught)
        val b = Recognizer(GraffitiEval.templates, GraffitiEval.model, taught.map { PersonalStroke(it.set, it.value, it.points.toList()) })
        for (s in GraffitiEval.load("device").values.flatten() + owner.values) {
            val x = a.score(s.stroke, SetName.EN_LETTER)
            val y = b.score(s.stroke, SetName.EN_LETTER)
            assertEquals(s.file.name, x?.letter, y?.letter)
            assertEquals(s.file.name, x?.sum, y?.sum)
            assertEquals(s.file.name, a.recognize(s.stroke, s.lang, s.shift), a.recognize(s.stroke, s.lang, s.shift))
        }
    }

    @Test
    fun `PS-08 the personal template is the stroke as a built-in owner template is stored`() {
        // en_a_o1 is both a file and a built-in template: the personal template of the file must be that template.
        val s = owner.getValue("en_a_o1")
        val built = GraffitiEval.templates.sets.getValue(SetName.EN_LETTER).single { it.id == "en_a_o1" }
        val mine = Recognizer.personalTemplate(personal("en_a_o1"), built.output)
        assertEquals(built.corners, mine.corners)
        assertEquals(TemplateMatcher.N, mine.strokes.single().size)
        for ((p, q) in mine.strokes.single().zip(built.strokes.single())) {
            assertEquals(q.first, p.first, STORED_TOL)
            assertEquals(q.second, p.second, STORED_TOL)
        }
        assertTrue(mine.strokes.single().all { (x, y) -> x in 0f..1f && y in 0f..1f })
        assertEquals(s.expect, Recognizer.valueOf(mine.output))
    }

    @Test
    fun `PS-09 the weighted sum takes the letter by both scores and returns its best built-in template`() {
        var checked = 0
        for (s in GraffitiEval.load("device").values.flatten() + GraffitiEval.load("device_held").values.flatten()) {
            val stroke: InkStroke = s.stroke
            if (StrokeRules.tap(stroke) != null || StrokeRules.line(stroke) != null) continue
            val set = TemplateSet.setNameFor(stroke.area, s.lang, Expect.NORMAL)
            val got = shipped.score(stroke, set)!!
            assertEquals(s.file.name, Recognizer.WEIGHT * got.template + (1f - Recognizer.WEIGHT) * got.network, got.sum, 0f)
            assertEquals(s.file.name, got.sum >= Recognizer.SUM_MIN && got.setTemplate >= Recognizer.TEMPLATE_MIN, got.accepted)
            assertTrue(s.file.name, got.setTemplate >= got.template)
            // The id is the letter's built-in template with the highest template score.
            val best = GraffitiEval.templates.sets.getValue(set).filter { it.strokes.isNotEmpty() && Recognizer.valueOf(it.output) == got.letter }
                .map { it.id to TemplateMatcher.best(stroke, listOf(it))!!.second }.maxByOrNull { it.second }!!
            assertEquals(s.file.name, best.second, got.template, 0f)
            assertEquals(s.file.name, got.template, TemplateMatcher.best(stroke, listOf(GraffitiEval.templates.sets.getValue(set).single { it.id == got.id }))!!.second, 0f)
            checked++
        }
        assertTrue("checked $checked", checked > 100)
    }

    private companion object {
        const val LOOPS = 432

        /** Measured 432 of 432 before teaching (LLD-11 §6.7); one D-like reading would still pass. */
        const val LOOPS_BEFORE_MIN = 431

        /** A stored coordinate is rounded to 3 decimals; the app resamples in 32-bit floats, the template builder in 64-bit. */
        const val STORED_TOL = 0.0011f
    }
}
