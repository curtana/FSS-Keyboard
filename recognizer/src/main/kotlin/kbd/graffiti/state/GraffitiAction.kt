package kbd.graffiti.state

import kbd.hangul.cheonjiin.CjElement
import kbd.hangul.jamo.Jamo

/** Action interpreted by GraffitiState (LLD-05 §2). */
sealed interface GraffitiAction {
    data class CommitText(val text: String) : GraffitiAction   // English, digit, symbol (commits Hangul composition first)
    data class Consonant(val c: Jamo) : GraffitiAction         // double consonants included
    data class Element(val e: CjElement) : GraffitiAction      // cheonjiin element
    data object Space : GraffitiAction
    data object Backspace : GraffitiAction
    data object Enter : GraffitiAction
    data object Finish : GraffitiAction                        // 'J' stroke
    data object ToggleLang : GraffitiAction
    data object StateOnly : GraffitiAction                     // shift, caps lock, extended/punct shift: no input (FR-CMN-03a)
    data object RecognitionFailed : GraffitiAction             // no input + error vibration (FR-GRF-08)
}
