package kbd.app.store

import java.io.File
import java.util.concurrent.Executor
import kbd.app.graffiti.GraffitiRecognizers
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
import kbd.graffiti.recognize.Expect
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.TemplateLoader
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** LLD-11 §7.10 MS-01..MS-08 (LLD-07 §8.2, SRS FR-TUT-15·16·17): the personal stroke book, its file and who writes it. */
class PersonalStrokeStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val log by lazy { ErrorLog(File(tmp.root, "errors.log")) }
    private fun file() = File(tmp.root, PersonalStrokeStore.FILE_NAME)
    private fun stroke(k: Int) = listOf(k.toFloat() to 0f, k + 10f to 5f, k + 20f to 30f)

    @Test
    fun `MS-01 up to eight strokes a letter, the oldest goes first, letters apart`() {
        var b = PersonalStrokeBook.EMPTY
        for (k in 1..9) b = b.add(SetName.KO_LETTER, "ㄱ", stroke(k))
        b = b.add(SetName.EN_LETTER, "a", stroke(100))
        assertEquals(8, b.count(SetName.KO_LETTER, "ㄱ"))
        assertEquals((2..9).map { stroke(it) }, b.letters.getValue(SetName.KO_LETTER to "ㄱ").map { it.points })
        assertEquals(1, b.count(SetName.EN_LETTER, "a"))
        assertEquals(0, b.count(SetName.EN_LETTER, "b"))
        assertEquals(2, b.taughtLetters)
        assertEquals(9, b.strokes.size)
    }

    @Test
    fun `MS-02 clearing a letter and everything, the revision goes up with each write`() {
        val s = PersonalStrokeStore(file(), log)
        assertEquals(0, s.revision)
        assertTrue(s.add(SetName.KO_LETTER, "ㄱ", stroke(1)))
        assertTrue(s.add(SetName.EN_LETTER, "a", stroke(2)))
        assertEquals(2, s.revision)
        assertTrue(s.clear(SetName.KO_LETTER, "ㄱ"))
        assertEquals(0, s.book().count(SetName.KO_LETTER, "ㄱ"))
        assertEquals(1, s.book().count(SetName.EN_LETTER, "a"))
        assertTrue(s.clearAll())
        assertEquals(0, s.book().strokes.size)
        assertEquals(4, s.revision)
        // a fresh store reads what was written
        assertEquals(0, PersonalStrokeStore(file(), log).book().strokes.size)
        assertFalse(File(tmp.root, PersonalStrokeStore.FILE_NAME + PersonalStrokeStore.TMP_SUFFIX).exists())
    }

    @Test
    fun `MS-03 the file gives back the same 32-bit coordinates, in order`() {
        val pts = listOf(1e-5f to 123.456f, 0.1f to 1f / 3f, 4096.75f to -2.5f)
        val s = PersonalStrokeStore(file(), log)
        s.add(SetName.EN_LETTER, "a", pts)
        s.add(SetName.EN_LETTER, "a", stroke(7))
        s.add(SetName.KO_LETTER, "ㅎ", stroke(8))
        val back = PersonalStrokeStore(file(), log).book()
        assertEquals(listOf(pts, stroke(7)), back.letters.getValue(SetName.EN_LETTER to "a").map { it.points })
        assertEquals(listOf(stroke(8)), back.letters.getValue(SetName.KO_LETTER to "ㅎ").map { it.points })
        assertTrue(file().readText().startsWith("{\"version\":1,\"letters\":["))
    }

    @Test
    fun `MS-04 a file that cannot be read is an empty book, logged by site only, and left as it is`() {
        val bad = listOf(
            "not json", "[]", "{\"version\":2,\"letters\":[]}", "{\"letters\":[]}",
            "{\"version\":1,\"letters\":[{\"set\":\"EN_LETTER\",\"value\":\"a\"}]}",
            "{\"version\":1,\"letters\":[{\"set\":\"EN_LETTER\",\"value\":\"a\",\"strokes\":[[[1,2,3]]]}]}",
            "{\"version\":1,\"letters\":[{\"set\":\"EN_LETTER\",\"value\":\"a\",\"strokes\":[[[\"x\",2]]]}]}",
            "{\"version\":1,\"letters\":[{\"set\":\"EN_LETTER\",\"value\":\"a\",\"strokes\":[[[1e999,2]]]}]}",
        )
        for (text in bad) {
            try {
                PersonalStrokeCodec.decode(text)
                fail(text)
            } catch (_: IllegalStateException) {
            }
            file().writeText(text)
            File(tmp.root, "errors.log").delete()
            val s = PersonalStrokeStore(file(), log)
            assertEquals(text, 0, s.book().strokes.size)
            assertEquals(text, file().readText())
            val line = log.readAll()
            assertTrue(line, line.startsWith(PersonalStrokeStore.SITE_READ + "\t"))
            assertFalse(line, line.contains("EN_LETTER"))
        }
        // a missing file is an empty book with nothing logged
        file().delete()
        File(tmp.root, "errors.log").delete()
        assertEquals(0, PersonalStrokeStore(file(), log).book().strokes.size)
        assertEquals("", log.readAll())
    }

    @Test
    fun `MS-05 read but left out - other sets, unknown sets, empty strokes, strokes past eight`() {
        val nine = (1..9).joinToString(",") { "[[$it,0],[${it + 5},9]]" }
        val text = """{"version":1,"letters":[
            {"set":"NUMBER","value":"1","strokes":[[[0,0],[1,9]]]},
            {"set":"NO_SUCH_SET","value":"x","strokes":[[[0,0],[1,9]]]},
            {"set":"EN_LETTER","value":"b","strokes":[[]]},
            {"set":"KO_LETTER","value":"ㄱ","strokes":[$nine]},
            {"set":"EN_LETTER","value":"ünknown","strokes":[[[0,0],[1,9]]]}]}"""
        val b = PersonalStrokeCodec.decode(text)
        assertEquals(setOf(SetName.KO_LETTER to "ㄱ", SetName.EN_LETTER to "ünknown"), b.letters.keys)
        assertEquals((2..9).map { listOf(it.toFloat() to 0f, it + 5f to 9f) }, b.letters.getValue(SetName.KO_LETTER to "ㄱ").map { it.points })
    }

    @Test
    fun `MS-06 a write that fails changes nothing and says so`() {
        val dir = File(tmp.root, "gone")
        val s = PersonalStrokeStore(File(dir, PersonalStrokeStore.FILE_NAME), log)   // the folder does not exist
        assertFalse(s.add(SetName.KO_LETTER, "ㄱ", stroke(1)))
        assertEquals(0, s.revision)
        assertEquals(0, s.book().strokes.size)
        assertTrue(log.readAll().startsWith(PersonalStrokeStore.SITE_WRITE + "\t"))
        assertFalse(dir.exists())
    }

    @Test
    fun `MS-07 only the my strokes screen writes personal strokes`() {
        val root = File("src/main/kotlin")
        assertTrue(root.absolutePath, root.isDirectory)
        val writes = Regex("""\b(store|personal\w*)\.(add|clear|clearAll)\(""")
        val writers = root.walk().filter { it.isFile && it.extension == "kt" && writes.containsMatchIn(it.readText()) }.map { it.name }.toSet()
        assertEquals(setOf("MyStrokes.kt"), writers)
    }

    @Test
    fun `MS-08 recognizing with the personal strokes writes nothing`() {
        val s = PersonalStrokeStore(file(), log)
        s.add(SetName.KO_LETTER, "ㄱ", stroke(1))
        val before = file().readBytes()
        val templates = checkNotNull(TemplateLoader::class.java.getResourceAsStream("/graffiti/templates.json")).use { TemplateLoader.load(it) }
        val sup = GraffitiRecognizers(templates, log, { s.revision }, { s.book().strokes }, executor = Executor { it.run() })
        val ink = InkStroke(stroke(1).mapIndexed { i, (x, y) -> InkPoint(x, y, i * 10L) }, PadArea.LETTER, RectF2(0f, 0f, 300f, 240f))
        repeat(3) { sup.get().recognize(ink, GraffitiLang.KO, Expect.NORMAL) }
        assertEquals(1, s.revision)
        assertArrayEquals(before, file().readBytes())
    }
}
