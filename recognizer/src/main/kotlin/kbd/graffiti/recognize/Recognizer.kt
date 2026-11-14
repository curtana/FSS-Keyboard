package kbd.graffiti.recognize

import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.nn.GraffitiModel
import kbd.graffiti.nn.NnBody
import kbd.graffiti.nn.StrokeFeatures

/**
 * A stroke the user stored in "my strokes" (LLD-05 §3.6, SRS FR-GRF-14): the raw pad samples in dp, the letter
 * ([value], a template's `output.value`) and the letter's own set. Only [SetName.KO_LETTER] and [SetName.EN_LETTER]
 * take personal strokes; one for another set or for a letter the set does not have is left out.
 */
class PersonalStroke(val set: SetName, val value: String, val points: List<Pair<Float, Float>>)

/**
 * Shape recognition of one graffiti stroke (LLD-05 §3.1–3.2, SRS FR-GRF-10): tap / corner tap rules, then
 * straight-line rules (looked up in the pending symbol table first), then the letters of the pending set and of
 * the area's normal set by the weighted sum of the template score and the network score. Independent of graffiti
 * state; deterministic (NFR-04). Immutable: [withPersonal] gives a new recognizer, so a stroke is never
 * recognized against a half-changed configuration.
 *
 * Without a [model] (it could not be read, or a test of the matcher alone) the letters go by the template score
 * alone, as before the network came in ([MATCH_MIN]).
 */
class Recognizer private constructor(
    private val templates: TemplateSet,
    private val model: GraffitiModel?,
    private val sets: Map<SetName, LetterSet>,
) {
    /**
     * [personal] strokes are added to their letters, on both sides: a personal template and an exemplar
     * (FR-GRF-14 ②). With a [model], every letter of [templates] must be a letter of the model's set
     * (FR-GRF-13 ③), else [IllegalStateException].
     */
    constructor(templates: TemplateSet, model: GraffitiModel? = null, personal: List<PersonalStroke> = emptyList()) :
        this(templates, model, build(templates, model, personal, emptyMap()))

    /** This recognizer with [personal] as its personal strokes in place of the ones it has; a stroke it already has (the same object) is not prepared again. */
    fun withPersonal(personal: List<PersonalStroke>): Recognizer = Recognizer(templates, model, build(templates, model, personal, sets))

    /**
     * The token for [stroke]. With [Expect.PUNCT] or [Expect.EXTENDED] that set is searched first, rule entries
     * for taps and straight lines included (CornerTap and LineLeft are never in it); a stroke
     * it does not match falls back to the area's normal set (FR-GRF-05c ⑥⑦). Letters go by
     * [InkStroke.area], the area the stroke started in (GRAFFITI 6.3).
     */
    fun recognize(stroke: InkStroke, lang: GraffitiLang, expect: Expect): StrokeToken {
        require(stroke.points.isNotEmpty()) { "empty stroke" }
        // Step 3: a tap or straight line is a symbol when the pending table has it, whatever its length or area.
        (StrokeRules.tap(stroke) ?: StrokeRules.line(stroke))?.let { r ->
            return templates.ruleSymbol(expect, r)?.let { StrokeToken.Symbol(it) } ?: r
        }
        val input = Input(stroke)
        if (expect != Expect.NORMAL) {
            match(input, TemplateSet.setNameFor(stroke.area, lang, expect))?.let { return it }
        }
        return match(input, TemplateSet.setNameFor(stroke.area, lang, Expect.NORMAL)) ?: StrokeToken.Unknown
    }

    /**
     * The best letter of [stroke] in the set of this context and how it was scored, whether or not it passes the
     * thresholds; null for a set with no template strokes. For tests, measurement and the "my strokes" screen;
     * rules are not looked at.
     */
    fun score(stroke: InkStroke, set: SetName): LetterScore? = sets[set]?.score(Input(stroke))

    private fun match(input: Input, name: SetName): StrokeToken? {
        val s = sets[name]?.score(input) ?: return null
        return if (s.accepted) StrokeToken.Symbol(s.id) else null
    }

    /** One stroke prepared once for all the sets it is looked up in. */
    private class Input(val stroke: InkStroke) {
        val prepared = TemplateMatcher.prepare(stroke)
        val features: FloatArray by lazy(LazyThreadSafetyMode.NONE) {
            val n = stroke.points.size
            StrokeFeatures.compute(DoubleArray(n) { stroke.points[it].x.toDouble() }, DoubleArray(n) { stroke.points[it].y.toDouble() })
        }
    }

    /** A personal stroke made ready: its template (as a built-in one is stored) and, with a model, its embedding. */
    private class PersonalEntry(val stroke: PersonalStroke, val letter: Int, val template: TemplateMatcher.PreparedTemplate, val embedding: FloatArray?)

    /**
     * One template set as the recognizer searches it. [letters] are the distinct outputs of its template strokes,
     * in the model's order with a model, else in template order; [letterOf]`[i]` is the letter of built-in
     * template `i`.
     */
    private class LetterSet(
        val letters: List<String>,
        val builtIn: List<TemplateMatcher.PreparedTemplate>,
        val letterOf: IntArray,
        val personal: List<PersonalEntry>,
        val body: NnBody?,
        val exemplarLetter: IntArray,
        val exemplars: FloatArray,
    ) {
        fun score(input: Input): LetterScore? {
            if (builtIn.isEmpty()) return null
            val n = letters.size
            val t = FloatArray(n) { NONE }          // best template score of each letter
            val builtInId = arrayOfNulls<String>(n)  // its best built-in template (the earlier one on a tie)
            val builtInScore = FloatArray(n) { NONE }
            var first = -1                           // the best template overall, the earlier one on a tie
            var firstScore = NONE
            for (i in builtIn.indices) {
                val s = TemplateMatcher.score(input.prepared, builtIn[i]) ?: continue
                val c = letterOf[i]
                if (s > builtInScore[c]) {
                    builtInScore[c] = s
                    builtInId[c] = builtIn[i].template.id
                }
                if (s > t[c]) t[c] = s
                if (s > firstScore) {
                    firstScore = s
                    first = c
                }
            }
            for (p in personal) {
                val s = TemplateMatcher.score(input.prepared, p.template) ?: continue
                if (s > t[p.letter]) t[p.letter] = s
                if (s > firstScore) {
                    firstScore = s
                    first = p.letter
                }
            }
            if (first < 0) return null
            if (body == null) {
                return LetterScore(letters[first], checkNotNull(builtInId[first]), firstScore, NO_SCORE, firstScore, firstScore, firstScore >= MATCH_MIN)
            }
            val z = body.embed(input.features)
            val nn = FloatArray(n) { NONE }           // best cosine of each letter
            for (e in exemplarLetter.indices) {
                var dot = 0f
                val o = e * NnBody.EMBEDDING
                for (k in 0 until NnBody.EMBEDDING) dot += exemplars[o + k] * z[k]
                val c = exemplarLetter[e]
                if (dot > nn[c]) nn[c] = dot
            }
            for (p in personal) {
                val pz = p.embedding ?: continue
                var dot = 0f
                for (k in 0 until NnBody.EMBEDDING) dot += pz[k] * z[k]
                if (dot > nn[p.letter]) nn[p.letter] = dot
            }
            var best = -1
            var bestSum = NONE
            for (c in 0 until n) {
                if (builtInId[c] == null) continue   // a letter of the model with no template stroke in this set
                val w = WEIGHT * t[c] + (1f - WEIGHT) * nn[c]
                if (w > bestSum) {
                    bestSum = w
                    best = c
                }
            }
            val accepted = bestSum.toDouble() >= SUM_MIN && firstScore.toDouble() >= TEMPLATE_MIN
            return LetterScore(letters[best], checkNotNull(builtInId[best]), t[best], nn[best], bestSum, firstScore, accepted)
        }
    }

    companion object {
        /** Template score a letter needs when there is no model (D-11, LLD-05 §3.1), tuned on real strokes. */
        const val MATCH_MIN = 0.80f

        /** a of FR-GRF-10 ④: sum = a × template score + (1 − a) × network score. */
        const val WEIGHT = 0.5f

        /** FR-GRF-10 ④ (가): the chosen letter's sum must reach this. */
        const val SUM_MIN = 0.80

        /** FR-GRF-10 ④ (나): the set's best template score, whatever its letter, must reach this (keeps scribbles out). */
        const val TEMPLATE_MIN = 0.78

        /** [LetterScore.network] when there is no model. */
        const val NO_SCORE = Float.NaN

        private const val NONE = -Float.MAX_VALUE

        /** The sets personal strokes go into (FR-GRF-14 ①). */
        val PERSONAL_SETS = setOf(SetName.KO_LETTER, SetName.EN_LETTER)

        /** A template output as the model and the personal strokes name a letter: `output.value` of the template JSON. */
        fun valueOf(output: TemplateOutput): String = when (output) {
            is TemplateOutput.Jamo -> output.value.compat.toString()
            is TemplateOutput.Text -> output.value
            is TemplateOutput.Control -> output.value
        }

        /**
         * A personal stroke as a template: resampled, fitted in the unit box by its long side, rounded to three
         * decimals — what the built-in owner strokes went through — with the corner count of that stroke.
         */
        internal fun personalTemplate(stroke: PersonalStroke, output: TemplateOutput): Template {
            val r = TemplateMatcher.resample(stroke.points)
            val minX = r.minOf { it.first }
            val minY = r.minOf { it.second }
            val long = maxOf(r.maxOf { it.first } - minX, r.maxOf { it.second } - minY)
            val side = if (long > 0f) long.toDouble() else 1.0
            val stored = r.map { (x, y) -> round3((x - minX) / side) to round3((y - minY) / side) }
            val corners = TemplateMatcher.cornerCount(TemplateMatcher.normalize(TemplateMatcher.resample(stored)))
            return Template(PERSONAL_ID, output, corners, listOf(stored))
        }

        private fun round3(v: Double): Float = (Math.rint(v * 1000.0) / 1000.0).toFloat()

        /** Id of every personal template; never returned (a [StrokeToken.Symbol] carries a built-in id). */
        private const val PERSONAL_ID = "personal"

        private fun build(templates: TemplateSet, model: GraffitiModel?, personal: List<PersonalStroke>, old: Map<SetName, LetterSet>): Map<SetName, LetterSet> {
            val out = LinkedHashMap<SetName, LetterSet>()
            for ((name, list) in templates.sets) {
                val drawn = list.filter { it.strokes.isNotEmpty() }
                val before = old[name]
                val builtIn = before?.builtIn ?: drawn.map { TemplateMatcher.prepare(it) }
                val ms = model?.sets?.get(name)
                check(model == null || ms != null || drawn.isEmpty()) { "no model set for $name" }
                val letters = ms?.classes ?: drawn.map { valueOf(it.output) }.distinct()
                val index = HashMap<String, Int>().apply { letters.forEachIndexed { i, v -> put(v, i) } }
                val outputs = HashMap<String, TemplateOutput>()
                val letterOf = IntArray(drawn.size) { i ->
                    val v = valueOf(drawn[i].output)
                    outputs.putIfAbsent(v, drawn[i].output)
                    index[v] ?: error("template ${drawn[i].id}: letter not in the model set $name")
                }
                val kept = before?.personal.orEmpty().associateBy { System.identityHashCode(it.stroke) }
                val entries = if (name !in PERSONAL_SETS) emptyList() else personal.filter { it.set == name && it.points.isNotEmpty() }.mapNotNull { p ->
                    val output = outputs[p.value] ?: return@mapNotNull null
                    kept[System.identityHashCode(p)]?.takeIf { it.stroke === p } ?: PersonalEntry(
                        stroke = p,
                        letter = index.getValue(p.value),
                        template = TemplateMatcher.prepare(personalTemplate(p, output)),
                        embedding = ms?.body?.embed(StrokeFeatures.compute(DoubleArray(p.points.size) { p.points[it].first.toDouble() }, DoubleArray(p.points.size) { p.points[it].second.toDouble() })),
                    )
                }
                out[name] = LetterSet(letters, builtIn, letterOf, entries, ms?.body, ms?.exemplarClass ?: IntArray(0), ms?.exemplars ?: FloatArray(0))
            }
            return out
        }
    }
}

/**
 * How one set scored a stroke ([Recognizer.score]): the chosen [letter] (`output.value`), the built-in template
 * [id] returned for it (the letter's best-scoring built-in template), the letter's [template] and [network]
 * scores, their [sum] (the template score when there is no model), the set's best template score [setTemplate]
 * and whether the thresholds are met.
 */
class LetterScore(
    val letter: String,
    val id: String,
    val template: Float,
    val network: Float,
    val sum: Float,
    val setTemplate: Float,
    val accepted: Boolean,
)
