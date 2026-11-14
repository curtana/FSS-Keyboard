package kbd.hangul.jamo

import kbd.hangul.jamo.Jamo.B
import kbd.hangul.jamo.Jamo.D
import kbd.hangul.jamo.Jamo.G
import kbd.hangul.jamo.Jamo.H
import kbd.hangul.jamo.Jamo.J
import kbd.hangul.jamo.Jamo.M
import kbd.hangul.jamo.Jamo.N
import kbd.hangul.jamo.Jamo.P
import kbd.hangul.jamo.Jamo.R
import kbd.hangul.jamo.Jamo.S
import kbd.hangul.jamo.Jamo.T

/** Consonant tables (LLD-03 §3.1). */
object JamoTables {
    /** Base consonant → double consonant (FR-HGL-05). */
    val DOUBLE_OF: Map<Jamo, Jamo> = mapOf(
        G to Jamo.GG,
        D to Jamo.DD,
        B to Jamo.BB,
        S to Jamo.SS,
        J to Jamo.JJ,
    )

    /** Consonants that can be a final: the 19 initials minus ㄸ ㅃ ㅉ, 16 in all (FR-HGL-05). */
    val JONG_CAPABLE: Set<Jamo> = Jamo.entries
        .filter { it.choIndex >= 0 && it.jongIndex > 0 }
        .toSet()

    /** (first final, second consonant) → compound final, 11 pairs (FR-HGL-02). */
    val COMPOUND_JONG: Map<Pair<Jamo, Jamo>, Jamo> = mapOf(
        (G to S) to Jamo.GS,
        (N to J) to Jamo.NJ,
        (N to H) to Jamo.NH,
        (R to G) to Jamo.RG,
        (R to M) to Jamo.RM,
        (R to B) to Jamo.RB,
        (R to S) to Jamo.RS,
        (R to T) to Jamo.RT,
        (R to P) to Jamo.RP,
        (R to H) to Jamo.RH,
        (B to S) to Jamo.BS,
    )

    /** Compound final → its two consonants (issue #242: one Dubeolsik stroke types a compound final). */
    val SPLIT_JONG: Map<Jamo, Pair<Jamo, Jamo>> = COMPOUND_JONG.entries.associate { (pair, c) -> c to pair }
}
