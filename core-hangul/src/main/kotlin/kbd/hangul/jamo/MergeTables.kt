package kbd.hangul.jamo

import kbd.hangul.jamo.Jamo.A
import kbd.hangul.jamo.Jamo.AE
import kbd.hangul.jamo.Jamo.E
import kbd.hangul.jamo.Jamo.EO
import kbd.hangul.jamo.Jamo.EU
import kbd.hangul.jamo.Jamo.I
import kbd.hangul.jamo.Jamo.O
import kbd.hangul.jamo.Jamo.U
import kbd.hangul.jamo.Jamo.WA
import kbd.hangul.jamo.Jamo.WEO

/** Merges the current medial [prev] with the next vowel [next]; null if they do not merge. */
fun interface VowelMergeTable {
    fun merge(prev: Jamo, next: Jamo): Jamo?
}

/** Vowel merge tables per input mode (LLD-03 §3.1). */
object MergeTables {
    /**
     * Dubeolsik: 17 pairs (FR-HGL-04a) — the 7 compound vowels, then (FSS#129) the paired vowels ㅣ + ㅓ ㅏ ㅜ ㅗ = ㅕ ㅑ ㅠ ㅛ,
     * ㅓ ㅏ ㅕ ㅑ + ㅣ = ㅔ ㅐ ㅖ ㅒ and ㅘ ㅝ + ㅣ = ㅙ ㅞ. A merged medial merges on by the same table (ㅣ ㅏ ㅣ = ㅒ,
     * ㅗ ㅏ ㅣ = ㅙ), one key one unit, so ⌫ takes back one key (괘 by ㄱ ㅗ ㅏ ㅣ: 괘 → 과 → 고).
     */
    val DUBEOLSIK: VowelMergeTable = tableOf(
        (O to A) to WA,
        (O to AE) to Jamo.WAE,
        (O to I) to Jamo.OE,
        (U to EO) to WEO,
        (U to E) to Jamo.WE,
        (U to I) to Jamo.WI,
        (EU to I) to Jamo.UI,
        (I to EO) to Jamo.YEO,
        (I to A) to Jamo.YA,
        (I to U) to Jamo.YU,
        (I to O) to Jamo.YO,
        (EO to I) to E,
        (A to I) to AE,
        (Jamo.YEO to I) to Jamo.YE,
        (Jamo.YA to I) to Jamo.YAE,
        (WA to I) to Jamo.WAE,
        (WEO to I) to Jamo.WE,
    )

    /** Never merges: graffiti (its vowels come assembled, FR-HGL-04c). */
    val NONE: VowelMergeTable = VowelMergeTable { _, _ -> null }

    private fun tableOf(vararg pairs: Pair<Pair<Jamo, Jamo>, Jamo>): VowelMergeTable {
        val map = mapOf(*pairs)
        return VowelMergeTable { prev, next -> map[prev to next] }
    }
}
