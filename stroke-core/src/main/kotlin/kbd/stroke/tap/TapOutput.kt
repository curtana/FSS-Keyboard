package kbd.stroke.tap

/**
 * Output of one tap-keyboard stroke (LLD-05A §2.2). Exactly one of [text] (letters, digits,
 * symbols) or [jamo] (Dubeolsik compatibility jamo; stroke-core does not depend on core-hangul,
 * LLD-03 §1.2) is set. [errorVibe] asks for the error vibration when a super/subscript is missing
 * (FR-TAP-01b). [vowel] is a second jamo typed right after [jamo], as if its key were tapped next: a Dubeolsik
 * consonant key stroked in a free up/down/left/right direction (FSS#122, SRS FR-TAP-02h, ㄱ ← = ㄱ then ㅓ = 거).
 * Declared with `TapKeyResolver` in the LLD; kept in its own file.
 */
data class TapOutput(val text: String?, val jamo: Char?, val errorVibe: Boolean, val vowel: Char? = null)
