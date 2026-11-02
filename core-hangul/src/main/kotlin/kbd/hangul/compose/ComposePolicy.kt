package kbd.hangul.compose

import kbd.hangul.event.ComposeResult
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.MergeTables
import kbd.hangul.jamo.VowelMergeTable

/**
 * Per-mode composing rules (LLD-04 §7.1). Backspace removes one input unit in every mode
 * (FR-CMN-07; swipe too since issue #150, so the former per-mode backspace field is gone).
 */
data class ComposePolicy(
    val name: String,
    val merge: VowelMergeTable,
    val mergeConsecutiveVowel: Boolean,  // two plain `Vowel` events may merge (dubeolsik only)
) {
    companion object {
        val DUBEOLSIK = ComposePolicy("dubeolsik", MergeTables.DUBEOLSIK, true)
        /**
         * Dubeolsik composing (finals, compound finals, dokkaebibul, FR-SWP-34) with no vowel merge:
         * every vowel is one swipe, two vowel swipes in a row are two syllables (고 + ㅏ = 고ㅏ,
         * FR-SWP-20, 21, FR-HGL-04b, SRS TBD-18 = B, issue #156).
         */
        val SWIPE = ComposePolicy("swipe", MergeTables.NONE, false)
        val GRAFFITI = ComposePolicy("graffiti", MergeTables.NONE, false)
    }
}

sealed interface BackspaceResult {
    data class Handled(val result: ComposeResult) : BackspaceResult

    /** Nothing is composing: the app deletes one committed character. */
    data object NothingComposing : BackspaceResult
}

/** Composer state for restore (LLD-09): the input units of the composing syllable. */
data class ComposerSnapshot(val units: List<JamoEvent>)
