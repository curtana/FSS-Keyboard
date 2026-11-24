package kbd.app.view

import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper

/**
 * Screen reader access to the suggestion bar (SRS FR-SUG-03 ⑤, NFR-07, LLD-12 §9, D-89): one button per cell with text,
 * numbered by the cell, named by [BarCell.spoken]. Empty cells are no node. The bar rebuilds the tree when its cells
 * change; nothing is announced (the user feels along the bar instead of hearing every change).
 */
class SuggestionAccessHelper(private val bar: SuggestionBarView) : ExploreByTouchHelper(bar) {

    private val bounds = Rect()

    override fun getVirtualViewAt(x: Float, y: Float): Int {
        val i = bar.cellAt(x)
        return if (i >= 0 && i < bar.filledCount && y >= 0 && y < bar.height) i else INVALID_ID
    }

    override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
        for (i in 0 until bar.filledCount) virtualViewIds += i
    }

    @Suppress("DEPRECATION")   // ExploreByTouchHelper still needs the bounds in parent
    override fun onPopulateNodeForVirtualView(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
        bar.cellBounds(virtualViewId, bounds)
        node.setBoundsInParent(bounds)
        val cell = bar.cellText(virtualViewId)
        node.contentDescription = cell?.spoken ?: ""   // the helper requires a name
        node.className = BUTTON_CLASS
        if (cell != null) node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
    }

    override fun onPerformActionForVirtualView(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
        if (action != AccessibilityNodeInfoCompat.ACTION_CLICK || bar.cellText(virtualViewId) == null) return false
        bar.click(virtualViewId)
        sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
        return true
    }

    private companion object {
        const val BUTTON_CLASS = "android.widget.Button"
    }
}
