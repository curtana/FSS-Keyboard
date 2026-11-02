package kbd.graffiti.recognize

import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkStroke

/**
 * Shape recognition of one graffiti stroke (LLD-05 §3.1–3.2): tap / corner tap rules, then straight-line
 * rules (looked up in the pending symbol table first), then template matching. Independent of graffiti
 * state; deterministic (NFR-04).
 */
class Recognizer(private val templates: TemplateSet) {

    /**
     * The token for [stroke]. With [Expect.PUNCT] or [Expect.EXTENDED] that set is searched first, rule entries
     * for taps and straight lines included (CornerTap and LineLeft are never in it); a stroke
     * it does not match falls back to the area's normal set (FR-GRF-05c ⑥⑦). Templates go by
     * [InkStroke.area], the area the stroke started in (GRAFFITI 6.3).
     */
    fun recognize(stroke: InkStroke, lang: GraffitiLang, expect: Expect): StrokeToken {
        require(stroke.points.isNotEmpty()) { "empty stroke" }
        // Step 3: a tap or straight line is a symbol when the pending table has it, whatever its length or area.
        (StrokeRules.tap(stroke) ?: StrokeRules.line(stroke))?.let { r ->
            return templates.ruleSymbol(expect, r)?.let { StrokeToken.Symbol(it) } ?: r
        }
        if (expect != Expect.NORMAL) {
            match(stroke, templates.forContext(stroke.area, lang, expect))?.let { return it }
        }
        return match(stroke, templates.forContext(stroke.area, lang, Expect.NORMAL)) ?: StrokeToken.Unknown
    }

    private fun match(stroke: InkStroke, set: List<Template>): StrokeToken? {
        val (id, score) = TemplateMatcher.best(stroke, set) ?: return null
        return if (score >= MATCH_MIN) StrokeToken.Symbol(id) else null
    }

    companion object {
        /** D-11 initial value (LLD-05 §3.1), tuned on real strokes. */
        const val MATCH_MIN = 0.80f
    }
}
