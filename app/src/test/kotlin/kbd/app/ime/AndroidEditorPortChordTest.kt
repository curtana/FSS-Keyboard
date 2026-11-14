package kbd.app.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import kbd.app.view.ViewTestSdk
import kbd.stroke.tap.ChordKey
import kbd.stroke.tap.ChordModifier
import kbd.stroke.tap.ChordModifier.ALT
import kbd.stroke.tap.ChordModifier.CTRL
import kbd.stroke.tap.ChordModifier.SHIFT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * LLD-11 §7.5 CP-01..CP-06: the key events `AndroidEditorPort.sendChord` gives the `InputConnection`
 * (LLD-08 §4, SRS FR-TAP-16 ④⑤, FSS#98).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
class AndroidEditorPortChordTest {

    /** Every `InputConnection` call by name; key events are kept. */
    private val calls = ArrayList<String>()
    private val events = ArrayList<KeyEvent>()

    private val connection = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(InputConnection::class.java)) { _, method, args ->
        calls += method.name
        if (method.name == "sendKeyEvent") events += args[0] as KeyEvent
        when (method.returnType) {
            Boolean::class.javaPrimitiveType -> true
            Int::class.javaPrimitiveType -> 0
            else -> null
        }
    } as InputConnection

    private class Ime(private val connection: InputConnection?) : InputMethodService() {
        override fun getCurrentInputConnection(): InputConnection? = connection
    }

    private val port = AndroidEditorPort(Ime(connection))

    private val ctrl = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
    private val alt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
    private val shift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON

    /** (action, key code, meta state) of each event sent. */
    private fun sent() = events.map { Triple(it.action, it.keyCode, it.metaState) }

    private fun down(code: Int, meta: Int) = Triple(KeyEvent.ACTION_DOWN, code, meta)
    private fun up(code: Int, meta: Int) = Triple(KeyEvent.ACTION_UP, code, meta)

    @Test
    fun `CP-01 Ctrl + C - Ctrl down, C down and up with Ctrl, Ctrl up with nothing`() {
        port.sendChord(ChordKey.C, setOf(CTRL))
        assertEquals(
            listOf(
                down(KeyEvent.KEYCODE_CTRL_LEFT, ctrl),
                down(KeyEvent.KEYCODE_C, ctrl), up(KeyEvent.KEYCODE_C, ctrl),
                up(KeyEvent.KEYCODE_CTRL_LEFT, 0),
            ),
            sent(),
        )
    }

    @Test
    fun `CP-02 Alt + tab - the same shape with Alt`() {
        port.sendChord(ChordKey.TAB, setOf(ALT))
        assertEquals(
            listOf(
                down(KeyEvent.KEYCODE_ALT_LEFT, alt),
                down(KeyEvent.KEYCODE_TAB, alt), up(KeyEvent.KEYCODE_TAB, alt),
                up(KeyEvent.KEYCODE_ALT_LEFT, 0),
            ),
            sent(),
        )
    }

    @Test
    fun `CP-03 Ctrl + Alt + Shift - downs in the order Ctrl, Alt, Shift, ups in reverse, each with what is held then`() {
        port.sendChord(ChordKey.LEFT_BRACKET, linkedSetOf(SHIFT, ALT, CTRL))   // the set's order does not matter
        assertEquals(
            listOf(
                down(KeyEvent.KEYCODE_CTRL_LEFT, ctrl),
                down(KeyEvent.KEYCODE_ALT_LEFT, ctrl or alt),
                down(KeyEvent.KEYCODE_SHIFT_LEFT, ctrl or alt or shift),
                down(KeyEvent.KEYCODE_LEFT_BRACKET, ctrl or alt or shift),
                up(KeyEvent.KEYCODE_LEFT_BRACKET, ctrl or alt or shift),
                up(KeyEvent.KEYCODE_SHIFT_LEFT, ctrl or alt),
                up(KeyEvent.KEYCODE_ALT_LEFT, ctrl),
                up(KeyEvent.KEYCODE_CTRL_LEFT, 0),
            ),
            sent(),
        )
        events.clear()
        port.sendChord(ChordKey.A, setOf(CTRL, SHIFT))
        assertEquals(
            listOf(
                down(KeyEvent.KEYCODE_CTRL_LEFT, ctrl), down(KeyEvent.KEYCODE_SHIFT_LEFT, ctrl or shift),
                down(KeyEvent.KEYCODE_A, ctrl or shift), up(KeyEvent.KEYCODE_A, ctrl or shift),
                up(KeyEvent.KEYCODE_SHIFT_LEFT, ctrl), up(KeyEvent.KEYCODE_CTRL_LEFT, 0),
            ),
            sent(),
        )
    }

    @Test
    fun `CP-04 key events only, never an editor command or text, with the flags and device of the other keys`() {
        for (k in listOf(ChordKey.A, ChordKey.C, ChordKey.V, ChordKey.X, ChordKey.Z, ChordKey.SPACE, ChordKey.ENTER, ChordKey.TAB)) {
            port.sendChord(k, setOf(CTRL))
        }
        assertEquals(setOf("sendKeyEvent"), calls.toSet())   // no performContextMenuAction, commitText, performEditorAction
        assertEquals(8 * 4, events.size)
        for (e in events) {
            assertEquals(KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE, e.flags)
            assertEquals(KeyCharacterMap.VIRTUAL_KEYBOARD, e.deviceId)
            assertEquals(0, e.repeatCount)
            assertTrue(e.eventTime >= e.downTime)
        }
    }

    @Test
    fun `CP-05 no input connection - nothing happens`() {
        AndroidEditorPort(Ime(null)).sendChord(ChordKey.C, setOf(CTRL))
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `CP-06 every chord key and modifier has the platform key code of its name`() {
        fun code(name: String) = KeyEvent::class.java.getField("KEYCODE_$name").getInt(null)
        for (k in ChordKey.entries) {
            val name = if (k in ChordKey.D0..ChordKey.D9) k.name.removePrefix("D") else k.name
            assertEquals("$k", code(name), AndroidEditorPort.keyCodeOf(k))
        }
        assertEquals(ChordKey.entries.size, ChordKey.entries.map { AndroidEditorPort.keyCodeOf(it) }.toSet().size)
        assertEquals(code("CTRL_LEFT"), AndroidEditorPort.keyCodeOf(CTRL))
        assertEquals(code("ALT_LEFT"), AndroidEditorPort.keyCodeOf(ALT))
        assertEquals(code("SHIFT_LEFT"), AndroidEditorPort.keyCodeOf(SHIFT))
        assertEquals(ctrl, AndroidEditorPort.metaOf(CTRL))
        assertEquals(alt, AndroidEditorPort.metaOf(ALT))
        assertEquals(shift, AndroidEditorPort.metaOf(SHIFT))
        assertEquals(3, ChordModifier.entries.size)
        // The keys the port sent before FSS#98 are unchanged.
        port.sendKey(EditorKey.DPAD_LEFT)
        port.deleteBefore()
        assertEquals(
            listOf(down(KeyEvent.KEYCODE_DPAD_LEFT, 0), up(KeyEvent.KEYCODE_DPAD_LEFT, 0), down(KeyEvent.KEYCODE_DEL, 0), up(KeyEvent.KEYCODE_DEL, 0)),
            sent(),
        )
    }
}
