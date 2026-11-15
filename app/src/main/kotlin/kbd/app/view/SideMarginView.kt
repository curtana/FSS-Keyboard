package kbd.app.view

import android.view.View
import android.view.ViewGroup

/**
 * A view of the keyboard's key area that is told the width of the side margins beside it (SRS FR-CMN-14 ⑧⑪, HLD 5A,
 * LLD-08 §7.2, FSS#84). The margins are the parent's place, outside this view; the view only needs their width to put
 * the window border's left and right sides at −margin and width + margin when it builds its key geometry, so that an
 * end key's outer edge is crossed like any other edge.
 */
interface SideMarginView {
    /** Width of one side margin, px. The parent sets it before it measures the view. */
    var sideMarginPx: Int
}

/** Tells [margin] px to this view and the views in it that are [SideMarginView]s. */
internal fun View.applySideMargin(margin: Int) {
    if (this is SideMarginView) sideMarginPx = margin
    if (this is ViewGroup) for (i in 0 until childCount) getChildAt(i).applySideMargin(margin)
}
