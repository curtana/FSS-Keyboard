package kbd.app.ime

import android.inputmethodservice.InputMethodService
import kbd.app.view.KeyboardRootView

/**
 * What the input method tells the platform about its window (SRS 3.3, FR-CMN-14 ⑫, HLD 5A·8장, LLD-08 §7.3, FSS#85).
 *
 * Content and visible insets start at the top of the input view, as before: the app is resized or panned by the
 * input view's height whatever the keyboard's width. The touchable place is the keyboard plate
 * ([KeyboardRootView.plate]), the same rectangle the root paints, in window coordinates; a touch that begins outside
 * it should go to the window below (the platform's part, not confirmed on a device: LLD-08 §7.3). While the plate is the whole input view (a full-width keyboard) or the view is not laid
 * out yet, the platform's `TOUCHABLE_INSETS_VISIBLE` is kept, which is what the keyboard reported before there was a
 * plate.
 */
internal object KeyboardInsets {
    fun compute(root: KeyboardRootView, out: InputMethodService.Insets) {
        val loc = IntArray(2)
        root.getLocationInWindow(loc)
        out.contentTopInsets = loc[1]
        out.visibleTopInsets = loc[1]
        val plate = root.plate
        if (plate.isEmpty || root.plateFillsView) {
            out.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_VISIBLE
            out.touchableRegion.setEmpty()
            return
        }
        out.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION
        out.touchableRegion.set(loc[0] + plate.left, loc[1] + plate.top, loc[0] + plate.right, loc[1] + plate.bottom)
    }
}
