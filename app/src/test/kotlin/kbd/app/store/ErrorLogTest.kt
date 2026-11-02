package kbd.app.store

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** EL-01..EL-05 (LLD-11 §7.5, LLD-07 §6.2). */
class ErrorLogTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun logFile(): File = File(tmp.root, "errors.log")

    private fun thrown(message: String = "boom"): Exception =
        try {
            throw IllegalStateException(message)
        } catch (e: Exception) {
            e
        }

    @Test
    fun `EL-01 line keeps class name and location but not the message`() {
        val log = ErrorLog(logFile())
        log.record("stroke.finish", thrown("ㄱ 입력"))
        val text = log.readAll()
        assertFalse(text.contains("ㄱ 입력"))
        assertTrue(text.contains("IllegalStateException"))
        assertTrue(text, text.contains("ErrorLogTest.thrown:"))
    }

    @Test
    fun `EL-02 disabled log leaves the file unchanged`() {
        val file = logFile()
        val log = ErrorLog(file)
        log.record("a", thrown())
        val before = file.readBytes()
        log.enabled = false
        log.record("b", thrown())
        assertTrue(before.contentEquals(file.readBytes()))
    }

    @Test
    fun `EL-02 disabled log creates no file`() {
        val file = logFile()
        val log = ErrorLog(file)
        log.enabled = false
        log.record("a", thrown())
        assertFalse(file.exists())
    }

    @Test
    fun `EL-03 exceeding maxBytes drops the front half and stays within the limit`() {
        val file = logFile()
        val log = ErrorLog(file)
        var n = 0
        while (file.length() <= 60 * 1024) log.record("site-${n++}", thrown())
        repeat(200) { log.record("site-${n++}", thrown()) }
        val lines = log.readAll().lines().filter { it.isNotEmpty() }
        assertTrue(file.length() <= 64 * 1024)
        assertFalse(lines.any { it.startsWith("site-0\t") })
        assertTrue(lines.last().startsWith("site-${n - 1}\t"))
        // Cut at a line boundary: every remaining line is whole.
        assertTrue(lines.all { it.split('\t').size == 3 })
        assertTrue(log.readAll().endsWith("\n"))
    }

    @Test
    fun `EL-03 rotation with a small limit keeps the newest lines`() {
        val file = logFile()
        val log = ErrorLog(file, maxBytes = 512)
        repeat(100) { log.record("s$it", thrown()) }
        assertTrue(file.length() <= 512)
        assertTrue(log.readAll().lines().filter { it.isNotEmpty() }.last().startsWith("s99\t"))
    }

    @Test
    fun `EL-04 write failure on a read-only file throws nothing`() {
        val file = logFile()
        file.writeText("old\n")
        assertTrue(file.setReadOnly())
        try {
            // Root ignores the read-only bit (cloud containers run as root): nothing to check there, skip.
            assumeFalse("read-only is not enforced for this user (root)", file.canWrite())
            ErrorLog(file).record("a", thrown())
            assertEquals("old\n", file.readText())
        } finally {
            file.setWritable(true)
        }
    }

    @Test
    fun `EL-04 write failure on a missing directory throws nothing`() {
        val log = ErrorLog(File(tmp.root, "missing/dir/errors.log"))
        log.record("a", thrown())
        assertEquals("", log.readAll())
    }

    @Test
    fun `EL-05 line has exactly three tab-separated fields`() {
        val log = ErrorLog(logFile())
        log.record("tap.output", thrown("secret\twith tab\nand newline"))
        val text = log.readAll()
        assertTrue(text.endsWith("\n"))
        val lines = text.removeSuffix("\n").split("\n")
        assertEquals(1, lines.size)
        val fields = lines[0].split('\t')
        assertEquals(3, fields.size)
        assertEquals("tap.output", fields[0])
        assertEquals("IllegalStateException", fields[1])
        assertTrue(fields[2], Regex("""ErrorLogTest\.thrown:\d+""").matches(fields[2]))
    }

    @Test
    fun `EL-05 exception without a kbd frame still writes three fields`() {
        val log = ErrorLog(logFile())
        val e = RuntimeException("x").apply { stackTrace = arrayOf(StackTraceElement("java.lang.Foo", "bar", "Foo.java", 1)) }
        log.record("s", e)
        assertEquals("s\tRuntimeException\t-\n", log.readAll())
    }
}
