package kbd.app.view

import android.os.Handler
import android.os.Looper
import java.time.Duration
import kbd.stroke.layout.FunctionKey
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * [RepeatTimer] on a real main-looper [Handler] (LLD-05A §6, LLD-11 §5.4 RT-05, gate G1).
 * Robolectric's paused looper: time moves only by `idleFor`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class RepeatTimerTest {

    private val fired = ArrayList<FunctionKey>()
    private val looper get() = shadowOf(Looper.getMainLooper())
    private val timer = RepeatTimer(Handler(Looper.getMainLooper()), RepeatSchedule(500, 50)) { fired += it }

    @Test
    fun `RT-05 backspace pressed, stopAll at 520 ms gives one repeat and no more`() {
        timer.start(0, FunctionKey.BACKSPACE)
        looper.idleFor(Duration.ofMillis(499))
        assertEquals(0, fired.size)
        looper.idleFor(Duration.ofMillis(21))   // 520 ms: the repeat at 500 has come
        assertEquals(listOf(FunctionKey.BACKSPACE), fired)
        timer.stopAll()   // ACTION_CANCEL (FR-CMN-11a ③)
        looper.idleFor(Duration.ofMillis(2_000))
        assertEquals(1, fired.size)
    }

    @Test
    fun `repeats every interval after the first, stop ends only that pointer`() {
        timer.start(0, FunctionKey.BACKSPACE)
        timer.start(1, FunctionKey.ARROW_LEFT)
        looper.idleFor(Duration.ofMillis(600))   // 500, 550, 600 for each
        assertEquals(3, fired.count { it == FunctionKey.BACKSPACE })
        assertEquals(3, fired.count { it == FunctionKey.ARROW_LEFT })
        timer.stop(1)
        looper.idleFor(Duration.ofMillis(100))
        assertEquals(5, fired.count { it == FunctionKey.BACKSPACE })
        assertEquals(3, fired.count { it == FunctionKey.ARROW_LEFT })
    }

    @Test
    fun `a fire that stops its own pointer posts nothing more`() {
        lateinit var t: RepeatTimer
        val n = intArrayOf(0)
        t = RepeatTimer(Handler(Looper.getMainLooper()), RepeatSchedule(500, 50)) { n[0]++; t.stop(0) }
        t.start(0, FunctionKey.BACKSPACE)
        looper.idleFor(Duration.ofMillis(1_000))
        assertEquals(1, n[0])
    }
}
