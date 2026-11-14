package kbd.hangul.compose

import kbd.hangul.event.ComposeResult
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.MergeTables
import kbd.hangul.jamo.VowelMergeTable

/**
 * Per-mode composing rules (LLD-04 §7.1). Backspace removes one input unit in every mode
 * (FR-CMN-07).
 */
data class ComposePolicy(
    val name: String,
    val merge: VowelMergeTable,
    val mergeConsecutiveVowel: Boolean,  // two plain `Vowel` events may merge (dubeolsik only)
) {
    companion object {
        val DUBEOLSIK = ComposePolicy("dubeolsik", MergeTables.DUBEOLSIK, true)
        /** No vowel merge: the cheonjiin assembler sends the vowel and its `ReplaceVowel` steps (FR-HGL-04c). */
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
