package kbd.app.view

import kbd.app.store.FloatPlace
import kbd.app.store.FloatRatio
import kbd.app.store.FloatingKeyboard
import kbd.app.store.PxRect
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Moving a floating keyboard by its handle, as a state machine (SRS FR-CMN-15 ⑦~⑩, HLD 5A "옮기기 상태" ⓐ~ⓚ,
 * LLD-08 §7.4, FSS#86). Times, places and limits come in as values: no Android, unit tested.
 *
 * Rest → (the window's first finger comes down on the handle) → waiting → (it stays within [slopPx] of where it came
 * down until [longPressMs] have gone by) → moving → (it goes up) → rest, the place put. A finger that moves [slopPx]
 * or more, or another finger coming down, while waiting drops the wait until that finger goes up. Coordinates are the
 * input view's, px: the view does not move while its plate does.
 *
 * The long press time is counted from the press and has gone by when it is that long or longer; an event at or after
 * that moment starts the move first, from where the finger was last seen, and is then handled as an event of the move.
 * The caller's timer ([timeout]) only brings that moment forward for a finger that does not move.
 */
internal class PlateMover(private val longPressMs: Long, private val slopPx: Float) {

    /** What the caller does after an event, in order. */
    sealed interface Effect {
        /** The move began: one vibration, the handle shown as held, the rest of the gesture taken from the keys. */
        data object Started : Effect

        /** The plate goes to [plate]. */
        data class Moved(val plate: PxRect) : Effect

        /** The plate is put at [plate]; [save] has the shares of the axes on which it is not where it started. */
        data class Put(val plate: PxRect, val save: FloatRatio) : Effect

        /** The move is given up: the plate goes back to [plate], where it started, and nothing is stored. */
        data class Cancelled(val plate: PxRect) : Effect
    }

    private sealed interface State
    private data object Rest : State
    private class Waiting(val id: Int, val x: Float, val y: Float, val at: Long, var lastX: Float, var lastY: Float) : State
    private class Moving(val id: Int, val from: PxRect, val x: Float, val y: Float, var plate: PxRect) : State
    private class Dropped(val id: Int) : State

    private var state: State = Rest

    /** True while a finger on the handle is waited on for the long press time. */
    val waiting: Boolean get() = state is Waiting

    /** True while the plate follows a finger. */
    val moving: Boolean get() = state is Moving

    /** The finger that is waited on or followed, or null. */
    val pointer: Int? get() = when (val s = state) {
        is Waiting -> s.id
        is Moving -> s.id
        else -> null
    }

    /** When the wait ends, or null when nothing is waited on. */
    val waitEnd: Long? get() = (state as? Waiting)?.let { it.at + longPressMs }

    /**
     * A finger came down at ([x], [y]). [first] = the window's first finger of the gesture (`ACTION_DOWN`),
     * [onHandle] = it is on the move handle of a floating keyboard.
     */
    fun down(first: Boolean, id: Int, x: Float, y: Float, time: Long, onHandle: Boolean, place: FloatPlace?): List<Effect> {
        var out = catchUp(time, place)
        if (first && state != Rest) out = out + cancel()   // a new gesture: whatever was left of the last one is over
        when (state) {
            Rest -> if (first && onHandle) state = Waiting(id, x, y, time, x, y)   // ⓐ; not the first finger: ⓑ
            is Waiting -> state = Dropped((state as Waiting).id)   // ⓔ, wherever the other finger came down
            is Moving, is Dropped -> Unit   // ⓗ
        }
        return out
    }

    /** The finger [pointer] is at ([x], [y]). */
    fun move(x: Float, y: Float, time: Long, place: FloatPlace?): List<Effect> {
        val out = catchUp(time, place)
        when (val s = state) {
            is Waiting ->
                if (abs(x - s.x) >= slopPx || abs(y - s.y) >= slopPx) {
                    state = Dropped(s.id)   // ⓒ
                } else {
                    s.lastX = x
                    s.lastY = y
                }
            is Moving -> if (place != null) {   // ⓖ
                val to = FloatingKeyboard.drag(s.from, (x - s.x).roundToInt(), (y - s.y).roundToInt(), place.area)
                if (to != s.plate) {
                    s.plate = to
                    return out + Effect.Moved(to)
                }
            }
            else -> Unit
        }
        return out
    }

    /** The finger [id] went up. */
    fun up(id: Int, time: Long, place: FloatPlace?): List<Effect> {
        val out = catchUp(time, place)
        when (val s = state) {
            is Waiting -> if (s.id == id) state = Rest   // ⓓ
            is Dropped -> if (s.id == id) state = Rest   // ⓚ
            is Moving -> if (s.id == id) {   // ⓘ
                state = Rest
                val r = if (place != null) FloatingKeyboard.ratios(s.plate, place.area) else FloatRatio(null, null)
                val save = FloatRatio(
                    fx = r.fx.takeIf { s.plate.left != s.from.left },
                    fy = r.fy.takeIf { s.plate.top != s.from.top },
                )
                return out + Effect.Put(s.plate, save)
            }
            Rest -> Unit
        }
        return out
    }

    /** The caller's timer: it is [time] now. */
    fun timeout(time: Long, place: FloatPlace?): List<Effect> = catchUp(time, place)

    /**
     * The touch was cancelled, the screen turned, the keyboard went down or it is being laid out again (ⓙ): a move goes
     * back to where it started, a wait is over.
     */
    fun cancel(): List<Effect> {
        val s = state
        state = Rest
        return if (s is Moving) listOf(Effect.Cancelled(s.from)) else emptyList()
    }

    /** ⓕ: the wait ends when [time] is the long press time or more after the press. */
    private fun catchUp(time: Long, place: FloatPlace?): List<Effect> {
        val s = state as? Waiting ?: return emptyList()
        if (time - s.at < longPressMs) return emptyList()
        if (place == null) {   // no floating keyboard to move any more
            state = Rest
            return emptyList()
        }
        state = Moving(s.id, place.plate, s.lastX, s.lastY, place.plate)
        return listOf(Effect.Started)
    }
}
