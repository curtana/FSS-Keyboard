package kbd.app.error

import java.io.File
import kbd.app.store.ErrorLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** EG-01..EG-04 (LLD-11 §7.5, LLD-09 §2). EG-05 needs the real InputDispatcher and belongs to T27. */
class EngineGuardTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class FakeState : GuardedState {
        var value = 0
        var snapshots = 0
        var restores = 0
        var restored: Any? = null

        override fun snapshot(): Any {
            snapshots++
            return value
        }

        override fun restore(s: Any) {
            restores++
            restored = s
            value = s as Int
        }
    }

    private val state = FakeState()
    private val log by lazy { ErrorLog(File(tmp.root, "errors.log")) }
    private val guard by lazy { EngineGuard(log, state) }

    private fun records(): List<String> = log.readAll().lines().filter { it.isNotEmpty() }

    @Test
    fun `EG-01 stroke that throws returns null, restores once and records once`() {
        state.value = 7
        val result = guard.stroke<String>("stroke.finish") {
            state.value = 99
            throw IllegalArgumentException("bad")
        }
        assertNull(result)
        assertEquals(1, state.restores)
        assertSame(7, state.restored)
        assertEquals(7, state.value)
        assertEquals(1, records().size)
        assertEquals(listOf("stroke.finish", "IllegalArgumentException"), records()[0].split('\t').take(2))
    }

    @Test
    fun `EG-02 stroke that succeeds returns the block result without restore`() {
        val result = guard.stroke("tap.output") {
            state.value = 3
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(1, state.snapshots)
        assertEquals(0, state.restores)
        assertEquals(3, state.value)
        assertEquals(0, records().size)
    }

    @Test
    fun `EG-03 run that throws returns null and records`() {
        val result = guard.run<Int>("classifier.update") { throw IllegalStateException("x") }
        assertNull(result)
        assertEquals(1, records().size)
        assertEquals(0, state.snapshots)
        assertEquals(0, state.restores)
        assertEquals(42, guard.run("classifier.update") { 42 })
    }

    @Test
    fun `EG-04 run does not catch Error`() {
        try {
            guard.run<Int>("classifier.update") { throw OutOfMemoryError("oom") }
            fail("Error must propagate")
        } catch (_: OutOfMemoryError) {
        }
        try {
            guard.stroke<Int>("stroke.finish") { throw StackOverflowError() }
            fail("Error must propagate")
        } catch (_: StackOverflowError) {
        }
        assertEquals(0, state.restores)
        assertEquals(0, records().size)
    }
}
