package kbd.app.view

import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapKeyKind
import kbd.stroke.tap.TapOutput
import org.junit.Assert.assertEquals
import org.junit.Test

/** LLD-11 §5.4 MQ-01..MQ-04, MQ-07. MQ-05, MQ-06, MQ-08 need the view and belong to T36. */
class MultiPointerQueueTest {

    private fun key(id: String) = TapKeyDef(id, row = 1, x = 0f, w = 36f, kind = TapKeyKind.QWERTY_LETTER, label = id)
    private fun out(text: String) = TapOutput(text, jamo = null, errorVibe = false)

    private val a = key("a") to out("a")
    private val b = key("b") to out("b")
    private val c = key("c") to out("c")

    private val queue = MultiPointerQueue<Pair<TapKeyDef, TapOutput>>()

    private fun up(id: Int, r: Pair<TapKeyDef, TapOutput>) = queue.onUp(id, r)

    @Test
    fun `MQ-01 later key released first waits for the earlier key`() {
        queue.onDown(1)
        queue.onDown(2)
        assertEquals(emptyList<Any>(), up(2, b))
        assertEquals(listOf(a, b), up(1, a))
    }

    @Test
    fun `MQ-02 separate taps come out one by one`() {
        queue.onDown(1)
        assertEquals(listOf(a), up(1, a))
        queue.onDown(2)
        assertEquals(listOf(b), up(2, b))
    }

    @Test
    fun `MQ-03 three overlapping keys come out in press order`() {
        queue.onDown(1)
        queue.onDown(2)
        queue.onDown(3)
        assertEquals(emptyList<Any>(), up(3, c))
        assertEquals(listOf(a), up(1, a))
        assertEquals(listOf(b, c), up(2, b))
    }

    @Test
    fun `MQ-04 cancelled slot is skipped and later result flows`() {
        queue.onDown(1)
        queue.onDown(2)
        assertEquals(emptyList<Any>(), queue.onCancel(1))
        assertEquals(listOf(b), up(2, b))
    }

    @Test
    fun `MQ-07 cancel all returns only released results in press order`() {
        queue.onDown(1)
        queue.onDown(2)
        assertEquals(emptyList<Any>(), up(2, b))
        queue.onDown(3)
        assertEquals(listOf(b), queue.onCancelAll())
        // Queue is empty afterwards: a new press flows on its own.
        queue.onDown(4)
        assertEquals(listOf(a), up(4, a))
    }
}
