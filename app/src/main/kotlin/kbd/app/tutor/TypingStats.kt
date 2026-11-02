package kbd.app.tutor

import kbd.hangul.jamo.Jamo
import kotlin.math.max

/**
 * One lesson-5 sentence (FR-TUT-10, SWIPE 10 "타수와 정확도"). [perMinute] is null ("–") when
 * nothing was typed or no time passed; [accuracy] is a percentage 0..100.
 */
data class SentenceStats(val keystrokes: Int, val millis: Long, val accuracy: Double, val perMinute: Double?)

/** All sentences: [perMinute] null when no sentence had one, [accuracy] null when there were no sentences. */
data class OverallStats(val perMinute: Double?, val accuracy: Double?)

/** Typing speed and accuracy of the tutor's sentences (LLD-07 §2.4, FR-TUT-10). Display only, never used for thresholds. */
object TypingStats {
    /** Medials typed with two dubeolsik keys: ㅘ ㅙ ㅚ ㅝ ㅞ ㅟ ㅢ (jung index). */
    private val TWO_KEY_JUNG = setOf(9, 10, 11, 14, 15, 16, 19)

    /** Compound finals ㄳ ㄵ ㄶ ㄺ ㄻ ㄼ ㄽ ㄾ ㄿ ㅀ ㅄ (jong index). Double consonants ㄲ ㅆ are one key. */
    private val TWO_KEY_JONG = setOf(3, 5, 6, 9, 10, 11, 12, 13, 14, 15, 18)

    /**
     * Keys pressed to type [text] on dubeolsik: one per jamo (shift not counted, so ㄲ, ㅒ, ㅖ are 1),
     * two for ㅘ ㅙ ㅚ ㅝ ㅞ ㅟ ㅢ and the compound finals; spaces, digits and symbols 1 each.
     * 곡기 = 5, 과일 = 6.
     */
    fun keystrokes(text: String): Int = text.sumOf { keysOf(it) }

    private fun keysOf(ch: Char): Int {
        val idx = ch.code - 0xAC00
        if (idx in 0 until 11172) {
            val jung = idx % 588 / 28
            val jong = idx % 28
            return 1 + (if (jung in TWO_KEY_JUNG) 2 else 1) + when {
                jong == 0 -> 0
                jong in TWO_KEY_JONG -> 2
                else -> 1
            }
        }
        val j = Jamo.ofCompat(ch) ?: return 1
        return when {
            j.jungIndex in TWO_KEY_JUNG -> 2
            j.jongIndex in TWO_KEY_JONG && j.choIndex == -1 -> 2   // a compound final written alone
            else -> 1
        }
    }

    /** Character-level Levenshtein distance (a syllable, space or symbol is one character). */
    fun editDistance(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) {
                val sub = prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(sub, prev[j] + 1, cur[j - 1] + 1)
            }
            prev = cur
        }
        return prev[b.length]
    }

    /** (target length − edit distance) ÷ target length × 100, not below 0. */
    fun accuracy(target: String, typed: String): Double {
        if (target.isEmpty()) return if (typed.isEmpty()) 100.0 else 0.0
        return max(0.0, (target.length - editDistance(target, typed)) * 100.0 / target.length)
    }

    /** [millis] = first stroke press → last stroke release of the sentence. */
    fun sentence(target: String, typed: String, millis: Long): SentenceStats {
        val keys = keystrokes(typed)
        val perMinute = if (typed.isEmpty() || millis <= 0L) null else keys * 60_000.0 / millis
        return SentenceStats(keys, millis, accuracy(target, typed), perMinute)
    }

    /** Overall speed = Σ keystrokes ÷ Σ minutes over sentences with a speed; overall accuracy = mean of all sentences. */
    fun overall(list: List<SentenceStats>): OverallStats {
        val timed = list.filter { it.perMinute != null }
        val millis = timed.sumOf { it.millis }
        val perMinute = if (timed.isEmpty() || millis <= 0L) null else timed.sumOf { it.keystrokes } * 60_000.0 / millis
        val accuracy = if (list.isEmpty()) null else list.sumOf { it.accuracy } / list.size
        return OverallStats(perMinute, accuracy)
    }
}
