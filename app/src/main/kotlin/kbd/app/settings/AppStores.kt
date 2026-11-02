package kbd.app.settings

import android.content.Context
import java.io.File
import kbd.app.ime.settingsDataStore
import kbd.app.store.ErrorLog
import kbd.app.store.SettingsStore
import kbd.app.store.ThresholdStore

/** The stores the app screens share with `KeyboardService` (same DataStore and files, LLD-07 §4, §6). */
class AppStores(context: Context) {
    private val app = context.applicationContext

    val errorLog = ErrorLog(File(app.filesDir, ERROR_LOG_FILE))
    val settings = SettingsStore(app.settingsDataStore, errorLog)
    val thresholds = ThresholdStore(app.filesDir, errorLog)

    private companion object {
        /** Same file as `KeyboardService` (LLD-07 §6.2). */
        const val ERROR_LOG_FILE = "errors.log"
    }
}
