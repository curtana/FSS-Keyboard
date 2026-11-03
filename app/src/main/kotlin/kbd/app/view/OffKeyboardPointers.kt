package kbd.app.view

import android.view.MotionEvent

/**
 * Pointers that went down off the keyboard (the blank band, the side margins of a narrowed keyboard, the system bar
 * padding) give no input, even while another finger is on the keyboard (SRS FR-CMN-14 ⑤, HLD 5A, LLD-08 §7.2).
 *
 * Android would attach such a pointer to the child that already has a touch (`ACTION_POINTER_DOWN` outside every
 * child goes to the earliest target) and, when it is the first finger, keep every later finger of the gesture from the
 * children. [filter] therefore gives the children the event without those pointers: a kept finger that comes down
 * while only dropped ones are down becomes `ACTION_DOWN`, the last kept one going up becomes `ACTION_UP`, and the
 * samples of a batched move are kept (strokes read the history).
 */
internal class OffKeyboardPointers(private val onKeyboard: (x: Float, y: Float) -> Boolean) {

    private val dropped = HashSet<Int>()
    private var downTime = 0L

    /** True while some pointer of the gesture is dropped (only then [filter] makes a new event). */
    val active: Boolean get() = dropped.isNotEmpty()

    /**
     * The event for the children, or null when it is only about dropped pointers. Call for every event before the
     * children see it; the caller recycles a returned event that is not [ev].
     */
    fun filter(ev: MotionEvent): MotionEvent? {
        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            dropped.clear()
            downTime = ev.downTime
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            dropped.clear()
            return ev   // every child must stop
        }
        val i = ev.actionIndex
        val id = ev.getPointerId(i)
        if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) && !onKeyboard(ev.getX(i), ev.getY(i))) {
            dropped += id
        }
        if (dropped.isEmpty()) return ev
        val ownAction = action != MotionEvent.ACTION_MOVE
        val out = when {
            ownAction && id in dropped -> null   // its own down or up: nothing for the children
            else -> rebuild(ev, action, id, ownAction)
        }
        if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) dropped -= id
        return out
    }

    private fun rebuild(ev: MotionEvent, action: Int, id: Int, ownAction: Boolean): MotionEvent? {
        val kept = (0 until ev.pointerCount).filter { ev.getPointerId(it) !in dropped }
        if (kept.isEmpty()) return null
        val newAction = when {
            !ownAction -> action
            action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_DOWN ->
                if (kept.size == 1) MotionEvent.ACTION_DOWN.also { downTime = ev.eventTime }
                else MotionEvent.ACTION_POINTER_DOWN or (kept.indexOfFirst { ev.getPointerId(it) == id } shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            else -> // ACTION_POINTER_UP or ACTION_UP of a kept pointer
                if (kept.size == 1) MotionEvent.ACTION_UP
                else MotionEvent.ACTION_POINTER_UP or (kept.indexOfFirst { ev.getPointerId(it) == id } shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        }
        val props = Array(kept.size) { MotionEvent.PointerProperties().also { p -> ev.getPointerProperties(kept[it], p) } }
        fun coords(h: Int?) = Array(kept.size) {
            MotionEvent.PointerCoords().also { c -> if (h == null) ev.getPointerCoords(kept[it], c) else ev.getHistoricalPointerCoords(kept[it], h, c) }
        }
        val n = ev.historySize
        val first = if (n > 0) coords(0) else coords(null)
        val firstTime = if (n > 0) ev.getHistoricalEventTime(0) else ev.eventTime
        val out = MotionEvent.obtain(
            downTime, firstTime, newAction, kept.size, props, first, ev.metaState, ev.buttonState,
            ev.xPrecision, ev.yPrecision, ev.deviceId, ev.edgeFlags, ev.source, ev.flags,
        )
        for (h in 1 until n) out.addBatch(ev.getHistoricalEventTime(h), coords(h), ev.metaState)
        if (n > 0) out.addBatch(ev.eventTime, coords(null), ev.metaState)
        return out
    }
}
