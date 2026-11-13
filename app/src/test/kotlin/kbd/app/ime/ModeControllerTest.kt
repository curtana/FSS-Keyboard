package kbd.app.ime

import kbd.app.ime.InputMode.DUBEOLSIK
import kbd.app.ime.InputMode.GRAFFITI
import kbd.app.ime.InputMode.QWERTY
import kbd.app.ime.InputMode.SWIPE
import kbd.app.store.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MC-01..MC-10, MC-12 (MC-11, MC-13 went with the public edition, issue #251), NF-01, NF-02, NF-04..NF-09, OM-01..OM-10 (LLD-11 §7.2, §7.2a, §7.2b; LLD-08 §5, §3.1). NF-03 is T31. */
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

    // --- OM: SRS FR-CMN-03f modes turned off, case table rows 1..8 (FSS#31) ---

    private val noGraffiti = setOf(SWIPE, QWERTY, DUBEOLSIK)
    private val tapOnly = setOf(QWERTY, DUBEOLSIK)

    /** `KeyboardService.applyEnabledModes` / `onStartInputView` mode part: the returned mode is saved. */
    private fun ModeController.apply(on: Set<InputMode>) {
        applyEnabled(on)?.let { saved += it }
    }

    @Test
    fun `OM-01 graffiti off, dubeolsik not typed, mode key skips graffiti to swipe`() {
        val mc = ModeController(ALL, DUBEOLSIK, noGraffiti)
        assertEquals(SWIPE, mc.modeKey())
        assertNull(mc.previous)
    }

    @Test
    fun `OM-02 graffiti and swipe off, qwerty not typed, mode key goes to dubeolsik`() {
        val mc = ModeController(ALL, QWERTY, tapOnly)
        assertEquals(DUBEOLSIK, mc.modeKey())
        assertNull(mc.previous)
    }

    @Test
    fun `OM-03 graffiti and swipe off, dubeolsik not typed, mode key goes to qwerty`() {
        val mc = ModeController(ALL, DUBEOLSIK, tapOnly)
        assertEquals(QWERTY, mc.modeKey())
        assertNull(mc.previous)
        assertEquals(DUBEOLSIK, mc.modeKey()) // English and Korean only (FR-CMN-03f ⑤)
    }

    @Test
    fun `OM-04 turning graffiti off clears previous graffiti, typed mode key goes in order`() {
        val mc = ModeController(ALL, DUBEOLSIK)
        mc.previous = GRAFFITI
        mc.apply(noGraffiti)
        assertNull(mc.previous)
        assertEquals(DUBEOLSIK, mc.current)
        mc.onInput()
        assertEquals(SWIPE, mc.modeKey())
        assertEquals(DUBEOLSIK, mc.previous)
    }

    @Test
    fun `OM-05 graffiti in use turned off switches to swipe, saves it, keeps typed and previous`() {
        val mc = ModeController(ALL, GRAFFITI)
        mc.previous = DUBEOLSIK
        mc.onInput()
        mc.apply(noGraffiti)
        assertState(mc, SWIPE, null, DUBEOLSIK)
        assertTrue(mc.typed)
        assertEquals(listOf(SWIPE), saved)
    }

    @Test
    fun `OM-06 swipe in use with graffiti and swipe off switches to qwerty, previous kept or cleared`() {
        val mc = ModeController(ALL, SWIPE)
        mc.previous = DUBEOLSIK
        mc.apply(tapOnly)
        assertState(mc, QWERTY, null, DUBEOLSIK)
        assertEquals(listOf(QWERTY), saved)

        saved.clear()
        val g = ModeController(ALL, SWIPE)
        g.previous = GRAFFITI
        g.apply(tapOnly)
        assertState(g, QWERTY, null, null)
        assertEquals(listOf(QWERTY), saved)
    }

    @Test
    fun `OM-07 process restart with saved swipe off starts in qwerty and saves it`() {
        val mc = ModeController(ALL, SWIPE, setOf(QWERTY, DUBEOLSIK, GRAFFITI))
        assertState(mc, QWERTY, null, null)
        assertFalse(mc.typed)
        assertEquals(QWERTY, mc.fixedStart)
        assertNull(ModeController(ALL, SWIPE).fixedStart) // on: nothing to save
        assertNull(ModeController(ALL, null, tapOnly).fixedStart) // nothing saved: first mode on, not saved
        assertEquals(QWERTY, ModeController(ALL, null, tapOnly).current)
    }

    @Test
    fun `OM-08 return mode graffiti turned off returns to swipe`() {
        // The settings change reaches the service while the keyboard is down in the numeric field.
        val mc = nfStart()
        mc.startField(numericLike = true)
        mc.apply(noGraffiti)
        assertState(mc, QWERTY, SWIPE, null)
        assertEquals(listOf(SWIPE), saved) // the fixed return mode is the last mode (FR-CMN-03f ③)
        mc.startField(numericLike = false)
        mc.apply(noGraffiti)
        assertState(mc, SWIPE, null, null)
        assertEquals(listOf(SWIPE), saved)

        // It reaches it only at the next onStartInputView (leave first, then applyEnabled).
        saved.clear()
        val late = nfStart()
        late.startField(numericLike = true)
        late.startField(numericLike = false)
        late.apply(noGraffiti)
        assertState(late, SWIPE, null, null)
        assertEquals(listOf(SWIPE), saved)
    }

    @Test
    fun `OM-09 always-on modes cannot be turned off and nothing changes when all stay on`() {
        val mc = ModeController(ALL, DUBEOLSIK)
        mc.previous = GRAFFITI
        assertNull(mc.applyEnabled(emptySet()))
        assertEquals(setOf(QWERTY, DUBEOLSIK), mc.enabled)
        assertEquals(DUBEOLSIK, mc.current)
        assertNull(mc.applyEnabled(ALL.toSet()))
        assertEquals(ALL.toSet(), mc.enabled)
        assertEquals(SWIPE, mc.nextEnabled(GRAFFITI)) // wraps
        assertEquals(SWIPE, ModeController(listOf(SWIPE), SWIPE, tapOnly).current) // a practice board's one mode
    }

    @Test
    fun `OM-10 default settings - first install starts in qwerty, saved swipe or graffiti moves on, mode key cycles English and Korean`() {
        val order = InputMode.entries.toList()                    // as KeyboardService
        val on = Settings.DEFAULT.enabledModes                     // FSS#94: graffiti and swipe off
        assertEquals(tapOnly, on)

        val fresh = ModeController(order, null, on)                // nothing saved: the first mode on in the order
        assertState(fresh, QWERTY, null, null)
        assertNull(fresh.fixedStart)                               // nothing to save

        for (last in listOf(SWIPE, GRAFFITI)) {                    // a device updated with that mode last and no stored switch
            val mc = ModeController(order, last, on)
            assertState(mc, QWERTY, null, null)                    // the next mode on from it (FR-CMN-03f ③)
            assertEquals(QWERTY, mc.fixedStart)                    // saved as the last mode
        }

        assertEquals(DUBEOLSIK, fresh.modeKey())                   // not typed: in order, graffiti and swipe skipped
        assertEquals(QWERTY, fresh.modeKey())
        fresh.onInput()
        assertEquals(DUBEOLSIK, fresh.modeKey())                   // typed without previous: in order
        fresh.onInput()
        assertEquals(QWERTY, fresh.modeKey())                      // typed: back to the previous
        assertEquals(DUBEOLSIK, fresh.previous)
        assertEquals(listOf(DUBEOLSIK, QWERTY, DUBEOLSIK, QWERTY), saved)
    }
}
