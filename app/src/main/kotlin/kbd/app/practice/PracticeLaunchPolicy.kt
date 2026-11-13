package kbd.app.practice

import kbd.app.store.Settings

/**
 * First app open (FR-TUT-01, LLD-07 §2.5): the practice topic list once, no practice started. Not while graffiti and
 * swipe are both off (FSS#94): the flag stays unset, so the first open after one of them is turned on shows it.
 */
object PracticeLaunchPolicy {
    fun shouldShowList(s: Settings): Boolean = !s.practiceListShown && (s.graffitiEnabled || s.swipeEnabled)

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
