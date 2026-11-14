package kbd.graffiti.nn

import java.util.Random
import kbd.graffiti.eval.GraffitiEval
import kbd.graffiti.ink.InkPoint
import kbd.graffiti.ink.InkStroke
import kbd.graffiti.ink.PadArea
import kbd.graffiti.recognize.StrokeRules
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Synthetic scribbles for LLD-11 §6.7 SR-01..SR-03: the eight families the thresholds were looked at with
 * (`tools/graffiti-nn/scribbles.py`), drawn here with a fixed seed so the test needs no stroke files. A scribble is
 * densified, scaled to 40–110 dp, sampled unevenly (1.5–4 dp) with a ±0.5 dp tremor like the synthetic letter
 * strokes; the ones a rule takes (tap, straight line) are left out, as they never reach the letters.
 * No scribble written by a person exists (SRS TBD-34).
 */
internal object ScribbleStrokes {
    const val SEED = 202L
    const val PER_FAMILY = 100

    private val families: List<Pair<String, (Random) -> List<Pair<Double, Double>>>> = listOf(
        "zigzag" to { r ->
            val n = 4 + r.nextInt(6)
            (0..n).map { i -> i.toDouble() / n + u(r, -.02, .02) to (i % 2) * u(r, .5, 1.0) + u(r, -.05, .05) }
        },
        "scratch" to { r ->
            val n = 4 + r.nextInt(5)
            val a = u(r, 0.0, PI)
            (0..n).map { i -> (i % 2) * cos(a) + u(r, -.1, .1) to (i % 2) * sin(a) + u(r, -.1, .1) }
        },
        "spiral" to { r ->
            val turns = u(r, 2.0, 3.5)
            val s = if (r.nextBoolean()) 1 else -1
            val t0 = u(r, 0.0, 6.3)
            (0..80).map { i ->
                val a = t0 + s * turns * 6.283 * i / 80
                0.5 + (0.1 + 0.4 * i / 80) * cos(a) to 0.5 + (0.1 + 0.4 * i / 80) * sin(a)
            }
        },
        "walk" to { r ->
            var x = .5
            var y = .5
            var a = u(r, 0.0, 6.3)
            (0 until 60).map {
                a += r.nextGaussian() * .6
                x += .05 * cos(a)
                y += .05 * sin(a)
                x to y
            }
        },
        "dloop" to { r ->
            val s = if (r.nextBoolean()) 1 else -1
            val n = 3 + r.nextInt(2)
            val t0 = u(r, 0.0, 6.3)
            (0..100).map { i -> i / 100.0 + .15 * cos(t0 + s * n * 6.283 * i / 100) to .3 * sin(t0 + s * n * 6.283 * i / 100) }
        },
        "star" to { r ->
            val s = if (r.nextBoolean()) 1 else -1
            val t0 = u(r, 0.0, 6.3)
            (0..5).map { i -> cos(t0 + s * 2 * 2 * PI * i / 5) to sin(t0 + s * 2 * 2 * PI * i / 5) }
        },
        "wave" to { r ->
            val n = u(r, 3.0, 6.0)
            (0..80).map { i -> i / 80.0 to .3 * sin(n * 6.283 * i / 80 + 1) }
        },
        "poly" to { r -> (0 until 5 + r.nextInt(5)).map { u(r, 0.0, 1.0) to u(r, 0.0, 1.0) } },
    )

    private fun u(r: Random, a: Double, b: Double) = a + (b - a) * r.nextDouble()

    /** Every scribble that is no rule stroke, as "family#n" to a letter-area stroke; the same list on every run. */
    val all: List<Pair<String, InkStroke>> by lazy {
        val r = Random(SEED)
        val out = ArrayList<Pair<String, InkStroke>>()
        for ((name, shape) in families) repeat(PER_FAMILY) { k ->
            val s = InkStroke(toStroke(r, shape(r)), PadArea.LETTER, GraffitiEval.LETTER_RECT)
            if (StrokeRules.tap(s) == null && StrokeRules.line(s) == null) out += "$name#$k" to s
        }
        out
    }

    private fun toStroke(r: Random, shape: List<Pair<Double, Double>>): List<InkPoint> {
        val dense = ArrayList<Pair<Double, Double>>()
        dense += shape[0]
        for (i in 1 until shape.size) {
            val (ax, ay) = shape[i - 1]
            val (bx, by) = shape[i]
            val n = maxOf(1, (hypot(bx - ax, by - ay) / 0.02).toInt())
            for (k in 1..n) dense += ax + (bx - ax) * k / n to ay + (by - ay) * k / n
        }
        val minX = dense.minOf { it.first }
        val minY = dense.minOf { it.second }
        val side = maxOf(dense.maxOf { it.first } - minX, dense.maxOf { it.second } - minY).takeIf { it > 0 } ?: 1.0
        val k = u(r, 40.0, 110.0) / side
        val pts = dense.map { (x, y) -> 50 + (x - minX) * k to 50 + (y - minY) * k }
        val kept = ArrayList<Pair<Double, Double>>()
        kept += pts[0]
        var acc = 0.0
        var step = u(r, 1.5, 4.0)
        for (i in 1 until pts.size) {
            acc += hypot(pts[i].first - pts[i - 1].first, pts[i].second - pts[i - 1].second)
            if (acc >= step) {
                kept += pts[i]
                acc = 0.0
                step = u(r, 1.5, 4.0)
            }
        }
        if (kept.last() != pts.last()) kept += pts.last()
        return kept.mapIndexed { i, (x, y) ->
            InkPoint(((x + r.nextGaussian() * 0.5) * 10).roundToInt() / 10f, ((y + r.nextGaussian() * 0.5) * 10).roundToInt() / 10f, i * 8L)
        }
    }
}
