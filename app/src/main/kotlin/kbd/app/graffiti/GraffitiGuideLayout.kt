package kbd.app.graffiti

import kbd.graffiti.guide.GuideCell
import kbd.graffiti.guide.GuideGroup
import kbd.graffiti.ink.RectF2

/**
 * Where the stroke guide panel puts its group titles and cells, in content dp (y grows down from the
 * content top, before scrolling), pure (LLD-05 §10.2). Every group starts on a new line under its title;
 * cells fill [columns] equal columns. Text-only cells (no marks) are [TEXT_CELL_DP] high, the others [CELL_DP].
 * The close mark ✕ is a fixed [CLOSE_DP] square at the panel's top-right, outside the content.
 */
class GraffitiGuideLayout(val widthDp: Float, groups: List<GuideGroup>) {

    data class Title(val text: String, val rect: RectF2)
    data class Cell(val cell: GuideCell, val rect: RectF2)

    /** Columns per line: as many [CELL_MIN_DP]-wide cells as fit, at least one. */
    val columns: Int = maxOf(1, ((widthDp - 2 * PAD_DP) / CELL_MIN_DP).toInt())
    val cellWidthDp: Float = (widthDp - 2 * PAD_DP) / columns

    val titles: List<Title>
    val cells: List<Cell>

    /** Content height; the panel scrolls when it is taller than the panel. */
    val contentHeightDp: Float

    init {
        val t = ArrayList<Title>()
        val c = ArrayList<Cell>()
        var y = PAD_DP
        for (g in groups) {
            t += Title(g.title, RectF2(PAD_DP, y, widthDp - PAD_DP - CLOSE_DP, y + TITLE_DP))
            y += TITLE_DP
            val h = if (g.cells.all { it.marks.isEmpty() }) TEXT_CELL_DP else CELL_DP
            g.cells.forEachIndexed { i, cell ->
                val col = i % columns
                if (i > 0 && col == 0) y += h
                val left = PAD_DP + col * cellWidthDp
                c += Cell(cell, RectF2(left, y, left + cellWidthDp, y + h))
            }
            if (g.cells.isNotEmpty()) y += h
            y += GAP_DP
        }
        titles = t
        cells = c
        contentHeightDp = y + PAD_DP
    }

    /** Largest scroll offset for a panel [panelHeightDp] high (0 when everything fits). */
    fun maxScrollDp(panelHeightDp: Float): Float = maxOf(0f, contentHeightDp - panelHeightDp)

    /** The fixed ✕ square of a panel [panelWidthDp] wide, in panel dp (not scrolled). */
    fun closeRect(panelWidthDp: Float = widthDp): RectF2 = RectF2(panelWidthDp - CLOSE_DP, 0f, panelWidthDp, CLOSE_DP)

    companion object {
        const val PAD_DP = 8f
        const val TITLE_DP = 32f   // PAD + TITLE = CLOSE: the first cells start under the ✕
        const val CELL_MIN_DP = 64f
        const val CELL_DP = 84f
        const val TEXT_CELL_DP = 44f
        const val GAP_DP = 8f
        const val CLOSE_DP = 40f
    }
}
