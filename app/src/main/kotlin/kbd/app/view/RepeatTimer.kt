package kbd.app.view

import android.os.Handler
import kbd.stroke.layout.FunctionKey

/**
 * Key repeat timing, pure (LLD-05A §6). The press itself is not a repeat; the first repeat comes
 * [initialDelayMs] after the press, then one every [intervalMs].
 * Values come from `ViewConfiguration.getKeyRepeatTimeout()` / `getKeyRepeatDelay()` (D-16).
 */
class RepeatSchedule(val initialDelayMs: Long, val intervalMs: Long) {
    init {
        require(initialDelayMs >= 0) { "initialDelayMs < 0" }
        require(intervalMs > 0) { "intervalMs <= 0" }
    }

    /** Number of repeats at times in `(from, to]` for a key pressed at [pressAt]. */
    fun firesBetween(pressAt: Long, from: Long, to: Long): Int = firesUpTo(pressAt, to) - firesUpTo(pressAt, from)

    /** Repeats at times `≤ t`: `pressAt + initialDelayMs + k·intervalMs`, k ≥ 0. */
    private fun firesUpTo(pressAt: Long, t: Long): Int {
        val first = pressAt + initialDelayMs
        if (t < first) return 0
        return ((t - first) / intervalMs + 1).toInt()
    }
}

/**
 * Handler-driven repeat for ⌫ and arrow keys, one per pointer (LLD-05A §6, FR-CMN-07c, FR-TAP-10).
 * One [fire] = one key. Callers give no key feedback on repeats (FR-CMN-11a ②) and stop on
 * up ([stop]), `ACTION_CANCEL` and window hide ([stopAll], FR-CMN-11a ③).
 */
class RepeatTimer(
    private val handler: Handler,
    private val schedule: RepeatSchedule,
    private val fire: (FunctionKey) -> Unit,
) {
    private val running = HashMap<Int, Runnable>()

    fun start(pointerId: Int, key: FunctionKey) {
        stop(pointerId)
        val tick = object : Runnable {
            override fun run() {
                if (running[pointerId] !== this) return
                fire(key)
                // fire may have stopped this pointer.
                if (running[pointerId] === this) handler.postDelayed(this, schedule.intervalMs)
            }
        }
        running[pointerId] = tick
        handler.postDelayed(tick, schedule.initialDelayMs)
    }

    fun stop(pointerId: Int) {
        running.remove(pointerId)?.let(handler::removeCallbacks)
    }

    fun stopAll() {
        running.values.forEach(handler::removeCallbacks)
        running.clear()
    }
}
