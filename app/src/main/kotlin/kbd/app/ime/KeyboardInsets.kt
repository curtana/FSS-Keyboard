package kbd.app.ime

import android.inputmethodservice.InputMethodService
import kbd.app.view.KeyboardRootView

/**
 * What the input method tells the platform about its window (SRS 3.3, FR-CMN-14 ⑫, HLD 5A·8장, LLD-08 §7.3, FSS#85).
 *
 * Content and visible insets start at the top of the input view, as before: the app is resized or panned by the
 * input view's height whatever the keyboard's width. The touchable place is the keyboard plate
 * ([KeyboardRootView.plate]), the same rectangle the root paints, in window coordinates; a touch that begins outside
 * it should go to the window below (the platform's part, not confirmed on a device: LLD-08 §7.3). While the plate
 * is the whole input view (a full-width keyboard) or the view is not laid out yet, the platform's
 * `TOUCHABLE_INSETS_VISIBLE` is kept, which is what the keyboard reported before there was a plate.
 *
 * A floating keyboard ([KeyboardRootView.floated], SRS FR-CMN-15 ③⑫, LLD-08 §7.4, FSS#86) is in an input view as high
 * as the window may be, and tells the app it takes no place: content and visible insets start at the input view's
 * bottom, and the touchable place is always the plate as a region (the visible insets would then leave nothing to
 * touch). Whether an app is in fact left where it is, is the platform's part, not confirmed on a device.
 */
internal object KeyboardInsets {
    fun compute(root: KeyboardRootView, out: InputMethodService.Insets) {
        val loc = IntArray(2)
        root.getLocationInWindow(loc)
        // A floating keyboard takes no place from the app: the insets start at the input view's bottom (FR-CMN-15 ⑫).
        val top = if (root.floated) loc[1] + root.height else loc[1]
        out.contentTopInsets = top
        out.visibleTopInsets = top
        val plate = root.plate
        if (!root.floated && (plate.isEmpty || root.plateFillsView)) {
            out.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_VISIBLE
            out.touchableRegion.setEmpty()
            return
        }
        out.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION
        out.touchableRegion.set(loc[0] + plate.left, loc[1] + plate.top, loc[0] + plate.right, loc[1] + plate.bottom)
    }
}
