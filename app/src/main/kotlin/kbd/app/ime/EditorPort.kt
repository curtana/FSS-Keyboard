package kbd.app.ime

import kbd.stroke.tap.ChordKey
import kbd.stroke.tap.ChordModifier

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

    /**
     * One key held with modifiers, as key events only (FSS#98, FR-TAP-16 ④): each of [mods] down in the order
     * Ctrl, Alt, Shift, then [key] down and up, then the modifiers up in reverse. [mods] is not empty. An editor
     * that takes no key events keeps this default and does nothing.
     */
    fun sendChord(key: ChordKey, mods: Set<ChordModifier>) {}
    fun performEditorAction(actionId: Int)
    fun setSelection(start: Int, end: Int)
    fun beginBatch()
    fun endBatch()
}
