package kbd.app.view

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `OffKeyboardPointers` (LLD-11 §7.7 OP-01~OP-05, LLD-08 §7.2, SRS FR-CMN-14 ⑤). The keyboard is x < 100; pointers are
 * placed by x only.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
class OffKeyboardPointersTest {

    private val f = OffKeyboardPointers { x, _ -> x < 100f }

    /** An event with pointers [ids] at x [xs] (y 0); [action] is masked, [index] its pointer index. */
    private fun ev(action: Int, index: Int, ids: IntArray, xs: FloatArray, time: Long = 10): MotionEvent {
        val props = Array(ids.size) { MotionEvent.PointerProperties().apply { id = ids[it] } }
        val coords = Array(ids.size) { MotionEvent.PointerCoords().apply { x = xs[it] } }
        return MotionEvent.obtain(0, time, action or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), ids.size, props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
    }

    @Test
    fun `OP-01 with nothing off the keyboard the event is passed as it is`() {
        val down = ev(MotionEvent.ACTION_DOWN, 0, intArrayOf(0), floatArrayOf(50f))
        assertSame(down, f.filter(down))
        assertFalse(f.active)
    }

    @Test
    fun `OP-02 a dropped first finger makes the next keyboard finger a DOWN, its own up gives nothing`() {
        assertNull(f.filter(ev(MotionEvent.ACTION_DOWN, 0, intArrayOf(0), floatArrayOf(150f), time = 5)))
        assertTrue(f.active)
        val d = f.filter(ev(MotionEvent.ACTION_POINTER_DOWN, 1, intArrayOf(0, 1), floatArrayOf(150f, 50f), time = 20))!!
        assertEquals(MotionEvent.ACTION_DOWN, d.actionMasked)
        assertEquals(1, d.pointerCount)
        assertEquals(1, d.getPointerId(0))
        assertEquals(20L, d.downTime)
        assertNull(f.filter(ev(MotionEvent.ACTION_POINTER_UP, 0, intArrayOf(0, 1), floatArrayOf(150f, 50f), time = 30)))
        assertFalse(f.active)
    }

    @Test
    fun `OP-03 a move keeps its batched samples without the dropped pointer`() {
        f.filter(ev(MotionEvent.ACTION_DOWN, 0, intArrayOf(0), floatArrayOf(50f)))
        f.filter(ev(MotionEvent.ACTION_POINTER_DOWN, 1, intArrayOf(0, 1), floatArrayOf(50f, 150f)))
        val move = ev(MotionEvent.ACTION_MOVE, 0, intArrayOf(0, 1), floatArrayOf(60f, 160f), time = 20)
        val c = arrayOf(MotionEvent.PointerCoords().apply { x = 70f }, MotionEvent.PointerCoords().apply { x = 170f })
        move.addBatch(30, c, 0)
        val m = f.filter(move)!!
        assertEquals(MotionEvent.ACTION_MOVE, m.actionMasked)
        assertEquals(1, m.pointerCount)
        assertEquals(1, m.historySize)
        assertEquals(60f, m.getHistoricalX(0, 0))
        assertEquals(20L, m.getHistoricalEventTime(0))
        assertEquals(70f, m.getX(0))
        assertEquals(30L, m.eventTime)
    }

    @Test
    fun `OP-04 with three fingers the kept pointers get new indexes`() {
        f.filter(ev(MotionEvent.ACTION_DOWN, 0, intArrayOf(0), floatArrayOf(150f)))   // dropped
        f.filter(ev(MotionEvent.ACTION_POINTER_DOWN, 1, intArrayOf(0, 1), floatArrayOf(150f, 10f)))   // becomes DOWN
        val d = f.filter(ev(MotionEvent.ACTION_POINTER_DOWN, 2, intArrayOf(0, 1, 2), floatArrayOf(150f, 10f, 20f)))!!
        assertEquals(MotionEvent.ACTION_POINTER_DOWN, d.actionMasked)
        assertEquals(1, d.actionIndex)
        assertEquals(2, d.getPointerId(1))
        val u = f.filter(ev(MotionEvent.ACTION_POINTER_UP, 1, intArrayOf(0, 1, 2), floatArrayOf(150f, 10f, 20f)))!!
        assertEquals(MotionEvent.ACTION_POINTER_UP, u.actionMasked)
        assertEquals(0, u.actionIndex)
        val last = f.filter(ev(MotionEvent.ACTION_POINTER_UP, 1, intArrayOf(0, 2), floatArrayOf(150f, 20f)))!!
        assertEquals(MotionEvent.ACTION_UP, last.actionMasked)   // the last kept finger
        assertEquals(2, last.getPointerId(0))
    }

    @Test
    fun `OP-05 a cancel is passed as it is and clears the dropped pointers`() {
        f.filter(ev(MotionEvent.ACTION_DOWN, 0, intArrayOf(0), floatArrayOf(150f)))
        val cancel = ev(MotionEvent.ACTION_CANCEL, 0, intArrayOf(0), floatArrayOf(150f))
        assertSame(cancel, f.filter(cancel))
        assertFalse(f.active)
    }
}
