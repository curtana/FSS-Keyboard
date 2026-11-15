package kbd.app.ime

import kbd.app.ime.InputMode.DUBEOLSIK
import kbd.app.ime.InputMode.GRAFFITI
import kbd.app.ime.InputMode.QWERTY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MC-01..MC-10, MC-12, MC-40..MC-42, NF-01, NF-02, NF-04..NF-09, OM-01..OM-07 (LLD-11 §7.2, §7.2a, §7.2b, §7.2c;
 * LLD-08 §5, §3.1). The expected values are the four case tables of SRS 3.1 in the order Dubeolsik, QWERTY, graffiti
 * (FSS#95). NF-03 is T31.
 */
class ModeControllerTest {

    private companion object {
        val ALL = listOf(DUBEOLSIK, QWERTY, GRAFFITI)
    }

    @Test
    fun `MC-00 the mode order is Dubeolsik, QWERTY, graffiti`() {
        assertEquals(ALL, InputMode.entries.toList())
        val mc = ModeController(InputMode.entries.toList(), null)
        assertEquals(DUBEOLSIK, mc.current)                      // nothing saved: the first mode (FR-CMN-10)
        assertEquals(listOf(QWERTY, GRAFFITI, DUBEOLSIK, QWERTY), List(4) { mc.onModeKey() })
    }

    // --- MC-01..MC-09: SRS mode-key example table rows 1..9, run as one continuous sequence ---

    /** Row n of the table: whether the user typed before pressing the mode key. */
    private val rowTyped = listOf(false, false, false, true, true, true, false, false, true)

    /** Nothing saved (a new install: Dubeolsik); runs rows 1..[row] in order and returns the controller. */
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
    fun `MC-01 dubeolsik not typed mode key goes to qwerty`() = assertRow(1, QWERTY, null)

    @Test
    fun `MC-02 qwerty not typed mode key goes to graffiti`() = assertRow(2, GRAFFITI, null)

    @Test
    fun `MC-03 graffiti not typed goes to dubeolsik - the end of the order wraps`() = assertRow(3, DUBEOLSIK, null)

    @Test
    fun `MC-04 dubeolsik typed without previous goes to qwerty in order`() = assertRow(4, QWERTY, DUBEOLSIK)

    @Test
    fun `MC-05 qwerty typed goes back to previous dubeolsik`() = assertRow(5, DUBEOLSIK, QWERTY)

    @Test
    fun `MC-06 dubeolsik typed goes back to previous qwerty`() = assertRow(6, QWERTY, DUBEOLSIK)

    @Test
    fun `MC-07 qwerty not typed goes to graffiti in order`() = assertRow(7, GRAFFITI, DUBEOLSIK)

    @Test
    fun `MC-08 graffiti not typed goes to dubeolsik in order`() = assertRow(8, DUBEOLSIK, DUBEOLSIK)

    @Test
    fun `MC-09 dubeolsik typed with previous equal to current goes in order`() = assertRow(9, QWERTY, DUBEOLSIK)

    @Test
    fun `MC-10 field change clears typed so mode key goes in order`() {
        val mc = ModeController(ALL, QWERTY)
        mc.previous = DUBEOLSIK
        mc.onInput()
        mc.onFieldChanged()
        assertEquals(GRAFFITI, mc.onModeKey())
        assertEquals(GRAFFITI, mc.current)
        assertEquals(DUBEOLSIK, mc.previous)
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

    /** Current GRAFFITI, previous null, typed false. */
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
        assertEquals(GRAFFITI, mc.modeKey())   // QWERTY → the next in order
        assertState(mc, GRAFFITI, GRAFFITI, null)
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
    fun `NF-06 digit input then mode key goes to previous dubeolsik inside numeric field`() {
        val mc = nfStart()
        mc.previous = DUBEOLSIK
        mc.startField(numericLike = true)
        mc.onInput() // digit row "3" tap (markInput)
        assertTrue(mc.typed)
        assertEquals(DUBEOLSIK, mc.modeKey())   // the previous mode, not graffiti (the next in order)
        assertState(mc, DUBEOLSIK, GRAFFITI, QWERTY)
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
        assertEquals(DUBEOLSIK, mc.modeKey()) // outside: saved once
        assertEquals(listOf(DUBEOLSIK), saved)

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
        mc.modeKey() // NF-02 state: GRAFFITI, return GRAFFITI
        assertState(mc, GRAFFITI, GRAFFITI, null)
        mc.startField(numericLike = true) // onStartInputView again on the same field
        assertState(mc, QWERTY, GRAFFITI, null)
        assertTrue(saved.isEmpty())
    }

    // --- OM: SRS FR-CMN-03f modes turned off, case table rows 1..6 (FSS#31; graffiti is the one mode that can be off) ---

    private val noGraffiti = setOf(DUBEOLSIK, QWERTY)

    /** `KeyboardService.applyEnabledModes` / `onStartInputView` mode part: the returned mode is saved. */
    private fun ModeController.apply(on: Set<InputMode>) {
        applyEnabled(on)?.let { saved += it }
    }

    @Test
    fun `OM-01 graffiti off, qwerty not typed, mode key skips graffiti to dubeolsik`() {
        val mc = ModeController(ALL, QWERTY, noGraffiti)
        assertEquals(DUBEOLSIK, mc.modeKey())
        assertNull(mc.previous)
    }

    @Test
    fun `OM-02 graffiti off, dubeolsik not typed, mode key goes to qwerty`() {
        val mc = ModeController(ALL, DUBEOLSIK, noGraffiti)
        assertEquals(QWERTY, mc.modeKey())
        assertNull(mc.previous)
        assertEquals(DUBEOLSIK, mc.modeKey()) // Korean and English only (FR-CMN-03f ⑤)
    }

    @Test
    fun `OM-03 turning graffiti off clears previous graffiti, typed mode key goes in order`() {
        val mc = ModeController(ALL, DUBEOLSIK)
        mc.previous = GRAFFITI
        mc.apply(noGraffiti)
        assertNull(mc.previous)
        assertEquals(DUBEOLSIK, mc.current)
        mc.onInput()
        assertEquals(QWERTY, mc.modeKey())
        assertEquals(DUBEOLSIK, mc.previous)
    }

    @Test
    fun `OM-04 graffiti in use turned off switches to dubeolsik, saves it, keeps typed and previous`() {
        val mc = ModeController(ALL, GRAFFITI)
        mc.previous = QWERTY
        mc.onInput()
        mc.apply(noGraffiti)
        assertState(mc, DUBEOLSIK, null, QWERTY)
        assertTrue(mc.typed)
        assertEquals(listOf(DUBEOLSIK), saved)
    }

    @Test
    fun `OM-05 process restart with saved graffiti off starts in dubeolsik and saves it`() {
        val mc = ModeController(ALL, GRAFFITI, noGraffiti)
        assertState(mc, DUBEOLSIK, null, null)
        assertFalse(mc.typed)
        assertEquals(DUBEOLSIK, mc.fixedStart)
        assertNull(ModeController(ALL, GRAFFITI).fixedStart) // on: nothing to save
        assertNull(ModeController(ALL, null, noGraffiti).fixedStart) // nothing saved: the first mode, not saved
        assertEquals(DUBEOLSIK, ModeController(ALL, null, noGraffiti).current)
    }

    @Test
    fun `OM-06 return mode graffiti turned off returns to dubeolsik`() {
        // The settings change reaches the service while the keyboard is down in the numeric field.
        val mc = nfStart()
        mc.startField(numericLike = true)
        mc.apply(noGraffiti)
        assertState(mc, QWERTY, DUBEOLSIK, null)
        assertEquals(listOf(DUBEOLSIK), saved) // the fixed return mode is the last mode (FR-CMN-03f ③)
        mc.startField(numericLike = false)
        mc.apply(noGraffiti)
        assertState(mc, DUBEOLSIK, null, null)
        assertEquals(listOf(DUBEOLSIK), saved)

        // It reaches it only at the next onStartInputView (leave first, then applyEnabled).
        saved.clear()
        val late = nfStart()
        late.startField(numericLike = true)
        late.startField(numericLike = false)
        late.apply(noGraffiti)
        assertState(late, DUBEOLSIK, null, null)
        assertEquals(listOf(DUBEOLSIK), saved)
    }

    @Test
    fun `OM-07 always-on modes cannot be turned off and nothing changes when all stay on`() {
        val mc = ModeController(ALL, DUBEOLSIK)
        mc.previous = GRAFFITI
        assertNull(mc.applyEnabled(emptySet()))
        assertEquals(setOf(DUBEOLSIK, QWERTY), mc.enabled)
        assertEquals(DUBEOLSIK, mc.current)
        assertNull(mc.applyEnabled(ALL.toSet()))
        assertEquals(ALL.toSet(), mc.enabled)
        assertEquals(DUBEOLSIK, mc.nextEnabled(GRAFFITI)) // wraps
        assertEquals(GRAFFITI, ModeController(listOf(GRAFFITI), GRAFFITI, noGraffiti).current) // a practice board's one mode
    }

    // --- MC-40..MC-42: SRS FR-CMN-10a cases (FSS#95). The store reads the last mode of a version that had
    // --- another mode as none (SS-15), so the controller starts with nothing saved.

    @Test
    fun `MC-40 a last mode that is gone - starts in dubeolsik, nothing to save`() {
        val mc = ModeController(ALL, null)
        assertState(mc, DUBEOLSIK, null, null)
        assertNull(mc.fixedStart)
        mc.startField(numericLike = false)   // case 1: a text field
        mc.apply(ALL.toSet())
        assertState(mc, DUBEOLSIK, null, null)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `MC-41 a last mode that is gone, numeric field - qwerty, then dubeolsik in a text field`() {
        val mc = ModeController(ALL, null)
        mc.startField(numericLike = true)    // case 2
        assertState(mc, QWERTY, DUBEOLSIK, null)
        mc.startField(numericLike = false)
        assertState(mc, DUBEOLSIK, null, null)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `MC-42 a last mode that is gone, graffiti off - dubeolsik and qwerty only`() {
        val mc = ModeController(ALL, null, noGraffiti)   // case 4
        assertEquals(DUBEOLSIK, mc.current)
        assertNull(mc.fixedStart)
        assertEquals(listOf(QWERTY, DUBEOLSIK, QWERTY), List(3) { mc.modeKey() })
    }
}
