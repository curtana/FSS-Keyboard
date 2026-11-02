package kbd.app.ime

import kbd.app.ime.InputMode.DUBEOLSIK
import kbd.app.ime.InputMode.GRAFFITI
import kbd.app.ime.InputMode.QWERTY
import kbd.app.ime.InputMode.SWIPE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MC-01..MC-10, MC-12 (MC-11, MC-13 went with the public edition, issue #251), NF-01, NF-02, NF-04..NF-09 (LLD-11 §7.2, §7.2a; LLD-08 §5, §3.1). NF-03 is T31. */
class ModeControllerTest {

    private companion object {
        val ALL = listOf(SWIPE, QWERTY, DUBEOLSIK, GRAFFITI)
    }

    // --- MC-01..MC-09: SRS mode-key example table rows 1..9, run as one continuous sequence ---

    /** Row n of the table: whether the user typed before pressing the mode key. */
    private val rowTyped = listOf(false, false, true, false, true, true, false, false, true)

    /** Personal, nothing saved; runs rows 1..[row] in order and returns the controller. */
    private fun runRows(row: Int): ModeController {
        val mc = ModeController(ALL, null)
        for (i in 0 until row) {
            if (rowTyped[i]) mc.onInput()
            mc.onModeKey()
        }
        return mc
    }

    private fun assertRow(row: Int, current: InputMode, previous: InputMode?) {
        val mc = runRows(row)
        assertEquals(current, mc.current)
        assertEquals(previous, mc.previous)
        assertFalse(mc.typed)
    }

    @Test
    fun `MC-01 swipe not typed mode key goes to qwerty`() = assertRow(1, QWERTY, null)

    @Test
    fun `MC-02 qwerty not typed mode key goes to dubeolsik`() = assertRow(2, DUBEOLSIK, null)

    @Test
    fun `MC-03 dubeolsik typed without previous goes to graffiti in order`() = assertRow(3, GRAFFITI, DUBEOLSIK)

    @Test
    fun `MC-04 graffiti not typed goes to swipe in order`() = assertRow(4, SWIPE, DUBEOLSIK)

    @Test
    fun `MC-05 swipe typed goes back to previous dubeolsik`() = assertRow(5, DUBEOLSIK, SWIPE)

    @Test
    fun `MC-06 dubeolsik typed goes back to previous swipe`() = assertRow(6, SWIPE, DUBEOLSIK)

    @Test
    fun `MC-07 swipe not typed goes to qwerty in order`() = assertRow(7, QWERTY, DUBEOLSIK)

    @Test
    fun `MC-08 qwerty not typed goes to dubeolsik in order`() = assertRow(8, DUBEOLSIK, DUBEOLSIK)

    @Test
    fun `MC-09 dubeolsik typed with previous equal to current goes in order`() = assertRow(9, GRAFFITI, DUBEOLSIK)

    @Test
    fun `MC-10 field change clears typed so mode key goes in order`() {
        val mc = ModeController(ALL, DUBEOLSIK)
        mc.previous = SWIPE
        mc.onInput()
        mc.onFieldChanged()
        assertEquals(GRAFFITI, mc.onModeKey())
        assertEquals(GRAFFITI, mc.current)
        assertEquals(SWIPE, mc.previous)
    }

    @Test
    fun `MC-12 saved graffiti starts in graffiti with no previous and not typed`() {
        val mc = ModeController(ALL, GRAFFITI)
        assertEquals(GRAFFITI, mc.current)
        assertNull(mc.previous)
        assertFalse(mc.typed)
        assertNull(mc.returnMode)
    }

    // --- NF: replay of LLD-08 §3.1 order with a recording saveMode stub ---

    private val saved = mutableListOf<InputMode>()

    /** `onStartInputView` mode part (LLD-08 §3.1). */
    private fun ModeController.startField(numericLike: Boolean) {
        onFieldChanged()
        if (numericLike) enterNumericField() else leaveNumericField()
    }

    /** `InputDispatcher.onModeKey` mode part (LLD-08 §6.1): save only outside numeric-like fields. */
    private fun ModeController.modeKey(): InputMode {
        val next = onModeKey()
        if (!inNumericField) saved += next
        return next
    }

    private fun assertState(mc: ModeController, current: InputMode, returnMode: InputMode?, previous: InputMode?) {
        assertEquals(current, mc.current)
        assertEquals(returnMode, mc.returnMode)
        assertEquals(previous, mc.previous)
    }

    /** Personal, current GRAFFITI, previous null, typed false. */
    private fun nfStart() = ModeController(ALL, GRAFFITI)

    @Test
    fun `NF-01 graffiti to numeric field starts in qwerty with return mode graffiti`() {
        val mc = nfStart()
        mc.startField(numericLike = true)
        assertState(mc, QWERTY, GRAFFITI, null)
        assertTrue(mc.inNumericField)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `NF-02 mode key without input inside numeric field goes in order and is not saved`() {
        val mc = nfStart()
        mc.startField(numericLike = true)
        assertEquals(DUBEOLSIK, mc.modeKey())
        assertState(mc, DUBEOLSIK, GRAFFITI, null)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `NF-04 password then phone field keeps first return mode`() {
        val mc = nfStart()
        mc.startField(numericLike = true) // text password
        assertState(mc, QWERTY, GRAFFITI, null)
        mc.startField(numericLike = true) // phone
        assertState(mc, QWERTY, GRAFFITI, null)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `NF-05 qwerty to numeric then text field stays qwerty and clears return mode`() {
        val mc = ModeController(ALL, QWERTY)
        mc.startField(numericLike = true)
        assertState(mc, QWERTY, QWERTY, null)
        mc.startField(numericLike = false)
        assertState(mc, QWERTY, null, null)
        assertFalse(mc.inNumericField)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `NF-06 digit input then mode key goes to previous swipe inside numeric field`() {
        val mc = nfStart()
        mc.previous = SWIPE
        mc.startField(numericLike = true)
        mc.onInput() // digit row "3" tap (markInput)
        assertTrue(mc.typed)
        assertEquals(SWIPE, mc.modeKey())
        assertState(mc, SWIPE, GRAFFITI, QWERTY)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `NF-07 process restart inside numeric field starts in qwerty with saved mode as return mode`() {
        val first = nfStart()
        first.startField(numericLike = true) // NF-01: nothing saved, last saved mode stays GRAFFITI
        assertTrue(saved.isEmpty())
        val mc = ModeController(ALL, GRAFFITI)
        mc.startField(numericLike = true)
        assertState(mc, QWERTY, GRAFFITI, null)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `NF-08 only mode key outside numeric field saves and auto switches keep previous`() {
        val mc = nfStart()
        mc.startField(numericLike = false)
        assertEquals(SWIPE, mc.modeKey()) // outside: saved once
        assertEquals(listOf(SWIPE), saved)

        saved.clear()
        mc.startField(numericLike = true) // enterNumericField
        assertTrue(saved.isEmpty())
        assertNull(mc.previous)
        mc.modeKey() // inside: not saved
        assertTrue(saved.isEmpty())

        mc.onInput()
        mc.modeKey()
        val prevBefore = mc.previous
        mc.enterNumericField()
        assertEquals(prevBefore, mc.previous)
        mc.leaveNumericField()
        assertEquals(prevBefore, mc.previous)
        mc.leaveNumericField() // no-op outside
        assertEquals(prevBefore, mc.previous)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `NF-09 same numeric field shown again restarts in qwerty and keeps return mode`() {
        val mc = nfStart()
        mc.startField(numericLike = true)
        mc.modeKey() // NF-02 state: DUBEOLSIK, return GRAFFITI
        assertState(mc, DUBEOLSIK, GRAFFITI, null)
        mc.startField(numericLike = true) // onStartInputView again on the same field
        assertState(mc, QWERTY, GRAFFITI, null)
        assertTrue(saved.isEmpty())
    }
}
