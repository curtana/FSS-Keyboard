package kbd.app.graffiti

import java.io.File
import java.util.concurrent.Executor
import kbd.app.store.ErrorLog
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
import kbd.graffiti.recognize.PersonalStroke
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.TemplateSet
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** LLD-11 §7.9 RP-01..RP-07 (LLD-05 §11): the recognizer supply — model read once, rebuilt by revision, never failing. */
class GraffitiRecognizersTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val log by lazy { ErrorLog(File(tmp.root, "errors.log")) }

    private val templates: TemplateSet by lazy {
        checkNotNull(TemplateLoader::class.java.getResourceAsStream("/graffiti/templates.json")).use { TemplateLoader.load(it) }
    }

    /** Runs a build at once, on the calling thread. */
    private val now = Executor { it.run() }

    /** A circle in the letter area (ㅇ / o). */
    private val circle: InkStroke = run {
        val rect = RectF2(0f, 0f, 300f, 240f)
        InkStroke((0..90).map { k -> val a = Math.toRadians(-90.0 + 4.0 * k); InkPoint((150 + 70 * cos(a)).toFloat(), (120 + 70 * sin(a)).toFloat(), k * 8L) }, PadArea.LETTER, rect)
    }

    /** A zigzag nobody would teach by accident, taught as ㅎ. */
    private val zigzag: List<Pair<Float, Float>> = listOf(20f to 20f, 120f to 60f, 20f to 100f, 120f to 140f, 20f to 180f, 120f to 220f)
    private fun zigzagStroke() = InkStroke(zigzag.mapIndexed { i, (x, y) -> InkPoint(x, y, i * 10L) }, PadArea.LETTER, RectF2(0f, 0f, 300f, 240f))

    @Test
    fun `RP-01 the model is read once and the same recognizer is given while the revision stays`() {
        var reads = 0
        val shared = GraffitiRecognizers.sharedModel(log)
        val sup = GraffitiRecognizers(templates, log, model = { reads++; shared }, executor = now)
        val a = sup.get()
        val b = sup.get()
        sup.warm()
        assertSame(a, b)
        assertSame(a, sup.get())
        assertEquals(1, reads)
        val s = checkNotNull(a.score(circle, SetName.KO_LETTER))
        assertFalse("the network scores", s.network.isNaN())
        assertEquals("ㅇ", s.letter)
        assertEquals("", log.readAll())
    }

    @Test
    fun `RP-02 a model that cannot be read leaves the templates alone, logged`() {
        assertEquals(null, GraffitiRecognizers.readModel(log, "/graffiti/no-such-model.bin"))
        assertTrue(log.readAll(), log.readAll().startsWith(GraffitiRecognizers.SITE_MODEL + "\t"))
        val sup = GraffitiRecognizers(templates, log, model = { null }, executor = now)
        val s = checkNotNull(sup.get().score(circle, SetName.KO_LETTER))
        assertTrue("templates alone", s.network.isNaN())
        assertEquals("ㅇ", s.letter)
        assertTrue(s.accepted)
    }

    @Test
    fun `RP-03 a new revision rebuilds with the personal strokes, the old recognizer stays as it was`() {
        var rev = 0
        var personal = emptyList<PersonalStroke>()
        val shared = GraffitiRecognizers.sharedModel(log)
        val sup = GraffitiRecognizers(templates, log, revision = { rev }, personal = { personal }, model = { shared }, executor = now)
        val before = sup.get()
        val beforeScore = checkNotNull(before.score(zigzagStroke(), SetName.KO_LETTER))
        personal = listOf(PersonalStroke(SetName.KO_LETTER, "ㅎ", zigzag))
        assertSame("same revision, same recognizer", before, sup.get())
        rev = 1
        val after = sup.get()
        assertNotSame(before, after)
        val afterScore = checkNotNull(after.score(zigzagStroke(), SetName.KO_LETTER))
        assertEquals("ㅎ", afterScore.letter)
        assertTrue("template ${afterScore.template}", afterScore.template > 0.95f)
        assertEquals(beforeScore.letter, checkNotNull(before.score(zigzagStroke(), SetName.KO_LETTER)).letter)
        assertSame(after, sup.get())
    }

    @Test
    fun `RP-04 a model that does not fit the templates falls back to the templates alone, logged`() {
        val json = """{"version":1,"sets":{"KO_LETTER":[{"id":"ko_x","output":{"type":"jamo","value":"ㄲ"},"corners":1,"strokes":[[[0.0,0.0],[1.0,0.0],[1.0,1.0]]]}]}}"""
        val odd = TemplateLoader.load(json.byteInputStream())
        val shared = GraffitiRecognizers.sharedModel(log)
        val rec = GraffitiRecognizers(odd, log, model = { shared }, executor = now).get()
        assertTrue(log.readAll(), log.readAll().startsWith(GraffitiRecognizers.SITE_BUILD + "\tIllegalStateException"))
        val ink = InkStroke(listOf(InkPoint(10f, 10f, 0), InkPoint(110f, 10f, 10), InkPoint(110f, 110f, 20)), PadArea.LETTER, RectF2(0f, 0f, 300f, 240f))
        val s = checkNotNull(rec.score(ink, SetName.KO_LETTER))
        assertTrue(s.network.isNaN())
        assertEquals("ㄲ", s.letter)
    }

    @Test
    fun `RP-05 warm starts the build and does not wait, get waits for it`() {
        val queued = ArrayList<Runnable>()
        val shared = GraffitiRecognizers.sharedModel(log)
        val sup = GraffitiRecognizers(templates, log, model = { shared }, executor = { queued += it })
        sup.warm()
        sup.warm()
        assertEquals("one build for one revision", 1, queued.size)
        val worker = Thread { Thread.sleep(50); queued.single().run() }
        worker.start()
        val r = sup.get()   // waits for the worker
        worker.join()
        assertSame(r, sup.get())
        assertEquals(1, queued.size)
    }

    @Test
    fun `RP-06 personal strokes that cannot be read and a failing model leave a working recognizer`() {
        val sup = GraffitiRecognizers(templates, log, personal = { error("unreadable") }, model = { throw IllegalStateException("bad model") }, executor = now)
        val r = sup.get()
        val lines = log.readAll().lines().filter { it.isNotEmpty() }
        assertEquals(lines.toString(), 2, lines.size)
        assertTrue(lines.all { it.startsWith(GraffitiRecognizers.SITE_BUILD + "\t") })
        assertEquals("ㅇ", checkNotNull(r.score(circle, SetName.KO_LETTER)).letter)
    }

    @Test
    fun `RP-07 an Error past the build is dropped, the templates stand in and the next revision builds afresh`() {
        var rev = 0
        var models = 0
        var fail = true
        val shared = GraffitiRecognizers.sharedModel(log)
        val sup = GraffitiRecognizers(templates, log, revision = { rev }, model = { models++; if (fail) throw OutOfMemoryError("test") else shared }, executor = now)
        val r = sup.get()
        val s = checkNotNull(r.score(circle, SetName.KO_LETTER))
        assertTrue("templates alone", s.network.isNaN())
        assertEquals("ㅇ", s.letter)
        assertTrue(log.readAll(), log.readAll().startsWith(GraffitiRecognizers.SITE_BUILD + "	OutOfMemoryError"))
        assertSame("the failed build is not retried for the same revision", r, sup.get())
        assertEquals(1, models)
        fail = false
        rev = 1
        val next = sup.get()
        assertEquals("a fresh build, not one on the stand-in", 2, models)
        assertFalse("the network scores again", checkNotNull(next.score(circle, SetName.KO_LETTER)).network.isNaN())
    }
}
