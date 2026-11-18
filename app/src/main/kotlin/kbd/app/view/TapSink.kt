package kbd.app.view

import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.tap.ChordModifier
import kbd.stroke.tap.TapGesture
import kbd.stroke.tap.TapOutput

/** What `TapKeyboardView` reports; implemented by `InputDispatcher` (LLD-05A §4, LLD-08 §6). */
interface TapSink {
    /** One resolved letter, digit or symbol key, in press order (`MultiPointerQueue`). */
    fun onTapOutput(key: TapKeyDef, out: TapOutput)

    /**
     * What the view calls for a resolved key: [onTapOutput] with the judged [gesture], which a chord needs to pick
     * its key (FSS#98, LLD-05A §2.4). A sink without chords keeps this default.
     */
    fun onTapResult(key: TapKeyDef, out: TapOutput, gesture: TapGesture) = onTapOutput(key, out)

    /** The modifiers turned on and waiting for a key, Ctrl and Alt (FR-TAP-15); the view only reads it to draw. */
    val chord: Set<ChordModifier> get() = emptySet()

    /** Function key pressed; also called once per repeat (LLD-05A §6). */
    fun onFunctionDown(key: FunctionKey)

    /** Function key released; a notification only, no input (FR-CMN-11a). */
    fun onFunctionUp(key: FunctionKey)

    /**
     * Up stroke on the mode key (issue #205): commit what is composing, then open the keyboard settings.
     * A mode key tap still comes as [onFunctionDown]/[onFunctionUp] of `MODE`, on release.
     */
    fun onSettingsStroke() {}
}
