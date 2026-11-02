package kbd.app.view

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View

/** Robolectric SDK of the view tests (T36, gate G1): the newest Robolectric 4.17 runs; targetSdk 37 is above it. */
internal object ViewTestSdk {
    const val SDK = 36
}

/**
 * Synthetic multi-pointer touches for one view (`MotionEvent.obtain`), coordinates in px. Pointers
 * go down and up in the order the test calls; each event carries every pointer down at that moment,
 * like the platform's. Event times are `SystemClock.uptimeMillis()`, the clock the views' `Handler`s use.
 */
internal class Touches(private val target: View) {
    private val pointers = sortedMapOf<Int, Pair<Float, Float>>()
    private var downTime = 0L

    fun down(id: Int, x: Float, y: Float) {
        val first = pointers.isEmpty()
        if (first) downTime = SystemClock.uptimeMillis()
        pointers[id] = x to y
        send(if (first) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_POINTER_DOWN, id)
    }

    /** Moves pointer [id] to (x, y) in [steps] equal moves, one event each. */
    fun move(id: Int, x: Float, y: Float, steps: Int = 1) {
        val (x0, y0) = pointers.getValue(id)
        for (i in 1..steps) {
            pointers[id] = (x0 + (x - x0) * i / steps) to (y0 + (y - y0) * i / steps)
            send(MotionEvent.ACTION_MOVE, id)
        }
    }

    fun up(id: Int) {
        send(if (pointers.size == 1) MotionEvent.ACTION_UP else MotionEvent.ACTION_POINTER_UP, id)
        pointers.remove(id)
    }

    /** `ACTION_CANCEL` for the whole gesture (FR-CMN-11a ③). */
    fun cancel() {
        if (pointers.isNotEmpty()) send(MotionEvent.ACTION_CANCEL, pointers.firstKey())
        pointers.clear()
    }

    /** Down and up of one pointer at (x, y). */
    fun tap(id: Int, x: Float, y: Float) {
        down(id, x, y)
        up(id)
    }

    /** What the view's `dispatchTouchEvent` returned for the last event. */
    var lastHandled = false
        private set

    private fun send(action: Int, id: Int) {
        val ids = pointers.keys.toList()
        val props = Array(ids.size) { i -> MotionEvent.PointerProperties().apply { this.id = ids[i]; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val coords = Array(ids.size) { i ->
            MotionEvent.PointerCoords().apply {
                val (px, py) = pointers.getValue(ids[i])
                x = px
                y = py
                pressure = 1f
                size = 1f
            }
        }
        val masked = if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP) {
            action or (ids.indexOf(id) shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        } else {
            action
        }
        val e = MotionEvent.obtain(
            downTime, SystemClock.uptimeMillis(), masked, ids.size, props, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
        lastHandled = target.dispatchTouchEvent(e)
        e.recycle()
    }
}

/** Measures and lays out [v] at exactly [wPx] × [hPx], as its parent would. */
internal fun layoutExactly(v: View, wPx: Int, hPx: Int) {
    v.measure(
        View.MeasureSpec.makeMeasureSpec(wPx, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(hPx, View.MeasureSpec.EXACTLY),
    )
    v.layout(0, 0, v.measuredWidth, v.measuredHeight)
}
