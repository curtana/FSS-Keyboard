package kbd.app.tutor

import kbd.app.store.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** LLD-11 §7.5 TU-10, TU-11 (LLD-07 §2.3 first-run flow). */
class TutorLaunchPolicyTest {

    @Test
    fun `TU-10 auto start only while the flag is not set`() {
        assertTrue(TutorLaunchPolicy.shouldAutoStart(Settings.DEFAULT.copy(tutorAutoStarted = false)))
        assertFalse(TutorLaunchPolicy.shouldAutoStart(Settings.DEFAULT.copy(tutorAutoStarted = true)))
    }

    /** Test-local stand-in for `SettingsStore` + `TutorActivity` launches: records calls in order. */
    private class Recorder {
        var stored = Settings.DEFAULT
        val calls = ArrayList<String>()

        fun openApp() = TutorLaunchPolicy.onAppOpen(
            stored,
            markAutoStarted = { calls += "setTutorAutoStarted(true)"; stored = stored.copy(tutorAutoStarted = true) },
            launchTutor = { calls += "launchTutor" },
        )
    }

    @Test
    fun `TU-11 first open stores the flag before launching, reopen after skip does not launch`() {
        val r = Recorder()

        r.openApp()                 // first open after install
        assertEquals(listOf("setTutorAutoStarted(true)", "launchTutor"), r.calls)

        // The user skips the tutor: nothing else is stored (FR-TUT-05).
        r.openApp()                 // open again
        assertEquals(1, r.calls.count { it == "launchTutor" })
        assertEquals(1, r.calls.count { it == "setTutorAutoStarted(true)" })
    }
}
