package kbd.suggest

/** Key events the app reports to a source (language-neutral kinds, HLD 5B.1). Closed set: extend this list. */
sealed interface SuggestKey {
    /** The text a letter, digit or symbol key produced, as is. */
    data class Text(val text: String) : SuggestKey

    data object Space : SuggestKey

    /** One ⌫ (one repeat of an auto-repeat counts as one). */
    data object Backspace : SuggestKey

    data object Enter : SuggestKey

    data object Tab : SuggestKey

    /** The four arrow keys, Esc, Del. */
    data object Navigation : SuggestKey

    /** Any other function key (nothing sends it in step 1). */
    data object Function : SuggestKey
}

/** Why the app resets the source (LLD-12 §6). Every reason has the same effect on the English source. */
enum class ResetReason { FIELD, MODE, CHORD, CURSOR, SETTING, FINISHED, CONFIG, INACTIVE }
