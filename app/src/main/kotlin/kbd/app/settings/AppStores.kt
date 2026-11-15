package kbd.app.settings

import android.content.Context
import java.io.File
import kbd.app.ime.settingsDataStore
import kbd.app.store.ErrorLog
import kbd.app.store.PersonalStrokeStore
import kbd.app.store.SettingsStore

/** The stores the app screens share with `KeyboardService` (same DataStore and files, LLD-07 §4, §6). */
class AppStores(context: Context) {
    private val app = context.applicationContext

    val errorLog = ErrorLog(File(app.filesDir, ERROR_LOG_FILE))
    val settings = SettingsStore(app.settingsDataStore, errorLog)

    /** The personal strokes of "my strokes" (LLD-07 §8.2): the one store of the process. */
    val personalStrokes: PersonalStrokeStore = personalStrokes(app, errorLog)

    companion object {
        /** Same file as `KeyboardService` (LLD-07 §6.2). */
        private const val ERROR_LOG_FILE = "errors.log"

        @Volatile
        private var shared: PersonalStrokeStore? = null

        /**
         * The process's personal stroke store, made on first use: the keyboard and the practice screen must share one
         * object so that a stroke stored on the screen is seen by the keyboard's recognizer (LLD-05 §11). The file is in
         * `noBackupFilesDir`, which backup and device transfer leave out (SRS FR-TUT-15 ③).
         */
        fun personalStrokes(context: Context, log: ErrorLog): PersonalStrokeStore = synchronized(this) {
            val file = File(context.applicationContext.noBackupFilesDir, PersonalStrokeStore.FILE_NAME)
            shared?.takeIf { it.file == file } ?: PersonalStrokeStore(file, log).also { shared = it }
        }
    }
}
