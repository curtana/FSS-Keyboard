package kbd.app.practice

import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.app.store.Settings
import kbd.graffiti.guide.GuideCatalog
import kbd.graffiti.guide.GuideMark
import kbd.graffiti.guide.GuideSection
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.StrokeToken
import kbd.graffiti.recognize.TemplateSet
import kbd.stroke.layout.FunctionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * LLD-11 PR-20, PR-21, PR-24~PR-26 (FSS#17, FSS#72): the graffiti items of topic ③ written stroke by stroke on the real
 * graffiti engine (built-in templates, `GraffitiInputHandler`, the graffiti composer) leave the item's steps; a failed
 * recognition is ✗. Letters are drawn from the template's own points, so recognition does not depend on hand-drawn wobble.
 */
class PracticeGraffitiItemsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val templates: TemplateSet by lazy {
        TemplateLoader::class.java.getResourceAsStream("/graffiti/templates.json")!!.use { TemplateLoader.load(it) }
    }

    private fun input(lang: GraffitiLang): Pair<PracticeInput, (InkStroke) -> Unit> {
        val noFeedback = object : FeedbackPort {
            override fun key() = Unit
            override fun error() = Unit
        }
        val i = PracticeInput(PracticeBoard.GRAFFITI, noFeedback, ErrorLog(tmp.newFile()), Settings.DEFAULT)
        val h = i.graffiti(templates, lang, {}, {})
        return i to h::onStroke
    }

    /** Stroke [stroke] of template [id], scaled into a 100 dp box in the letter area. */
    private fun template(id: String, stroke: Int = 0): InkStroke {
        val t = templates.sets.values.flatten().single { it.id == id }
        val pts = t.strokes[stroke].mapIndexed { k, (x, y) -> InkPoint(20f + x * 100f, 20f + y * 100f, k * 8L) }
        return InkStroke(pts, PadArea.LETTER, LETTER)
    }

    /** Every drawing the stroke guide shows for the one-stroke letter [label] in [section] (its ① and ②). */
    private fun drawings(section: GuideSection, label: String): List<InkStroke> =
        GuideCatalog.groups.filter { it.section == section }.flatMap { it.cells }.filter { it.label == label }.map { cell ->
            when (val m = cell.marks.single()) {
                is GuideMark.Template -> template(m.id, m.stroke)
                is GuideMark.Rule -> {
                    assertEquals(label, StrokeToken.LineDown, m.token)   // I: the only letter drawn by a rule
                    line(0f, 80f)
                }
            }
        }.also { assertTrue("$label has a drawing", it.isNotEmpty()) }

    private fun line(dx: Float, dy: Float, area: PadArea = PadArea.LETTER): InkStroke {
        val x0 = if (area == PadArea.LETTER) 60f else 240f
        val pts = (0..30).map { k -> InkPoint(x0 + dx * k / 30, 30f + dy * k / 30, k * 8L) }
        return InkStroke(pts, area, if (area == PadArea.LETTER) LETTER else NUMBER)
    }

    private val tap = InkStroke(listOf(InkPoint(80f, 80f, 0), InkPoint(80.5f, 80.5f, 30), InkPoint(81f, 80.5f, 60)), PadArea.LETTER, LETTER)

    private fun judge(item: PracticeItem, i: PracticeInput) =
        PracticeItemJudge.commit(item, i.editor.text.dropLast(i.editor.composing.length), i.errored)

    /** Draws [strokes], nothing judged on the way (FSS#43); ✓ at once when the last stroke committed, else after the pad's space key. */
    private fun check(item: PracticeItem, vararg strokes: InkStroke) {
        val (i, draw) = input(checkNotNull(item.lang))
        val seen = ArrayList<String>()
        var v = Verdict.PENDING
        strokes.forEachIndexed { k, s ->
            assertEquals("${item.target} before stroke $k", Verdict.PENDING, v)
            i.errored = false
            draw(s)
            seen += i.editor.text
            v = judge(item, i)
        }
        assertEquals(item.target, item.steps, seen)
        if (v == Verdict.PENDING) {
            i.tapSink.onFunctionDown(FunctionKey.SPACE)
            v = judge(item, i)
        }
        assertEquals(item.target, Verdict.RIGHT, v)
    }

    private val graffitiItems get() = PracticeTopics.GRAFFITI_KO.items + PracticeTopics.GRAFFITI_EN.items

    private fun item(target: String) = graffitiItems.single { it.target == target }

    @Test
    fun `PR-20 graffiti items leave their steps`() {
        check(item("가"), template("ko_g"), line(0f, 80f), tap)
        check(item("고"), template("ko_g"), tap, line(80f, 0f))
        check(item("a"), template("en_a"))
        check(item("1"), line(0f, 80f, PadArea.NUMBER))
    }

    @Test
    fun `PR-24 every consonant, double consonant and letter is right with every drawing of its guide cell (FSS#72)`() {
        for (c in CONSONANTS) for (d in drawings(GuideSection.HANGUL, c)) check(item(c), d)
        for ((c, base) in DOUBLES) for (d in drawings(GuideSection.HANGUL, base)) check(item(c), line(0f, -80f), d)
        for (c in 'a'..'z') for (d in drawings(GuideSection.ENGLISH, "${c.uppercaseChar()}")) check(item("$c"), d)
    }

    @Test
    fun `PR-25 the graffiti topics cover every consonant and letter once, and their cells are in the guide (FSS#72)`() {
        assertEquals(CONSONANTS + DOUBLES.map { it.first } + listOf("가", "고"), PracticeTopics.GRAFFITI_KO.items.map { it.target })
        assertEquals(('a'..'z').map { "$it" } + "1", PracticeTopics.GRAFFITI_EN.items.map { it.target })
        for (it in graffitiItems) {
            val cells = GuideCatalog.groups.filter { g -> g.section == it.guideSection() }.flatMap { g -> g.cells }.map { c -> c.label }
            assertTrue(it.target, it.guideCells.isNotEmpty() && cells.containsAll(it.guideCells))
        }
    }

    @Test
    fun `PR-26 a wrong consonant is judged at the space, shift then a letter is not the lower case target (FSS#72)`() {
        val g = item("ㄱ")
        val (i, draw) = input(GraffitiLang.KO)
        draw(template("ko_n"))
        assertEquals(Verdict.PENDING, judge(g, i))                       // composing: no result yet (FSS#43)
        i.tapSink.onFunctionDown(FunctionKey.SPACE)
        assertEquals(Verdict.WRONG, judge(g, i))
        val (j, drawJ) = input(GraffitiLang.KO)                          // ㄲ without the shift stroke is ㄱ
        drawJ(template("ko_g"))
        j.tapSink.onFunctionDown(FunctionKey.SPACE)
        assertEquals(Verdict.WRONG, judge(item("ㄲ"), j))
        val a = item("a")
        val (k, drawK) = input(GraffitiLang.EN)
        drawK(line(0f, -80f))
        assertEquals(Verdict.PENDING, judge(a, k))
        drawK(template("en_a"))
        assertEquals("A", k.editor.text)
        assertEquals(Verdict.WRONG, judge(a, k))
    }

    @Test
    fun `PR-21 a stroke the recognizer cannot read is wrong`() {
        val it = item("a")
        val (i, draw) = input(GraffitiLang.EN)
        i.errored = false
        // A tight zig-zag matches no template.
        val pts = (0..40).map { k -> InkPoint(20f + k * 4f, if (k % 2 == 0) 20f else 120f, k * 8L) }
        draw(InkStroke(pts, PadArea.LETTER, LETTER))
        assertEquals(Verdict.WRONG, judge(it, i))
    }

    private companion object {
        val CONSONANTS = listOf("ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")
        val DOUBLES = listOf("ㄲ" to "ㄱ", "ㄸ" to "ㄷ", "ㅃ" to "ㅂ", "ㅆ" to "ㅅ", "ㅉ" to "ㅈ")
        val LETTER = RectF2(0f, 0f, 200f, 150f)
        val NUMBER = RectF2(200f, 0f, 300f, 150f)
    }
}
