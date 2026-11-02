package kbd.graffiti.recognize

import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.PadArea

/** Template sets (LLD-05 §3.3); names match the `sets` keys of the template JSON (§7). */
enum class SetName { KO_LETTER, EN_LETTER, NUMBER, PUNCT, EXTENDED }

/** A template's output, mirroring `output.type` of the template JSON (LLD-05 §7). */
sealed interface TemplateOutput {
    /** Hangul consonant, double consonants included. */
    data class Jamo(val value: kbd.hangul.jamo.Jamo) : TemplateOutput

    /** English (lower case), digit or symbol. */
    data class Text(val value: String) : TemplateOutput

    /** `SPACE_V`, `FINISH_J`. */
    data class Control(val value: String) : TemplateOutput
}

/**
 * One template. [strokes] are alternative strokes, each a sequence of 0..1 normalized points whose
 * first point is the start; [corners] is the expected corner count (LLD-05 §3.2 step 5).
 * [rule] makes it a rule entry of a symbol table: the symbol drawn as that tap / straight-line token,
 * with no strokes to match (LLD-05 §3.1 step 3, §7 `rule`).
 */
data class Template(
    val id: String,
    val output: TemplateOutput,
    val corners: Int,
    val strokes: List<List<Pair<Float, Float>>>,
    val rule: StrokeToken? = null,
)

class TemplateSet(val sets: Map<SetName, List<Template>>) {

    // Ids are unique across the whole file (§7, enforced by TemplateLoader); first in SetName order wins otherwise.
    private val byId: Map<String, TemplateOutput> = buildMap {
        for (name in SetName.entries) {
            for (t in sets[name].orEmpty()) putIfAbsent(t.id, t.output)
        }
    }

    // Rule entries of the two symbol tables; one per rule and set (TemplateLoader), first one wins otherwise.
    private val byRule: Map<Pair<SetName, StrokeToken>, String> = buildMap {
        for (name in listOf(SetName.PUNCT, SetName.EXTENDED)) {
            for (t in sets[name].orEmpty()) t.rule?.let { putIfAbsent(name to it, t.id) }
        }
    }

    /**
     * Id of the [expect] symbol table's entry drawn as [rule] (a tap or straight-line token), or null; always
     * null for [Expect.NORMAL] (LLD-05 §3.1 step 3: pending symbol tables come before the control strokes).
     */
    fun ruleSymbol(expect: Expect, rule: StrokeToken): String? = when (expect) {
        Expect.NORMAL -> null
        Expect.PUNCT -> byRule[SetName.PUNCT to rule]
        Expect.EXTENDED -> byRule[SetName.EXTENDED to rule]
    }

    /** The set to search for this context (LLD-05 §3.3 table). A set missing from [sets] is empty. */
    fun forContext(area: PadArea, lang: GraffitiLang, expect: Expect): List<Template> =
        sets[setNameFor(area, lang, expect)].orEmpty()

    /** Output of the template [id] in any set, or null (interim, gap G3: GraffitiState.interpret, LLD-05 §5.2). */
    fun outputOf(id: String): TemplateOutput? = byId[id]

    private companion object {
        fun setNameFor(area: PadArea, lang: GraffitiLang, expect: Expect): SetName = when (expect) {
            Expect.PUNCT -> SetName.PUNCT
            Expect.EXTENDED -> SetName.EXTENDED
            Expect.NORMAL -> when {
                area == PadArea.NUMBER -> SetName.NUMBER
                lang == GraffitiLang.KO -> SetName.KO_LETTER
                else -> SetName.EN_LETTER
            }
        }
    }
}
