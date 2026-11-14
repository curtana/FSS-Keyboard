package kbd.app.store

import kbd.app.ime.InputMode

/** User settings persisted by `SettingsStore` (LLD-07 §6.1). [lastMode] null = none saved. */
data class Settings(
    val lastMode: InputMode?,
    val haptic: Boolean,
    val sound: Boolean,
    val rowHeightDp: Float,
    val graffitiLetterRatio: Float,
    /** The topic list of the input practice was shown once on the first app open (FR-TUT-01; stored key `tutor_auto_started`). */
    val practiceListShown: Boolean,
    /** Ids of the practice topics done to the end (the ✓ of the topic list, SRS appendix A.1); never used for judgement. */
    val practiceDone: Set<String> = emptySet(),
    /** Graffiti on in the keyboard (FR-SET-08, FSS#31); Dubeolsik and QWERTY are always on. Default on. */
    val graffitiEnabled: Boolean = true,
    /** Key size and alignment on the portrait screen (FR-CMN-14, FR-SET-09, FSS#25); the default key size fills. */
    val portraitWidth: WidthSetting = WidthSetting.DEFAULT,
    /** The same on the landscape screen; the default key size is the portrait one (FR-CMN-14 ③). */
    val landscapeWidth: WidthSetting = WidthSetting.DEFAULT,

    /** Floating and the place last put, on the portrait screen (FR-CMN-15, FR-SET-09, FSS#86). Default off, never put. */
    val portraitFloat: FloatSetting = FloatSetting.DEFAULT,
    /** The same on the landscape screen; the two are kept apart (FR-CMN-15 ⑤). */
    val landscapeFloat: FloatSetting = FloatSetting.DEFAULT,
) {
    /** The modes on in the keyboard, for `ModeController.applyEnabled` (FR-CMN-03f). */
    val enabledModes: Set<InputMode>
        get() = buildSet {
            add(InputMode.DUBEOLSIK)
            add(InputMode.QWERTY)
            if (graffitiEnabled) add(InputMode.GRAFFITI)
        }

    companion object {
        /** LLD-07 §6.1 defaults: vibration on, sound off (DCR-14), row 48 dp, letters 2 : numbers 1. */
        val DEFAULT = Settings(
            lastMode = null,
            haptic = true,
            sound = false,
            rowHeightDp = RowHeight.DEFAULT_DP,
            graffitiLetterRatio = 2f / 3f,
            practiceListShown = false,
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
