package kbd.app.view

/**
 * Press-order output queue for overlapping fingers on the tap keyboard (LLD-05A §5, FR-CMN-11, 11a).
 * One slot per pressed pointer in press order; a released key's result waits until every key pressed
 * before it has a result. Function keys never enter this queue. [R] is what a released key gives: the key, its
 * output and the judged gesture (`TapTouchLogic`).
 */
class MultiPointerQueue<R : Any> {

    private class Slot<R>(val pointerId: Int) {
        var done = false
        var result: R? = null
    }

    private val slots = ArrayList<Slot<R>>()

    /** Appends an empty slot for [pointerId]. */
    fun onDown(pointerId: Int) {
        slots += Slot(pointerId)
    }

    /** Fills the slot of [pointerId] and returns the filled slots from the front, in press order. */
    fun onUp(pointerId: Int, result: R): List<R> = fill(pointerId, result)

    /** Fills the slot of [pointerId] with no result (classification failed) and flushes the same way. */
    fun onCancel(pointerId: Int): List<R> = fill(pointerId, null)

    /**
     * `ACTION_CANCEL`: slots not yet released get no result; every waiting result is returned in
     * press order and the queue is emptied (FR-CMN-11a ③).
     */
    fun onCancelAll(): List<R> {
        val outs = slots.mapNotNull { it.result }
        slots.clear()
        return outs
    }

    fun clear() {
        slots.clear()
    }

    private fun fill(pointerId: Int, result: R?): List<R> {
        val slot = slots.firstOrNull { it.pointerId == pointerId && !it.done } ?: return emptyList()
        slot.done = true
        slot.result = result
        val outs = ArrayList<R>()
        while (slots.isNotEmpty() && slots[0].done) {
            slots.removeAt(0).result?.let { outs += it }
        }
        return outs
    }
}
