package kbd.app.store

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kbd.app.ime.InputMode
import kbd.app.ime.InputMode.GRAFFITI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
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

/** SS-01..SS-05 (LLD-11 §7.5, LLD-07 §6.1, LLD-09 §4). */
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
        assertFalse(s.tutorAutoStarted)
        assertTrue(s.showDirectionGuide)
        assertTrue(s.showNextChar)
        assertTrue(s.showCurrentChar)   // #135: default on
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
    fun `SS-05 setTutorAutoStarted true reads back`() {
        runBlocking { SettingsStore(open()).setTutorAutoStarted(true) }
        assertTrue(SettingsStore(reopen()).loadBlocking().tutorAutoStarted)
    }

    @Test
    fun `update writes every field and null lastMode removes the key`() {
        val changed = Settings(
            lastMode = GRAFFITI,
            showDirectionGuide = false,
            showNextChar = false,
            showCurrentChar = false,
            haptic = false,
            sound = true,
            rowHeightDp = 56f,
            graffitiLetterRatio = 0.5f,
            tutorAutoStarted = true,
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
