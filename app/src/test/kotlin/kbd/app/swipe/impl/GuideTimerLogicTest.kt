package kbd.app.swipe.impl

import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §7.5 GT-01..GT-08. Defaults: guideDelayMs = 120. The hold timer is gone (issue #142). */
class GuideTimerLogicTest {

    private val allOn = GuideFlags(directionOn = true, nextCharOn = true, bigCurrentOn = true)
    private val slopDp = 8f

    private fun logic(flags: GuideFlags = allOn) = GuideTimerLogic(ThresholdDefaults.VALUES, flags, slopDp)

    private val hidden = GuideView(showDirection = false, showCurrent = false, showNext = false)

    @Test
    fun `GT-01 release at 100ms without moving shows nothing`() {
        val g = logic()
        val timers = g.onDown(0)
        assertEquals(listOf(TimerRequest(TimerKind.SHOW_DELAY, 120)), timers)
        assertNull(g.onMove(movedDp = 2f, t = 50))
        assertEquals(GuidePhase.PRESSED_WAITING, g.phase)
        assertEquals(hidden, g.onUp())
        assertEquals(GuidePhase.IDLE, g.phase)
        // Timers that still fire after up are ignored.
        assertNull(g.onTimer(TimerKind.SHOW_DELAY, 120))
        assertEquals(GuidePhase.IDLE, g.phase)
    }

    @Test
    fun `GT-02 show delay timer at 120ms shows the direction guide`() {
        val g = logic()
        g.onDown(0)
        val v = g.onTimer(TimerKind.SHOW_DELAY, 120)!!
        assertEquals(GuidePhase.SHOWING, g.phase)
        assertTrue(v.showDirection)
    }

    @Test
    fun `GT-03 moving past the slop at 50ms shows at once`() {
        val g = logic()
        g.onDown(0)
        val v = g.onMove(movedDp = slopDp, t = 50)!!
        assertEquals(GuidePhase.SHOWING, g.phase)
        assertTrue(v.showDirection)
        // Show delay was cancelled: its late callback changes nothing.
        assertNull(g.onTimer(TimerKind.SHOW_DELAY, 120))
        // Further moves change nothing either.
        assertNull(g.onMove(movedDp = 20f, t = 60))
    }

    @Test
    fun `GT-04 only the show delay timer, resting past the double window changes nothing`() {
        val g = logic()
        assertEquals(listOf(TimerRequest(TimerKind.SHOW_DELAY, 120)), g.onDown(0))
        assertEquals(TimerKind.entries.toList(), listOf(TimerKind.SHOW_DELAY))
        g.onTimer(TimerKind.SHOW_DELAY, 120)
        assertNull(g.onTimer(TimerKind.SHOW_DELAY, 350))
        assertNull(g.onMove(movedDp = 0f, t = 900))
        assertEquals(GuidePhase.SHOWING, g.phase)
        assertEquals(hidden, g.onUp())
    }

    @Test
    fun `GT-06 direction guide off hides direction only`() {
        val g = logic(allOn.copy(directionOn = false))
        g.onDown(0)
        val v = g.onTimer(TimerKind.SHOW_DELAY, 120)!!
        assertFalse(v.showDirection)
        assertTrue(v.showCurrent)
        assertTrue(v.showNext)
    }

    @Test
    fun `GT-07 password field hides the big current char`() {
        // bigCurrentOn = !isPassword (FR-CMN-08 ①; no-learning fields show it, ②)
        val g = logic(allOn.copy(bigCurrentOn = false))
        g.onDown(0)
        val v = g.onTimer(TimerKind.SHOW_DELAY, 120)!!
        assertFalse(v.showCurrent)
        assertTrue(v.showDirection)
        assertTrue(v.showNext)
    }

    @Test
    fun `GT-08 next char off hides next only`() {
        val g = logic(allOn.copy(nextCharOn = false))
        g.onDown(0)
        val v = g.onTimer(TimerKind.SHOW_DELAY, 120)!!
        assertFalse(v.showNext)
        assertTrue(v.showDirection)
        assertTrue(v.showCurrent)
    }
}
