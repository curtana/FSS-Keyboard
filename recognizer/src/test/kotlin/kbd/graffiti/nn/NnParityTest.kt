package kbd.graffiti.nn

import kbd.graffiti.eval.GraffitiEval
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.StrokeRules
import kotlin.math.abs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §6.7 NN-01..NN-05 (SRS FR-GRF-13 acceptance, HLD 11.1 가): on the fixed sample `graffiti/nn-parity.json`
 * — 440 strokes of the repository's stroke files with what the training side computed for them — the app side
 * computes the same features, embedding, network letter, weighted letter and final answer. The sample and the
 * model file come from one run of `tools/graffiti-nn/export.py` and are replaced together.
 */
class NnParityTest {

    private class SetRow(
        val set: SetName, val featSum: Double, val centreSum: Double, val emb: DoubleArray,
        val nn: String, val nnScore: Double, val sum: String, val sumScore: Double, val tmpl: String, val tmplScore: Double,
    )

    private class Row(val file: String, val expect: String, val answer: String, val sets: List<SetRow>)

    private val root = NnParityTest::class.java.getResourceAsStream("/graffiti/nn-parity.json")!!.use {
        Json.parseToJsonElement(it.reader(Charsets.UTF_8).readText()).jsonObject
    }

    private val rows: List<Row> = root.getValue("samples").jsonArray.map { e ->
        val o = e.jsonObject
        Row(
            o.getValue("file").jsonPrimitive.content, o.getValue("expect").jsonPrimitive.content, o.getValue("answer").jsonPrimitive.content,
            o.getValue("sets").jsonArray.map { x ->
                val s = x.jsonObject
                fun d(k: String) = s.getValue(k).jsonPrimitive.double
                fun t(k: String) = s.getValue(k).jsonPrimitive.content
                SetRow(SetName.valueOf(t("set")), d("featSum"), d("centreSum"), s.getValue("emb").jsonArray.map { it.jsonPrimitive.double }.toDoubleArray(),
                    t("nn"), d("nnScore"), t("sum"), d("sumScore"), t("tmpl"), d("tmplScore"))
            },
        )
    }

    private fun features(row: Row): FloatArray {
        val p = GraffitiEval.file(row.file).stroke.points
        return StrokeFeatures.compute(DoubleArray(p.size) { p[it].x.toDouble() }, DoubleArray(p.size) { p[it].y.toDouble() })
    }

    @Test
    fun `NN-01 the sample is the 440 strokes and the constants the recognizer uses`() {
        assertEquals(440, rows.size)
        assertEquals(kbd.graffiti.recognize.Recognizer.WEIGHT.toDouble(), root.getValue("a").jsonPrimitive.double, 0.0)
        assertEquals(kbd.graffiti.recognize.Recognizer.SUM_MIN, root.getValue("sumMin").jsonPrimitive.double, 0.0)
        assertEquals(kbd.graffiti.recognize.Recognizer.TEMPLATE_MIN, root.getValue("templateMin").jsonPrimitive.double, 0.0)
        for (r in rows) {
            val s = GraffitiEval.file(r.file)
            assertEquals(r.file, r.expect, s.expect)
            assertEquals(r.file, r.expect, r.answer)   // every sample is one the training side reads right
            assertTrue(r.file, StrokeRules.tap(s.stroke) == null && StrokeRules.line(s.stroke) == null)
            assertTrue(r.file, r.sets.size in 1..2)
        }
    }

    @Test
    fun `NN-02 features - sum of all values and of the centre channel`() {
        var worst = 0.0
        var centred = 0
        for (r in rows) {
            val f = features(r)
            assertEquals(StrokeFeatures.SIZE, f.size)
            assertTrue(r.file, f.all { it in 0f..1f })
            val sum = f.sumOf { it.toDouble() }
            val centre = (StrokeFeatures.DIRECTIONS * 64 until StrokeFeatures.SIZE).sumOf { f[it].toDouble() }
            for (s in r.sets) {
                worst = maxOf(worst, abs(sum - s.featSum))
                assertEquals(r.file, s.featSum, sum, FEATURE_SUM_TOL)
                assertEquals(r.file, s.centreSum, centre, 0.0)   // multiples of 0.25: exact
                if (s.centreSum > 0) centred++
            }
        }
        println("NN-02 worst feature-sum difference $worst, set rows with a centre value $centred")
        assertTrue(centred > 0)
    }

    @Test
    fun `NN-03 embedding of each sample in each of its sets`() {
        var worst = 0.0
        for (r in rows) {
            val f = features(r)
            for (s in r.sets) {
                val z = GraffitiEval.model.sets.getValue(s.set).body.embed(f)
                for (k in z.indices) {
                    worst = maxOf(worst, abs(z[k] - s.emb[k]))
                    assertEquals("${r.file} ${s.set} [$k]", s.emb[k], z[k].toDouble(), EMBEDDING_TOL)
                }
            }
        }
        println("NN-03 worst embedding difference $worst")
    }

    @Test
    fun `NN-04 network letter, template letter and weighted letter with their scores`() {
        var worstNn = 0.0
        var worstSum = 0.0
        var worstT = 0.0
        for (r in rows) {
            val stroke = GraffitiEval.file(r.file).stroke
            val f = features(r)
            for (s in r.sets) {
                val ms = GraffitiEval.model.sets.getValue(s.set)
                val z = ms.body.embed(f)
                val best = FloatArray(ms.classes.size) { -1f }
                for (e in ms.exemplarClass.indices) {
                    var dot = 0f
                    for (k in z.indices) dot += ms.exemplars[e * z.size + k] * z[k]
                    if (dot > best[ms.exemplarClass[e]]) best[ms.exemplarClass[e]] = dot
                }
                val c = best.indices.maxByOrNull { best[it] }!!
                assertEquals("${r.file} ${s.set} network letter", s.nn, ms.classes[c])
                worstNn = maxOf(worstNn, abs(best[c] - s.nnScore))
                assertEquals("${r.file} ${s.set} network score", s.nnScore, best[c].toDouble(), SCORE_TOL)

                val got = GraffitiEval.recognizer.score(stroke, s.set)!!
                assertEquals("${r.file} ${s.set} weighted letter", s.sum, got.letter)
                worstSum = maxOf(worstSum, abs(got.sum - s.sumScore))
                assertEquals("${r.file} ${s.set} weighted score", s.sumScore, got.sum.toDouble(), SCORE_TOL)
                worstT = maxOf(worstT, abs(got.setTemplate - s.tmplScore))
                assertEquals("${r.file} ${s.set} best template score", s.tmplScore, got.setTemplate.toDouble(), SCORE_TOL)
                val alone = GraffitiEval.templateOnly.score(stroke, s.set)!!
                assertEquals("${r.file} ${s.set} template letter", s.tmpl, alone.letter)
            }
        }
        println("NN-04 worst differences: network score $worstNn, weighted score $worstSum, template score $worstT")
    }

    @Test
    fun `NN-05 final answer of each sample`() {
        for (r in rows) {
            val s = GraffitiEval.file(r.file)
            assertEquals(r.file, r.answer, GraffitiEval.label(GraffitiEval.recognizer.recognize(s.stroke, s.lang, s.shift), s))
        }
    }

    private companion object {
        /** LLD-11 §6.7: the sample's sums are rounded to 5 decimals, embeddings and scores to 6; measured differences are about 7e-7 (6e-6 for the feature sum). */
        const val FEATURE_SUM_TOL = 1e-4
        const val EMBEDDING_TOL = 1e-5
        const val SCORE_TOL = 1e-5
    }
}
