package kbd.hangul.event

import kbd.hangul.jamo.Jamo

/** Input to the composer (LLD-03 §3.2; HLD 4.3 output list plus `ReplaceVowel` from HLD 4.4·5). */
sealed interface JamoEvent {
    /** `c.kind == CONSONANT`; a double consonant may also come this way. */
    data class Consonant(val c: Jamo) : JamoEvent

    /** Same as `Consonant(DOUBLE_OF[base])`; the swipe double tap (HLD 4.3). */
    data class DoubleConsonant(val base: Jamo) : JamoEvent

    data class Vowel(val v: Jamo) : JamoEvent

    /** Graffiti cheonjiin: replace the current medial with [v]. */
    data class ReplaceVowel(val v: Jamo) : JamoEvent

    /**
     * Undo the last input unit of the composing syllable (never across a commit). The swipe double
     * consonant sends it before `DoubleConsonant` to turn the tap just typed into ㄲ (HLD 4.3).
     */
    data object UndoLast : JamoEvent
}
