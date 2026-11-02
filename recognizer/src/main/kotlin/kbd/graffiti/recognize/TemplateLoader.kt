package kbd.graffiti.recognize

import java.io.InputStream
import kbd.hangul.jamo.Jamo
import kbd.hangul.jamo.JamoKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads the template JSON (LLD-05 §7) once at app start. `version != 1`, a duplicate `id` anywhere in the
 * file, or a malformed entry (negative `corners`, a coordinate outside 0..1, a control value other than
 * [CONTROL_VALUES], a `rule` that is not in [RULES], outside PUNCT / EXTENDED, with strokes, or twice in a
 * set) → [IllegalStateException]; the caller then runs the pad with recognition
 * failures only (LLD-09 §4). The stream is not closed.
 */
object TemplateLoader {

    /** `output.value` of a `control` template (LLD-05 §7), what GraffitiState rows 21–22 handle. */
    val CONTROL_VALUES = setOf("SPACE_V", "FINISH_J")

    /**
     * `rule` values of a symbol-table rule entry (LLD-05 §7). LineLeft (⌫, owner decision #79 OD-3 A) and
     * CornerTap (language toggle) are never symbols, so they have no name here and are rejected.
     */
    val RULES: Map<String, StrokeToken> = mapOf(
        "TAP" to StrokeToken.Tap,
        "LINE_DOWN" to StrokeToken.LineDown,
        "LINE_UP" to StrokeToken.LineUp,
        "LINE_RIGHT" to StrokeToken.LineRight,
        "DIAG_DOWN_LEFT" to StrokeToken.DiagDownLeft,
        "DIAG_DOWN_RIGHT" to StrokeToken.DiagDownRight,
    )

    private val json = Json { isLenient = false }

    fun load(stream: InputStream): TemplateSet =
        try {
            read(json.parseToJsonElement(stream.reader(Charsets.UTF_8).readText()).jsonObject)
        } catch (e: IllegalArgumentException) {   // malformed JSON or wrong JSON kind (SerializationException included)
            throw IllegalStateException("malformed template JSON", e)
        }

    private fun read(root: JsonObject): TemplateSet {
        val version = field(root, "version").jsonPrimitive.int
        check(version == 1) { "unsupported template version $version" }
        val ids = HashSet<String>()
        val sets = LinkedHashMap<SetName, List<Template>>()
        for ((name, list) in field(root, "sets").jsonObject) {
            val setName = SetName.entries.firstOrNull { it.name == name } ?: error("unknown template set $name")
            val rules = HashSet<StrokeToken>()
            sets[setName] = list.jsonArray.map { e ->
                val t = template(e.jsonObject)
                check(ids.add(t.id)) { "duplicate template id ${t.id}" }
                t.rule?.let { r ->
                    check(setName == SetName.PUNCT || setName == SetName.EXTENDED) { "rule entry ${t.id} outside a symbol table" }
                    check(t.strokes.isEmpty()) { "rule entry ${t.id} has strokes" }
                    check(rules.add(r)) { "second rule entry for $r in $name" }
                }
                t
            }
        }
        return TemplateSet(sets)
    }

    private fun template(o: JsonObject): Template = Template(
        id = field(o, "id").jsonPrimitive.content,
        output = output(field(o, "output").jsonObject),
        corners = field(o, "corners").jsonPrimitive.int.also { check(it >= 0) { "negative corners $it" } },
        rule = o["rule"]?.let { r -> RULES[r.jsonPrimitive.content] ?: error("unknown rule ${r.jsonPrimitive.content}") },
        strokes = field(o, "strokes").jsonArray.map { stroke ->
            stroke.jsonArray.map { p ->
                val xy = p.jsonArray
                check(xy.size == 2) { "point must be [x, y]" }
                val x = xy[0].jsonPrimitive.float
                val y = xy[1].jsonPrimitive.float
                check(x in 0f..1f && y in 0f..1f) { "point [$x, $y] outside 0..1" }
                x to y
            }.also { check(it.isNotEmpty()) { "empty stroke" } }
        },
    )

    private fun output(o: JsonObject): TemplateOutput {
        val value = field(o, "value").jsonPrimitive.content
        return when (val type = field(o, "type").jsonPrimitive.content) {
            "jamo" -> {
                val jamo = value.singleOrNull()?.let { Jamo.ofCompat(it) }
                check(jamo != null && jamo.kind == JamoKind.CONSONANT) { "not a consonant: $value" }
                TemplateOutput.Jamo(jamo)
            }
            "text" -> TemplateOutput.Text(value)
            "control" -> {
                check(value in CONTROL_VALUES) { "unknown control value $value" }
                TemplateOutput.Control(value)
            }
            else -> error("unknown output type $type")
        }
    }

    private fun field(o: JsonObject, key: String): JsonElement = o[key] ?: error("missing \"$key\"")
}
