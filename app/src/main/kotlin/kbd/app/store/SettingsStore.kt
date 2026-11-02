package kbd.app.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kbd.app.ime.InputMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * [Settings] over Jetpack DataStore Preferences (LLD-07 §6.1). A failed read gives the defaults and
 * is recorded (LLD-09 §4).
 *
 * `last_mode` is stored as the [InputMode] name; a name this build does not know reads as none and
 * `ModeController` starts in the first mode (FR-CMN-10). Every build has all four modes (issue #251).
 */
class SettingsStore(private val ds: DataStore<Preferences>, private val errorLog: ErrorLog? = null) {

    val flow: Flow<Settings> = ds.data
        .catch { e ->
            if (e !is IOException) throw e
            errorLog?.record("SettingsStore.read", e)
            emit(emptyPreferences())
        }
        .map(::decode)

    /** `runBlocking { flow.first() }`, once when the service starts (D-24). */
    fun loadBlocking(): Settings = runBlocking { flow.first() }

    suspend fun setLastMode(m: InputMode) {
        ds.edit { it[LAST_MODE] = m.name }
    }

    /** Writes every field of `f(current)`; `lastMode = null` removes the key. */
    suspend fun update(f: (Settings) -> Settings) {
        ds.edit { p ->
            val s = f(decode(p))
            val mode = s.lastMode
            if (mode == null) p.remove(LAST_MODE) else p[LAST_MODE] = mode.name
            p[SHOW_DIRECTION_GUIDE] = s.showDirectionGuide
            p[SHOW_NEXT_CHAR] = s.showNextChar
            p[SHOW_CURRENT_CHAR] = s.showCurrentChar
            p[HAPTIC] = s.haptic
            p[SOUND] = s.sound
            p[ROW_HEIGHT_DP] = s.rowHeightDp
            p[GRAFFITI_LETTER_RATIO] = s.graffitiLetterRatio
            p[TUTOR_AUTO_STARTED] = s.tutorAutoStarted
        }
    }

    suspend fun setTutorAutoStarted(v: Boolean) {
        ds.edit { it[TUTOR_AUTO_STARTED] = v }
    }

    private fun decode(p: Preferences): Settings {
        val d = Settings.DEFAULT
        return Settings(
            lastMode = p[LAST_MODE]?.let { name -> InputMode.entries.firstOrNull { it.name == name } },
            showDirectionGuide = p[SHOW_DIRECTION_GUIDE] ?: d.showDirectionGuide,
            showNextChar = p[SHOW_NEXT_CHAR] ?: d.showNextChar,
            showCurrentChar = p[SHOW_CURRENT_CHAR] ?: d.showCurrentChar,
            haptic = p[HAPTIC] ?: d.haptic,
            sound = p[SOUND] ?: d.sound,
            // D-33: out-of-range stored values snap to the nearest end; 40–47 dp are kept (TBD-16).
            rowHeightDp = RowHeight.clamp(p[ROW_HEIGHT_DP] ?: d.rowHeightDp),
            graffitiLetterRatio = p[GRAFFITI_LETTER_RATIO] ?: d.graffitiLetterRatio,
            tutorAutoStarted = p[TUTOR_AUTO_STARTED] ?: d.tutorAutoStarted,
        )
    }

    companion object {
        val LAST_MODE = stringPreferencesKey("last_mode")
        val SHOW_DIRECTION_GUIDE = booleanPreferencesKey("show_direction_guide")
        val SHOW_NEXT_CHAR = booleanPreferencesKey("show_next_char")
        val SHOW_CURRENT_CHAR = booleanPreferencesKey("show_current_char")
        val HAPTIC = booleanPreferencesKey("haptic")
        val SOUND = booleanPreferencesKey("sound")
        val ROW_HEIGHT_DP = floatPreferencesKey("row_height_dp")
        val GRAFFITI_LETTER_RATIO = floatPreferencesKey("graffiti_letter_ratio")
        val TUTOR_AUTO_STARTED = booleanPreferencesKey("tutor_auto_started")
    }
}
