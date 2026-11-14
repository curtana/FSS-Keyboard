package kbd.app.view

import kbd.app.store.KeyboardAlign
import kbd.app.store.KeyboardWidth
import kbd.app.store.WidthSetting
import kbd.stroke.cross.EdgeCrossingTracker
import kbd.stroke.geom.Dir
import kbd.stroke.geom.Edge
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.StrokePoint
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.TapGesture
import kbd.stroke.tap.TapGestureClassifier
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The keyboard's side margins and the end keys' outward strokes (LLD-11 §7.7 SM-01~SM-03, SM-05, SRS FR-CMN-14 ⑧⑪,
 * FR-SWP-03·45, LLD-08 §7.2, FSS#84). The key geometry is the tap keyboard view's own ([TapGeometry]) at the
 * width and side margin [KeyboardWidth.place] gives; density 1, so px = dp.
 *
 * An end key's outer edge is judged like an inner key's edge of the same direction: an exit at [MIN] dp past the
 * edge, nothing short of it, with the keyboard filling the width and narrowed alike.
 */
class SideMarginStrokeTest {

    /** (keys' width, side margin) in dp: a 360 dp screen filled, and 40 dp keys on an 800 dp screen. */
    private val boards: List<Pair<Float, Float>> = listOf(
        KeyboardWidth.place(360, 360, 1f, WidthSetting.DEFAULT, WidthSetting.DEFAULT, isLandscape = false),
        KeyboardWidth.place(800, 800, 1f, WidthSetting(40f, KeyboardAlign.CENTER), WidthSetting.DEFAULT, isLandscape = false),
    ).map { it.keysWidth.toFloat() to it.margin.toFloat() }

    private fun cx(g: KeyGeom) = (g.rect.left + g.rect.right) / 2
    private fun cy(g: KeyGeom) = (g.rect.top + g.rect.bottom) / 2

    /** The tap gesture of a stroke from the centre of [g] straight to x = [toX]. */
    private fun tapGesture(g: KeyGeom, toX: Float): TapGesture {
        val c = TapGestureClassifier(g, MIN)
        c.begin(StrokePoint(cx(g), cy(g), 0))
        c.add(StrokePoint((cx(g) + toX) / 2, cy(g), 5))
        return c.finish(StrokePoint(toX, cy(g), 10))
    }


    @Test
    fun `SM-01 the boards - full width 328 + 2 x 16 = 360, narrowed 400 + 2 x 20`() {
        assertEquals(listOf(328f to 16f, 400f to 20f), boards)
    }

    @Test
    fun `SM-02 tap keyboards - no key has a left or right border edge, the bottom row keeps its bottom`() {
        for ((w, side) in boards) for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            val geo = TapGeometry.build(layout, w, ROW, side)
            for (g in geo.geoms) {
                assertTrue("${layout.name} ${g.id} at $w", Edge.LEFT !in g.outerEdges && Edge.RIGHT !in g.outerEdges)
                assertEquals("${layout.name} ${g.id} at $w", g.rect.bottom >= geo.heightDp - 0.5f, Edge.BOTTOM in g.outerEdges)
            }
            // Without the margin the same keys are at the border (the geometry before FSS#84).
            val old = TapGeometry.build(layout, w, ROW)
            assertTrue(old.geoms.any { Edge.LEFT in it.outerEdges } && old.geoms.any { Edge.RIGHT in it.outerEdges })
        }
    }

    @Test
    fun `SM-03 tap keyboards - an end key's outward stroke is an exit 8 dp past its edge, like an inner key's`() {
        for ((w, side) in boards) for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            val geo = TapGeometry.build(layout, w, ROW, side)
            val leftEnds = geo.geoms.filter { it.rect.left <= 0.5f }
            val rightEnds = geo.geoms.filter { it.rect.right >= w - 0.5f }
            // Rows that begin or end with an indent blank have no end key there; the edit, number, top letter and
            // function rows do.
            assertTrue("${layout.name} at $w", leftEnds.size >= 4 && rightEnds.size >= 4)
            // q and p (ㅂ and ㅔ in Dubeolsik: the key ids are the QWERTY letters in both layouts).
            assertTrue("${layout.name} at $w", leftEnds.any { it.id == "q" } && rightEnds.any { it.id == "p" })
            val inner = geo.geoms.filter { it.rect.left > 0.5f && it.rect.right < w - 0.5f }
            for (g in leftEnds + inner) {
                val at = "${layout.name} ${g.id} ← at $w"
                assertEquals(at, TapGesture.Tap, tapGesture(g, g.rect.left))   // on the edge: not out yet
                assertEquals(at, TapGesture.Tap, tapGesture(g, g.rect.left - MIN + 0.1f))
                assertEquals(at, TapGesture.Exit(Dir.LEFT), tapGesture(g, g.rect.left - MIN))
            }
            for (g in rightEnds + inner) {
                val at = "${layout.name} ${g.id} → at $w"
                assertEquals(at, TapGesture.Tap, tapGesture(g, g.rect.right))
                assertEquals(at, TapGesture.Tap, tapGesture(g, g.rect.right + MIN - 0.1f))
                assertEquals(at, TapGesture.Exit(Dir.RIGHT), tapGesture(g, g.rect.right + MIN))
            }
            // The exit point of every end key lies inside the side margin: the finger need not leave the keyboard.
            assertTrue(side >= MIN)
        }
    }

    @Test
    fun `SM-05 the bottom border keeps its rule - the bottom row is out when the stroke reaches it`() {
        val (w, side) = boards[0]
        val geo = TapGeometry.build(TapLayouts.QWERTY, w, ROW, side)
        val g = geo.geoms.first { it.rect.bottom >= geo.heightDp - 0.5f && it.rect.left > 0.5f }
        val t = EdgeCrossingTracker(g, MIN)
        t.begin(StrokePoint(cx(g), g.rect.bottom - 12f, 0))
        t.add(StrokePoint(cx(g), g.rect.bottom, 10))   // pinned to the window's bottom (FR-SWP-45)
        assertEquals(Edge.BOTTOM, t.crossings.single().edge)
    }

    private companion object {
        const val ROW = 48f
        val MIN = ThresholdDefaults.MIN_CROSS_DP
    }
}
