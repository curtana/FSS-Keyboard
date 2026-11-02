package kbd.gesture.progress

import kbd.gesture.classify.StrokeClassifier
import kbd.gesture.classify.StrokeState
import kbd.gesture.classify.SwipeKeyKind
import kbd.stroke.geom.Dir
import kbd.stroke.geom.Edge
import kbd.stroke.geom.StrokePoint
import kbd.stroke.geom.opposite
import kbd.stroke.geom.unit
import kotlin.math.abs

/*
 * Progress probes (LLD-04 §5.2). Each probe feeds virtual legs into a copy of the stroke state
 * and judges the copy with the same `classify*` code as the release. A result that differs from
 * `current` becomes a candidate; an unchanged result (an impossible combination, an unassigned direction such as
 * a border-side one, a stage that equals the exit) produces nothing (FR-DSP-05).
 */

/** Margin past each threshold so a probe surely meets it (LLD-04 §5.2 ε). */
private const val EPS_DP = 1f

/** Spacing of virtual points (LLD-04 §5.2). */
private const val STEP_DP = 2f

/** One virtual movement: [dist] dp towards [dir]. */
private class Leg(val dir: Dir, val dist: Float)

/** A probe to try: its display direction and distance, kind and legs. */
private class Probe(val dir: Dir, val offsetDp: Float, val kind: ProbeKind, val legs: List<Leg>)

private val EDGE_OF = Edge.entries.associateBy { it.dir }

/** Candidates of the probe table (LLD-04 §5.2) that change this stroke's result from [current]. */
internal fun probeCandidates(c: StrokeClassifier, current: String): List<Candidate> {
    val out = ArrayList<Candidate>()
    for (p in probesOf(c)) {
        // A failing probe is dropped alone; the guide never takes part in the judgement (HLD 1).
        val label = try {
            judge(c, p.legs)
        } catch (e: Exception) {
            null
        }
        if (label.isNullOrEmpty() || label == current) continue   // "" = an ignored stroke
        // Vowel strokes are not told apart by dp distance (SWIPE 9.1, issue #179): a dot and a long probe that
        // give the same character in the same direction are one hint, the nearer one (review PR #183 round 2).
        if ((p.kind == ProbeKind.DOT || p.kind == ProbeKind.LONG) && out.any { it.dir == p.dir && it.label == label }) continue
        out += Candidate(p.dir, p.offsetDp, label, p.kind)
    }
    return out
}

/** Result text of the current state plus [legs], judged on a copy. */
private fun judge(c: StrokeClassifier, legs: List<Leg>): String {
    val s = c.state().copy()
    for (leg in legs) walk(s, leg)
    return c.textOf(c.outcomeOf(s))
}

/** Feeds points every [STEP_DP] along [leg], ending exactly at its length; time stays `last.t`. */
private fun walk(s: StrokeState, leg: Leg) {
    if (leg.dist <= 0f) return
    val from = s.last
    val (ux, uy) = leg.dir.unit
    var d = 0f
    while (d < leg.dist) {
        d = minOf(d + STEP_DP, leg.dist)
        s.add(StrokePoint(from.x + ux * d, from.y + uy * d, from.t))
    }
}

/** The probes switched on by the current state (LLD-04 §5.2 table). */
private fun probesOf(c: StrokeClassifier): List<Probe> {
    val s = c.state()
    val th = c.th
    val m = th.minCrossDp
    val kind = c.key.kind
    val consonant = kind == SwipeKeyKind.CONSONANT
    val e1 = s.e1
    val probes = ArrayList<Probe>()

    // Consonant keys (issue #173, SWIPE 9.1–9.2): the vowel strokes that can follow, ↓ and →, a dot and a
    // long stroke, measured from the press point; their labels come from the assembler's preview.
    if (consonant) {
        for (d in listOf(Dir.DOWN, Dir.RIGHT)) {
            val done = if (d == Dir.DOWN) s.last.y - s.first.y else s.last.x - s.first.x
            val dot = (m + th.longDp) / 2
            val long = th.longDp + EPS_DP
            if (dot > done) probes += Probe(d, dot - done, ProbeKind.DOT, listOf(Leg(d, dot - done)))
            if (long > done) probes += Probe(d, long - done, ProbeKind.LONG, listOf(Leg(d, long - done)))
        }
        return probes
    }

    if (e1 == null) {
        // RING: just past each edge
        for (d in Dir.entries) {
            val toEdge = toEdge(s, d)
            probes += Probe(d, toEdge + m, ProbeKind.RING, listOf(Leg(d, toEdge + m + EPS_DP)))
        }
        // Symbol keys have no stages since issue #208: the ring is all (SWIPE 9.2).
        // Number key ↑/↓ rubs: superscript and subscript (FR-SWP-42)
        if (kind == SwipeKeyKind.NUMBER) {
            for (d in listOf(Dir.UP, Dir.DOWN)) {
                probes += Probe(d, toEdge(s, d) + th.longDp, ProbeKind.RUB, stageLegs(s, d, m, 3))
            }
        }
        return probes
    }

    // RUB: (back inside past m) then out past m again. Shown towards the first move:
    // back (u.opposite) while outside, out again (u) once the finger is inside (LLD-04 §5.2).
    run {
        val u = e1.dir
        val beyond = beyond(s, e1)
        val inside = s.tracker.zone.inside
        val inward = if (inside) 0f else maxOf(0f, beyond + m + EPS_DP)
        val outward = m + EPS_DP - (beyond - inward)
        val rubDir = if (inside) u else u.opposite
        probes += Probe(rubDir, abs(beyond), ProbeKind.RUB, listOf(Leg(u.opposite, inward), Leg(u, outward)))
    }
    return probes
}

/** Legs that leave through the [d] edge and cross it [n] times in all (out, in, out…). */
private fun stageLegs(s: StrokeState, d: Dir, m: Float, n: Int): List<Leg> {
    val legs = ArrayList<Leg>(n)
    legs += Leg(d, toEdge(s, d) + m + EPS_DP)
    val back = 2 * (m + EPS_DP)
    for (i in 2..n) legs += Leg(if (i % 2 == 0) d.opposite else d, back)
    return legs
}

/** Distance from the last point to the edge in direction [d]; negative past it. */
private fun toEdge(s: StrokeState, d: Dir): Float = -beyond(s, EDGE_OF.getValue(d))

/** Distance of the last point beyond edge [e] along its axis; negative inside. */
private fun beyond(s: StrokeState, e: Edge): Float {
    val r = s.key.rect
    val p = s.last
    return when (e) {
        Edge.RIGHT -> p.x - r.right
        Edge.LEFT -> r.left - p.x
        Edge.TOP -> r.top - p.y
        Edge.BOTTOM -> p.y - r.bottom
    }
}
