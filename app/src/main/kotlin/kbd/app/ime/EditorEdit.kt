package kbd.app.ime

/**
 * One editor command of a suggestion plan (LLD-12 §8.3, D-84). The app's own shape, so the app does not depend on
 * the suggestion module's types.
 */
sealed interface EditorEdit {
    /** Deletes [count] characters before the cursor. */
    data class DeleteBefore(val count: Int) : EditorEdit

    /** Commits [text] at the cursor. */
    data class Insert(val text: String) : EditorEdit
}
