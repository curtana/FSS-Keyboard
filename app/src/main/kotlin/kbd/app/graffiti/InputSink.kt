package kbd.app.graffiti

import kbd.app.ime.FieldInfo

/** What `GraffitiInputHandler` reports back; implemented by `InputDispatcher` (LLD-05 §6, LLD-08 §6). */
interface InputSink {
    /** Called after every "typed" action (FR-CMN-03a). */
    fun markInput()

    fun error()

    /** `FieldInfo.hidesDisplay`: a password field, where the glyph flash is off (FR-CMN-08 ①, FR-GRF-07); a no-learning field flashes. */
    val hidesDisplay: Boolean

    /** Interim (gap G2): field for `ComposingController.enter(f)` on the graffiti Enter action (LLD-08 §4). */
    val field: FieldInfo
}
