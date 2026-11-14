package kbd.graffiti.nn

import java.io.ByteArrayInputStream
import java.io.File
import kbd.graffiti.eval.GraffitiEval
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.Template
import kbd.graffiti.recognize.TemplateOutput
import kbd.graffiti.recognize.TemplateSet
import kotlin.math.sqrt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §6.7 GM-01..GM-08 (SRS FR-GRF-13, HLD 11.1 "모델 자산", 10.1 R17): the shipped model file fits the
 * production templates — a body per set, the same letters, 32 synthetic exemplars a letter plus the owner strokes
 * that are built-in templates — and the loader refuses a damaged file.
 */
class GraffitiModelTest {

    private val model = GraffitiEval.model
    private val bytes = GraffitiModelTest::class.java.getResourceAsStream(GraffitiModelLoader.RESOURCE)!!.use { it.readBytes() }

    private fun letters(set: SetName) = GraffitiEval.templates.sets.getValue(set).filter { it.strokes.isNotEmpty() }.map { Recognizer.valueOf(it.output) }.distinct()

    private fun embed(set: SetName, points: List<Pair<Double, Double>>): FloatArray =
        model.sets.getValue(set).body.embed(StrokeFeatures.compute(DoubleArray(points.size) { points[it].first }, DoubleArray(points.size) { points[it].second }))

    /** The best cosine of [z] with the exemplars of letter [value] in [set]. */
    private fun bestCosine(set: SetName, value: String, z: FloatArray): Float {
        val ms = model.sets.getValue(set)
        val c = ms.classes.indexOf(value)
        var best = -1f
        for (e in ms.exemplarClass.indices) {
            if (ms.exemplarClass[e] != c) continue
            var dot = 0f
            for (k in z.indices) dot += ms.exemplars[e * z.size + k] * z[k]
            if (dot > best) best = dot
        }
        return best
    }

    @Test
    fun `GM-01 five sets, each with a body of its own`() {
        assertEquals(SetName.entries.toSet(), model.sets.keys)
        assertEquals(5, model.bodyCount)
        assertEquals(5, model.sets.values.map { it.bodyIndex }.toSet().size)
        assertEquals(5, model.sets.values.map { System.identityHashCode(it.body) }.toSet().size)
    }

    @Test
    fun `GM-02 the letters of each model set are the letters of the template set`() {
        for (set in SetName.entries) assertEquals("$set", letters(set), model.sets.getValue(set).classes)
        assertEquals(listOf(25, 16, 9), listOf(SetName.EN_LETTER, SetName.KO_LETTER, SetName.NUMBER).map { model.sets.getValue(it).classes.size })
        // The recognizer refuses templates with a letter the model does not have (R17).
        val extra = Template("en_new", TemplateOutput.Text("i"), 0, listOf(listOf(0f to 0f, 1f to 1f, 0f to 1f)))
        val sets = GraffitiEval.templates.sets.toMutableMap().also { it[SetName.EN_LETTER] = it.getValue(SetName.EN_LETTER) + extra }
        assertThrows(IllegalStateException::class.java) { Recognizer(TemplateSet(sets), model) }
    }

    @Test
    fun `GM-03 32 synthetic exemplars a letter, plus the owner strokes - 156 English and 117 Korean`() {
        for ((set, owner) in listOf(SetName.EN_LETTER to 156, SetName.KO_LETTER to 117, SetName.NUMBER to 0, SetName.PUNCT to 0, SetName.EXTENDED to 0)) {
            val ms = model.sets.getValue(set)
            val per = IntArray(ms.classes.size)
            for (c in ms.exemplarClass) per[c]++
            assertTrue("$set", per.all { it >= SYNTHETIC_PER_LETTER })
            assertEquals("$set", ms.classes.size * SYNTHETIC_PER_LETTER + owner, ms.exemplarClass.size)
            // As many extra exemplars of a letter as the letter has owner templates.
            val templates = GraffitiEval.templates.sets.getValue(set).filter { OWNER_ID.matches(it.id) }.groupingBy { Recognizer.valueOf(it.output) }.eachCount()
            for ((i, v) in ms.classes.withIndex()) assertEquals("$set $v", templates[v] ?: 0, per[i] - SYNTHETIC_PER_LETTER)
        }
    }

    @Test
    fun `GM-04 every exemplar has unit length`() {
        for ((set, ms) in model.sets) for (e in ms.exemplarClass.indices) {
            var sq = 0.0
            for (k in 0 until 32) sq += ms.exemplars[e * 32 + k].toDouble() * ms.exemplars[e * 32 + k]
            assertEquals("$set $e", 1.0, sqrt(sq), 1e-5)
        }
    }

    @Test
    fun `GM-05 the English owner exemplars are the strokes of the owner templates`() {
        val files = GraffitiEval.load("owner_en").values.flatten().filter { OWNER_ID.matches(it.file.nameWithoutExtension) }
        assertEquals(156, files.size)
        for (s in files) {
            val cos = bestCosine(SetName.EN_LETTER, s.expect, embed(SetName.EN_LETTER, s.stroke.points.map { it.x.toDouble() to it.y.toDouble() }))
            assertTrue("${s.file.name} $cos", cos > SAME_STROKE_COS)
        }
        // The strokes left out of the templates (FR-GRF-03b) are not exemplars either.
        for (s in GraffitiEval.load("owner_en").values.flatten().filter { LEFT_OUT_ID.matches(it.file.nameWithoutExtension) }) {
            val cos = bestCosine(SetName.EN_LETTER, s.expect, embed(SetName.EN_LETTER, s.stroke.points.map { it.x.toDouble() to it.y.toDouble() }))
            assertTrue("${s.file.name} $cos", cos < SAME_STROKE_COS)
        }
    }

    @Test
    fun `GM-06 the Korean owner exemplars are the strokes of the owner templates`() {
        val url = checkNotNull(GraffitiModelTest::class.java.getResource("/strokes/tools/owner_strokes.json"))
        val root = Json.parseToJsonElement(File(url.toURI()).readText(Charsets.UTF_8)).jsonObject
        var n = 0
        for ((jamo, strokes) in root) for (stroke in strokes.jsonArray) {
            val pts = stroke.jsonArray.map { p -> p.jsonArray[0].jsonPrimitive.double.toFloat().toDouble() to p.jsonArray[1].jsonPrimitive.double.toFloat().toDouble() }
            val cos = bestCosine(SetName.KO_LETTER, jamo, embed(SetName.KO_LETTER, pts))
            assertTrue("$jamo #$n $cos", cos > SAME_STROKE_COS)
            n++
        }
        assertEquals(117, n)
    }

    @Test
    fun `GM-07 the file is the size the design counts with and reads back the same`() {
        assertEquals(MODEL_BYTES, bytes.size)
        val again = GraffitiModelLoader.load(ByteArrayInputStream(bytes))
        for ((set, ms) in model.sets) {
            assertEquals(ms.classes, again.sets.getValue(set).classes)
            assertTrue(ms.exemplars.contentEquals(again.sets.getValue(set).exemplars))
        }
    }

    @Test
    fun `GM-08 a damaged file is refused`() {
        fun load(b: ByteArray) = GraffitiModelLoader.load(ByteArrayInputStream(b))
        assertNotNull(load(bytes))
        fun changed(at: Int, value: Int) = bytes.copyOf().also { it[at] = value.toByte() }
        assertThrows("magic", IllegalStateException::class.java) { load(changed(0, 'X'.code)) }
        assertThrows("version", IllegalStateException::class.java) { load(changed(4, 2)) }
        assertThrows("grid", IllegalStateException::class.java) { load(changed(8, 16)) }
        assertThrows("directions", IllegalStateException::class.java) { load(changed(12, 9)) }
        assertThrows("embedding", IllegalStateException::class.java) { load(changed(28, 64)) }
        assertThrows("body count", IllegalStateException::class.java) { load(changed(32, 0)) }
        assertThrows("cut in a body", IllegalStateException::class.java) { load(bytes.copyOf(1000)) }
        assertThrows("cut in the exemplars", IllegalStateException::class.java) { load(bytes.copyOf(bytes.size - 7)) }
        assertThrows("bytes after the end", IllegalStateException::class.java) { load(bytes + 0) }
        assertThrows("empty", IllegalStateException::class.java) { load(ByteArray(0)) }
        val firstSetName = 36 + 5 * BODY_BYTES + 4 + 1   // header, bodies, set count, name length
        assertThrows("set name", IllegalStateException::class.java) { load(changed(firstSetName, 'Z'.code)) }
    }

    private companion object {
        const val SYNTHETIC_PER_LETTER = 32
        val OWNER_ID = Regex("(ko|en)_.*_o[0-9]+")
        val LEFT_OUT_ID = Regex("en_[a-z]_x[0-9]+")

        /** An exemplar made from the same stroke: cosine 1 but for a step falling in the next cell when the points are read as 32-bit floats (measured 0.9998 or more). */
        const val SAME_STROKE_COS = 0.999f

        /** LLD-05 §7.1: 36 header + 5 × 89,472 bodies + 4 + sets 414,489. */
        const val MODEL_BYTES = 861_889
        const val BODY_BYTES = 89_472
    }
}
