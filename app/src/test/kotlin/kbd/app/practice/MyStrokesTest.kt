package kbd.app.practice

import java.io.File
import java.util.concurrent.Executor
import kbd.app.graffiti.GraffitiRecognizers
import kbd.app.store.ErrorLog
import kbd.app.store.PersonalStrokeStore
import kbd.graffiti.guide.GuideMark
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.TemplateSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** LLD-11 §7.10 MY-01..MY-04 (LLD-07 §8.1, §8.3 actions; SRS FR-TUT-14·16): letters, what is stored, the screen's model. */
class MyStrokesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val templates: TemplateSet by lazy {
        checkNotNull(TemplateLoader::class.java.getResourceAsStream("/graffiti/templates.json")).use { TemplateLoader.load(it) }
    }
    private val letterRect = RectF2(0f, 0f, 300f, 240f)

    private fun ink(pts: List<Pair<Float, Float>>, area: PadArea = PadArea.LETTER) =
        InkStroke(pts.mapIndexed { i, (x, y) -> InkPoint(x, y, i * 10L) }, area, if (area == PadArea.LETTER) letterRect else RectF2(300f, 0f, 450f, 240f))

    /** Template [id]'s first stroke in a 100 dp box, a point every 2 dp along it. */
    private fun drawn(id: String): InkStroke {
        val s = templates.sets.values.flatten().single { it.id == id }.strokes[0].map { (x, y) -> 20f + x * 100f to 20f + y * 100f }
        val pts = ArrayList<Pair<Float, Float>>()
        for (i in 1 until s.size) {
            val (ax, ay) = s[i - 1]
            val (bx, by) = s[i]
            val n = maxOf(1, (Math.hypot((bx - ax).toDouble(), (by - ay).toDouble()) / 2).toInt())
            for (k in 0 until n) pts += ax + (bx - ax) * k / n to ay + (by - ay) * k / n
        }
        pts += s.last()
        return ink(pts)
    }

    @Test
    fun `MY-01 the letters are the 14 Korean consonants then the 25 English letters the templates draw`() {
        val l = MyStrokes.letters(templates)
        assertEquals("ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎ", l.filter { it.set == SetName.KO_LETTER }.joinToString("") { it.label })
        assertEquals("ABCDEFGHJKLMNOPQRSTUVWXYZ", l.filter { it.set == SetName.EN_LETTER }.joinToString("") { it.label })
        assertEquals(39, l.size)
        assertEquals(SetName.KO_LETTER, l.first().set)
        assertEquals("a", l.first { it.label == "A" }.value)
        assertEquals(emptyList<MyLetter>(), MyStrokes.letters(TemplateSet(emptyMap())))
    }

    @Test
    fun `MY-02 taps, straight lines and strokes begun in the number area are not stored, anything else is`() {
        assertEquals(MyStrokeVerdict.RULE_STROKE, MyStrokes.verdict(ink(listOf(50f to 50f, 51f to 51f))))
        assertEquals(MyStrokeVerdict.RULE_STROKE, MyStrokes.verdict(ink((0..20).map { 50f to 20f + it * 8f })))
        assertEquals(MyStrokeVerdict.RULE_STROKE, MyStrokes.verdict(ink((0..20).map { 20f + it * 8f to 100f })))
        assertEquals(MyStrokeVerdict.NUMBER_AREA, MyStrokes.verdict(ink(listOf(320f to 20f, 380f to 20f, 380f to 100f), PadArea.NUMBER)))
        assertEquals(MyStrokeVerdict.SAVE, MyStrokes.verdict(drawn("ko_g")))
        assertEquals(MyStrokeVerdict.SAVE, MyStrokes.verdict(drawn("ko_m")))
    }

    @Test
    fun `MY-03 the guide cells of a letter are its own, both cells where it has two`() {
        val m = MyStrokes.letters(templates).first { it.label == "ㅁ" }
        val cells = MyStrokes.guideGroups(templates, m).flatMap { it.cells }
        assertTrue(cells.size >= 2)
        for (c in cells) {
            val mark = c.marks.single() as GuideMark.Template
            assertEquals("ㅁ", templates.outputOf(mark.id)?.let(kbd.graffiti.recognize.Recognizer::valueOf))
        }
    }

    @Test
    fun `MY-04 the screen's model - read before storing, store, relearn, reset, clear everything`() {
        val log = ErrorLog(File(tmp.root, "e.log"))
        val store = PersonalStrokeStore(File(tmp.root, PersonalStrokeStore.FILE_NAME), log)
        val sup = GraffitiRecognizers(templates, log, { store.revision }, { store.book().strokes }, executor = Executor { it.run() })
        val my = MyStrokesModel(templates, store, sup)
        assertEquals(MyStrokesModel.Screen.CLOSED, my.screen)
        my.open()
        assertEquals(MyStrokesModel.Screen.LETTERS, my.screen)
        val g = my.letters.first { it.label == "ㄱ" }
        my.pick(g)
        assertEquals(MyStrokesModel.Screen.WRITE, my.screen)

        my.onStroke(drawn("ko_g"))
        assertEquals(MyStrokeMessage.Saved("ㄱ"), my.message)
        assertEquals(1, my.count(g))
        my.onStroke(ink(listOf(50f to 50f, 51f to 51f)))
        assertEquals(MyStrokeMessage.RuleStroke, my.message)
        my.onStroke(ink(listOf(320f to 20f, 380f to 20f, 380f to 100f), PadArea.NUMBER))
        assertEquals(MyStrokeMessage.NumberArea, my.message)
        assertEquals(1, my.count(g))
        // a zigzag that is no ㄱ: stored all the same (no collision check, TBD-30 ⑥)
        my.onStroke(ink(listOf(20f to 20f, 120f to 60f, 20f to 100f, 120f to 140f, 20f to 180f, 120f to 220f)))
        assertTrue(my.message is MyStrokeMessage.Saved)
        assertEquals(2, my.count(g))
        assertEquals(1, my.book.taughtLetters)
        assertEquals(2, store.book().strokes.size)

        my.relearn()
        assertEquals(MyStrokeMessage.Cleared, my.message)
        assertEquals(0, my.count(g))
        assertEquals(MyStrokesModel.Screen.WRITE, my.screen)

        my.onStroke(drawn("ko_g"))
        my.reset()
        assertEquals(MyStrokesModel.Screen.LETTERS, my.screen)
        assertNull(my.letter)
        assertEquals(0, my.count(g))

        my.pick(g)
        my.onStroke(drawn("ko_g"))
        my.backToLetters()
        my.askClearAll()
        assertTrue(my.askingClearAll)
        my.answerClearAll(false)
        assertEquals(1, my.count(g))
        my.askClearAll()
        my.answerClearAll(true)
        assertEquals(0, my.book.taughtLetters)
        my.close()
        assertEquals(MyStrokesModel.Screen.CLOSED, my.screen)
    }
}
