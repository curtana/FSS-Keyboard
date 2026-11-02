package kbd.app.view

import org.junit.Assert.assertEquals
import org.junit.Test

/** LLD-11 §5.4 RT-01, RT-02. RT-05 needs a real Handler and belongs to T36. */
class RepeatScheduleTest {

    private val schedule = RepeatSchedule(500, 50)

    @Test
    fun `RT-01 release before first delay gives no repeat`() {
        assertEquals(0, schedule.firesBetween(pressAt = 0, from = 0, to = 499))
    }

    @Test
    fun `RT-02 release at 600 gives three repeats at 500 550 600`() {
        assertEquals(3, schedule.firesBetween(pressAt = 0, from = 0, to = 600))
        // Same three, one per window (from, to].
        assertEquals(1, schedule.firesBetween(0, 0, 500))
        assertEquals(1, schedule.firesBetween(0, 500, 550))
        assertEquals(1, schedule.firesBetween(0, 550, 600))
        assertEquals(0, schedule.firesBetween(0, 600, 649))
    }
}
