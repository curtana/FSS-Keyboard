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

/**
 * MC-01..MC-10, MC-12, MC-15, MC-16 (MC-11, MC-13 went with the public edition, issue #251), NF-01, NF-02, NF-04..NF-09,
 * OM-01..OM-12 (LLD-11 §7.2, §7.2a, §7.2b; LLD-08 §5, §3.1). NF-03 is T31. Cycle order: Dubeolsik → QWERTY → swipe →
 * graffiti (SRS 0.85 FR-CMN-03 ②, FSS#93).
 */
class ModeControllerTest {

    private companion object {
        val ALL = InputMode.entries.toList()
    }

    @Test
    fun `ALL is the cycle order dubeolsik qwerty swipe graffiti`() =
        assertEquals(listOf(DUBEOLSIK, QWERTY, SWIPE, GRAFFITI), ALL)

    // --- MC-01..MC-09: SRS mode-key example table rows 1..9, run as one continuous sequence from a fresh install ---

    /** Row n of the table: whether the user typed before pressing the mode key. */
    private val rowTyped = listOf(false, false, true, false, true, true, false, false, true)

    /** Nothing saved (fresh install, starts in DUBEOLSIK); runs rows 1..[row] in order and returns the controller. */
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
    fun `MC-01 row 1 dubeolsik not typed mode key goes to qwerty`() = assertRow(1, QWERTY, null)

    @Test
    fun `MC-02 row 2 qwerty not typed mode key goes to swipe`() = assertRow(2, SWIPE, null)

    @Test
    fun `MC-03 row 3 swipe typed without previous goes to graffiti in order`() = assertRow(3, GRAFFITI, SWIPE)

    @Test
    fun `MC-04 row 4 graffiti not typed goes to dubeolsik in order`() = assertRow(4, DUBEOLSIK, SWIPE)

    @Test
    fun `MC-05 row 5 dubeolsik typed goes back to previous swipe`() = assertRow(5, SWIPE, DUBEOLSIK)

    @Test
    fun `MC-06 row 6 swipe typed goes back to previous dubeolsik`() = assertRow(6, DUBEOLSIK, SWIPE)

    @Test
    fun `MC-07 row 7 dubeolsik not typed goes to qwerty in order`() = assertRow(7, QWERTY, SWIPE)

    @Test
    fun `MC-08 row 8 qwerty not typed goes to swipe in order`() = assertRow(8, SWIPE, SWIPE)

    @Test
    fun `MC-09 row 9 swipe typed with previous equal to current goes in order`() = assertRow(9, GRAFFITI, SWIPE)

    @Test
    fun `MC-10 row 10 field change clears typed so mode key goes in order`() {
        val mc = ModeController(ALL, DUBEOLSIK)
        mc.previous = SWIPE
        mc.onInput()
        mc.onFieldChanged()
        assertEquals(QWERTY, mc.onModeKey())
        assertEquals(QWERTY, mc.current)
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

    @Test
    fun `MC-15 fresh install with nothing saved and all modes on starts in dubeolsik`() {
        val mc = ModeController(ALL, null)
        assertEquals(DUBEOLSIK, mc.current)
        assertNull(mc.previous)
        assertFalse(mc.typed)
        assertNull(mc.fixedStart)
    }

    @Test
    fun `MC-16 all on not typed cycles dubeolsik qwerty swipe graffiti and back`() {
        val mc = ModeController(ALL, null)
        assertEquals(listOf(QWERTY, SWIPE, GRAFFITI, DUBEOLSIK), List(4) { mc.onModeKey() })
        assertNull(mc.previous)
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
    fun `NF-01 row 1 graffiti to numeric field starts in qwerty with return mode graffiti`() {
        val mc = nfStart()
        mc.startField(numericLike = true)
        assertState(mc, QWERTY, GRAFFITI, null)
        assertTrue(mc.inNumericField)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `NF-02 row 2 mode key without input inside numeric field goes in order to swipe and is not saved`() {
        val mc = nfStart()
        mc.startField(numericLike = true)
        assertEquals(SWIPE, mc.modeKey())
        assertState(mc, SWIPE, GRAFFITI, null)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `NF-04 row 4 password then phone field keeps first return mode`() {
        val mc = nfStart()
        mc.startField(numericLike = true) // text password
        assertState(mc, QWERTY, GRAFFITI, null)
        mc.startField(numericLike = true) // phone
        assertState(mc, QWERTY, GRAFFITI, null)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `NF-05 row 5 qwerty to numeric then text field stays qwerty and clears return mode`() {
        val mc = ModeController(ALL, QWERTY)
        mc.startField(numericLike = true)
        assertState(mc, QWERTY, QWERTY, null)
        mc.startField(numericLike = false)
        assertState(mc, QWERTY, null, null)
        assertFalse(mc.inNumericField)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `NF-06 row 6 digit input then mode key goes to previous dubeolsik not next swipe`() {
        val mc = nfStart()
        mc.previous = DUBEOLSIK
        mc.startField(numericLike = true)
        mc.onInput() // digit row "3" tap (markInput)
        assertTrue(mc.typed)
        assertEquals(DUBEOLSIK, mc.modeKey())
        assertState(mc, DUBEOLSIK, GRAFFITI, QWERTY)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `NF-07 row 7 process restart inside numeric field starts in qwerty with saved mode as return mode`() {
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
        mc.modeKey() // NF-02 state: SWIPE, return GRAFFITI
        assertState(mc, SWIPE, GRAFFITI, null)
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

    private val noSwipe = setOf(QWERTY, DUBEOLSIK, GRAFFITI)

    @Test
    fun `OM-01 row 1 graffiti off, swipe not typed, mode key skips graffiti to dubeolsik`() {
        val mc = ModeController(ALL, SWIPE, noGraffiti)
        assertEquals(DUBEOLSIK, mc.modeKey())
        assertNull(mc.previous)
    }

    @Test
    fun `OM-02 row 2 graffiti and swipe off, qwerty not typed, mode key goes to dubeolsik`() {
        val mc = ModeController(ALL, QWERTY, tapOnly)
        assertEquals(DUBEOLSIK, mc.modeKey())
        assertNull(mc.previous)
    }

    @Test
    fun `OM-03 row 3 graffiti and swipe off, dubeolsik not typed, mode key goes to qwerty`() {
        val mc = ModeController(ALL, DUBEOLSIK, tapOnly)
        assertEquals(QWERTY, mc.modeKey())
        assertNull(mc.previous)
        assertEquals(DUBEOLSIK, mc.modeKey()) // English and Korean only (FR-CMN-03f ⑤)
    }

    @Test
    fun `OM-04 row 4 turning graffiti off clears previous graffiti, typed mode key goes in order to qwerty`() {
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
    fun `OM-05 row 5 graffiti in use turned off wraps to dubeolsik, saves it, keeps typed and previous`() {
        val mc = ModeController(ALL, GRAFFITI)
        mc.previous = QWERTY
        mc.onInput()
        mc.apply(noGraffiti)
        assertState(mc, DUBEOLSIK, null, QWERTY)
        assertTrue(mc.typed)
        assertEquals(listOf(DUBEOLSIK), saved)
    }

    @Test
    fun `OM-06 row 6 swipe in use with graffiti and swipe off switches to dubeolsik, previous kept or cleared`() {
        val mc = ModeController(ALL, SWIPE)
        mc.previous = QWERTY
        mc.apply(tapOnly)
        assertState(mc, DUBEOLSIK, null, QWERTY)
        assertEquals(listOf(DUBEOLSIK), saved)

        saved.clear()
        val g = ModeController(ALL, SWIPE)
        g.previous = GRAFFITI
        g.apply(tapOnly)
        assertState(g, DUBEOLSIK, null, null)
        assertEquals(listOf(DUBEOLSIK), saved)
    }

    @Test
    fun `OM-07 row 7 process restart with saved swipe off starts in graffiti and saves it`() {
        val mc = ModeController(ALL, SWIPE, noSwipe)
        assertState(mc, GRAFFITI, null, null)
        assertFalse(mc.typed)
        assertEquals(GRAFFITI, mc.fixedStart)
        assertNull(ModeController(ALL, SWIPE).fixedStart) // on: nothing to save
        assertNull(ModeController(ALL, null, tapOnly).fixedStart) // nothing saved: first mode on, not saved
        assertEquals(DUBEOLSIK, ModeController(ALL, null, tapOnly).current)
    }

    @Test
    fun `OM-08 row 8 return mode graffiti turned off returns to dubeolsik`() {
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
    fun `OM-09 always-on modes cannot be turned off and nothing changes when all stay on`() {
        val mc = ModeController(ALL, DUBEOLSIK)
        mc.previous = GRAFFITI
        assertNull(mc.applyEnabled(emptySet()))
        assertEquals(setOf(QWERTY, DUBEOLSIK), mc.enabled)
        assertEquals(DUBEOLSIK, mc.current)
        assertNull(mc.applyEnabled(ALL.toSet()))
        assertEquals(ALL.toSet(), mc.enabled)
        assertEquals(DUBEOLSIK, mc.nextEnabled(GRAFFITI)) // wraps
        assertEquals(SWIPE, ModeController(listOf(SWIPE), SWIPE, tapOnly).current) // a practice board's one mode
    }

    /** Not typed: the mode key goes in order through the modes on, [n] presses from [start]. */
    private fun cycle(on: Set<InputMode>, start: InputMode, n: Int): List<InputMode> {
        val mc = ModeController(ALL, start, on)
        return List(n) { mc.onModeKey() }
    }

    @Test
    fun `OM-10 swipe off cycles dubeolsik qwerty graffiti`() =
        assertEquals(listOf(QWERTY, GRAFFITI, DUBEOLSIK), cycle(noSwipe, DUBEOLSIK, 3))

    @Test
    fun `OM-11 graffiti off cycles dubeolsik qwerty swipe`() =
        assertEquals(listOf(QWERTY, SWIPE, DUBEOLSIK), cycle(noGraffiti, DUBEOLSIK, 3))

    @Test
    fun `OM-12 graffiti and swipe off cycles dubeolsik qwerty only`() =
        assertEquals(listOf(QWERTY, DUBEOLSIK, QWERTY, DUBEOLSIK), cycle(tapOnly, DUBEOLSIK, 4))
}
