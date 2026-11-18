package kbd.app.ime

import android.content.res.Configuration
import android.text.InputType
import android.view.inputmethod.EditorInfo
import kbd.app.view.ViewTestSdk
import kbd.stroke.layout.FunctionKey
import kbd.stroke.tap.ChordModifier
import kbd.stroke.tap.ChordModifier.ALT
import kbd.stroke.tap.ChordModifier.CTRL
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * LLD-11 §7.5 CL-01..CL-05: `KeyboardService` turns Ctrl and Alt off at its lifecycle points and not on a cursor
 * move (LLD-08 §3, SRS FR-TAP-17 ①②, FSS#98). The real service, created by Robolectric; no input view is needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
class KeyboardServiceChordTest {

    private lateinit var service: KeyboardService

    @Before
    fun setUp() {
        service = Robolectric.buildService(KeyboardService::class.java).create().get()
    }

    private fun turnOn() {
        service.dispatcher.onFunctionDown(FunctionKey.CTRL)
        service.dispatcher.onFunctionUp(FunctionKey.CTRL)
        service.dispatcher.onFunctionDown(FunctionKey.ALT)
        service.dispatcher.onFunctionUp(FunctionKey.ALT)
        assertEquals(setOf(CTRL, ALT), service.dispatcher.chord)
    }

    private fun assertOff() = assertEquals(emptySet<ChordModifier>(), service.dispatcher.chord)

    private fun textField() = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }

    @Test
    fun `CL-01 a new field, or the same one shown again, turns them off`() {
        turnOn()
        service.onStartInputView(textField(), false)
        assertOff()
        turnOn()
        service.onStartInputView(textField(), true)   // restarting
        assertOff()
        turnOn()
        service.onStartInputView(EditorInfo().apply { inputType = InputType.TYPE_CLASS_NUMBER }, false)   // the automatic change to QWERTY
        assertOff()
    }

    @Test
    fun `CL-02 the keyboard going down and the end of input turn them off`() {
        turnOn()
        service.onFinishInputView(false)
        assertOff()
        turnOn()
        service.onFinishInputView(true)
        assertOff()
        turnOn()
        service.onFinishInput()
        assertOff()
    }

    @Test
    fun `CL-03 a rotation turns them off`() {
        turnOn()
        service.onConfigurationChanged(Configuration(service.resources.configuration).apply { orientation = Configuration.ORIENTATION_LANDSCAPE })
        assertOff()
    }

    @Test
    fun `CL-04 a cursor or selection change does not`() {
        service.onStartInputView(textField(), false)
        turnOn()
        service.onUpdateSelection(0, 0, 2, 5, -1, -1)   // the user selected text
        assertEquals(setOf(CTRL, ALT), service.dispatcher.chord)
        service.onUpdateSelection(2, 5, 7, 7, -1, -1)
        assertEquals(setOf(CTRL, ALT), service.dispatcher.chord)
    }

    @Test
    fun `CL-05 the mode key turns them off`() {
        service.onStartInputView(textField(), false)
        turnOn()
        service.dispatcher.onFunctionDown(FunctionKey.MODE)
        assertOff()
    }
}
