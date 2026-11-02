package kbd.gesture.classify

import kbd.gesture.layout.SwipeKeyDef
import kbd.gesture.layout.SwipeLayout
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.testing.Leg
import kbd.stroke.testing.Trajectories
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds

/** Shared helpers for the classifier tests (LLD-11 §1.1, §4.1–4.2). */
internal object ClassifierTestSupport {
    // Real layout geometry for NK/SK (LLD-11 §4.2): 51 × 48 dp cells, rows filling the window bottom.
    const val COL_W = 51f
    const val ROW_H = 48f
    private val WINDOW_W = SwipeLayout.COLS * COL_W
    private val WINDOW_H = SwipeLayout.ROWS * ROW_H

    fun keyDef(id: String): SwipeKeyDef = SwipeLayout.KEYS.first { it.id == id }

    fun layoutGeom(k: SwipeKeyDef): KeyGeom {
        val rect = RectDp(k.col * COL_W, k.row * ROW_H, (k.col + k.colSpan) * COL_W, (k.row + 1) * ROW_H)
        return KeyGeom.build(k.id, rect, WINDOW_W, WINDOW_H)
    }

    /** Runs a stroke through begin / update… / finish and returns the result. */
    fun run(c: StrokeClassifier, points: List<StrokePoint>): StrokeResult {
        c.begin(points.first())
        for (i in 1 until points.size - 1) c.update(points[i])
        return c.finish(points.last())
    }

    /** Stroke from the centre of [geom] along [legs] (2 dp steps, 2 ms per dp). */
    fun stroke(geom: KeyGeom, legs: List<Leg>): List<StrokePoint> {
        val r = geom.rect
        return Trajectories.expand(((r.left + r.right) / 2).toDouble(), ((r.top + r.bottom) / 2).toDouble(), 0L, legs)
    }

    fun outcomeOf(id: String, legs: List<Leg>, th: Thresholds = ThresholdDefaults.VALUES): StrokeOutcome {
        val k = keyDef(id)
        val g = layoutGeom(k)
        return run(StrokeClassifier(k, th, g), stroke(g, legs)).outcome
    }

    fun text(s: String) = StrokeOutcome.Text(s, false)

    // Leg shorthands: exits clear the 14 dp margin from the centre (24 dp vertically, 25.5 dp horizontally).
    val TAP = emptyList<Leg>()
    val U = listOf(Leg.U(42.0))
    val D = listOf(Leg.D(42.0))
    val L = listOf(Leg.L(45.0))
    val R = listOf(Leg.R(45.0))
    val U_BACK = listOf(Leg.U(42.0), Leg.D(42.0))
    val D_BACK = listOf(Leg.D(42.0), Leg.U(42.0))
    val L_BACK = listOf(Leg.L(45.0), Leg.R(45.0))
    val R_BACK = listOf(Leg.R(45.0), Leg.L(45.0))
    val U_RUB = listOf(Leg.U(42.0), Leg.D(42.0), Leg.U(42.0))
    val D_RUB = listOf(Leg.D(42.0), Leg.U(42.0), Leg.D(42.0))
    val L_RUB = listOf(Leg.L(45.0), Leg.R(45.0), Leg.L(45.0))
    val R_RUB = listOf(Leg.R(45.0), Leg.L(45.0), Leg.R(45.0))
}
