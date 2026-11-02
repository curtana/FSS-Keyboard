package kbd.app.tutor

import kbd.app.store.Settings

/** First-run auto start of the tutor (LLD-07 §2.3, FR-TUT-01). */
object TutorLaunchPolicy {
    fun shouldAutoStart(s: Settings): Boolean = !s.tutorAutoStarted

    /**
     * App open (`SettingsActivity.onCreate`): on the first open, store the flag **before**
     * launching the tutor (D-34), so finishing, skipping or closing midway never starts it again.
     * Later the tutor starts only from settings (FR-TUT-06). No "defaults in use" notice (FR-TUT-05).
     */
    inline fun onAppOpen(s: Settings, markAutoStarted: () -> Unit, launchTutor: () -> Unit) {
        if (!shouldAutoStart(s)) return
        markAutoStarted()
        launchTutor()
    }
}
