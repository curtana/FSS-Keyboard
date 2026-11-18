package kbd.app.ime

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import kbd.stroke.tap.ChordKey
import kbd.stroke.tap.ChordModifier

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

    /**
     * The events of a hardware keyboard for the chord (LLD-08 §4, FR-TAP-16 ④): modifier downs (Ctrl, Alt, Shift),
     * the key down and up, modifier ups in reverse. Each event carries the modifiers held at that moment; a
     * modifier's own down already has its bit and its own up no longer has it. Never an editor command
     * (`performContextMenuAction`): a terminal expects Ctrl + C as the key (FR-TAP-16 ⑤).
     */
    override fun sendChord(key: ChordKey, mods: Set<ChordModifier>) {
        val c = ic ?: return
        val t = SystemClock.uptimeMillis()
        val held = ChordModifier.entries.filter { it in mods }
        var meta = 0
        for (m in held) {
            meta = meta or metaOf(m)
            c.sendKeyEvent(event(t, KeyEvent.ACTION_DOWN, keyCodeOf(m), meta))
        }
        val code = keyCodeOf(key)
        c.sendKeyEvent(event(t, KeyEvent.ACTION_DOWN, code, meta))
        c.sendKeyEvent(event(t, KeyEvent.ACTION_UP, code, meta))
        for (m in held.asReversed()) {
            meta = meta and metaOf(m).inv()
            c.sendKeyEvent(event(t, KeyEvent.ACTION_UP, keyCodeOf(m), meta))
        }
    }

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
        c.sendKeyEvent(event(t, KeyEvent.ACTION_DOWN, keyCode, 0))
        c.sendKeyEvent(event(t, KeyEvent.ACTION_UP, keyCode, 0))
    }

    /** A soft keyboard key event pressed at [downTime], with the flags and the device of every key this port sends. */
    private fun event(downTime: Long, action: Int, keyCode: Int, meta: Int): KeyEvent {
        val now = if (action == KeyEvent.ACTION_DOWN) downTime else SystemClock.uptimeMillis()
        val flags = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
        return KeyEvent(downTime, now, action, keyCode, 0, meta, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags)
    }

    internal companion object {
        /** The left modifier keys, as a hardware keyboard's (LLD-08 §4). */
        fun keyCodeOf(m: ChordModifier): Int = when (m) {
            ChordModifier.CTRL -> KeyEvent.KEYCODE_CTRL_LEFT
            ChordModifier.ALT -> KeyEvent.KEYCODE_ALT_LEFT
            ChordModifier.SHIFT -> KeyEvent.KEYCODE_SHIFT_LEFT
        }

        fun metaOf(m: ChordModifier): Int = when (m) {
            ChordModifier.CTRL -> KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
            ChordModifier.ALT -> KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
            ChordModifier.SHIFT -> KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        }

        /** Letter and digit key codes are consecutive on the platform, like [ChordKey]'s A..Z and D0..D9. */
        fun keyCodeOf(k: ChordKey): Int = when (k) {
            in ChordKey.A..ChordKey.Z -> KeyEvent.KEYCODE_A + (k.ordinal - ChordKey.A.ordinal)
            in ChordKey.D0..ChordKey.D9 -> KeyEvent.KEYCODE_0 + (k.ordinal - ChordKey.D0.ordinal)
            ChordKey.GRAVE -> KeyEvent.KEYCODE_GRAVE
            ChordKey.MINUS -> KeyEvent.KEYCODE_MINUS
            ChordKey.EQUALS -> KeyEvent.KEYCODE_EQUALS
            ChordKey.LEFT_BRACKET -> KeyEvent.KEYCODE_LEFT_BRACKET
            ChordKey.RIGHT_BRACKET -> KeyEvent.KEYCODE_RIGHT_BRACKET
            ChordKey.BACKSLASH -> KeyEvent.KEYCODE_BACKSLASH
            ChordKey.SEMICOLON -> KeyEvent.KEYCODE_SEMICOLON
            ChordKey.APOSTROPHE -> KeyEvent.KEYCODE_APOSTROPHE
            ChordKey.COMMA -> KeyEvent.KEYCODE_COMMA
            ChordKey.PERIOD -> KeyEvent.KEYCODE_PERIOD
            ChordKey.SLASH -> KeyEvent.KEYCODE_SLASH
            ChordKey.SPACE -> KeyEvent.KEYCODE_SPACE
            ChordKey.ENTER -> KeyEvent.KEYCODE_ENTER
            ChordKey.TAB -> KeyEvent.KEYCODE_TAB
            ChordKey.DPAD_LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
            ChordKey.DPAD_UP -> KeyEvent.KEYCODE_DPAD_UP
            ChordKey.DPAD_DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
            ChordKey.DPAD_RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
            ChordKey.ESCAPE -> KeyEvent.KEYCODE_ESCAPE
            ChordKey.FORWARD_DEL -> KeyEvent.KEYCODE_FORWARD_DEL
            ChordKey.DEL -> KeyEvent.KEYCODE_DEL
            else -> error("unreachable: $k")   // the two ranges above cover the letters and digits
        }
    }
}
