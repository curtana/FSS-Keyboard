package kbd.app.practice

import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.app.store.Settings
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.TemplateSet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * LLD-11 PR-20, PR-21 (FSS#17): the graffiti items of topic ③ written stroke by stroke on the real graffiti engine
 * (built-in templates, `GraffitiInputHandler`, the graffiti composer) leave the item's steps; a failed recognition is ✗.
 * Letters are drawn from the template's own points, so recognition does not depend on hand-drawn wobble.
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

    /** Stroke 0 of template [id], scaled into a 100 dp box in the letter area. */
    private fun template(id: String): InkStroke {
        val t = templates.sets.values.flatten().single { it.id == id }
        val pts = t.strokes[0].mapIndexed { k, (x, y) -> InkPoint(20f + x * 100f, 20f + y * 100f, k * 8L) }
        return InkStroke(pts, PadArea.LETTER, LETTER)
    }

    private fun line(dx: Float, dy: Float, area: PadArea = PadArea.LETTER): InkStroke {
        val x0 = if (area == PadArea.LETTER) 60f else 240f
        val pts = (0..30).map { k -> InkPoint(x0 + dx * k / 30, 30f + dy * k / 30, k * 8L) }
        return InkStroke(pts, area, if (area == PadArea.LETTER) LETTER else NUMBER)
    }

    private val tap = InkStroke(listOf(InkPoint(80f, 80f, 0), InkPoint(80.5f, 80.5f, 30), InkPoint(81f, 80.5f, 60)), PadArea.LETTER, LETTER)

    private fun check(item: PracticeItem, vararg strokes: InkStroke) {
        val (i, draw) = input(checkNotNull(item.lang))
        val seen = ArrayList<String>()
        var v = Verdict.PENDING
        strokes.forEachIndexed { k, s ->
            i.errored = false
            draw(s)
            seen += i.editor.text
            v = PracticeItemJudge.step(item, k, i.editor.text, i.errored)
        }
        assertEquals(item.target, item.steps, seen)
        assertEquals(item.target, Verdict.RIGHT, v)
    }

    private fun item(target: String) = PracticeTopics.GRAFFITI.items.single { it.target == target }

    @Test
    fun `PR-20 graffiti items leave their steps`() {
        check(item("가"), template("ko_g"), line(0f, 80f), tap)
        check(item("고"), template("ko_g"), tap, line(80f, 0f))
        check(item("a"), template("en_a"))
        check(item("1"), line(0f, 80f, PadArea.NUMBER))
    }

    @Test
    fun `PR-21 a stroke the recognizer cannot read is wrong`() {
        val it = item("a")
        val (i, draw) = input(GraffitiLang.EN)
        i.errored = false
        // A tight zig-zag matches no template.
        val pts = (0..40).map { k -> InkPoint(20f + k * 4f, if (k % 2 == 0) 20f else 120f, k * 8L) }
        draw(InkStroke(pts, PadArea.LETTER, LETTER))
        assertEquals(Verdict.WRONG, PracticeItemJudge.step(it, 0, i.editor.text, i.errored))
    }

    private companion object {
        val LETTER = RectF2(0f, 0f, 200f, 150f)
        val NUMBER = RectF2(200f, 0f, 300f, 150f)
    }
}
