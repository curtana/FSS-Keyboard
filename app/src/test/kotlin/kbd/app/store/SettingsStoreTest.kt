package kbd.app.store

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.io.File
import kbd.app.ime.InputMode
import kbd.app.ime.InputMode.GRAFFITI
import kbd.app.ime.ModeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** SS-01..SS-05, SS-07, SS-08, SS-10..SS-15 (LLD-11 §7.5, LLD-07 §6.1, LLD-09 §4). */
class SettingsStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private var scope: CoroutineScope? = null

    private val file: File by lazy { File(tmp.root, "settings.preferences_pb") }

    @After
    fun tearDown() {
        scope?.cancel()
    }

    /**
     * File-backed through `OkioStorage`: the Android artifact's `File` storage replaces the file with
     * `renameTo`, which fails on Windows once the file exists, so a second write fails on a Windows JVM.
     */
    private fun open(): DataStore<Preferences> {
        val s = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope = s
        val storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer, producePath = { file.toOkioPath() })
        return PreferenceDataStoreFactory.create(storage = storage, scope = s)
    }

    /** Only one DataStore per file may be active, so the previous one is closed first. */
    private fun reopen(): DataStore<Preferences> {
        runBlocking { scope?.coroutineContext?.get(Job)?.cancelAndJoin() }
        return open()
    }

    @Test
    fun `SS-01 empty store gives the defaults`() {
        val s = SettingsStore(open()).loadBlocking()
        assertEquals(Settings.DEFAULT, s)
        assertNull(s.lastMode)
        assertTrue(s.haptic)
        assertFalse(s.sound)
        assertEquals(48f, s.rowHeightDp)
        assertFalse(s.practiceListShown)
        assertTrue(s.practiceDone.isEmpty())
        assertEquals(2f / 3f, s.graffitiLetterRatio)
    }

    @Test
    fun `SS-02 lastMode reads back after reopening`() {
        for (m in InputMode.entries) {
            runBlocking { SettingsStore(reopen()).setLastMode(m) }
            assertEquals(m, SettingsStore(reopen()).loadBlocking().lastMode)
        }
    }

    @Test
    fun `SS-03 unknown last_mode name reads as none`() {
        val ds = open()
        runBlocking { ds.edit { it[stringPreferencesKey("last_mode")] = "HANDWRITING" } }
        assertNull(SettingsStore(ds).loadBlocking().lastMode)
    }

    @Test
    fun `SS-04 stored row height is clamped to 40 to 64 on read`() {
        val cases = listOf(30f to 40f, 70f to 64f, 44f to 44f, 40f to 40f, 64f to 64f)
        for ((stored, expected) in cases) {
            runBlocking { reopen().edit { it[SettingsStore.ROW_HEIGHT_DP] = stored } }
            assertEquals("stored $stored", expected, SettingsStore(reopen()).loadBlocking().rowHeightDp)
        }
    }

    @Test
    fun `SS-10 key size defaults, read back with the full mark, null removes the key`() {
        val d = SettingsStore(open()).loadBlocking()
        assertEquals(WidthSetting(null, KeyboardAlign.CENTER), d.portraitWidth)   // FR-CMN-14 ③: fills
        assertEquals(WidthSetting(null, KeyboardAlign.CENTER), d.landscapeWidth)   // the portrait key size
        val p = WidthSetting(40f, KeyboardAlign.LEFT)
        val l = WidthSetting(KeyboardWidth.FULL, KeyboardAlign.RIGHT)
        runBlocking { SettingsStore(reopen()).update { it.copy(portraitWidth = p, landscapeWidth = l) } }
        val s = SettingsStore(reopen())
        assertEquals(p, s.loadBlocking().portraitWidth)
        assertEquals(l, s.loadBlocking().landscapeWidth)
        runBlocking { s.update { it.copy(landscapeWidth = it.landscapeWidth.copy(keyDp = null)) } }
        val r = reopen()
        assertEquals(WidthSetting(null, KeyboardAlign.RIGHT), SettingsStore(r).loadBlocking().landscapeWidth)
        assertNull(runBlocking { r.data.first()[SettingsStore.KEY_LANDSCAPE] })
    }

    @Test
    fun `SS-11 stored key size snaps to whole dp in 32 to 160, unknown alignment reads as centre`() {
        val cases = listOf(10f to 32f, 400f to 160f, 36.4f to 36f, 36.6f to 37f)
        for ((stored, expected) in cases) {
            runBlocking { reopen().edit { it[SettingsStore.KEY_PORTRAIT] = stored } }
            assertEquals("stored $stored", expected, SettingsStore(reopen()).loadBlocking().portraitWidth.keyDp)
        }
        runBlocking { reopen().edit { it[SettingsStore.ALIGN_LANDSCAPE] = "UP" } }
        assertEquals(KeyboardAlign.CENTER, SettingsStore(reopen()).loadBlocking().landscapeWidth.align)
    }

    @Test
    fun `SS-12 floating is off and never put by default, each orientation reads back by itself`() {
        val d = SettingsStore(open()).loadBlocking()
        assertEquals(FloatSetting(false, null, null), d.portraitFloat)
        assertEquals(FloatSetting(false, null, null), d.landscapeFloat)
        runBlocking { SettingsStore(reopen()).update { it.copy(landscapeFloat = FloatSetting(true, 0.25f, 0.75f)) } }
        var s = SettingsStore(reopen()).loadBlocking()
        assertEquals(FloatSetting(true, 0.25f, 0.75f), s.landscapeFloat)
        assertEquals(FloatSetting(false, null, null), s.portraitFloat)   // the other orientation is as it was
        runBlocking { SettingsStore(reopen()).update { it.copy(portraitFloat = FloatSetting(true, null, 0.5f)) } }
        s = SettingsStore(reopen()).loadBlocking()
        assertEquals(FloatSetting(true, null, 0.5f), s.portraitFloat)
        assertEquals(FloatSetting(true, 0.25f, 0.75f), s.landscapeFloat)
        // Switched off: the place stays stored. A null share removes its key.
        runBlocking { SettingsStore(reopen()).update { it.copy(landscapeFloat = it.landscapeFloat.copy(on = false, fx = null)) } }
        val r = reopen()
        assertEquals(FloatSetting(false, null, 0.75f), SettingsStore(r).loadBlocking().landscapeFloat)
        assertNull(runBlocking { r.data.first()[SettingsStore.FLOAT_X_LANDSCAPE] })
    }

    @Test
    fun `SS-13 setFloatPosition writes only the shares given, for the orientation given`() {
        runBlocking { SettingsStore(open()).update { it.copy(portraitFloat = FloatSetting(true, null, null), landscapeFloat = FloatSetting(true, 0.1f, 0.2f)) } }
        runBlocking { SettingsStore(reopen()).setFloatPosition(landscape = false, fx = null, fy = 0.4f) }
        var s = SettingsStore(reopen()).loadBlocking()
        assertEquals(FloatSetting(true, null, 0.4f), s.portraitFloat)   // across: still never put
        assertEquals(FloatSetting(true, 0.1f, 0.2f), s.landscapeFloat)
        runBlocking { SettingsStore(reopen()).setFloatPosition(landscape = true, fx = 0.9f, fy = null) }
        s = SettingsStore(reopen()).loadBlocking()
        assertEquals(FloatSetting(true, 0.9f, 0.2f), s.landscapeFloat)   // down: what was stored stays
        assertEquals(FloatSetting(true, null, 0.4f), s.portraitFloat)
        runBlocking { SettingsStore(reopen()).setFloatPosition(landscape = false, fx = 0.3f, fy = 0.6f) }
        runBlocking { SettingsStore(reopen()).setFloatPosition(landscape = false, fx = null, fy = null) }   // nothing to write
        s = SettingsStore(reopen()).loadBlocking()
        assertEquals(FloatSetting(true, 0.3f, 0.6f), s.portraitFloat)
        // The switch written from the settings screen keeps the place (update writes what it read).
        runBlocking { SettingsStore(reopen()).update { it.copy(portraitFloat = it.portraitFloat.copy(on = false)) } }
        assertEquals(FloatSetting(false, 0.3f, 0.6f), SettingsStore(reopen()).loadBlocking().portraitFloat)
    }

    @Test
    fun `SS-14 a stored share outside 0 to 1 reads as the nearer end, not a number as none`() {
        val cases = listOf(-0.5f to 0f, 1.5f to 1f, 0f to 0f, 1f to 1f, 0.37f to 0.37f, Float.NaN to null)
        for ((stored, expected) in cases) {
            runBlocking {
                reopen().edit {
                    it[SettingsStore.FLOAT_X_PORTRAIT] = stored
                    it[SettingsStore.FLOAT_Y_LANDSCAPE] = stored
                }
            }
            val s = SettingsStore(reopen()).loadBlocking()
            assertEquals("stored $stored", expected, s.portraitFloat.fx)
            assertEquals("stored $stored", expected, s.landscapeFloat.fy)
            assertNull(s.portraitFloat.fy)
            assertFalse(s.portraitFloat.on)
        }
    }

    @Test
    fun `SS-05 setPracticeListShown true reads back`() {
        runBlocking { SettingsStore(open()).setPracticeListShown(true) }
        assertTrue(SettingsStore(reopen()).loadBlocking().practiceListShown)
    }

    @Test
    fun `SS-07 practice topics marked done read back as a set`() {
        runBlocking {
            val st = SettingsStore(open())
            st.markPracticeDone("symbol")
            st.markPracticeDone("upper")
            st.markPracticeDone("symbol")
        }
        assertEquals(setOf("symbol", "upper"), SettingsStore(reopen()).loadBlocking().practiceDone)
    }

    @Test
    fun `SS-08 modes on by default, graffiti turns off and reads back`() {
        val all = setOf(InputMode.DUBEOLSIK, InputMode.QWERTY, GRAFFITI)
        val d = SettingsStore(open()).loadBlocking()
        assertTrue(d.graffitiEnabled)
        assertEquals(all, d.enabledModes)
        runBlocking { SettingsStore(reopen()).update { it.copy(graffitiEnabled = false) } }
        assertEquals(setOf(InputMode.DUBEOLSIK, InputMode.QWERTY), SettingsStore(reopen()).loadBlocking().enabledModes)
    }

    /** What a version that still had the swipe mode may have left in the store (SRS FR-CMN-10a, FSS#95). */
    private fun writeOldVersionValues(lastMode: String) = runBlocking {
        reopen().edit {
            it[stringPreferencesKey("last_mode")] = lastMode
            it[booleanPreferencesKey("swipe_enabled")] = false
            it[booleanPreferencesKey("show_stroke_trail")] = false
            it[booleanPreferencesKey("show_direction_guide")] = false
            it[booleanPreferencesKey("show_next_char")] = false
            it[booleanPreferencesKey("show_current_char")] = false
            it[stringSetPreferencesKey("practice_done")] = setOf("swipe", "upper")
            it[SettingsStore.HAPTIC] = false
            it[SettingsStore.ROW_HEIGHT_DP] = 56f
        }
    }

    @Test
    fun `SS-15 values left by a version with the swipe mode - the last mode reads as none and the keyboard starts in Dubeolsik`() {
        writeOldVersionValues("SWIPE")
        val s = SettingsStore(reopen()).loadBlocking()   // no exception
        assertNull(s.lastMode)                           // a mode that is gone = nothing saved (FR-CMN-10a ①)
        assertEquals(setOf(InputMode.DUBEOLSIK, InputMode.QWERTY, GRAFFITI), s.enabledModes)
        assertFalse(s.haptic)                            // the other values read as they were stored
        assertEquals(56f, s.rowHeightDp)
        assertEquals(setOf("swipe", "upper"), s.practiceDone)   // kept; a topic that is gone matches nothing (PR-36)

        // The service's start (LLD-08 §3): the first mode of the order, nothing to save.
        val mc = ModeController(InputMode.entries.toList(), s.lastMode, s.enabledModes)
        assertEquals(InputMode.DUBEOLSIK, mc.current)
        assertNull(mc.fixedStart)
        mc.enterNumericField()                           // a numeric field: QWERTY, back to Dubeolsik after it
        assertEquals(InputMode.QWERTY, mc.current)
        assertEquals(InputMode.DUBEOLSIK, mc.returnMode)
        // Graffiti off as well (SRS case 4): Dubeolsik and QWERTY only.
        val two = ModeController(InputMode.entries.toList(), s.lastMode, s.copy(graffitiEnabled = false).enabledModes)
        assertEquals(listOf(InputMode.DUBEOLSIK, InputMode.QWERTY, InputMode.DUBEOLSIK), listOf(two.current, two.onModeKey(), two.onModeKey()))
    }

    @Test
    fun `SS-15 values left by a version with the swipe mode - writes go through, the old keys are neither read nor removed`() {
        writeOldVersionValues("SWIPE")
        runBlocking {
            val st = SettingsStore(reopen())
            st.update { it.copy(sound = true) }          // the settings screen
            st.markPracticeDone("symbol")
            st.setLastMode(InputMode.QWERTY)             // the mode key
        }
        val r = reopen()
        val s = SettingsStore(r).loadBlocking()
        assertEquals(InputMode.QWERTY, s.lastMode)
        assertTrue(s.sound)
        assertFalse(s.haptic)
        assertEquals(setOf("swipe", "upper", "symbol"), s.practiceDone)
        val raw = runBlocking { r.data.first() }
        for (name in listOf("swipe_enabled", "show_stroke_trail", "show_direction_guide", "show_next_char", "show_current_char")) {
            assertEquals(name, false, raw[booleanPreferencesKey(name)])   // left as it was (LLD-07 §6.1, D-69)
        }
    }

    @Test
    fun `SS-15 a last mode that is still there reads as it was, whatever else the old version left`() {
        writeOldVersionValues("GRAFFITI")                // SRS case 3
        val s = SettingsStore(reopen()).loadBlocking()
        assertEquals(GRAFFITI, s.lastMode)
        assertTrue(s.graffitiEnabled)
        assertEquals(GRAFFITI, ModeController(InputMode.entries.toList(), s.lastMode, s.enabledModes).current)
    }

    @Test
    fun `update writes every field and null lastMode removes the key`() {
        val changed = Settings(
            lastMode = GRAFFITI,
            haptic = false,
            sound = true,
            rowHeightDp = 56f,
            graffitiLetterRatio = 0.5f,
            practiceListShown = true,
            practiceDone = setOf("graffiti"),
            graffitiEnabled = false,
            portraitWidth = WidthSetting(44f, KeyboardAlign.RIGHT),
            landscapeWidth = WidthSetting(KeyboardWidth.FULL, KeyboardAlign.LEFT),
        )
        runBlocking { SettingsStore(open()).update { changed } }
        val store = SettingsStore(reopen())
        assertEquals(changed, store.loadBlocking())
        runBlocking { store.update { it.copy(lastMode = null) } }
        assertEquals(changed.copy(lastMode = null), SettingsStore(reopen()).loadBlocking())
    }

    @Test
    fun `corrupt file gives the defaults and is recorded`() {
        file.writeBytes(byteArrayOf(0x7f, 0x01, 0x02, 0x03))
        val log = ErrorLog(File(tmp.root, "errors.log"))
        assertEquals(Settings.DEFAULT, SettingsStore(open(), log).loadBlocking())
        assertTrue(log.readAll().startsWith("SettingsStore.read\t"))
    }
}
