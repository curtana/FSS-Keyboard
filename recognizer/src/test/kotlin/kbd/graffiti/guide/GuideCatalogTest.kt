package kbd.graffiti.guide

import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.StrokeToken
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.TemplateOutput
import kbd.graffiti.recognize.TemplateSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The stroke guide catalog against the production templates (LLD-11 §6.6 GC-01~GC-08, LLD-05 §10.1, SRS FR-GRF-09b·09c). */
class GuideCatalogTest {

    /** The owner's own strokes (FSS#64): recognition only, the guide shows the drawn templates. */
    private val OWNER_ID = Regex("ko_.+_o[0-9]+")

    private val templates: TemplateSet =
        GuideCatalogTest::class.java.getResourceAsStream("/graffiti/templates.json")!!.use { TemplateLoader.load(it) }

    private val groups = GuideCatalog.groups
    private val cells = groups.flatMap { it.cells }
    private fun section(s: GuideSection) = groups.filter { it.section == s }.flatMap { it.cells }

    /** `TemplateLoader`'s output text of a template, as the cell label shows it. */
    private fun label(set: SetName, o: TemplateOutput): String = when (o) {
        is TemplateOutput.Jamo -> o.value.compat.toString()
        is TemplateOutput.Text -> when {
            o.value == "\t" -> GuideCatalog.TAB_LABEL
            set == SetName.EN_LETTER -> o.value.uppercase()   // names are capitals (FR-GRF-09b)
            else -> o.value
        }
        is TemplateOutput.Control -> o.value
    }

    @Test
    fun `GC-01 sections come in the fixed order Hangul, English, number, control, symbol`() {
        assertEquals(
            listOf(GuideSection.HANGUL, GuideSection.ENGLISH, GuideSection.NUMBER, GuideSection.CONTROL, GuideSection.SYMBOL),
            groups.map { it.section }.distinct(),
        )
        // Each section is one run: no section comes back after another one.
        assertEquals(groups.map { it.section }, groups.map { it.section }.sortedBy { it.ordinal })
        assertEquals(
            listOf("한글 · 문자 영역", "한글 · 모음 입력 순서", "영문 · 문자 영역", "숫자 영역", "제어 획", "기호 · 구두점 (탭 뒤)", "기호 · 확장 (↘ 뒤)"),
            groups.map { it.title },
        )
    }

    @Test
    fun `GC-02 cell counts per section`() {
        val ko = groups[0].cells
        assertEquals(20, ko.size)   // 14 consonants + ㅁ②, ㅎ② + shift + consonant + 3 elements
        assertEquals("ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎ".toList().map { "$it" }, ko.take(16).filter { it.alt != 2 }.map { it.label })
        assertEquals("시프트 + 자음", ko[16].note)
        assertEquals(listOf("ㅣ", "ㅡ", "ㆍ"), ko.takeLast(3).map { it.label })
        assertEquals(21, groups[1].cells.size)   // GRAFFITI 4.2 vowel table
        assertTrue(groups[1].cells.all { it.marks.isEmpty() && it.note != null })
        val en = section(GuideSection.ENGLISH)
        assertEquals(('A'..'Z').map { "$it" }, en.map { it.label }.distinct())
        assertEquals(35, en.size)   // 26 + ② of B D F G M O V X Y (P, R show their ② only)
        val numbers = section(GuideSection.NUMBER)
        assertEquals(('0'..'9').map { "$it" }, numbers.map { it.label }.distinct())
        assertEquals(12, numbers.size)   // 10 + 0 ②, 5 ② (8 ② has no template)
        assertEquals(12, section(GuideSection.CONTROL).size)
        val symbols = section(GuideSection.SYMBOL)
        assertEquals(57, symbols.size)   // 33 punctuation + 24 extended (TBD-26)
        assertEquals(33, groups[5].cells.size)
        assertEquals(24, groups[6].cells.size)
    }

    @Test
    fun `GC-03 every mark resolves to a real template stroke or a rule shape`() {
        for (c in cells) for (m in c.marks) {
            assertNotNull("${c.label} $m", GuideCatalog.shape(m, templates))
            if (m is GuideMark.Template) {
                val t = templates.sets[m.set].orEmpty().single { it.id == m.id }
                assertNull("${m.id} is a rule entry", t.rule)
            }
        }
    }

    @Test
    fun `GC-04 a template cell shows the output of its template`() {
        for (g in groups) for (c in g.cells) {
            if (g.section == GuideSection.CONTROL) continue
            val t = c.marks.singleOrNull() as? GuideMark.Template ?: continue   // "shift + consonant" is two marks
            assertEquals(t.id, c.label, label(t.set, templates.outputOf(t.id)!!))
        }
        val control = section(GuideSection.CONTROL).flatMap { it.marks }.filterIsInstance<GuideMark.Template>().map { it.id }
        assertEquals(listOf("ko_space_v", "ko_finish_j"), control)
    }

    @Test
    fun `GC-05 symbol cells drawn as a rule are the symbol table's rule entries`() {
        fun ruleCells(group: GuideGroup) =
            group.cells.filter { it.marks.singleOrNull() is GuideMark.Rule }.associate { (it.marks.single() as GuideMark.Rule).token to it.label }
        fun ruleEntries(set: SetName) = templates.sets[set].orEmpty().filter { it.rule != null }
            .associate { it.rule!! to (it.output as TemplateOutput.Text).value }
        assertEquals(ruleEntries(SetName.PUNCT), ruleCells(groups[5]))
        assertEquals(ruleEntries(SetName.EXTENDED), ruleCells(groups[6]))
    }

    @Test
    fun `GC-06 every template is in a cell except the recognition-only variants`() {
        val used = cells.flatMap { it.marks }.filterIsInstance<GuideMark.Template>().map { it.id }.toSet()
        val all = templates.sets.values.flatten().filter { it.rule == null }.map { it.id }.toSet()
        assertEquals(setOf("ko_d2", "ko_b2"), (all - used).filterNot { OWNER_ID.matches(it) }.toSet())
        assertTrue(used.all { it in all })
    }

    @Test
    fun `GC-07 interim cells are exactly those on the TEMPLATES_md interim templates`() {
        val md = GuideCatalogTest::class.java.getResourceAsStream("/strokes/TEMPLATES.md")!!.bufferedReader(Charsets.UTF_8).readLines()
        val interimIds = md.filter { it.startsWith("| ") }.mapNotNull { line ->
            val cols = line.split(" | ")
            val id = Regex("`([a-z0-9_]+)`").find(cols.getOrNull(1) ?: "")?.groupValues?.get(1) ?: return@mapNotNull null
            if (cols.getOrNull(6)?.trim() == "yes") id else null
        }.toSet()
        assertEquals(setOf("en_b", "en_d", "en_g", "en_h", "en_o", "en_x", "num_0", "num_8", "x_degree", "x_euro"), interimIds)
        for (c in cells) {
            val ids = c.marks.filterIsInstance<GuideMark.Template>().map { it.id }
            assertEquals(c.label + " " + ids, ids.any { it in interimIds }, c.interim)
        }
    }

    @Test
    fun `GC-08 unsettled symbols have no drawing and say so, every other cell but the vowel order draws`() {
        val unsettled = cells.filter { it.marks.isEmpty() && it.note == GuideCatalog.UNSETTLED }.map { it.label }
        assertEquals(listOf("_", "|", "`", ";", ":", "∫", "Ø"), unsettled)
        val textOnly = cells.filter { it.marks.isEmpty() }
        assertEquals(unsettled.size + 21, textOnly.size)
    }

    @Test
    fun `GC-09 rule shapes start where the rule stroke starts, and a missing template has no shape`() {
        val down = GuideCatalog.shape(GuideMark.Rule(StrokeToken.LineDown), templates) as GuideShape.Line
        assertEquals(listOf(0.5f to 0f, 0.5f to 1f), down.points)
        val ret = GuideCatalog.shape(GuideMark.Rule(StrokeToken.DiagDownLeft), templates) as GuideShape.Line
        assertEquals(1f to 0f, ret.points.first())
        assertEquals(GuideShape.Tap, GuideCatalog.shape(GuideMark.Rule(StrokeToken.Tap), templates))
        assertEquals(GuideShape.CornerTap, GuideCatalog.shape(GuideMark.Rule(StrokeToken.CornerTap), templates))
        val g = GuideMark.Template(SetName.KO_LETTER, "ko_g")
        assertNull(GuideCatalog.shape(g, TemplateSet(emptyMap())))   // load failure (LLD-09 §4): text only
        assertNull(GuideCatalog.shape(g.copy(stroke = 5), templates))
        val first = (GuideCatalog.shape(g, templates) as GuideShape.Line).points.first()
        assertEquals(templates.sets[SetName.KO_LETTER]!!.first { it.id == "ko_g" }.strokes[0][0], first)
    }
}
