package kbd.gesture.input

import kbd.gesture.classify.StrokeOutcome
import kbd.gesture.classify.StrokeResult
import kbd.gesture.classify.SwipeKeyKind
import kbd.gesture.layout.SwipeLayout
import kbd.hangul.cheonjiin.CjElement
import kbd.hangul.compose.BackspaceResult
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.hangul.event.ComposeResult
import kbd.hangul.stroke.ConsonantBox
import kbd.hangul.stroke.StrokeAxis
import kbd.hangul.stroke.StrokePop
import kbd.hangul.stroke.VowelStroke
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds

/**
 * Action-sequence harness (LLD-11 §4.4): stroke results go through [SwipeInputMapper.accept] and
 * into `HangulComposer(SWIPE)`, ⌫ through the open assembly and the composer, as the swipe keyboard
 * does (LLD-04 §9.3, LLD-08 §6.1). [shown] = committed + composing + temporary glyph. Keys are
 * compatibility jamo ("ㄱ", "ㄲ"). Vowels are written stroke by stroke in stroke order (issue #173);
 * [vowel] feeds the canonical strokes of [SHAPES]. There is no time judgement.
 */
class SwipeTypist(val th: Thresholds = ThresholdDefaults.VALUES) {
    val mapper = SwipeInputMapper(th)
    val composer = HangulComposer(ComposePolicy.SWIPE)
    private val committed = StringBuilder()
    private var t = 0L

    /** Error vibrations: an ignored stroke or element, or unfinished vowel strokes dropped when the assembly closed. */
    var errors = 0
        private set

    /** Committed + composing + the temporary glyph (ㆍ·‥, SWIPE 3.4). */
    val shown: String get() = committed.toString() +
        composer.composingText().let { if (mapper.pendingReplacesVowel()) it.dropLast(1) else it } + mapper.pendingGlyph()

    /** A tap on consonant key [key]: its consonant. */
    fun tap(key: String): SwipeTypist = accept(StrokeResult(key, SwipeKeyKind.CONSONANT, StrokeOutcome.Tap, t, null, t + 60))

    /**
     * One vowel stroke (from key [key], which does not matter). As the keyboard does, the mapper gets C when
     * the composer has a consonant for the vowel (the center consonant shows, SWIPE 9.5).
     */
    fun stroke(s: VowelStroke, key: String = "ㄱ"): SwipeTypist {
        mapper.consonant = if (composer.lastConsonant() != null) C else null
        return accept(StrokeResult(key, SwipeKeyKind.CONSONANT, StrokeOutcome.Stroke(s), t, null, t + 100))
    }

    /** A stroke against the stroke order (← / ↑). */
    fun reverse(key: String = "ㄱ"): SwipeTypist = accept(StrokeResult(key, SwipeKeyKind.CONSONANT, StrokeOutcome.Reverse, t, null, t + 100))

    /** The canonical strokes of [v] ("ㅏ", "ㅘ" …), from any consonant key: beside C after a consonant, else bare. */
    fun vowel(v: String): SwipeTypist {
        (if (composer.lastConsonant() != null) SHAPES_C else SHAPES).getValue(v).forEach { stroke(it) }
        return this
    }

    /** Cheonjiin key strokes, one per character of [elements] ("ㆍ", "ㅣ", "ㅡ") (SWIPE 5.4). */
    fun cj(elements: String): SwipeTypist {
        for (ch in elements) {
            val e = when (ch) {
                'ㆍ' -> CjElement.DOT
                'ㅣ' -> CjElement.I
                'ㅡ' -> CjElement.EU
                else -> error("not a cheonjiin element: $ch")
            }
            accept(StrokeResult(SwipeLayout.CHEONJIIN_ID, SwipeKeyKind.CHEONJIIN, StrokeOutcome.Element(e), t,
                if (e == CjElement.DOT) null else t + 40, t + 100))
        }
        return this
    }

    /** A number or symbol key stroke typing [s]: the composition is committed first (LLD-08 §6). */
    fun text(s: String): SwipeTypist = accept(StrokeResult(s, SwipeKeyKind.NUMBER, StrokeOutcome.Text(s, false), t, null, t + 60))

    /**
     * ⌫ as `InputDispatcher.backspace` does it: an open assembly gives back one stroke or element first
     * (undoing the composer's last unit when the vowel changed), else the composer one unit, else the
     * last committed character.
     */
    fun backspace(): SwipeTypist {
        when (val pop = mapper.popElement()) {
            is StrokePop.Popped -> if (pop.undoComposer) composer.undoLast() else ComposeResult("", composer.composingText())
            StrokePop.Empty -> when (val b = composer.backspace()) {
                is BackspaceResult.Handled -> committed.append(b.result.committed)
                BackspaceResult.NothingComposing -> if (committed.isNotEmpty()) committed.setLength(committed.length - 1)
            }
        }
        t += 300
        return this
    }

    /** Space: composition committed, " " typed, mapper reset (LLD-08 §6). */
    fun space(): SwipeTypist {
        committed.append(composer.finish()).append(' ')
        if (mapper.reset()) errors++
        t += 300
        return this
    }

    private fun accept(r: StrokeResult): SwipeTypist {
        t = r.upTime + 300
        when (val m = mapper.accept(r)) {
            is MapResult.Jamo -> {
                if (m.errorVibe) errors++
                committed.append(composer.applyAll(m.events).committed)
            }
            MapResult.Ignored -> errors++
            is MapResult.Text -> {
                if (m.errorVibe) errors++
                committed.append(composer.finish()).append(m.text)
            }
        }
        return this
    }

    companion object {
        fun v(x: Float, y0: Float, y1: Float) = VowelStroke(StrokeAxis.VERTICAL, x, y0, x, y1)
        fun h(y: Float, x0: Float, x1: Float) = VowelStroke(StrokeAxis.HORIZONTAL, x0, y, x1, y)

        /** C of the 360 dp portrait swipe view with 48 dp rows (SWIPE 9.5, centred, issue #213): ≈ (139, 52)–(221, 148). */
        val C: ConsonantBox = SwipeLayout.consonantBox(360f, 48f)

        private val I = v(100f, 40f, 160f)
        private val DOT_A = h(100f, 100f, 125f)

        /** Strokes of each vowel written beside C after a consonant (SWIPE 3.6.3, 9.5 space check; t = 20). */
        val SHAPES_C: Map<String, List<VowelStroke>> = mapOf(
            "ㅣ" to listOf(v(235f, 56f, 150f)),
            "ㅡ" to listOf(h(160f, 135f, 245f)),
            "ㅏ" to listOf(v(235f, 56f, 150f), h(100f, 235f, 260f)),
            "ㅑ" to listOf(v(235f, 56f, 150f), h(90f, 235f, 260f), h(120f, 235f, 260f)),
            "ㅐ" to listOf(v(235f, 56f, 150f), h(100f, 235f, 260f), v(270f, 56f, 150f)),
            "ㅒ" to listOf(v(235f, 56f, 150f), h(90f, 235f, 260f), h(120f, 235f, 260f), v(270f, 56f, 150f)),
            "ㅓ" to listOf(h(100f, 225f, 250f), v(255f, 56f, 160f)),
            "ㅕ" to listOf(h(90f, 225f, 250f), h(120f, 225f, 250f), v(255f, 56f, 160f)),
            "ㅔ" to listOf(h(100f, 225f, 250f), v(255f, 56f, 160f), v(280f, 56f, 160f)),
            "ㅖ" to listOf(h(90f, 225f, 250f), h(120f, 225f, 250f), v(255f, 56f, 160f), v(280f, 56f, 160f)),
            "ㅗ" to listOf(v(175f, 152f, 175f), h(185f, 135f, 245f)),
            "ㅛ" to listOf(v(165f, 152f, 172f), v(190f, 152f, 172f), h(185f, 135f, 225f)),
            "ㅜ" to listOf(h(160f, 135f, 245f), v(175f, 168f, 195f)),
            "ㅠ" to listOf(h(160f, 135f, 245f), v(175f, 168f, 195f), v(200f, 168f, 195f)),
            "ㅚ" to listOf(v(175f, 152f, 175f), h(185f, 135f, 245f), v(260f, 56f, 200f)),
            "ㅘ" to listOf(v(175f, 152f, 175f), h(185f, 135f, 245f), v(260f, 56f, 200f), h(110f, 260f, 285f)),
            "ㅙ" to listOf(v(175f, 152f, 175f), h(185f, 135f, 245f), v(260f, 56f, 200f), h(110f, 260f, 285f), v(300f, 56f, 200f)),
            "ㅝ" to listOf(h(160f, 135f, 245f), v(175f, 168f, 195f), h(185f, 185f, 210f), v(225f, 56f, 220f)),
            "ㅞ" to listOf(h(160f, 135f, 245f), v(175f, 168f, 195f), h(185f, 185f, 210f), v(225f, 56f, 220f), v(255f, 56f, 220f)),
            "ㅟ" to listOf(h(160f, 135f, 245f), v(185f, 168f, 195f), v(260f, 56f, 200f)),
            "ㅢ" to listOf(h(160f, 135f, 225f), v(240f, 56f, 180f)),
        )

        /** C's horizontal dot (ㅓ·ㅕ's first stroke beside C): no vowel yet, "ㄱㆍ". */
        val DOT_C: VowelStroke = h(100f, 225f, 245f)

        /** Canonical strokes of each bare vowel (no consonant composing: no C; SWIPE 3.6.3 "앞 획 기준", t = 20). */
        val SHAPES: Map<String, List<VowelStroke>> = mapOf(
            "ㅣ" to listOf(I),
            "ㅡ" to listOf(h(100f, 50f, 150f)),
            "ㅏ" to listOf(I, DOT_A),
            "ㅑ" to listOf(I, h(80f, 100f, 125f), h(120f, 100f, 125f)),
            "ㅐ" to listOf(I, DOT_A, v(130f, 40f, 160f)),
            "ㅒ" to listOf(I, h(80f, 100f, 125f), h(120f, 100f, 125f), v(130f, 40f, 160f)),
            "ㅓ" to listOf(h(100f, 60f, 85f), v(87f, 40f, 160f)),
            "ㅕ" to listOf(h(80f, 60f, 85f), h(120f, 60f, 85f), v(87f, 40f, 160f)),
            "ㅔ" to listOf(h(100f, 60f, 85f), v(87f, 40f, 160f), v(120f, 40f, 160f)),
            "ㅖ" to listOf(h(80f, 60f, 85f), h(120f, 60f, 85f), v(87f, 40f, 160f), v(120f, 40f, 160f)),
            "ㅗ" to listOf(v(100f, 60f, 85f), h(90f, 50f, 150f)),
            "ㅛ" to listOf(v(80f, 60f, 85f), v(120f, 60f, 85f), h(90f, 50f, 150f)),
            "ㅜ" to listOf(h(100f, 50f, 150f), v(100f, 100f, 125f)),
            "ㅠ" to listOf(h(100f, 50f, 150f), v(80f, 100f, 125f), v(120f, 100f, 125f)),
            "ㅚ" to listOf(v(100f, 60f, 85f), h(90f, 50f, 150f), v(160f, 40f, 160f)),
            "ㅘ" to listOf(v(100f, 60f, 85f), h(90f, 50f, 150f), v(160f, 40f, 160f), h(100f, 160f, 185f)),
            "ㅙ" to listOf(v(100f, 60f, 85f), h(90f, 50f, 150f), v(160f, 40f, 160f), h(100f, 160f, 185f), v(190f, 40f, 160f)),
            "ㅝ" to listOf(h(100f, 50f, 150f), v(100f, 100f, 125f), h(130f, 110f, 135f), v(137f, 60f, 170f)),
            "ㅞ" to listOf(h(100f, 50f, 150f), v(100f, 100f, 125f), h(130f, 110f, 135f), v(137f, 60f, 170f), v(170f, 60f, 170f)),
            "ㅟ" to listOf(h(100f, 50f, 150f), v(100f, 100f, 125f), v(160f, 60f, 170f)),
            "ㅢ" to listOf(h(100f, 50f, 150f), v(160f, 20f, 130f)),   // ㅡ on the ㅣ's lower third (SWIPE 0.33)
        )
    }
}
