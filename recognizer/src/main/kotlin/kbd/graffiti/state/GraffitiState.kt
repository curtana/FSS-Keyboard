package kbd.graffiti.state

import kbd.hangul.cheonjiin.CjElement
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.ink.PadArea
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.StrokeToken
import kbd.graffiti.recognize.TemplateOutput
import kbd.graffiti.recognize.TemplateSet
import kbd.hangul.jamo.JamoTables

/** Value copy of [GraffitiState] for EngineGuard rollback (LLD-09 §2). */
data class GraffitiStateSnapshot(
    val lang: GraffitiLang,
    val capsLock: Boolean,
    val shiftPending: Boolean,
    val extPending: Boolean,
    val punctPending: Boolean,
)

/**
 * Graffiti stroke interpretation (LLD-05 §5). [interpret] is the 25-row table of §5.2: the first
 * matching row wins, and "one-shot off" happens after the row's result is decided (so rows 18, 19
 * and 23 see the shift from before).
 *
 * [templates] supplies the output of `Symbol(id)` (interim, gap G3).
 */
class GraffitiState(private val templates: TemplateSet) {

    var lang: GraffitiLang = GraffitiLang.KO
    var capsLock: Boolean = false
    var shiftPending: Boolean = false
    var extPending: Boolean = false
    var punctPending: Boolean = false

    // Ids are unique across sets (LLD-05 §3.3), so set membership separates rows 2·3 from the rest.
    private val extendedIds: Set<String> = templates.sets[SetName.EXTENDED].orEmpty().mapTo(HashSet()) { it.id }
    private val punctIds: Set<String> = templates.sets[SetName.PUNCT].orEmpty().mapTo(HashSet()) { it.id }

    fun interpret(t: StrokeToken, area: PadArea): GraffitiAction {
        val shift = shiftPending
        val koLetter = lang == GraffitiLang.KO && area == PadArea.LETTER
        return when (t) {
            StrokeToken.Tap -> when {
                extPending -> oneShotOff(GraffitiAction.ToggleLang)                            // 1
                koLetter -> oneShotOff(GraffitiAction.Element(CjElement.DOT))                  // 13
                else -> { clearOneShot(); punctPending = true; GraffitiAction.StateOnly }      // 12
            }
            is StrokeToken.Symbol -> symbol(t.id, shift)                                      // 2, 3, 19–24
            StrokeToken.LineLeft -> GraffitiAction.Backspace                                   // 4
            StrokeToken.DiagDownLeft -> oneShotOff(GraffitiAction.Enter)                       // 5
            StrokeToken.DiagDownRight -> { clearOneShot(); extPending = true; GraffitiAction.StateOnly }  // 6
            StrokeToken.LineUp -> lineUp()                                                     // 7–10
            StrokeToken.CornerTap -> oneShotOff(GraffitiAction.ToggleLang)                     // 11
            StrokeToken.LineRight ->
                oneShotOff(if (koLetter) GraffitiAction.Element(CjElement.EU) else GraffitiAction.Space)  // 14, 15
            StrokeToken.LineDown -> oneShotOff(
                when {
                    koLetter -> GraffitiAction.Element(CjElement.I)                            // 16
                    area == PadArea.NUMBER -> GraffitiAction.CommitText("1")                   // 17
                    else -> GraffitiAction.CommitText(if (capsLock || shift) "I" else "i")     // 18
                },
            )
            StrokeToken.Unknown -> GraffitiAction.RecognitionFailed                            // 25
        }
    }

    /** Clears all three pendings; true if any was set. ⌫ calls this first (FR-GRF-05c ⑤). */
    fun cancelPending(): Boolean {
        val any = shiftPending || extPending || punctPending
        clearOneShot()
        return any
    }

    /** Mode switch, cursor move, `onStartInputView`, commit (HLD ch. 5 reset). */
    fun clearOneShot() {
        shiftPending = false
        extPending = false
        punctPending = false
    }

    /** New entry into graffiti mode (FR-GRF-05a). */
    fun enterMode() {
        clearOneShot()
        capsLock = false
        lang = GraffitiLang.KO
    }

    fun snapshot(): GraffitiStateSnapshot =
        GraffitiStateSnapshot(lang, capsLock, shiftPending, extPending, punctPending)

    fun restore(s: GraffitiStateSnapshot) {
        lang = s.lang
        capsLock = s.capsLock
        shiftPending = s.shiftPending
        extPending = s.extPending
        punctPending = s.punctPending
    }

    private fun lineUp(): GraffitiAction {
        when {
            lang == GraffitiLang.EN && capsLock -> { capsLock = false; clearOneShot() }        // 7
            lang == GraffitiLang.EN && shiftPending -> { shiftPending = false; capsLock = true }  // 8
            lang == GraffitiLang.KO && shiftPending -> shiftPending = false                    // 9
            else -> { clearOneShot(); shiftPending = true }                                    // 10
        }
        return GraffitiAction.StateOnly
    }

    private fun symbol(id: String, shift: Boolean): GraffitiAction {
        // An id unknown to the template set is a recognition failure (row 25, task T23 step 1).
        val out = templates.outputOf(id) ?: return GraffitiAction.RecognitionFailed
        if (out is TemplateOutput.Text) {
            if (extPending && id in extendedIds) return oneShotOff(GraffitiAction.CommitText(out.value))   // 2
            if (punctPending && id in punctIds) return oneShotOff(GraffitiAction.CommitText(out.value))    // 3
        }
        return when (out) {
            is TemplateOutput.Jamo -> {
                val c = out.value
                // Row 20 (c is already a double) and row 19 without a double both give c itself.
                oneShotOff(GraffitiAction.Consonant(if (shift) JamoTables.DOUBLE_OF[c] ?: c else c))  // 19, 20
            }
            is TemplateOutput.Control -> when (out.value) {
                SPACE_V -> oneShotOff(GraffitiAction.Space)                                    // 21
                FINISH_J -> oneShotOff(GraffitiAction.Finish)                                  // 22
                else -> GraffitiAction.RecognitionFailed  // not in the §7 vocabulary: treated like an unknown id
            }
            is TemplateOutput.Text -> {
                val s = out.value
                if (isEnglishLetter(s)) oneShotOff(GraffitiAction.CommitText(if (capsLock || shift) s.uppercase() else s))  // 23
                else oneShotOff(GraffitiAction.CommitText(s))                                  // 24
            }
        }
    }

    private fun oneShotOff(a: GraffitiAction): GraffitiAction {
        clearOneShot()
        return a
    }

    private companion object {
        const val SPACE_V = "SPACE_V"
        const val FINISH_J = "FINISH_J"

        fun isEnglishLetter(s: String): Boolean = s.length == 1 && s[0] in 'a'..'z'
    }
}
