package kbd.app.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
            p[SHOW_STROKE_TRAIL] = s.showStrokeTrail
            p[HAPTIC] = s.haptic
            p[SOUND] = s.sound
            p[ROW_HEIGHT_DP] = s.rowHeightDp
            p[GRAFFITI_LETTER_RATIO] = s.graffitiLetterRatio
            p[TUTOR_AUTO_STARTED] = s.practiceListShown
            p[PRACTICE_DONE] = s.practiceDone
            p[GRAFFITI_ENABLED] = s.graffitiEnabled
            p[SWIPE_ENABLED] = s.swipeEnabled
            writeWidth(p, s.portraitWidth, KEY_PORTRAIT, ALIGN_PORTRAIT)
            writeWidth(p, s.landscapeWidth, KEY_LANDSCAPE, ALIGN_LANDSCAPE)
            writeFloat(p, s.portraitFloat, FLOAT_PORTRAIT, FLOAT_X_PORTRAIT, FLOAT_Y_PORTRAIT)
            writeFloat(p, s.landscapeFloat, FLOAT_LANDSCAPE, FLOAT_X_LANDSCAPE, FLOAT_Y_LANDSCAPE)
        }
    }

    /**
     * The floating keyboard was put somewhere on the landscape ([landscape]) or portrait screen (FR-CMN-15 ⑨): only the
     * shares given are written, a null one leaves what is stored on that axis as it is.
     */
    suspend fun setFloatPosition(landscape: Boolean, fx: Float?, fy: Float?) {
        if (fx == null && fy == null) return
        ds.edit { p ->
            fx?.let { p[if (landscape) FLOAT_X_LANDSCAPE else FLOAT_X_PORTRAIT] = it }
            fy?.let { p[if (landscape) FLOAT_Y_LANDSCAPE else FLOAT_Y_PORTRAIT] = it }
        }
    }

    /** FR-TUT-01: stored before the topic list opens on the first app open (D-34). */
    suspend fun setPracticeListShown(v: Boolean) {
        ds.edit { it[TUTOR_AUTO_STARTED] = v }
    }

    /** The practice topic [id] was done to the end (SWIPE 10.0 ✓). */
    suspend fun markPracticeDone(id: String) {
        ds.edit { it[PRACTICE_DONE] = (it[PRACTICE_DONE] ?: emptySet()) + id }
    }

    private fun decode(p: Preferences): Settings {
        val d = Settings.DEFAULT
        return Settings(
            lastMode = p[LAST_MODE]?.let { name -> InputMode.entries.firstOrNull { it.name == name } },
            showDirectionGuide = p[SHOW_DIRECTION_GUIDE] ?: d.showDirectionGuide,
            showNextChar = p[SHOW_NEXT_CHAR] ?: d.showNextChar,
            showCurrentChar = p[SHOW_CURRENT_CHAR] ?: d.showCurrentChar,
            showStrokeTrail = p[SHOW_STROKE_TRAIL] ?: d.showStrokeTrail,
            haptic = p[HAPTIC] ?: d.haptic,
            sound = p[SOUND] ?: d.sound,
            // D-33: out-of-range stored values snap to the nearest end; 40–47 dp are kept (TBD-16).
            rowHeightDp = RowHeight.clamp(p[ROW_HEIGHT_DP] ?: d.rowHeightDp),
            graffitiLetterRatio = p[GRAFFITI_LETTER_RATIO] ?: d.graffitiLetterRatio,
            practiceListShown = p[TUTOR_AUTO_STARTED] ?: d.practiceListShown,
            practiceDone = p[PRACTICE_DONE] ?: d.practiceDone,
            graffitiEnabled = p[GRAFFITI_ENABLED] ?: d.graffitiEnabled,
            swipeEnabled = p[SWIPE_ENABLED] ?: d.swipeEnabled,
            portraitWidth = readWidth(p, d.portraitWidth, KEY_PORTRAIT, ALIGN_PORTRAIT),
            landscapeWidth = readWidth(p, d.landscapeWidth, KEY_LANDSCAPE, ALIGN_LANDSCAPE),
            portraitFloat = readFloat(p, FLOAT_PORTRAIT, FLOAT_X_PORTRAIT, FLOAT_Y_PORTRAIT),
            landscapeFloat = readFloat(p, FLOAT_LANDSCAPE, FLOAT_X_LANDSCAPE, FLOAT_Y_LANDSCAPE),
        )
    }

    /** A null share removes its key (never put on that axis, FR-CMN-15 ⑥). */
    private fun writeFloat(p: MutablePreferences, s: FloatSetting, on: Preferences.Key<Boolean>, x: Preferences.Key<Float>, y: Preferences.Key<Float>) {
        p[on] = s.on
        if (s.fx == null) p.remove(x) else p[x] = s.fx
        if (s.fy == null) p.remove(y) else p[y] = s.fy
    }

    /** Absent = off, never put. A stored share outside 0–1 reads as the nearer end (FR-CMN-15 ⑤), not a number as none. */
    private fun readFloat(p: Preferences, on: Preferences.Key<Boolean>, x: Preferences.Key<Float>, y: Preferences.Key<Float>) = FloatSetting(
        on = p[on] ?: false,
        fx = FloatingKeyboard.ratio(p[x]),
        fy = FloatingKeyboard.ratio(p[y]),
    )

    /** A null key size removes the key (the default, FR-CMN-14 ③); [KeyboardWidth.FULL] is stored as it is. */
    private fun writeWidth(p: MutablePreferences, s: WidthSetting, keyDp: Preferences.Key<Float>, align: Preferences.Key<String>) {
        val k = s.keyDp
        if (k == null) p.remove(keyDp) else p[keyDp] = k
        p[align] = s.align.name
    }

    /** Stored key sizes snap to whole dp in 32–160 (as D-33); an unknown alignment name reads as the default. */
    private fun readWidth(p: Preferences, d: WidthSetting, keyDp: Preferences.Key<Float>, align: Preferences.Key<String>) = WidthSetting(
        keyDp = p[keyDp]?.let(KeyboardWidth::snap) ?: d.keyDp,
        align = p[align]?.let { name -> KeyboardAlign.entries.firstOrNull { it.name == name } } ?: d.align,
    )

    companion object {
        val LAST_MODE = stringPreferencesKey("last_mode")
        val SHOW_DIRECTION_GUIDE = booleanPreferencesKey("show_direction_guide")
        val SHOW_NEXT_CHAR = booleanPreferencesKey("show_next_char")
        val SHOW_CURRENT_CHAR = booleanPreferencesKey("show_current_char")
        /** FR-DSP-09 (FSS#57): absent = on. */
        val SHOW_STROKE_TRAIL = booleanPreferencesKey("show_stroke_trail")
        val HAPTIC = booleanPreferencesKey("haptic")
        val SOUND = booleanPreferencesKey("sound")
        val ROW_HEIGHT_DP = floatPreferencesKey("row_height_dp")
        val GRAFFITI_LETTER_RATIO = floatPreferencesKey("graffiti_letter_ratio")
        /** Kept under its old name (FSS#17): an install that already ran the old tutor does not open the list again. */
        val TUTOR_AUTO_STARTED = booleanPreferencesKey("tutor_auto_started")
        val PRACTICE_DONE = stringSetPreferencesKey("practice_done")
        /** FR-SET-08 (FSS#31): absent = on. */
        val GRAFFITI_ENABLED = booleanPreferencesKey("graffiti_enabled")
        val SWIPE_ENABLED = booleanPreferencesKey("swipe_enabled")
        val KEY_PORTRAIT = floatPreferencesKey("key_portrait_dp")
        val ALIGN_PORTRAIT = stringPreferencesKey("align_portrait")
        val KEY_LANDSCAPE = floatPreferencesKey("key_landscape_dp")
        val ALIGN_LANDSCAPE = stringPreferencesKey("align_landscape")
        /** FR-CMN-15 (FSS#86): absent = docked; the shares are absent until the keyboard is put on that axis. */
        val FLOAT_PORTRAIT = booleanPreferencesKey("float_portrait")
        val FLOAT_X_PORTRAIT = floatPreferencesKey("float_x_portrait")
        val FLOAT_Y_PORTRAIT = floatPreferencesKey("float_y_portrait")
        val FLOAT_LANDSCAPE = booleanPreferencesKey("float_landscape")
        val FLOAT_X_LANDSCAPE = floatPreferencesKey("float_x_landscape")
        val FLOAT_Y_LANDSCAPE = floatPreferencesKey("float_y_landscape")
    }
}
