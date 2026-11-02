package kbd.app.feedback

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import java.io.File
import kbd.app.store.Settings
import kbd.app.store.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowAudioManager

/**
 * [Feedback] with the [SettingsStore] defaults (LLD-11 §5.4 FB-04, LLD-08 §8, FR-SET-03, DCR-14; gate G1).
 * Vibration is counted on the view, the key click on the `AudioManager` (a Robolectric shadow that
 * records `playSoundEffect`, which the stock shadow does not).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [kbd.app.view.ViewTestSdk.SDK], shadows = [FeedbackTest.CountingAudioManager::class])
class FeedbackTest {

    @Implements(AudioManager::class)
    class CountingAudioManager : ShadowAudioManager() {
        val effects = ArrayList<Int>()

        @Implementation
        protected fun playSoundEffect(effectType: Int) {
            effects += effectType
        }
    }

    /** A view that counts haptic calls and still lets the platform (shadow) record them. */
    private class HapticView(context: Context) : View(context) {
        val haptics = ArrayList<Int>()
        override fun performHapticFeedback(feedbackConstant: Int): Boolean {
            haptics += feedbackConstant
            return super.performHapticFeedback(feedbackConstant)
        }
    }

    @get:Rule
    val tmp = TemporaryFolder()

    private var scope: CoroutineScope? = null
    private lateinit var view: HapticView
    private lateinit var audio: AudioManager

    @Before
    fun setUp() {
        val ctx = RuntimeEnvironment.getApplication()
        view = HapticView(ctx)
        audio = ctx.getSystemService(AudioManager::class.java)
    }

    @After
    fun tearDown() {
        scope?.cancel()
    }

    /** An empty file-backed store, as `SettingsStoreTest` opens it (`OkioStorage`, see there). */
    private fun emptyStore(): SettingsStore {
        val s = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope = s
        val file = File(tmp.root, "settings.preferences_pb")
        val storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer, producePath = { file.toOkioPath() })
        return SettingsStore(PreferenceDataStoreFactory.create(storage = storage, scope = s))
    }

    private val sounds: List<Int> get() = (shadowOf(audio) as CountingAudioManager).effects

    @Test
    fun `FB-04 the empty store's defaults give one vibration and no sound per key`() {
        val settings = emptyStore().loadBlocking()
        val fb = Feedback({ view }, audio, { settings })
        fb.key()
        assertEquals(listOf(HapticFeedbackConstants.KEYBOARD_TAP), view.haptics)
        assertEquals(HapticFeedbackConstants.KEYBOARD_TAP, shadowOf(view).lastHapticFeedbackPerformed())
        assertEquals(emptyList<Int>(), sounds)
    }

    @Test
    fun `sound on gives the key click, vibration off gives no vibration`() {
        val s = Settings.DEFAULT.copy(haptic = false, sound = true)
        Feedback({ view }, audio, { s }).key()
        assertEquals(emptyList<Int>(), view.haptics)
        assertEquals(listOf(AudioManager.FX_KEY_CLICK), sounds)
    }

    @Test
    fun `no view yet gives no vibration and no crash`() {
        Feedback({ null }, audio, { Settings.DEFAULT }).key()
        assertEquals(emptyList<Int>(), sounds)
    }

    @Test
    fun `error vibrates with the reject pattern only when vibration is on`() {
        Feedback({ view }, audio, { Settings.DEFAULT }).error()
        Feedback({ view }, audio, { Settings.DEFAULT.copy(haptic = false) }).error()
        assertEquals(listOf(HapticFeedbackConstants.REJECT), view.haptics)
        assertEquals(emptyList<Int>(), sounds)
    }
}
