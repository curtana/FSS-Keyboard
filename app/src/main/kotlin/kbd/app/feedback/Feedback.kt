package kbd.app.feedback

import android.media.AudioManager
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import kbd.app.store.Settings

/** Key-press vibration and sound (LLD-08 §8). Defaults: vibration on, sound off (DCR-14). */
class Feedback(
    private val view: () -> View?,
    private val audio: AudioManager,
    private val settings: () -> Settings,
) : FeedbackPort {

    override fun key() {
        val s = settings()
        if (s.haptic) view()?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (s.sound) audio.playSoundEffect(AudioManager.FX_KEY_CLICK)
    }

    override fun grab() {
        if (settings().haptic) view()?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun error() {
        if (!settings().haptic) return
        val c = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        view()?.performHapticFeedback(c)
    }
}
