package kbd.graffiti.bench

import java.io.File
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.ink.RectF2
import kbd.graffiti.nn.GraffitiModelLoader
import kbd.graffiti.nn.StrokeFeatures
import kbd.graffiti.recognize.Expect
import kbd.graffiti.recognize.PersonalStroke
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.StrokeRules
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.TemplateSet
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Times the recognizer on a device (LLD-11 §6.7 DV-NN, SRS NFR-13, HLD 11.1 마). Plain JVM code with a `main`, so
 * `tools/graffiti-nn/device_bench.sh` can pack it with the recognizer's classes and resources into a dex jar and run
 * it with `app_process` from `/data/local/tmp`, without touching the installed keyboard. Not a unit test.
 *
 * Argument: a file of strokes, one JSON object a line (`area`, `lang`, `shift`, `expect`, `points`), written by the script
 * from the repository's stroke files. Prints one `BENCH` line per measurement, times in milliseconds.
 */
object RecognitionBench {
    private class Sample(val stroke: InkStroke, val lang: GraffitiLang, val shift: Expect, val expect: String, val group: String)

    private val LETTER = RectF2(0f, 0f, 300f, 240f)
    private val NUMBER = RectF2(300f, 0f, 450f, 240f)

    private fun ms(block: () -> Unit): Double {
        val t = System.nanoTime()
        block()
        return (System.nanoTime() - t) / 1e6
    }

    private fun line(name: String, times: List<Double>) {
        val s = times.sorted()
        fun q(p: Double) = s[((s.size - 1) * p).toInt()]
        println("BENCH %-44s n %5d  median %8.3f  p95 %8.3f  max %8.3f".format(name, s.size, q(0.5), q(0.95), s.last()))
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val samples = File(args[0]).readLines(Charsets.UTF_8).filter { it.isNotBlank() }.map { l ->
            val o = Json.parseToJsonElement(l).jsonObject
            val area = PadArea.valueOf(o.getValue("area").jsonPrimitive.content)
            val pts = o.getValue("points").jsonArray.mapIndexed { i, p -> InkPoint(p.jsonArray[0].jsonPrimitive.float, p.jsonArray[1].jsonPrimitive.float, i * 8L) }
            Sample(
                InkStroke(pts, area, if (area == PadArea.LETTER) LETTER else NUMBER), GraffitiLang.valueOf(o.getValue("lang").jsonPrimitive.content),
                Expect.valueOf(o.getValue("shift").jsonPrimitive.content), o.getValue("expect").jsonPrimitive.content, o.getValue("group").jsonPrimitive.content,
            )
        }.filter { StrokeRules.tap(it.stroke) == null && StrokeRules.line(it.stroke) == null }
        println("BENCH strokes ${samples.size} (rule strokes left out), groups ${samples.groupingBy { it.group }.eachCount()}")

        fun templates(): TemplateSet = TemplateLoader::class.java.getResourceAsStream("/graffiti/templates.json")!!.use { TemplateLoader.load(it) }
        fun model() = GraffitiModelLoader::class.java.getResourceAsStream(GraffitiModelLoader.RESOURCE)!!.use { GraffitiModelLoader.load(it) }

        // Reading: the first call is what the keyboard pays once per process.
        val tFirst = ms { templates() }
        val mFirst = ms { model() }
        println("BENCH first read: templates %.1f ms, model %.1f ms".format(tFirst, mFirst))
        line("read templates.json (again)", List(10) { ms { templates() } })
        line("read graffiti-nn.bin (again)", List(10) { ms { model() } })
        val t = templates()
        val m = model()
        println("BENCH first Recognizer(templates, model): %.1f ms".format(ms { Recognizer(t, m) }))
        line("Recognizer(templates, model) (again)", List(10) { ms { Recognizer(t, m) } })
        val rec = Recognizer(t, m)
        val alone = Recognizer(t)

        // Personal strokes: 8 a letter for the 39 letters (the store's limit), stand-ins taken from the samples.
        val letters = samples.filter { it.shift == Expect.NORMAL && it.stroke.area == PadArea.LETTER }.groupBy { (if (it.lang == GraffitiLang.KO) SetName.KO_LETTER else SetName.EN_LETTER) to it.expect }
        val personal = letters.flatMap { (k, list) -> list.take(8).map { s -> PersonalStroke(k.first, k.second, s.stroke.points.map { it.x to it.y }) } }
        println("BENCH personal strokes ${personal.size} of ${letters.size} letters")
        line("withPersonal(all personal strokes)", List(5) { ms { rec.withPersonal(personal) } })
        val full = rec.withPersonal(personal)
        line("withPersonal(one more stroke)", List(10) { ms { full.withPersonal(personal + PersonalStroke(SetName.EN_LETTER, "a", personal[0].points)) } })

        // One stroke. The first pass warms the runtime up and is reported apart.
        var sink = 0
        fun pass(r: Recognizer, list: List<Sample>, expect: (Sample) -> Expect) = list.map { s -> ms { sink += r.recognize(s.stroke, s.lang, expect(s)).hashCode() } }
        line("recognize, first pass (cold)", pass(rec, samples) { it.shift })
        repeat(3) { pass(rec, samples) { it.shift } }
        repeat(3) { pass(alone, samples) { it.shift } }
        for ((group, list) in samples.groupBy { it.group }) line("recognize $group", (1..5).flatMap { pass(rec, list) { it.shift } })
        line("recognize, all", (1..5).flatMap { pass(rec, samples) { it.shift } })
        line("recognize, templates alone, all", (1..5).flatMap { pass(alone, samples) { it.shift } })
        line("recognize, with personal strokes, all", (1..5).flatMap { pass(full, samples) { it.shift } })
        // Pending shift with a letter stroke: the symbol set does not take it, so both sets are searched (two forward passes).
        val normal = samples.filter { it.shift == Expect.NORMAL && it.stroke.area == PadArea.LETTER }
        repeat(2) { pass(rec, normal) { Expect.PUNCT } }
        line("recognize letters, punctuation pending", (1..5).flatMap { pass(rec, normal) { Expect.PUNCT } })

        // Parts.
        val ko = m.sets.getValue(SetName.KO_LETTER).body
        fun arrays(s: Sample) = DoubleArray(s.stroke.points.size) { s.stroke.points[it].x.toDouble() } to DoubleArray(s.stroke.points.size) { s.stroke.points[it].y.toDouble() }
        repeat(3) { for (s in samples) arrays(s).let { (x, y) -> sink += ko.embed(StrokeFeatures.compute(x, y)).size } }
        line("part: features", (1..5).flatMap { samples.map { s -> val (x, y) = arrays(s); ms { sink += StrokeFeatures.compute(x, y).size } } })
        val feats = samples.map { s -> arrays(s).let { (x, y) -> StrokeFeatures.compute(x, y) } }
        line("part: forward pass", (1..5).flatMap { feats.map { f -> ms { sink += ko.embed(f).size } } })
        println("BENCH done $sink")
    }
}
