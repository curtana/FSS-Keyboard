package kbd.app.tutor

import kbd.hangul.cheonjiin.CjElement
import kbd.hangul.jamo.Jamo
import kbd.hangul.stroke.ConsonantBox
import kbd.hangul.stroke.StrokeAxis
import kbd.hangul.stroke.StrokePush
import kbd.hangul.stroke.VowelStroke
import kbd.hangul.stroke.VowelStrokeAssembler
import kbd.stroke.geom.Edge
import kbd.stroke.threshold.Thresholds
import kotlin.math.abs
import kotlin.math.max

/**
 * One action of a practice item as the target asks for it (LLD-07 §2.5, SWIPE 10.1 table, issue #173).
 * The measuring steps (1-1, 1-2) judge only what the measured thresholds do not decide: the key, and
 * for a vowel stroke its axis and direction; 3-1 and 3-4 measure nothing and are judged by the result.
 */
sealed interface ExpectedAction {
    /** 1-1, 1-2 ①: a touch that starts on [key]; its movement is not looked at. */
    data class Press(val key: String) : ExpectedAction

    /** 3-1 ①, 3-4 ①: a tap on [key] by the real decision (displacement under the decision `minCrossDp`). */
    data class Tap(val key: String) : ExpectedAction

    /**
     * 1-2: a vowel stroke on the [vertical] axis going → / ↓, whatever its length (any displacement
     * counts as a stroke; none at all has no axis and is wrong, LLD decision D-60).
     */
    data class Stroke(val vertical: Boolean) : ExpectedAction

    /**
     * 3-1 (judged by result): a real vowel stroke (≥ `minCrossDp`) on the [vertical] axis going → / ↓
     * that the assembler takes after the try's earlier strokes (stroke order and position, SWIPE 3.6);
     * the last one must leave the item's vowel.
     */
    data class Written(val vertical: Boolean) : ExpectedAction

    /** 3-4 (issue #160, judged by result): a stroke on the cheonjiin key that gives element [e] (tap ㆍ, ↑·↓ ㅣ, → ㅡ). */
    data class Element(val e: CjElement) : ExpectedAction
}

/**
 * What one action did (HLD 7.1 "동작 기록"): the pressed [key]; its press-to-release displacement
 * ([x0], [y0]) → ([x1], [y1]) in dp; [tap] = its larger component is under the decision `minCrossDp`;
 * [firstEdge] = first key edge crossed at that `minCrossDp` (the cheonjiin key's element).
 */
data class ActionRecord(
    val key: String,
    val x0: Float, val y0: Float, val x1: Float, val y1: Float,
    val tap: Boolean,
    val firstEdge: Edge?,
) {
    val dx: Float get() = x1 - x0
    val dy: Float get() = y1 - y0

    /** The larger displacement component, the measure of taps, dots and long strokes alike (HLD 7.1). */
    val length: Float get() = max(abs(dx), abs(dy))

    /** null when the finger did not move at all: no axis (D-60). */
    val vertical: Boolean? get() = if (dx == 0f && dy == 0f) null else abs(dy) > abs(dx)

    /** → / ↓ (SWIPE 3.2); false for ← / ↑ and for no movement. */
    val forward: Boolean get() = when (vertical) {
        null -> false
        true -> dy > 0f
        false -> dx > 0f
    }

    /** The stroke as the assembler takes it (SWIPE 3.1: axis and ends; its length decides nothing, issue #179). */
    fun stroke() = VowelStroke(if (vertical == true) StrokeAxis.VERTICAL else StrokeAxis.HORIZONTAL, x0, y0, x1, y1)

    /**
     * The cheonjiin element this stroke gives on the cheonjiin key (SWIPE 5.4, FR-SWP-61): tap ㆍ,
     * first edge up or down ㅣ, right ㅡ. Left is the screen edge on the keyboard (never crossed),
     * so a left exit counts as the tap it is there.
     */
    val element: CjElement
        get() = when (firstEdge) {
            Edge.TOP, Edge.BOTTOM -> CjElement.I
            Edge.RIGHT -> CjElement.EU
            null, Edge.LEFT -> CjElement.DOT
        }
}

/**
 * How a practice item is judged and taught: the [glyph] shown on ✓, the [actions] in order, the
 * [howTo] line on ✗, and for 3-1 the [vowel] the strokes must leave.
 */
data class ItemSpec(val glyph: String, val actions: List<ExpectedAction>, val howTo: String, val vowel: Jamo? = null)

/** Result of judging the actions of one try so far. */
sealed interface Judgement {
    /** All actions so far match; more are needed. */
    data object Pending : Judgement

    /** As many actions as the item has, all matching (✓). */
    data object Right : Judgement

    /** The last action does not match its turn (✗). */
    data object Wrong : Judgement
}

/**
 * Item judgement of the tutor (LLD-07 §2.5, HLD 7.1, SWIPE 10.1, FR-TUT-12). Pure Kotlin; the
 * session makes one [ActionRecord] per released action and judges the try after each one.
 */
object TutorItemJudge {

    /** The record of [raw] at [th] (the decision values: the echo's, FR-TUT-09). */
    fun record(raw: RawStroke, th: Thresholds): ActionRecord {
        val a = raw.points.first()
        val b = raw.points.last()
        val r = ActionRecord(raw.key, a.x, a.y, b.x, b.y, tap = false, firstEdge = TutorMeasure.firstExit(raw, th.minCrossDp)?.edge)
        return r.copy(tap = r.length < th.minCrossDp)
    }

    /**
     * Judges the last of [records] against its turn in [spec]; earlier records are the matching ones before it.
     * [consonantBox] = the practice keyboard's place C (3-1 is written after a consonant tap, so judged against
     * C as the echo is, SWIPE 10.1 "입력 표시와 같다"); null = bare rules (no keyboard geometry known).
     */
    fun judge(spec: ItemSpec, records: List<ActionRecord>, th: Thresholds, consonantBox: ConsonantBox? = null): Judgement {
        val i = records.size - 1
        require(i in spec.actions.indices) { "record ${i + 1} of an item with ${spec.actions.size} actions" }
        val r = records[i]
        val last = i == spec.actions.size - 1
        val ok = when (val e = spec.actions[i]) {
            is ExpectedAction.Press -> r.key == e.key
            is ExpectedAction.Tap -> r.tap && r.key == e.key
            is ExpectedAction.Stroke -> r.vertical == e.vertical && r.forward
            is ExpectedAction.Written -> !r.tap && r.vertical == e.vertical && r.forward && written(spec, records, th, last, consonantBox)
            is ExpectedAction.Element -> r.key == HowTo.CHEONJIIN_KEY && r.element == e.e
        }
        if (!ok) return Judgement.Wrong
        return if (last) Judgement.Right else Judgement.Pending
    }

    /** 3-1: the try's strokes so far are all taken by the assembler, and the last leaves the item's vowel. */
    private fun written(spec: ItemSpec, records: List<ActionRecord>, th: Thresholds, last: Boolean, c: ConsonantBox?): Boolean {
        val a = VowelStrokeAssembler(th.posTolDp)
        for ((k, r) in records.withIndex()) {
            if (spec.actions[k] !is ExpectedAction.Written) continue
            if (a.push(r.stroke(), c) == StrokePush.Ignored) return false
        }
        return !last || a.vowel == spec.vowel
    }
}

/**
 * The how-to line of SWIPE 10.1 ("<목표> = <동작> → <동작>"): `→` is only the order, the swipe
 * direction is in words (LLD-07 §2.5). Never mentions measuring (FR-TUT-02).
 */
object HowTo {
    private const val CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"

    /**
     * The 19 practice consonants in dictionary order (issue #168, SWIPE 10장 "연습 자음을 돌려 쓴다"):
     * the 14 basic ones and the 5 doubles, the same order as the syllable table's initials.
     */
    val ROTATION: List<String> = CHO.map { it.toString() }

    /** [template] (an open syllable) with its initial replaced by [consonant]: 가 + ㄴ = 나. */
    fun withInitial(template: String, consonant: String): String {
        val code = template.single().code - 0xAC00
        val cho = CHO.indexOf(consonant.single())
        require(code in 0 until 11172 && cho >= 0) { "not an open syllable or initial: $template, $consonant" }
        return (0xAC00 + cho * 588 + code % 588).toChar().toString()
    }
    private const val JUNG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"

    /** One stroke of a vowel's stroke order (SWIPE 3.3): its axis, its role (ㅣ·ㅡ or a dot: the 1-2 sample kind only, never shown), and its place words (SWIPE 10.1 위치 말). */
    data class VowelStep(val vertical: Boolean, val long: Boolean, val place: String = "")

    // SWIPE 0.35 10.1 위치 말: the first stroke by the drawn consonant C, the others by the earlier strokes (3.6.3).
    private val I = VowelStep(true, true, "자음 오른쪽에서 ")
    private val EU = VowelStep(false, true, "자음 아래에 ")
    private val H_DOT = VowelStep(false, false, "자음 오른쪽에 ")
    private val V_DOT = VowelStep(true, false, "자음 아래에 ")
    private val A_DOT = VowelStep(false, false, "ㅣ 오른쪽에 ")
    private val EO_I = VowelStep(true, true, "점 오른쪽 끝에서 점이 위나 가운데에 오게 ")
    private val UI_I = VowelStep(true, true, "ㅡ 오른쪽 끝에서 ㅡ가 아래쪽에 오게 ")
    private val DOT_END_I = VowelStep(true, true, "점 오른쪽 끝에서 ")     // ㅕ, ㅝ, ㅐ·ㅒ·ㅙ
    private val O_EU = VowelStep(false, true, "점 아래에 ")
    private val U_DOT = VowelStep(true, false, "ㅡ 아래에서 ")
    private val BEYOND_I = VowelStep(true, true, "ㅡ 오른쪽 끝에서 ")       // ㅚ, ㅟ
    private val YEO_DOT = VowelStep(false, false, "첫 점 아래에 ")
    private val YO_DOT = VowelStep(true, false, "첫 점 오른쪽 옆에 ")
    private val WEO_DOT = VowelStep(false, false, "ㅜ의 점 오른쪽에 ")
    private val E_I = VowelStep(true, true, "앞 ㅣ 오른쪽 옆에 ")

    /** Each vowel's strokes in order (SWIPE 3.3) with the place words of SWIPE 10.1 (LLD decision D-59: on every stroke). */
    val STROKES: Map<Char, List<VowelStep>> = mapOf(
        'ㅣ' to listOf(I), 'ㅡ' to listOf(EU),
        'ㅏ' to listOf(I, A_DOT), 'ㅑ' to listOf(I, A_DOT, A_DOT),
        'ㅓ' to listOf(H_DOT, EO_I), 'ㅕ' to listOf(H_DOT, YEO_DOT, DOT_END_I),
        'ㅗ' to listOf(V_DOT, O_EU), 'ㅛ' to listOf(V_DOT, YO_DOT, O_EU),
        'ㅜ' to listOf(EU, U_DOT), 'ㅠ' to listOf(EU, U_DOT, U_DOT),
        'ㅐ' to listOf(I, A_DOT, DOT_END_I), 'ㅒ' to listOf(I, A_DOT, A_DOT, DOT_END_I),
        'ㅔ' to listOf(H_DOT, EO_I, E_I), 'ㅖ' to listOf(H_DOT, YEO_DOT, DOT_END_I, E_I),
        'ㅚ' to listOf(V_DOT, O_EU, BEYOND_I), 'ㅘ' to listOf(V_DOT, O_EU, BEYOND_I, A_DOT),
        'ㅙ' to listOf(V_DOT, O_EU, BEYOND_I, A_DOT, DOT_END_I),
        'ㅟ' to listOf(EU, U_DOT, BEYOND_I), 'ㅝ' to listOf(EU, U_DOT, WEO_DOT, DOT_END_I),
        'ㅞ' to listOf(EU, U_DOT, WEO_DOT, DOT_END_I, E_I),
        'ㅢ' to listOf(EU, UI_I),
    )

    /** The words of one stroke (SWIPE 10.1 위치 말 + 동작 말). "길게·짧게" are gone (issue #179). */
    fun stroke(s: VowelStep): String = s.place + if (s.vertical) "아래로" else "오른쪽으로"

    /** The extra line after a reversed stroke (SWIPE 10.1 덧붙임, 3.2). */
    const val REVERSE_HINT = "획은 아래로, 오른쪽으로만 그어요"

    /** The initial consonant and the vowel of a syllable with no final (가 → ㄱ, ㅏ). */
    fun split(syllable: String): Pair<String, Char> {
        val code = syllable.single().code - 0xAC00
        require(code in 0 until 11172 && code % 28 == 0) { "not an open syllable: $syllable" }
        return CHO[code / 588].toString() to JUNG[code % 588 / 28]
    }

    fun tap(key: String) = "$key 탭"

    fun line(glyph: String, actions: List<String>) = "$glyph = " + actions.joinToString(" → ")

    /** Label of the swipe keyboard's cheonjiin key (SWIPE 5.4, FR-SWP-60); the practice key carries it as [RawStroke.key]. */
    const val CHEONJIIN_KEY = "ㆍ ㅡ ㅣ"

    /** The how-to words of a cheonjiin element (SWIPE 10.1: ㆍ = 천지인 키 탭, ㅣ = 위로, ㅡ = 오른쪽으로). */
    fun element(e: CjElement) = when (e) {
        CjElement.DOT -> "천지인 키 탭"
        CjElement.I -> "천지인 키에서 위로 긋기"
        CjElement.EU -> "천지인 키에서 오른쪽으로 긋기"
    }

    /**
     * 3-4 (issue #160): tap the consonant of [glyph], then the cheonjiin elements [elements] in order;
     * judged by result (no measuring in this step, SWIPE 10.1).
     */
    fun cheonjiin(glyph: String, elements: List<CjElement>): ItemSpec {
        val c = split(glyph).first
        return ItemSpec(
            glyph,
            listOf(ExpectedAction.Tap(c)) + elements.map { ExpectedAction.Element(it) },
            line(glyph, listOf(tap(c)) + elements.map(::element)),
        )
    }

    /** 1-1: tap [key]. Only the key is judged. */
    fun press(key: String) = ItemSpec(key, listOf(ExpectedAction.Press(key)), line(key, listOf(tap(key))))

    /**
     * 1-2 (measuring): press the consonant key, then the vowel's strokes judged by axis and direction
     * only (SWIPE 10.1: the measured length is not looked at).
     */
    fun measured(glyph: String): ItemSpec {
        val (c, v) = split(glyph)
        val steps = checkNotNull(STROKES[v]) { "no strokes for $v" }
        return ItemSpec(glyph, listOf(ExpectedAction.Press(c)) + steps.map { ExpectedAction.Stroke(it.vertical) },
            line(glyph, listOf(tap(c)) + steps.map(::stroke)))
    }

    /** 3-1 (judged by result): tap the consonant, then write the vowel (SWIPE 10.1). */
    fun written(glyph: String): ItemSpec {
        val (c, v) = split(glyph)
        val steps = checkNotNull(STROKES[v]) { "no strokes for $v" }
        return ItemSpec(glyph, listOf(ExpectedAction.Tap(c)) + steps.map { ExpectedAction.Written(it.vertical) },
            line(glyph, listOf(tap(c)) + steps.map(::stroke)), vowel = Jamo.ofCompat(v))
    }
}
