package kbd.app.ime

import android.inputmethodservice.InputMethodService
import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import kbd.app.view.ViewTestSdk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * LLD-11 §7.12 (아) CP-07..CP-10: reads and the n-character delete of `AndroidEditorPort` over a recording
 * `InputConnection` (LLD-12 §8.2).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
class AndroidEditorPortReadTest {

    /** Every `InputConnection` call as name(args). */
    private val calls = ArrayList<String>()

    /** What the connection returns for each method name; a missing name gives the type's default (null for text). */
    private val answers = HashMap<String, CharSequence?>()

    private val connection = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(InputConnection::class.java)) { _, method, args ->
        calls += "${method.name}(${args.orEmpty().joinToString(",")})"
        when (method.returnType) {
            Boolean::class.javaPrimitiveType -> true
            Int::class.javaPrimitiveType -> 0
            CharSequence::class.java -> answers[method.name]
            else -> null
        }
    } as InputConnection

    private class Ime(private val connection: InputConnection?) : InputMethodService() {
        override fun getCurrentInputConnection(): InputConnection? = connection
    }

    private val port = AndroidEditorPort(Ime(connection))

    @Test
    fun `CP-07 text before and after the cursor come from the connection, or null`() {
        answers["getTextBeforeCursor"] = "mon"
        answers["getTextAfterCursor"] = "x"
        assertEquals("mon", port.textBeforeCursor(3))
        assertEquals("x", port.textAfterCursor(1))
        assertEquals(listOf("getTextBeforeCursor(3,0)", "getTextAfterCursor(1,0)"), calls)

        answers.clear()
        assertNull(port.textBeforeCursor(3))
        assertNull(port.textAfterCursor(1))
    }

    @Test
    fun `CP-08 selection - text is true, empty or null is false`() {
        answers["getSelectedText"] = "ab"
        assertEquals(true, port.hasSelection())
        answers["getSelectedText"] = ""
        assertEquals(false, port.hasSelection())
        answers["getSelectedText"] = null
        assertEquals(false, port.hasSelection())
        assertEquals(List(3) { "getSelectedText(0)" }, calls)
    }

    @Test
    fun `CP-09 delete of n is one deleteSurroundingText, no key events`() {
        port.deleteBeforeCursor(3)
        assertEquals(listOf("deleteSurroundingText(3,0)"), calls)
    }

    @Test
    fun `CP-10 no connection - reads null, delete does nothing, can still read`() {
        val none = AndroidEditorPort(Ime(null))
        assertNull(none.textBeforeCursor(3))
        assertNull(none.textAfterCursor(1))
        assertNull(none.hasSelection())
        none.deleteBeforeCursor(3)
        assertTrue(none.canReadText)
        assertFalse(calls.isNotEmpty())
    }
}
