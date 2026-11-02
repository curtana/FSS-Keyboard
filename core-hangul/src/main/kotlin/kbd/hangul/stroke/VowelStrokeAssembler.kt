package kbd.hangul.stroke

import kbd.hangul.jamo.Jamo
import kotlin.math.max
import kotlin.math.min

/** Axis of a vowel stroke (SWIPE 3.1): the larger component of the press-to-release displacement. */
enum class StrokeAxis { HORIZONTAL, VERTICAL }

/**
 * One handwriting vowel stroke (SWIPE 3.1, 3.6, HLD 4.3, issues #173, #179): its axis and its start and
 * end in screen dp. Its length never decides what it is (issue #179); only the two relative-size rules
 * of SWIPE 3.6.3 compare two strokes. Only → and ↓ strokes get here (a reversed stroke is dropped
 * before), so `x0 ≤ x1` for a horizontal and `y0 ≤ y1` for a vertical one.
 */
data class VowelStroke(val axis: StrokeAxis, val x0: Float, val y0: Float, val x1: Float, val y1: Float) {
    val vertical: Boolean get() = axis == StrokeAxis.VERTICAL

    /** A vertical stroke's place x / a horizontal stroke's height y (its start, SWIPE 3.6.3 "자리 x", "높이 y"). */
    val lineX: Float get() = x0
    val lineY: Float get() = y0
    val top: Float get() = min(y0, y1)
    val bottom: Float get() = max(y0, y1)
    val left: Float get() = min(x0, x1)
    val right: Float get() = max(x0, x1)

    /** The middle height (y0 + y1) / 2; with [midX] the middle point P of C's two zones (SWIPE 0.43 3.6.3, issue #217). */
    val midY: Float get() = (y0 + y1) / 2

    /** The middle x (x0 + x1) / 2; with [midY] the middle point P of C's two zones (SWIPE 0.43 3.6.3, issue #217). */
    val midX: Float get() = (x0 + x1) / 2

    /** Length along the axis, for the two relative-size rules only (SWIPE 3.6.3: ㅏ / ㅗ bare, ㅓ / ㅢ 0.5). */
    val span: Float get() = if (vertical) bottom - top else right - left
}

/**
 * The drawn consonant's place C (SWIPE 3.6.3 "자음 기준 C", 9.5, SRS TBD-24, LLD-04 §6.3): the bounding box,
 * in the same screen dp as the strokes, where the center consonant is drawn. A layout constant
 * (`SwipeLayout.consonantBox`); the assembler gets it whenever a consonant is composing.
 */
data class ConsonantBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** What a stroke did (LLD-04 §6.2): the composer's vowel started, changed, stayed (pending), or the stroke was ignored. */
sealed interface StrokePush {
    data class Started(val v: Jamo) : StrokePush
    data class Changed(val v: Jamo) : StrokePush
    /** Taken, the composer's vowel unchanged; the app shows [VowelStrokeAssembler.pendingGlyph]. */
    data object Pending : StrokePush
    /** Fits no candidate's role and place (SWIPE 3.3, 3.6): no input, the error vibration. */
    data object Ignored : StrokePush
}

/** ⌫ on the assembler (SWIPE 3.5): nothing to take back, or one stroke went and the composer must undo its last unit or not. */
sealed interface StrokePop {
    data object Empty : StrokePop
    data class Popped(val undoComposer: Boolean) : StrokePop
}

/**
 * Where a pending dot stands beside the syllable in the center place (SWIPE 9.5, owner 2026-11-02, FSS#36): a vertical
 * stroke's dot (ㅗ·ㅛ's) under it, two side by side; a horizontal stroke's dot (ㅓ·ㅕ's) right of it, two one above the other.
 */
enum class DotPlace { UNDER, RIGHT }

/** Value copy for error recovery (LLD-09): the strokes and the C they were judged against. */
data class StrokeSnapshot(val strokes: List<VowelStroke>, val consonant: ConsonantBox? = null)

/**
 * Handwriting vowel assembler (SWIPE 3.3, 3.6 rules 15–16, HLD 4.3, issues #173, #179). The state is the
 * strokes taken so far and the consonant place C of the assembly; everything shown is a pure function of
 * them. Each stroke is read against the next-candidate table of SWIPE 3.6.2. The first stroke keeps both roles
 * (ㅣ / dot, ㅡ / dot) as candidates and the second stroke's place relative to it decides, with or without C
 * (SWIPE 0.43 3.6.3, owner 2026-11-02, issue #217). C's two zones — under-left / right-up of the 45° line through
 * its bottom-right corner — give the first stroke's shown role, break the ㅏ / ㅗ tie and close a one-stroke
 * assembly, and give later vertical strokes their role; without C (a bare vowel) ㅏ / ㅗ go by the two strokes'
 * relative size. No stroke is dropped for its place against C alone.
 *
 * A stroke that fits no candidate is ignored. Length decides nothing else; the
 * one other length rule is the ㅓ / ㅢ 0.5 exception (TBD-22).
 *
 * The composer's vowel is that of the longest prefix that is a vowel; the strokes after it show as ㆍ / ‥.
 * [tol] is the position tolerance t in dp (SRS 1.3, default key width × 0.4).
 */
class VowelStrokeAssembler(var tol: Float) {

    private val seq = mutableListOf<VowelStroke>()

    /** C of the open assembly, fixed by its first stroke (null = a bare vowel). */
    var consonant: ConsonantBox? = null
        private set

    val strokes: List<VowelStroke> get() = seq.toList()

    /** The composer's vowel now (null = none yet). */
    val vowel: Jamo? get() = reading(seq, consonant)?.composerVowel

    val isEmpty: Boolean get() = seq.isEmpty()

    /** Strokes after the shown vowel that make no vowel yet (> 0 = ㆍ / ‥ shown). */
    val pendingStrokes: Int get() = reading(seq, consonant)?.pending ?: 0

    /**
     * Takes [s] if a candidate fits. [c] = the consonant place now (null = none composing); it is read only
     * by the first stroke of an assembly and kept until the assembly closes (SWIPE 9.5: C changes only on a
     * consonant tap or ⌫, which close or shorten the assembly).
     */
    fun push(s: VowelStroke, c: ConsonantBox? = null): StrokePush {
        val cc = if (seq.isEmpty()) c else consonant
        val next = reading(seq + s, cc) ?: return StrokePush.Ignored
        val before = reading(seq, consonant)?.composerVowel
        if (seq.isEmpty()) consonant = cc
        seq += s
        val after = next.composerVowel
        return when {
            after == before -> StrokePush.Pending
            before == null -> StrokePush.Started(checkNotNull(after))
            else -> StrokePush.Changed(checkNotNull(after))
        }
    }

    /** What [s] would show if pushed now with [c], without pushing (the guide, SWIPE 9.1): the vowel or ㆍ / ‥, null if ignored. */
    fun preview(s: VowelStroke, c: ConsonantBox? = null): String? {
        val r = reading(seq + s, if (seq.isEmpty()) c else consonant) ?: return null
        return if (r.pending > 0) r.glyph else r.composerVowel?.compat?.toString()
    }

    fun pop(): StrokePop {
        if (seq.isEmpty()) return StrokePop.Empty
        val before = vowel
        seq.removeAt(seq.lastIndex)
        val changed = vowel != before
        if (seq.isEmpty()) consonant = null
        return StrokePop.Popped(undoComposer = changed)
    }

    /**
     * Another input closes the assembly (SWIPE 3.3 "조립 끝"); true when strokes that made no vowel yet
     * were dropped (the caller vibrates). A closed vowel never reopens.
     */
    fun close(): Boolean {
        val dropped = pendingStrokes > 0
        seq.clear()
        consonant = null
        return dropped
    }

    /** The temporary glyph after the composition: ㆍ for one pending stroke, ‥ for two or more (SWIPE 3.4). */
    fun pendingGlyph(): String = reading(seq, consonant)?.glyph ?: ""

    /**
     * True in the bare ‥ states (SWIPE 0.36 3.4, D-64): [pendingGlyph] "‥" takes the place of the composer's last
     * character (its ㅣ / ㅡ) on screen; the composer itself keeps that vowel for ⌫ and closing.
     */
    val pendingReplacesVowel: Boolean get() = reading(seq, consonant)?.hidesVowel ?: false

    /**
     * The dots [pendingGlyph] stands for, one per stroke, for the center place (SWIPE 9.5, owner 2026-11-02, FSS#36):
     * a vertical stroke is ㅗ·ㅛ's dot ([DotPlace.UNDER]), a horizontal one ㅓ·ㅕ's ([DotPlace.RIGHT]). In the ‥ states
     * that hide the composer's vowel ([pendingReplacesVowel]) both strokes are dots. Empty when no dot is pending.
     */
    val pendingDots: List<DotPlace>
        get() {
            val r = reading(seq, consonant) ?: return emptyList()
            val n = if (r.hidesVowel) seq.size else r.pending
            return seq.takeLast(n).map { if (it.vertical) DotPlace.UNDER else DotPlace.RIGHT }
        }

    fun snapshot(): StrokeSnapshot = StrokeSnapshot(seq.toList(), consonant)

    fun restore(s: StrokeSnapshot) {
        seq.clear()
        seq += s.strokes
        consonant = if (s.strokes.isEmpty()) null else s.consonant
    }

    /** The state [s] reaches with [c] (tests, LLD-11 VS-13: one state per stroke list, so at most one vowel shows); null = some stroke ignored. */
    internal fun stateFor(s: List<VowelStroke>, c: ConsonantBox? = null): String? = reading(s, c)?.states?.lastOrNull()?.name

    /** The vowel [s] shows with [c] (tests). */
    internal fun vowelFor(s: List<VowelStroke>, c: ConsonantBox? = null): Jamo? = reading(s, c)?.composerVowel

    // --- debug text (issue #217: the personal debug build's logcat; never judged with) ---------------

    /**
     * [s]'s zone by C if it were pushed now with [c] (SWIPE 0.43 3.6.3 two zones), with the numbers it was read
     * from: the middle point P, d = (P.y − C.B) − (P.x − C.R) (> 0 = under-left), and what that zone means here.
     */
    fun roleText(s: VowelStroke, c: ConsonantBox? = null): String {
        val cc = (if (seq.isEmpty()) c else consonant) ?: return "no C (bare vowel)"
        val dd = d(s, cc)
        val ul = dd > 0f
        val at = "P (${f(s.midX)},${f(s.midY)}), d ${f(dd)}; C.R ${f(cc.right)}, C.B ${f(cc.bottom)}"
        val zone = if (ul) "under-left" else "right-up"
        return when {
            seq.isEmpty() && s.vertical -> "first vertical $zone: shows ${if (ul) "a dot" else "ㅣ"}, both roles kept ($at)"
            seq.isEmpty() -> "first horizontal $zone: shows ${if (ul) "ㅡ" else "a dot"}, both roles kept ($at)"
            s.vertical -> "vertical $zone = ${if (ul) "dot" else "ㅣ"} ($at)"
            else -> "not by C (a later horizontal stroke)"
        }
    }

    /** Why [s] would be ignored if pushed now with [c]; null when it would be taken. */
    fun ignoreReason(s: VowelStroke, c: ConsonantBox? = null): String? {
        if (reading(seq + s, if (seq.isEmpty()) c else consonant) != null) return null
        return "no ${if (s.vertical) "vertical" else "horizontal"} zone of the next candidates fits"
    }

    /** The assembly now: state[candidates], shown vowel and ㆍ / ‥, e.g. "V1[ㅣㅏㅑㅐㅒㅗㅛㅚㅘㅙ] vowel=- pending=ㆍ". */
    fun stateText(): String {
        val r = reading(seq, consonant) ?: return "invalid"
        val name = r.states.lastOrNull()?.let { "${it.name}[${it.candidates}]" } ?: "empty"
        return "$name vowel=${r.composerVowel?.compat ?: "-"} pending=${r.glyph.ifEmpty { "-" }}"
    }

    // --- reading the strokes (SWIPE 3.6.2 next-candidate table, 3.6.3 C roles and zones) ------------

    /** One state per stroke of the candidate table (SWIPE 3.6.2), [v] its complete vowel (null = not a vowel yet). */
    private enum class St(val v: Jamo?, val candidates: String) {   // candidates: the debug log only (issue #217)
        V1(Jamo.I, "ㅣㅏㅑㅐㅒㅗㅛㅚㅘㅙ"), H1(Jamo.EU, "ㅡㅓㅕㅔㅖㅢㅜㅠㅟㅝㅞ"), // one vertical (ㅣ, or ㅗ·ㅛ's dot) / one horizontal (ㅡ, or ㅓ·ㅕ's dot); with C too
        A(Jamo.A, "ㅏㅑㅐㅒ"), YA(Jamo.YA, "ㅑㅒ"), AE(Jamo.AE, "ㅐ"), YAE(Jamo.YAE, "ㅒ"),
        O(Jamo.O, "ㅗㅚㅘㅙ"), OE(Jamo.OE, "ㅚㅘㅙ"), WA(Jamo.WA, "ㅘㅙ"), WAE(Jamo.WAE, "ㅙ"), VV(null, "ㅛ"), YO(Jamo.YO, "ㅛ"),
        U(Jamo.U, "ㅜㅠㅟㅝㅞ"), YU(Jamo.YU, "ㅠ"), WI(Jamo.WI, "ㅟ"), UDOT(null, "ㅝㅞ"), WEO(Jamo.WEO, "ㅝㅞ"), WE(Jamo.WE, "ㅞ"),
        EOUI(null, "ㅓㅢㅔ"), E(Jamo.E, "ㅔ"), HH(null, "ㅕㅖ"), YEO(Jamo.YEO, "ㅕㅖ"), YE(Jamo.YE, "ㅖ"),   // EOUI: ㅓ or ㅢ, by [eoOrUi]
    }

    /**
     * The states after each stroke, the composer's vowel, and the strokes after it (ㆍ / ‥). [hidesVowel]: the
     * composer keeps [composerVowel] but the screen shows ‥ in its place (bare ‥ states, SWIPE 0.36 3.4, D-64).
     */
    private class Reading(val states: List<St>, val composerVowel: Jamo?, val pending: Int, val hidesVowel: Boolean = false) {
        val glyph: String get() = when {
            hidesVowel -> "‥"
            pending == 0 -> ""
            pending == 1 -> "ㆍ"
            else -> "‥"
        }
    }

    private fun reading(s: List<VowelStroke>, c: ConsonantBox?): Reading? {
        val states = ArrayList<St>(s.size)
        var st: St? = null
        for (i in s.indices) {
            st = next(st, s, i, c, tol) ?: return null
            states += st
        }
        // Bare two verticals / two horizontals (before ㅛ / ㅕ): the second stroke made the first a dot, so the
        // screen shows "‥" alone; the composer keeps the first stroke's ㅣ / ㅡ, which ⌫ or closing brings back
        // (SWIPE 0.36 3.4, design decision #192, LLD D-64).
        // With C the same when the first stroke showed its ㅣ / ㅡ by its zone; when it showed a dot, "‥" is just the two dots
        // (SWIPE 0.43 3.6.3 ㅛ ②·ㅕ ②: the second stroke makes the first a dot).
        if (st == St.VV || st == St.HH) {
            val first = vowelOf(states[0], s, c) ?: return Reading(states, null, s.size)
            return Reading(states, first, 1, hidesVowel = true)
        }
        for (k in states.size downTo 1) {
            val v = vowelOf(states[k - 1], s, c)
            if (v != null) return Reading(states, v, s.size - k)
        }
        return Reading(states, null, s.size)
    }

    /** [st]'s vowel; with C one stroke shows by its zone (SWIPE 0.43 3.6.3 "보임"): ㅣ right-up, ㅡ under-left, else a dot. */
    private fun vowelOf(st: St, s: List<VowelStroke>, c: ConsonantBox?): Jamo? = when {
        st == St.EOUI -> eoOrUi(s[0], s[1])
        c == null -> st.v
        st == St.V1 -> if (underLeft(s[0], c)) null else Jamo.I
        st == St.H1 -> if (underLeft(s[0], c)) Jamo.EU else null
        else -> st.v
    }

    /** A later vertical stroke's role by C's zone: right-up = ㅣ, under-left = dot (SWIPE 0.43 3.6.3). */
    private enum class Role { I, DOT }

    companion object {
        /*
         * Constants of SWIPE 3.6.3 in one place ("LLD는 이 값을 상수 한곳에 둔다"). t = posTolDp; the multiples
         * of t are design defaults (1, 1/2, 1/4). No margin depends on a stroke's length (issue #179: the
         * 0.34 code's m = min(t, len / 3) and the key-width distance caps are gone).
         */
        private const val HALF = 0.5f
        private const val QUARTER = 0.25f

        /** ㅓ / ㅢ split (issue #179): the line above 2/3 of the ㅣ's height from its top is ㅓ's dot. */
        const val EO_UI_SPLIT = 2f / 3f

        /** TBD-22 (owner): on the lower third a line shorter than the ㅣ × this is still ㅓ's dot. */
        const val EO_SHORT_RATIO = 0.5f

        private fun f(v: Float) = String.format(java.util.Locale.ROOT, "%.1f", v)

        /**
         * C's two zones (SWIPE 0.43 3.6.3, owner decision 2026-11-02, issue #217): the stroke's middle point P (start
         * and end averaged) against the 45° line through C's bottom-right corner, d = (P.y − C.B) − (P.x − C.R).
         * d > 0 = under-left (a vertical dot, a ㅡ), else right-up (a ㅣ, a horizontal dot); the line itself is right-up.
         */
        private fun d(s: VowelStroke, c: ConsonantBox) = (s.midY - c.bottom) - (s.midX - c.right)
        private fun underLeft(s: VowelStroke, c: ConsonantBox) = d(s, c) > 0f

        /** A vertical stroke's role by C's zone. */
        private fun vRole(v: VowelStroke, c: ConsonantBox): Role = if (underLeft(v, c)) Role.DOT else Role.I

        /** With C, a later vertical must have [want]'s role; a bare vowel has no C to check. */
        private fun role(v: VowelStroke, c: ConsonantBox?, want: Role) = c == null || vRole(v, c) == want

        // Zones relative to the earlier strokes (SWIPE 3.6.3 "앞 획 기준 구역"): sides and heights only.

        /** "오른쪽": a horizontal within vertical [sv]'s height that reaches its right (ㅏ ②, ㅑ ③, ㅘ ④). Its start L is free. */
        private fun right(sv: VowelStroke, h: VowelStroke, t: Float) =
            h.lineY >= sv.top - QUARTER * t && h.lineY <= sv.bottom + QUARTER * t && h.right >= sv.lineX + HALF * t

        /** "밑줄" of ㅗ ②: under dot [d] (passes below it) at or below its middle height. */
        private fun underO(d: VowelStroke, h: VowelStroke, t: Float) =
            h.left <= d.lineX + t && h.right >= d.lineX - t && h.lineY >= (d.top + d.bottom) / 2

        /** "밑줄" of ㅛ ③: below both dots, passing under them. */
        private fun underYo(d1: VowelStroke, d2: VowelStroke, h: VowelStroke, t: Float) =
            h.lineY >= max(d1.bottom, d2.bottom) - HALF * t && h.right >= d1.lineX - t && h.left <= d2.lineX + t

        /** "옆": right beside vertical [p], overlapping its range (ㅛ ②, ㅔ ③, ㅖ ④, ㅞ ⑤). How much does not matter. */
        private fun beside(p: VowelStroke, v: VowelStroke, t: Float) =
            v.lineX >= p.lineX + HALF * t && v.top <= p.bottom + QUARTER * t && v.bottom >= p.top - QUARTER * t

        /** "끝": at horizontal [h]'s right end or beyond (no height, no distance cap). */
        private fun end(h: VowelStroke, v: VowelStroke, t: Float) = v.lineX >= h.right - t

        /** "밑": a horizontal under [h]. */
        private fun under(h: VowelStroke, n: VowelStroke, t: Float) = n.lineY >= h.lineY + HALF * t

        /** Bare ㅜ ② (and ㅠ ③'s place): below ㅡ [h], before its right end, starting near or below its height. */
        private fun belowBare(h: VowelStroke, v: VowelStroke, t: Float) =
            v.lineX >= h.left - t && v.lineX < h.right - t && v.top >= h.lineY - HALF * t

        /**
         * ㅓ / ㅢ (SWIPE 3.6.3, D-62): by the height thirds r = (y − T) / (B − T) — r < 2/3 = ㅓ, else ㅢ unless the
         * line is shorter than the ㅣ × 0.5 (TBD-22); with C too, whatever the first stroke's zone (SWIPE 0.43).
         */
        private fun eoOrUi(h: VowelStroke, i: VowelStroke): Jamo = when {
            heightShare(h, i) < EO_UI_SPLIT -> Jamo.EO
            h.span < i.span * EO_SHORT_RATIO -> Jamo.EO
            else -> Jamo.UI
        }

        /** Where [h]'s height falls on [i]'s range from the top: r = (y − T) / (B − T). */
        internal fun heightShare(h: VowelStroke, i: VowelStroke): Float {
            val span = i.bottom - i.top
            return if (span <= 0f) 0f else (h.lineY - i.top) / span
        }

        /**
         * The state after stroke [i] of [s] from [st] (null = before the first), or null when it fits no
         * candidate (SWIPE 3.6.2 table, row by row). [c] = C (null = bare vowel).
         */
        private fun next(st: St?, s: List<VowelStroke>, i: Int, c: ConsonantBox?, t: Float): St? {
            val n = s[i]
            val v = n.vertical
            fun I(want: Role = Role.I) = role(n, c, want)
            return when (st) {
                null -> if (v) St.V1 else St.H1   // both roles stay candidates, with C too (SWIPE 0.43 3.6.3)
                // ㅣ or ㅗ·ㅛ's dot: the second stroke decides
                St.V1 -> when {
                    v -> if (I(Role.DOT) && beside(s[0], n, t)) St.VV else null   // ㅛ ②: with C an under-left dot
                    c != null -> {
                        val r = right(s[0], n, t)
                        val u = underO(s[0], n, t)
                        when {
                            r && u -> if (underLeft(s[0], c)) St.O else St.A   // both fit: the first stroke's zone (3.6.4 row 1)
                            r -> St.A
                            u -> St.O
                            else -> null
                        }
                    }
                    // bare: ㅏ / ㅗ by the strokes' relative size, equal = ㅏ
                    n.span <= s[0].span -> if (right(s[0], n, t)) St.A else null
                    else -> if (underO(s[0], n, t)) St.O else null
                }
                // ㅡ or ㅓ·ㅕ's dot
                St.H1 -> when {
                    !v -> if (under(s[0], n, t) && n.left <= s[0].right + QUARTER * t && n.right >= s[0].left - QUARTER * t) St.HH else null
                    // with C the vertical's zone picks ㅜ (dot, below) or ㅓ·ㅢ (ㅣ, end or crossing); no x split (D-63)
                    c != null -> when {
                        n.lineX < s[0].left - t -> null
                        I(Role.DOT) -> St.U
                        else -> St.EOUI
                    }
                    end(s[0], n, t) -> St.EOUI
                    n.lineX >= s[0].left - t -> if (n.top >= s[0].lineY - HALF * t) St.U else St.EOUI   // below = ㅜ, from above = crossing
                    else -> null
                }
                St.A -> when {
                    v -> if (I() && end(s[1], n, t) && n.lineX >= s[0].lineX + t) St.AE else null
                    else -> if (right(s[0], n, t) && n.lineY >= s[1].lineY + HALF * t) St.YA else null
                }
                St.YA -> if (v && I() && n.lineX >= max(s[1].right, s[2].right) - t && n.lineX >= s[0].lineX + t) St.YAE else null
                St.O -> if (v && I() && end(s[1], n, t)) St.OE else null
                St.OE -> if (!v && right(s[2], n, t)) St.WA else null
                St.WA -> if (v && I() && end(s[3], n, t) && n.lineX >= s[2].lineX + t) St.WAE else null
                St.VV -> if (!v && underYo(s[0], s[1], n, t)) St.YO else null
                St.U -> when {
                    !v -> if (under(s[0], n, t) && n.left >= s[1].lineX - HALF * t) St.UDOT else null
                    c != null -> when {
                        I(Role.DOT) -> if (n.lineX >= s[1].lineX + HALF * t) St.YU else null
                        I(Role.I) -> if (end(s[0], n, t)) St.WI else null
                        else -> null
                    }
                    end(s[0], n, t) -> St.WI
                    belowBare(s[0], n, t) && n.lineX >= s[1].lineX + HALF * t -> St.YU
                    else -> null
                }
                St.UDOT -> if (v && I() && n.lineX >= s[2].left - t) St.WEO else null
                St.WEO -> if (v && I() && beside(s[3], n, t)) St.WE else null
                St.EOUI -> if (v && I() && beside(s[1], n, t)) St.E else null
                St.HH -> if (v && I() && n.lineX >= min(s[0].left, s[1].left) - t) St.YEO else null
                St.YEO -> if (v && I() && beside(s[2], n, t)) St.YE else null
                St.AE, St.YAE, St.WAE, St.YO, St.YU, St.WI, St.WE, St.E, St.YE -> null
            }
        }
    }
}
