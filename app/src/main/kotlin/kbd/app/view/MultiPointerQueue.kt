package kbd.app.view

import kbd.stroke.layout.TapKeyDef
import kbd.stroke.tap.TapOutput

/**
 * Press-order output queue for overlapping fingers on the tap keyboard (LLD-05A §5, FR-CMN-11, 11a).
 * One slot per pressed pointer in press order; a released key's result waits until every key pressed
 * before it has a result. Function keys never enter this queue.
 */
class MultiPointerQueue {

    private class Slot(val pointerId: Int) {
        var done = false
        var result: Pair<TapKeyDef, TapOutput>? = null
    }

    private val slots = ArrayList<Slot>()

    /** Appends an empty slot for [pointerId]. */
    fun onDown(pointerId: Int) {
        slots += Slot(pointerId)
    }

    /** Fills the slot of [pointerId] and returns the filled slots from the front, in press order. */
    fun onUp(pointerId: Int, key: TapKeyDef, out: TapOutput): List<Pair<TapKeyDef, TapOutput>> =
        fill(pointerId, key to out)

    /** Fills the slot of [pointerId] with no result (classification failed) and flushes the same way. */
    fun onCancel(pointerId: Int): List<Pair<TapKeyDef, TapOutput>> = fill(pointerId, null)

    /**
     * `ACTION_CANCEL`: slots not yet released get no result; every waiting result is returned in
     * press order and the queue is emptied (FR-CMN-11a ③).
     */
    fun onCancelAll(): List<Pair<TapKeyDef, TapOutput>> {
        val outs = slots.mapNotNull { it.result }
        slots.clear()
        return outs
    }

    fun clear() {
        slots.clear()
    }

    private fun fill(pointerId: Int, result: Pair<TapKeyDef, TapOutput>?): List<Pair<TapKeyDef, TapOutput>> {
        val slot = slots.firstOrNull { it.pointerId == pointerId && !it.done } ?: return emptyList()
        slot.done = true
        slot.result = result
        val outs = ArrayList<Pair<TapKeyDef, TapOutput>>()
        while (slots.isNotEmpty() && slots[0].done) {
            slots.removeAt(0).result?.let { outs += it }
        }
        return outs
    }
}
