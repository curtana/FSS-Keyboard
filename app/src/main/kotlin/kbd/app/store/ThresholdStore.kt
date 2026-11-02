package kbd.app.store

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds

enum class LoadStatus { OK, DEFAULT_MISSING, DEFAULT_CORRUPT }

/** [status] is for tests and error records only; `DEFAULT_*` runs on defaults with no notice (FR-TUT-05, HLD 9). */
data class Loaded(val thresholds: Thresholds, val status: LoadStatus)

sealed class ImportResult {
    object Ok : ImportResult()
    /** Nothing was saved; the settings screen shows its message (FR-TUT-07). */
    object Invalid : ImportResult()
}

/**
 * `filesDir/thresholds.json` (LLD-07 §4, HLD 7.3). A corrupt file is left in place for the next
 * save to overwrite (LLD-09 §3).
 */
class ThresholdStore(private val filesDir: File, private val errorLog: ErrorLog) {

    private val file = File(filesDir, FILE_NAME)

    fun load(): Loaded {
        if (!file.isFile) return Loaded(ThresholdDefaults.VALUES, LoadStatus.DEFAULT_MISSING)
        return try {
            Loaded(ThresholdCodec.decode(file.readText(Charsets.UTF_8)), LoadStatus.OK)
        } catch (e: Exception) {
            errorLog.record("ThresholdStore.load", e)
            Loaded(ThresholdDefaults.VALUES, LoadStatus.DEFAULT_CORRUPT)
        }
    }

    /**
     * Writes a temp file in the same folder and moves it over the old one, so a crash mid-write
     * keeps the previous file (D-21). A failure is recorded and rethrown; the old file stays.
     */
    fun save(t: Thresholds, calibration: Calibration? = null) {
        val tmp = File(filesDir, "$FILE_NAME.tmp")
        try {
            tmp.writeText(ThresholdCodec.encode(t, calibration), Charsets.UTF_8)
            // File.renameTo does not replace an existing file on every JVM; Files.move does.
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: IOException) {
            tmp.delete()
            errorLog.record("ThresholdStore.save", e)
            throw e
        }
    }

    /** Modification time of the file, 0 if there is none; `KeyboardService` re-reads when it changes (D-22). */
    fun lastModified(): Long = file.lastModified()

    /** Writes the current thresholds (defaults if the file is missing or corrupt). */
    fun exportTo(out: OutputStream) {
        out.write(ThresholdCodec.encode(load().thresholds).toByteArray(Charsets.UTF_8))
        out.flush()
    }

    fun importFrom(inp: InputStream): ImportResult {
        val text = inp.readBytes().toString(Charsets.UTF_8)
        val t = try {
            ThresholdCodec.decode(text)
        } catch (_: Exception) {
            return ImportResult.Invalid
        }
        save(t, ThresholdCodec.decodeCalibration(text))   // the tutor's record goes along (FR-TUT-13)
        return ImportResult.Ok
    }

    /** FR-SET-04: saves the defaults so the keyboard picks them up on the next input (FR-TUT-04). */
    fun resetToDefault() = save(ThresholdDefaults.VALUES)

    companion object {
        const val FILE_NAME = "thresholds.json"
    }
}
