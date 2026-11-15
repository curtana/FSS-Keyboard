package kbd.app.view

import kbd.app.store.FloatPlace
import kbd.app.store.FloatRatio
import kbd.app.store.PxRect
import kbd.app.view.PlateMover.Effect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `PlateMover`, moving a floating keyboard by its handle (LLD-11 §7.7 PM-01~PM-12, SRS FR-CMN-15 ⑦~⑩, HLD 5A
 * "옮기기 상태" ⓐ~ⓚ, LLD-08 §7.4, FSS#86). Pure: times in ms, places in px.
 *
 * Long press time 400 ms, slop 16 px. The area is (0, 50)–(1000, 1950), the plate 440 × 600 at (280, 1350): 280 px of
 * range to the left and right, 1300 px up.
 */
class PlateMoverTest {

    private val area = PxRect(0, 50, 1000, 1950)
    private val start = PxRect(280, 1350, 720, 1950)
    private var place: FloatPlace? = FloatPlace(area, start)

    private val m = PlateMover(LONG, SLOP)

    /** The caller's part: the place follows what the machine says. */
    private fun apply(effects: List<Effect>): List<Effect> {
        for (e in effects) when (e) {
            is Effect.Moved -> place = place!!.copy(plate = e.plate)
            is Effect.Put -> place = place!!.copy(plate = e.plate)
            is Effect.Cancelled -> place = place!!.copy(plate = e.plate)
            Effect.Started -> Unit
        }
        return effects
    }

    private fun down(id: Int, x: Float, y: Float, t: Long, first: Boolean = true, onHandle: Boolean = true) =
        apply(m.down(first, id, x, y, t, onHandle, place))

    private fun move(x: Float, y: Float, t: Long) = apply(m.move(x, y, t, place))
    private fun up(id: Int, t: Long) = apply(m.up(id, t, place))
    private fun timeout(t: Long) = apply(m.timeout(t, place))

    private val none = emptyList<Effect>()

    @Test
    fun `PM-01 the first finger on the handle waits - the long press time ends the wait, 1 ms before it does not`() {
        assertEquals(none, down(0, 500f, 1360f, T))   // ⓐ
        assertTrue(m.waiting)
        assertEquals(0, m.pointer)
        assertEquals(T + LONG, m.waitEnd)
        assertEquals(none, timeout(T + LONG - 1))
        assertTrue(m.waiting)
        assertEquals(listOf(Effect.Started), timeout(T + LONG))   // ⓕ: that long or longer
        assertTrue(m.moving)
        assertFalse(m.waiting)
        assertNull(m.waitEnd)
        assertEquals(none, timeout(T + LONG + 50))   // the start is told once
    }

    @Test
    fun `PM-02 not the first finger, or not on the handle - nothing begins`() {
        assertEquals(none, down(0, 500f, 1700f, T, onHandle = false))   // a key
        assertNull(m.pointer)
        assertEquals(none, down(1, 500f, 1360f, T + 10, first = false))   // ⓑ: the handle, another finger is down
        assertNull(m.pointer)
        assertEquals(none, timeout(T + 5 * LONG))
        assertEquals(none, up(0, T + 5 * LONG))
        assertEquals(none, timeout(T + 9 * LONG))   // the finger left on the handle never begins a move
        assertEquals(none, move(900f, 300f, T + 9 * LONG))
        assertEquals(none, up(1, T + 9 * LONG))
        assertEquals(start, place!!.plate)
    }

    @Test
    fun `PM-03 the slop - less keeps the wait, the slop itself across or down drops it for good`() {
        down(0, 500f, 1360f, T)
        assertEquals(none, move(500f + SLOP - 0.5f, 1360f - SLOP + 0.5f, T + 100))
        assertTrue(m.waiting)
        assertEquals(none, move(500f + SLOP, 1360f, T + 150))   // ⓒ: across
        assertFalse(m.waiting)
        assertNull(m.pointer)
        assertEquals(none, move(500f, 1360f, T + 200))   // back and still: no move begins
        assertEquals(none, timeout(T + LONG))
        assertEquals(none, move(500f, 1360f, T + 3 * LONG))
        assertFalse(m.moving)
        assertEquals(none, up(0, T + 3 * LONG))   // ⓚ
        // Down: the same; and a new gesture can wait again.
        down(0, 500f, 1360f, T2)
        assertTrue(m.waiting)
        move(500f, 1360f - SLOP, T2 + 100)
        assertFalse(m.waiting)
        assertEquals(none, timeout(T2 + LONG))
        up(0, T2 + LONG)
        assertEquals(start, place!!.plate)
    }

    @Test
    fun `PM-04 up before the long press time - nothing`() {
        down(0, 500f, 1360f, T)
        assertEquals(none, up(0, T + LONG - 1))   // ⓓ
        assertNull(m.pointer)
        assertEquals(none, timeout(T + LONG))
        assertEquals(start, place!!.plate)
    }

    @Test
    fun `PM-05 another finger while waiting drops the wait, wherever it comes down`() {
        for (onHandle in listOf(true, false)) {
            val mover = PlateMover(LONG, SLOP)
            assertEquals(none, mover.down(true, 0, 500f, 1360f, T, true, place))
            assertEquals(none, mover.down(false, 1, 300f, 1700f, T + 100, onHandle, place))   // ⓔ
            assertFalse("$onHandle", mover.waiting)
            assertEquals("$onHandle", none, mover.timeout(T + LONG, place))
            assertEquals("$onHandle", none, mover.up(1, T + LONG + 10, place))   // the other finger up: still dropped
            assertEquals("$onHandle", none, mover.timeout(T + 2 * LONG, place))
            assertEquals("$onHandle", none, mover.move(500f, 1360f, T + 2 * LONG, place))
            assertFalse("$onHandle", mover.moving)
            assertEquals("$onHandle", none, mover.up(0, T + 2 * LONG + 10, place))
        }
    }

    @Test
    fun `PM-06 moving - the plate is the start plus the finger's way, stops at the ends and comes back`() {
        down(0, 500f, 1360f, T)
        move(505f, 1358f, T + 100)   // within the slop: the move will begin from here
        assertEquals(listOf(Effect.Started), timeout(T + LONG))
        assertEquals(none, move(505f, 1358f, T + LONG + 10))   // no way yet
        assertEquals(listOf(Effect.Moved(PxRect(180, 1150, 620, 1750))), move(405f, 1158f, T + LONG + 20))   // ⓖ
        assertEquals(none, move(405.2f, 1158.3f, T + LONG + 25))   // the same px
        assertEquals(listOf(Effect.Moved(PxRect(0, 50, 440, 650))), move(-900f, -5000f, T + LONG + 30))   // past two ends
        assertEquals(none, move(-950f, -6000f, T + LONG + 40))   // further out: it stays at the ends
        assertEquals(listOf(Effect.Moved(PxRect(560, 1250, 1000, 1850))), move(5000f, 1258f, T + LONG + 50))
        assertEquals(listOf(Effect.Moved(PxRect(270, 1350, 710, 1950))), move(495f, 9000f, T + LONG + 60))   // back: no offset left
        assertTrue(m.moving)
    }

    @Test
    fun `PM-07 an event at or after the long press time begins the move before it is handled`() {
        // The move event comes before the timer: the move begins where the finger was last seen and follows at once.
        down(0, 500f, 1360f, T)
        move(504f, 1360f, T + 200)
        assertEquals(listOf(Effect.Started, Effect.Moved(PxRect(380, 1350, 820, 1950))), move(604f, 1360f, T + LONG))
        up(0, T + LONG + 5)
        // A move past the slop, 1 ms before: dropped.
        place = FloatPlace(area, start)
        down(0, 500f, 1360f, T2)
        assertEquals(none, move(600f, 1360f, T2 + LONG - 1))
        assertEquals(none, timeout(T2 + LONG))
        up(0, T2 + LONG)
        // Up at the long press time: the move began and is put where it started, with nothing to store.
        down(0, 500f, 1360f, T3)
        assertEquals(listOf(Effect.Started, Effect.Put(start, FloatRatio(null, null))), up(0, T3 + LONG))
        // Another finger at the long press time: the move began, the finger does not drop it.
        down(0, 500f, 1360f, T4)
        assertEquals(listOf(Effect.Started), down(1, 300f, 1700f, T4 + LONG, first = false, onHandle = false))
        assertTrue(m.moving)
    }

    @Test
    fun `PM-08 another finger while moving changes nothing - the plate follows the first one only`() {
        down(0, 500f, 1360f, T)
        timeout(T + LONG)
        assertEquals(none, down(1, 300f, 1700f, T + LONG + 10, first = false, onHandle = false))   // ⓗ
        assertEquals(none, down(2, 500f, 1360f, T + LONG + 12, first = false, onHandle = true))    // ⓑ while moving
        assertEquals(0, m.pointer)
        assertEquals(listOf(Effect.Moved(PxRect(290, 1340, 730, 1940))), move(510f, 1350f, T + LONG + 20))
        assertEquals(none, up(1, T + LONG + 30))
        assertEquals(none, up(2, T + LONG + 30))
        assertTrue(m.moving)
        assertEquals(1, up(0, T + LONG + 40).size)
        assertFalse(m.moving)
    }

    @Test
    fun `PM-09 putting - the shares of the axes that changed, once - none when it did not move`() {
        // Not moved: nothing to store.
        down(0, 500f, 1360f, T)
        timeout(T + LONG)
        assertEquals(listOf(Effect.Put(start, FloatRatio(null, null))), up(0, T + LONG + 300))   // ⓘ
        assertNull(m.pointer)
        assertEquals(none, up(0, T + LONG + 301))
        // Moved and brought back to the px it started at: nothing to store either.
        down(0, 500f, 1360f, T2)
        timeout(T2 + LONG)
        move(300f, 900f, T2 + LONG + 10)
        move(500.4f, 1360f, T2 + LONG + 20)
        assertEquals(listOf(Effect.Put(start, FloatRatio(null, null))), up(0, T2 + LONG + 30))
        // Across only: the share across, none down. 280 − 70 = 210 of 560.
        down(0, 500f, 1360f, T3)
        timeout(T3 + LONG)
        move(430f, 1360f, T3 + LONG + 10)
        assertEquals(listOf(Effect.Put(PxRect(210, 1350, 650, 1950), FloatRatio(210f / 560, null))), up(0, T3 + LONG + 20))
        // Down only, from there: the share down, none across. 1300 − 650 = 650 of 1300.
        down(0, 400f, 1360f, T4)
        timeout(T4 + LONG)
        move(400f, 710f, T4 + LONG + 10)
        assertEquals(listOf(Effect.Put(PxRect(210, 700, 650, 1300), FloatRatio(null, 0.5f))), up(0, T4 + LONG + 20))
        // Both, to an end: 0 across, 0 down.
        down(0, 400f, 710f, T4 + 5000)
        timeout(T4 + 5000 + LONG)
        move(-400f, -710f, T4 + 5000 + LONG + 10)
        assertEquals(listOf(Effect.Put(PxRect(0, 50, 440, 650), FloatRatio(0f, 0f))), up(0, T4 + 5000 + LONG + 20))
    }

    @Test
    fun `PM-10 an axis with no range is not stored - a full-width plate stores down only`() {
        val wide = PxRect(0, 1350, 1000, 1950)
        place = FloatPlace(area, wide)
        down(0, 500f, 1360f, T)
        timeout(T + LONG)
        assertEquals(listOf(Effect.Moved(PxRect(0, 1050, 1000, 1650))), move(900f, 1060f, T + LONG + 10))
        assertEquals(listOf(Effect.Put(PxRect(0, 1050, 1000, 1650), FloatRatio(null, 1000f / 1300))), up(0, T + LONG + 20))
    }

    @Test
    fun `PM-11 cancel - a move goes back to where it started and stores nothing, a wait or a dropped wait just ends`() {
        // The four causes (the touch is cancelled, the screen turns, the keyboard goes down, laid out again) are one call.
        down(0, 500f, 1360f, T)
        timeout(T + LONG)
        move(300f, 800f, T + LONG + 10)
        assertEquals(PxRect(80, 790, 520, 1390), place!!.plate)
        assertEquals(listOf(Effect.Cancelled(start)), apply(m.cancel()))   // ⓙ
        assertEquals(start, place!!.plate)
        assertNull(m.pointer)
        assertEquals(none, move(100f, 100f, T + LONG + 20))   // the finger is still down: nothing follows it
        assertEquals(none, up(0, T + LONG + 30))
        assertEquals(none, apply(m.cancel()))
        // While waiting.
        down(0, 500f, 1360f, T2)
        assertEquals(none, apply(m.cancel()))
        assertFalse(m.waiting)
        assertEquals(none, timeout(T2 + LONG))
        assertEquals(none, up(0, T2 + LONG))
        // While dropped.
        down(0, 500f, 1360f, T3)
        move(600f, 1360f, T3 + 10)
        assertEquals(none, apply(m.cancel()))
        assertEquals(none, up(0, T3 + 20))
        // A new gesture's first finger ends whatever was left of the last one.
        down(0, 500f, 1360f, T4)
        timeout(T4 + LONG)
        move(300f, 800f, T4 + LONG + 10)
        assertEquals(listOf(Effect.Cancelled(start)), down(5, 500f, 1360f, T4 + 2000))
        assertTrue(m.waiting)
        assertEquals(5, m.pointer)
    }

    @Test
    fun `PM-12 no floating keyboard when the wait ends - nothing begins`() {
        down(0, 500f, 1360f, T)
        place = null   // docked in the meantime
        assertEquals(none, m.timeout(T + LONG, null))
        assertNull(m.pointer)
        assertEquals(none, m.up(0, T + LONG + 10, null))
    }

    private companion object {
        const val LONG = 400L
        const val SLOP = 16f
        const val T = 10_000L
        const val T2 = 20_000L
        const val T3 = 30_000L
        const val T4 = 40_000L
    }
}
