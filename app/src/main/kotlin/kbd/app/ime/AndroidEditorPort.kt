package kbd.app.ime

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnection

/**
 * [EditorPort] over `InputConnection` (LLD-08 §4). Each call fetches `currentInputConnection`
 * and does nothing when it is null (HLD 9). Return values (`false` = failed) are ignored and
 * never turned into exceptions (LLD-09 §2 D-32).
 */
class AndroidEditorPort(private val service: InputMethodService) : EditorPort {

    private val ic: InputConnection? get() = service.currentInputConnection

    override fun commitText(text: String) {
        ic?.commitText(text, 1)
    }

    override fun setComposingText(text: String) {
        ic?.setComposingText(text, 1)
    }

    override fun finishComposingText() {
        ic?.finishComposingText()
    }

    /** `KEYCODE_DEL` down and up: one character, or the selection (FR-CMN-07). */
    override fun deleteBefore() = sendDownUp(KeyEvent.KEYCODE_DEL)

    override fun sendKey(key: EditorKey) = sendDownUp(
        when (key) {
            EditorKey.DPAD_LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
            EditorKey.DPAD_RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
            EditorKey.DPAD_UP -> KeyEvent.KEYCODE_DPAD_UP
            EditorKey.DPAD_DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
            EditorKey.ENTER -> KeyEvent.KEYCODE_ENTER
            EditorKey.ESCAPE -> KeyEvent.KEYCODE_ESCAPE
            EditorKey.FORWARD_DEL -> KeyEvent.KEYCODE_FORWARD_DEL
        },
    )

    override fun performEditorAction(actionId: Int) {
        ic?.performEditorAction(actionId)
    }

    override fun setSelection(start: Int, end: Int) {
        ic?.setSelection(start, end)
    }

    override fun beginBatch() {
        ic?.beginBatchEdit()
    }

    override fun endBatch() {
        ic?.endBatchEdit()
    }

    /** Same events as `InputMethodService.sendDownUpKeyEvents`, on one connection. */
    private fun sendDownUp(keyCode: Int) {
        val c = ic ?: return
        val t = SystemClock.uptimeMillis()
        val flags = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
        c.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
        c.sendKeyEvent(KeyEvent(t, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
    }
}
