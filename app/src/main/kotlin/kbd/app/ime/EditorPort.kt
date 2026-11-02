package kbd.app.ime

/** Keys the keyboard sends to the editor as key events (LLD-08 §4). */
enum class EditorKey { DPAD_LEFT, DPAD_RIGHT, DPAD_UP, DPAD_DOWN, ENTER, ESCAPE, FORWARD_DEL }   // ESCAPE, FORWARD_DEL: issue #198

/**
 * Android-free editor boundary (LLD-08 §4). `AndroidEditorPort` implements it with `InputConnection` (T32).
 */
interface EditorPort {
    fun commitText(text: String)
    fun setComposingText(text: String)
    fun finishComposingText()

    /** `KEYCODE_DEL` down and up. */
    fun deleteBefore()
    fun sendKey(key: EditorKey)
    fun performEditorAction(actionId: Int)
    fun setSelection(start: Int, end: Int)
    fun beginBatch()
    fun endBatch()
}
