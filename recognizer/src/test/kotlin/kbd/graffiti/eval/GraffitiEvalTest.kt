package kbd.graffiti.eval

import java.io.File
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
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

    /** Every `*.json` under `strokes/<split>/`, grouped by sub-folder; throws if the folder or a file cannot be read. */
    fun load(split: String): Map<String, List<EvalStroke>> {
        val url = GraffitiEval::class.java.getResource("/strokes/$split") ?: error("strokes/$split not on the test classpath")
        val root = File(url.toURI())
        val files = root.walkTopDown().filter { it.isFile && it.extension == "json" }.sortedBy { it.path }.toList()
        check(files.isNotEmpty()) { "no stroke files in $root" }
        return files.map { read(it) }.groupBy { it.file.parentFile.name }.toSortedMap()
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

    fun score(name: String, strokes: List<EvalStroke>, set: TemplateSet = templates): Score {
        val rec = Recognizer(set)
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
        val rec = Recognizer(GraffitiEval.templates)
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
