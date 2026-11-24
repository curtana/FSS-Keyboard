package kbd.app.ime

/**
 * Cursor ledger for word suggestions (LLD-12 §8.4, D-82, SRS FR-SUG-04 ②): tells whether a cursor notice is
 * explained by the keyboard's own writes. It is separate from the Hangul composing verdict of [ComposingController]
 * (`predictedSel`, `expected`, [SelectionVerdict]), which it never changes. Every write of [ComposingController]
 * reports here.
 *
 * Positions are UTF-16 units. A new cursor c always sets the expected selection to (c, c) and adds c at the end of
 * the expected cursor queue.
 */
internal class OwnEditLedger {

    /** One of our writes made while waiting for the result notice of an [unknown] write. */
    private sealed interface Write {
        data class Committed(val len: Int) : Write
        data object DeletedKey : Write
        data class DeletedBefore(val n: Int) : Write
    }

    /** Expected selection; −1 = unknown. */
    private var s = -1
    private var e = -1

    /** Expected cursors of our writes, oldest first. */
    private val expected = ArrayDeque<Int>()

    /** True = the result notice of an [unknown] write has not come yet. */
    private var awaiting = false

    /** Our writes made while [awaiting], replayed once the result notice comes. */
    private val pending = ArrayList<Write>()

    /** Our writes since the last notice; used only in the unknown state that is not [awaiting]. */
    private var writesSinceUpdate = 0

    private val known: Boolean get() = s >= 0 && e >= 0

    /** `onStart`: known when both are ≥ 0, otherwise unknown (not awaiting). */
    fun start(selS: Int, selE: Int) {
        if (selS >= 0 && selE >= 0) set(minOf(selS, selE), maxOf(selS, selE)) else forget()
        awaiting = false
        expected.clear()
        pending.clear()
        writesSinceUpdate = 0
    }

    /** Committed text of [len] UTF-16 units (it replaces a selection; the cursor goes from its start). */
    fun committed(len: Int) = write(Write.Committed(len))

    /** One ⌫ key event. */
    fun deletedKey() = write(Write.DeletedKey)

    /** `deleteSurroundingText(n, 0)`. */
    fun deletedBefore(n: Int) = write(Write.DeletedBefore(n))

    /** A write whose resulting cursor cannot be predicted: the next notice is taken as its result. */
    fun unknown() {
        writesSinceUpdate += 1
        forget()
        pending.clear()
        awaiting = true
    }

    /** Whether the notice ([newS], [newE]) is explained by our writes (LLD-12 §8.4 `explains` 1–6). Once per notice. */
    fun explains(newS: Int, newE: Int): Boolean {
        val w = writesSinceUpdate
        writesSinceUpdate = 0
        val lo = minOf(newS, newE)
        val hi = maxOf(newS, newE)
        if (awaiting) {
            // The result of the unknown write; then the writes made meanwhile go onto the expected cursors.
            set(lo, hi)
            awaiting = false
            val replay = pending.toList()
            pending.clear()
            replay.forEach(::write)
            return true
        }
        if (!known) {
            // Unknown without awaiting is never a reset; resync only when no write of ours is still in flight.
            if (w == 0) set(lo, hi)
            return true
        }
        if (newS == newE) {
            val i = expected.indexOf(newE)
            if (i >= 0) {
                repeat(i + 1) { expected.removeFirst() }
                return true
            }
        }
        if (lo == s && hi == e) {
            expected.clear()
            return true
        }
        set(lo, hi)
        expected.clear()
        return false
    }

    private fun write(wr: Write) {
        writesSinceUpdate += 1
        if (awaiting) {
            pending += wr
            return
        }
        if (!known) return
        when (wr) {
            is Write.Committed -> moveTo(s + wr.len)
            Write.DeletedKey -> when {
                s != e -> moveTo(s)
                e >= 1 -> moveTo(e - 1)
                else -> forget()
            }
            is Write.DeletedBefore -> if (s == e && e >= wr.n) moveTo(e - wr.n) else forget()
        }
    }

    private fun moveTo(c: Int) {
        set(c, c)
        expected.addLast(c)
    }

    private fun set(selS: Int, selE: Int) {
        s = selS
        e = selE
    }

    /** Unknown; the expected cursors are dropped so a later resync does not match a stale one. */
    private fun forget() {
        s = -1
        e = -1
        expected.clear()
    }
}
