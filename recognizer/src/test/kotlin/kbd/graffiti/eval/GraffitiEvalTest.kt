package kbd.graffiti.eval

import java.io.File
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
import kbd.graffiti.nn.GraffitiModel
import kbd.graffiti.nn.GraffitiModelLoader
import kbd.graffiti.nn.ModelSet
import kbd.graffiti.nn.StrokeFeatures
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.Expect
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.StrokeToken
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.TemplateMatcher
import kbd.graffiti.recognize.TemplateOutput
import kbd.graffiti.recognize.TemplateSet
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertTrue
import org.junit.Test

/** One stroke file of LLD-11 §1.3, plus the optional `shift` (PUNCT / EXTENDED pending, default none). */
internal data class EvalStroke(val file: File, val stroke: InkStroke, val lang: GraffitiLang, val expect: String, val shift: Expect)

/** Reads `strokes/<split>/` and scores the production templates on it (NFR-09, NFR-12). */
internal object GraffitiEval {
    // Pad of 450 x 240 dp split 2 : 1 (FR-GRF-01), the layout gen_strokes.py draws into.
    val LETTER_RECT = RectF2(0f, 0f, 300f, 240f)
    val NUMBER_RECT = RectF2(300f, 0f, 450f, 240f)

    val templates: TemplateSet by lazy {
        GraffitiEval::class.java.getResourceAsStream("/graffiti/templates.json")!!.use { TemplateLoader.load(it) }
    }

    /** The shipped model (LLD-05 §7.1), as the keyboard reads it. */
    val model: GraffitiModel by lazy {
        GraffitiEval::class.java.getResourceAsStream(GraffitiModelLoader.RESOURCE)!!.use { GraffitiModelLoader.load(it) }
    }

    /** The recognizer of the keyboard: production templates and the shipped model (SRS FR-GRF-10). */
    val recognizer: Recognizer by lazy { Recognizer(templates, model) }

    /** The production templates alone, as recognition was before the network score came in (the baseline of LLD-11 §6.7). */
    val templateOnly: Recognizer by lazy { Recognizer(templates) }

    /**
     * The shipped model without the exemplars that are [strokes] themselves (for each stroke the exemplar of its letter
     * nearest to the stroke's own embedding, cosine 1 but for the arithmetic): what the model would be had those owner strokes not been put in (FR-GRF-13 ③).
     */
    fun modelWithout(strokes: List<EvalStroke>, set: SetName): GraffitiModel {
        val ms = model.sets.getValue(set)
        val n = ms.exemplarClass.size
        val size = ms.exemplars.size / n
        val drop = BooleanArray(n)
        for (s in strokes) {
            val p = s.stroke.points
            val z = ms.body.embed(StrokeFeatures.compute(DoubleArray(p.size) { p[it].x.toDouble() }, DoubleArray(p.size) { p[it].y.toDouble() }))
            val c = ms.classes.indexOf(s.expect)
            var best = -1
            var bestDot = SAME_STROKE_COS
            for (e in 0 until n) {
                if (ms.exemplarClass[e] != c || drop[e]) continue
                var dot = 0f
                for (k in 0 until size) dot += ms.exemplars[e * size + k] * z[k]
                if (dot > bestDot) {
                    bestDot = dot
                    best = e
                }
            }
            check(best >= 0) { "${s.file.name} is no exemplar of the model" }
            drop[best] = true
        }
        val keep = (0 until n).filter { !drop[it] }
        val cut = ModelSet(ms.body, ms.bodyIndex, ms.classes, IntArray(keep.size) { ms.exemplarClass[keep[it]] }, FloatArray(keep.size * size) { ms.exemplars[keep[it / size] * size + it % size] })
        return GraffitiModel(model.sets + (set to cut), model.bodyCount)
    }

    /** An exemplar made from the same stroke: cosine 1 but for a step falling in the next cell when the points are read as 32-bit floats (measured 0.9998 or more). */
    const val SAME_STROKE_COS = 0.999f

    /** Every `*.json` under `strokes/<split>/`, grouped by sub-folder; throws if the folder or a file cannot be read. */
    fun load(split: String): Map<String, List<EvalStroke>> {
        val url = GraffitiEval::class.java.getResource("/strokes/$split") ?: error("strokes/$split not on the test classpath")
        val root = File(url.toURI())
        val files = root.walkTopDown().filter { it.isFile && it.extension == "json" }.sortedBy { it.path }.toList()
        check(files.isNotEmpty()) { "no stroke files in $root" }
        return files.map { read(it) }.groupBy { it.file.parentFile.name }.toSortedMap()
    }

    /** One stroke file by its path under `strokes/`. */
    fun file(path: String): EvalStroke {
        val url = GraffitiEval::class.java.getResource("/strokes/$path") ?: error("strokes/$path not on the test classpath")
        return read(File(url.toURI()))
    }

    private fun read(file: File): EvalStroke = try {
        val o = Json.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject
        val area = PadArea.valueOf(o.getValue("area").jsonPrimitive.content)
        val points = o.getValue("points").jsonArray.map { p ->
            val a = p.jsonArray
            InkPoint(a[0].jsonPrimitive.float, a[1].jsonPrimitive.float, a[2].jsonPrimitive.long)
        }
        check(points.isNotEmpty()) { "no points" }
        EvalStroke(
            file = file,
            stroke = InkStroke(points, area, if (area == PadArea.LETTER) LETTER_RECT else NUMBER_RECT),
            lang = GraffitiLang.valueOf(o.getValue("lang").jsonPrimitive.content),
            expect = o.getValue("expect").jsonPrimitive.content,
            shift = o["shift"]?.let { Expect.valueOf(it.jsonPrimitive.content) } ?: Expect.NORMAL,
        )
    } catch (e: Exception) {
        throw IllegalStateException("cannot read stroke file $file", e)
    }

    /**
     * What the user gets for [token], as the `expect` text: a template's output, or for the rule strokes the
     * Cheonjiin element / letter / digit GraffitiState makes of them (LLD-05 §5.2 rows 13, 14, 16–18).
     * Anything else is the token's name, which never equals an `expect`.
     */
    fun label(token: StrokeToken, s: EvalStroke): String {
        val koLetter = s.lang == GraffitiLang.KO && s.stroke.area == PadArea.LETTER
        return when (token) {
            is StrokeToken.Symbol -> when (val out = templates.outputOf(token.id)) {
                is TemplateOutput.Jamo -> out.value.compat.toString()
                is TemplateOutput.Text -> out.value
                is TemplateOutput.Control -> out.value
                null -> "?${token.id}"
            }
            StrokeToken.LineDown -> when {
                s.stroke.area == PadArea.NUMBER -> "1"
                koLetter -> "ㅣ"
                else -> "i"
            }
            StrokeToken.LineRight -> if (koLetter) "ㅡ" else "Space"
            StrokeToken.Tap -> if (koLetter) "ㆍ" else "Tap"
            else -> token.toString()
        }
    }

    class Score(val name: String, val total: Int, val correct: Int, val misses: Map<String, Int>) {
        val accuracy: Double get() = if (total == 0) 0.0 else correct * 100.0 / total
        override fun toString() = "%-6s %4d / %4d = %6.2f %%".format(name, correct, total, accuracy)
    }

    /** [strokes] read with [set] and the shipped model. */
    fun score(name: String, strokes: List<EvalStroke>, set: TemplateSet): Score = score(name, strokes, Recognizer(set, model))

    fun score(name: String, strokes: List<EvalStroke>, rec: Recognizer = recognizer): Score {
        val misses = sortedMapOf<String, Int>()
        var correct = 0
        for (s in strokes) {
            val got = label(rec.recognize(s.stroke, s.lang, s.shift), s)
            if (got == s.expect) correct++ else misses.merge("${s.expect} -> $got", 1, Int::plus)
        }
        return Score(name, strokes.size, correct, misses)
    }

    /** Per-folder scores plus the English / Korean jamo summary lines (LLD-11 §6.1 GR-EVAL). */
    fun report(split: String, withMisses: Boolean): String {
        val sets = load(split)
        val scores = sets.map { (folder, list) -> score(folder, list) }
        return buildString {
            appendLine("Graffiti recognition, strokes/$split (${sets.values.sumOf { it.size }} strokes)")
            for (s in scores) appendLine("  $s")
            appendLine("  English letters (en):     %.2f %%".format(scores.first { it.name == "en" }.accuracy))
            appendLine("  Korean jamo (ko):         %.2f %%".format(scores.first { it.name == "ko" }.accuracy))
            if (withMisses) for (s in scores) for ((k, n) in s.misses) appendLine("  miss ${s.name}: $k x$n")
        }
    }
}

/** GR-EVAL (LLD-11 §6.1, NFR-12): accuracy on the eval set. The target is TBD-12, so only unreadable files fail. */
class GraffitiEvalTest {

    @Test
    fun `GR-EVAL accuracy on strokes eval per set, English and Korean jamo`() {
        val report = GraffitiEval.report("eval", withMisses = false)
        println(report)
        assertTrue(report.contains("Korean jamo"))
    }

    @Test
    fun `dev set accuracy with misses, for tuning the templates`() {
        println(GraffitiEval.report("dev", withMisses = true))
        val rec = GraffitiEval.recognizer
        for ((_, list) in GraffitiEval.load("dev")) for (s in list) {
            val got = GraffitiEval.label(rec.recognize(s.stroke, s.lang, s.shift), s)
            if (got == s.expect) continue
            val set = GraffitiEval.templates.forContext(s.stroke.area, s.lang, s.shift) +
                if (s.shift != Expect.NORMAL) GraffitiEval.templates.forContext(s.stroke.area, s.lang, Expect.NORMAL) else emptyList()
            val top = set.mapNotNull { t -> TemplateMatcher.best(s.stroke, listOf(t)) }
                .sortedByDescending { it.second }.take(3).joinToString { "%s %.3f".format(it.first, it.second) }
            println("DBG ${s.file.name} ${s.expect} -> $got : $top")
        }
    }
}
