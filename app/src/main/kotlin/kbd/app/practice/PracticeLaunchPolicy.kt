package kbd.app.practice

import kbd.app.store.Settings

/** First app open (FR-TUT-01, LLD-07 §2.5): the practice topic list once, no practice started. */
object PracticeLaunchPolicy {
    fun shouldShowList(s: Settings): Boolean = !s.practiceListShown

    /**
     * App open (`SettingsActivity.onCreate`): on the first open, store the flag **before** opening the topic list
     * (D-34), so closing it never opens it again. Later the practice opens only from settings (FR-TUT-06).
     */
    inline fun onAppOpen(s: Settings, markShown: () -> Unit, openList: () -> Unit) {
        if (!shouldShowList(s)) return
        markShown()
        openList()
    }
}
