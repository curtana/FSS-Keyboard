package kbd.graffiti.eval

import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.StrokeRules
import kbd.graffiti.recognize.TemplateMatcher
import kbd.graffiti.recognize.TemplateOutput
import kbd.graffiti.recognize.TemplateSet
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GR-36 (FSS#47): real tablet strokes in `strokes/device/` must all be recognized. They are raw touches
 * read off the device while "갸" (ㄱ, ㅣ, ㆍ, ㆍ) was written over and over: 84 strokes, 23 of them ㅣ. The long
 * ㅣ strokes end in a short move back up as the finger lifts; seven ㅣ failed before the lift-off tail rule. FSS#52 added ㄴ and ㅂ strokes (`ko_n_*`, `ko_b_*`), FSS#70 the phone's Q and V strokes (`en/`).
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

    /** `strokes/owner_en/`: a file named `en_<letter>_oN` is the template of that id. `en_i_rN` is a down line, a rule. `en_<letter>_xN` was left out of the templates because it took other strokes (FR-GRF-03b). */
    private fun ownerDays(): Map<String, List<EvalStroke>> = GraffitiEval.load("owner_en")

    private fun EvalStroke.id() = file.nameWithoutExtension

    /** The production templates without the strokes of [day], so that day's strokes are measured as ones the set has not seen (FR-GRF-03b). */
    private fun withoutDay(day: List<EvalStroke>): TemplateSet {
        val ids = day.map { it.id() }.toSet()
        return TemplateSet(GraffitiEval.templates.sets.mapValues { (_, list) -> list.filter { it.id !in ids } })
    }

    /** FSS#80: both days of the owner's letters are templates, so each day is read with the other day's strokes only. */
    @Test
    fun `GR-41 owner English strokes of one day are read with only the other day in the templates`() {
        val days = ownerDays()
        assertEquals(setOf("day1", "day2"), days.keys)
        for ((day, list) in days) {
            val s = GraffitiEval.score(day, list, withoutDay(list))
            println("$s misses ${s.misses}")
            assertTrue("$s misses ${s.misses}", s.accuracy >= HELD_OUT_MIN)
        }
    }

    /** FSS#80: the owner saw h misread on the phone (h -> l, h -> v). */
    @Test
    fun `GR-42 owner h strokes of one day are h with only the other day in the templates`() {
        var total = 0
        for ((day, list) in ownerDays()) {
            val h = list.filter { it.expect == "h" }
            val s = GraffitiEval.score("$day h", h, withoutDay(list))
            println(s)
            assertEquals("${s.name} misses ${s.misses}", s.total, s.correct)
            total += s.total
        }
        assertEquals(8, total)
    }

    /** GR-41 means nothing if a file does not match the template of its name: file and template must be the same stroke. */
    @Test
    fun `GR-43 owner English files and the EN_LETTER owner templates are the same strokes`() {
        val all = ownerDays().values.flatten()
        assertEquals(all.size, all.map { it.id() }.toSet().size)
        val rules = all.filter { StrokeRules.tap(it.stroke) != null || StrokeRules.line(it.stroke) != null }
        assertEquals(rules.map { it.id() }, all.filter { it.expect == "i" }.map { it.id() })
        val owned = all.filter { Regex("en_[a-z]_o[0-9]+").matches(it.id()) }
        assertEquals(setOf("en_d_x1", "en_d_x3", "en_d_x4", "en_d_x7", "en_u_x1", "en_u_x2"), (all - rules.toSet() - owned.toSet()).map { it.id() }.toSet())
        val templates = GraffitiEval.templates.sets.getValue(SetName.EN_LETTER).associateBy { it.id }
        assertEquals(
            owned.map { it.id() }.toSet(),
            templates.keys.filter { Regex("en_[a-z]_o[0-9]+").matches(it) }.toSet(),
        )
        for (s in owned) {
            val t = templates.getValue(s.id())
            assertEquals(s.id(), TemplateOutput.Text(s.expect), t.output)
            val file = TemplateMatcher.normalize(TemplateMatcher.resample(s.stroke.points.map { it.x to it.y }))
            val stored = TemplateMatcher.normalize(t.strokes.single())
            val far = file.indices.maxOf { i -> hypot(file[i].first - stored[i].first, file[i].second - stored[i].second) }
            assertTrue("${s.id()} differs from its template by $far", far < SAME_STROKE_MAX)
        }
    }

    private companion object {
        const val HELD_OUT_MIN = 95.0

        /** In the matcher's normalized box (long side 1): the template is the file's stroke, resampled like the matcher and rounded to 3 decimals. */
        const val SAME_STROKE_MAX = 0.01f
    }
}
