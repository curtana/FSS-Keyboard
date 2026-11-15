package kbd.app.view

import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper

/**
 * Screen reader access to the tap keyboard keys (LLD-05A §7, NFR-07; QWERTY and Dubeolsik only).
 * One virtual view per key, numbered by its index in the layout, bounds = the key rectangle (↵: one node over both
 * of its rows, FSS#105). Ctrl and Alt are toggles (FSS#98).
 */
class TapKeyAccessHelper(private val view: TapKeyboardView) : ExploreByTouchHelper(view) {

    private val bounds = Rect()

    override fun getVirtualViewAt(x: Float, y: Float): Int {
        val i = view.keyIndexAtPx(x, y)
        return if (i < 0) INVALID_ID else i
    }

    override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
        for (i in 0 until view.keyCount) virtualViewIds += i
    }

    @Suppress("DEPRECATION")   // ExploreByTouchHelper still needs the bounds in parent
    override fun onPopulateNodeForVirtualView(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
        view.keyBoundsPx(virtualViewId, bounds)   // empty when the layout changed under the framework
        node.setBoundsInParent(bounds)
        val key = view.keyAt(virtualViewId)
        node.contentDescription = key?.let(view::describe) ?: ""   // the helper requires one
        if (key != null) node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
        if (key != null && view.modifierOf(key) != null) {   // Ctrl, Alt: a toggle that tells its state (FR-TAP-18)
            node.className = TOGGLE_CLASS
            node.isCheckable = true
            node.isChecked = view.isChordOn(key)
        }
    }

    override fun onPerformActionForVirtualView(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
        if (action != AccessibilityNodeInfoCompat.ACTION_CLICK || view.keyAt(virtualViewId) == null) return false
        view.clickKey(virtualViewId)
        sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
        return true
    }

    private companion object {
        /** The class a screen reader announces as an on/off switch. */
        const val TOGGLE_CLASS = "android.widget.ToggleButton"
    }
}
