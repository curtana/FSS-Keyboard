package kbd.app.store

import kbd.app.ime.InputMode

/** User settings persisted by `SettingsStore` (LLD-07 §6.1). [lastMode] null = none saved. */
data class Settings(
    val lastMode: InputMode?,
    val showDirectionGuide: Boolean,
    val showNextChar: Boolean,
    /** Big current character (FR-DSP-08, issue #135); password fields turn it off anyway (FR-CMN-08 ①; no-learning fields show it). */
    val showCurrentChar: Boolean,
    val haptic: Boolean,
    val sound: Boolean,
    val rowHeightDp: Float,
    val graffitiLetterRatio: Float,
    val tutorAutoStarted: Boolean,
) {
    companion object {
        /** LLD-07 §6.1 defaults: vibration on, sound off (DCR-14), row 48 dp, letters 2 : numbers 1. */
        val DEFAULT = Settings(
            lastMode = null,
            showDirectionGuide = true,
            showNextChar = true,
            showCurrentChar = true,
            haptic = true,
            sound = false,
            rowHeightDp = RowHeight.DEFAULT_DP,
            graffitiLetterRatio = 2f / 3f,
            tutorAutoStarted = false,
        )
    }
}

/** Selectable row height range (FR-SET-06, NFR-06, TBD-16). Out-of-range stored values snap to the nearest end (D-33). */
object RowHeight {
    const val MIN_DP = 40f
    const val MAX_DP = 64f
    const val DEFAULT_DP = 48f

    fun clamp(v: Float): Float = v.coerceIn(MIN_DP, MAX_DP)
}
