package kbd.app.store

import java.io.File

/**
 * Size-bounded error log (LLD-07 §6.2, NFR-05, D-26).
 *
 * One line per error: `site` TAB exception simple name TAB first `kbd.` stack frame. The exception
 * message, typed text, coordinates and time are never written.
 */
class ErrorLog(private val file: File, private val maxBytes: Int = 64 * 1024) {

    /** False in password and no-learning fields (FR-CMN-08). */
    @Volatile
    var enabled: Boolean = true

    @Synchronized
    fun record(site: String, e: Throwable) {
        if (!enabled) return
        val line = site + "\t" + e.javaClass.simpleName + "\t" + location(e) + "\n"
        // NFR-03: a failed write must never take the input method down.
        try {
            file.appendText(line, Charsets.UTF_8)
            if (file.length() > maxBytes) dropFrontHalf()
        } catch (_: Exception) {
        }
    }

    /** For tests and development only; there is no user screen. */
    @Synchronized
    fun readAll(): String = try {
        if (file.isFile) file.readText(Charsets.UTF_8) else ""
    } catch (_: Exception) {
        ""
    }

    /** HLD 7.3: keep the back half, cut at a line boundary. */
    private fun dropFrontHalf() {
        val bytes = file.readBytes()
        var start = maxOf(1, bytes.size / 2, bytes.size - maxBytes)
        while (start < bytes.size && bytes[start - 1] != '\n'.code.toByte()) start++
        file.writeBytes(bytes.copyOfRange(start, bytes.size))
    }

    /** `Class.method:line` of the first frame in our package, `-` if there is none. */
    private fun location(e: Throwable): String {
        val frame = e.stackTrace.firstOrNull { it.className.startsWith("kbd.") } ?: return "-"
        return frame.className.substringAfterLast('.') + "." + frame.methodName + ":" + frame.lineNumber
    }
}
