package kbd.stroke.testing

import kbd.stroke.geom.Edge
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * Trajectory test data (LLD-11 §1.1), shared with gesture-engine tests through `testFixtures`.
 * Kept free of gesture-engine types: [kind], [expectOutcome] and [expectValue] stay strings.
 */
data class TrajectoryKey(val kind: String, val label: String?, val rect: RectDp, val outer: Set<Edge>) {
    fun geom(): KeyGeom = KeyGeom(label ?: kind, rect, outer)
}

data class Trajectory(
    val id: String,
    val key: TrajectoryKey,
    val thresholds: Thresholds,
    val points: List<StrokePoint>,
    val expectOutcome: String?,
    val expectValue: String?,
    /** EC tests: `[["RIGHT","OUT"], …]` as (edge, dir) name pairs. */
    val expectCrossings: List<Pair<String, String>>?,
)

/** A movement leg (LLD-11 §1.1 `legs`): `["R"|"L"|"U"|"D", distance]` or `["V", dx, dy]`. */
sealed interface Leg {
    val dx: Double
    val dy: Double

    data class R(val d: Double) : Leg { override val dx get() = d; override val dy get() = 0.0 }
    data class L(val d: Double) : Leg { override val dx get() = -d; override val dy get() = 0.0 }
    data class U(val d: Double) : Leg { override val dx get() = 0.0; override val dy get() = -d }
    data class D(val d: Double) : Leg { override val dx get() = 0.0; override val dy get() = d }
    data class V(override val dx: Double, override val dy: Double) : Leg
}

object Trajectories {
    /** Default test key (LLD-11 §1.1): ㄱ, (0,0)-(51,48), no outer edges, start at its centre. */
    val DEFAULT_RECT = RectDp(0f, 0f, 51f, 48f)
    const val DEFAULT_STEP_DP = 2.0
    const val DEFAULT_MS_PER_DP = 2.0

    private val json = Json { isLenient = false }

    /** Loads `trajectories/<id>.json` from the test classpath. */
    fun load(id: String): Trajectory {
        val path = "trajectories/$id.json"
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(path)
            ?: Trajectories::class.java.classLoader.getResourceAsStream(path)
            ?: error("trajectory resource not found: $path")
        return parse(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
    }

    fun parse(text: String): Trajectory {
        val o = json.parseToJsonElement(text).jsonObject
        require((o["version"]?.jsonPrimitive?.long ?: 1L) == 1L) { "unsupported trajectory version" }
        val id = o["id"]?.jsonPrimitive?.content ?: ""
        val key = parseKey(o["key"]?.jsonObject)
        val thresholds = parseThresholds(o["thresholds"])
        val points = o["points"]?.let { parsePoints(it.jsonArray) } ?: run {
            val start = o["start"]?.jsonArray?.let { it[0].num() to it[1].num() }
                ?: (((key.rect.left + key.rect.right) / 2.0) to ((key.rect.top + key.rect.bottom) / 2.0))
            expand(
                startX = start.first, startY = start.second,
                t0 = o["t0"]?.jsonPrimitive?.long ?: 0L,
                legs = o["legs"]?.jsonArray?.map { parseLeg(it.jsonArray) } ?: emptyList(),
                stepDp = o["stepDp"]?.num() ?: DEFAULT_STEP_DP,
                msPerDp = o["msPerDp"]?.num() ?: DEFAULT_MS_PER_DP,
            )
        }
        val expect = o["expect"]?.jsonObject
        return Trajectory(
            id = id,
            key = key,
            thresholds = thresholds,
            points = points,
            expectOutcome = expect?.get("outcome")?.jsonPrimitive?.content,
            expectValue = expect?.get("value")?.jsonPrimitive?.content,
            expectCrossings = expect?.get("crossings")?.jsonArray?.map {
                val pair = it.jsonArray
                pair[0].jsonPrimitive.content to pair[1].jsonPrimitive.content
            },
        )
    }

    /**
     * Expands legs into samples: the start point at [t0], then points every [stepDp] along each
     * leg (the leg end always included), timed at [msPerDp] per dp of path travelled.
     */
    fun expand(
        startX: Double, startY: Double, t0: Long, legs: List<Leg>,
        stepDp: Double = DEFAULT_STEP_DP, msPerDp: Double = DEFAULT_MS_PER_DP,
    ): List<StrokePoint> {
        require(stepDp > 0) { "stepDp must be positive" }
        val out = mutableListOf(StrokePoint(startX.toFloat(), startY.toFloat(), t0))
        var x0 = startX
        var y0 = startY
        var travelled = 0.0
        for (leg in legs) {
            val len = sqrt(leg.dx * leg.dx + leg.dy * leg.dy)
            if (len == 0.0) continue
            val n = ceil(len / stepDp).toInt()
            for (i in 1..n) {
                val s = minOf(i * stepDp, len)
                val x = if (s == len) x0 + leg.dx else x0 + leg.dx * s / len
                val y = if (s == len) y0 + leg.dy else y0 + leg.dy * s / len
                out += StrokePoint(x.toFloat(), y.toFloat(), t0 + ((travelled + s) * msPerDp).roundToLong())
            }
            x0 += leg.dx
            y0 += leg.dy
            travelled += len
        }
        return out
    }

    private fun parseKey(k: JsonObject?): TrajectoryKey {
        if (k == null) return TrajectoryKey("CONSONANT", "ㄱ", DEFAULT_RECT, emptySet())
        val rect = k["rect"]?.jsonArray?.let { r ->
            RectDp(r[0].num().toFloat(), r[1].num().toFloat(), r[2].num().toFloat(), r[3].num().toFloat())
        } ?: DEFAULT_RECT
        return TrajectoryKey(
            kind = k["kind"]?.jsonPrimitive?.content ?: "CONSONANT",
            label = k["label"]?.jsonPrimitive?.content,
            rect = rect,
            outer = k["outer"]?.jsonArray?.map { Edge.valueOf(it.jsonPrimitive.content) }?.toSet() ?: emptySet(),
        )
    }

    private fun parseThresholds(e: JsonElement?): Thresholds {
        val d = ThresholdDefaults.VALUES
        if (e == null || (e is JsonPrimitive && e.content == "default")) return d
        var t = d
        for ((name, v) in e.jsonObject) {
            val n = v.num()
            t = when (name) {
                "minCrossDp" -> t.copy(minCrossDp = n.toFloat())
                else -> error("unknown threshold field: $name")
            }
        }
        return t
    }

    private fun parseLeg(a: JsonArray): Leg = when (val tag = a[0].jsonPrimitive.content) {
        "R" -> Leg.R(a[1].num())
        "L" -> Leg.L(a[1].num())
        "U" -> Leg.U(a[1].num())
        "D" -> Leg.D(a[1].num())
        "V" -> Leg.V(a[1].num(), a[2].num())
        else -> error("unknown leg: $tag")
    }

    private fun parsePoints(a: JsonArray): List<StrokePoint> = a.map { p ->
        val q = p.jsonArray
        StrokePoint(q[0].num().toFloat(), q[1].num().toFloat(), q[2].jsonPrimitive.long)
    }

    private fun JsonElement.num(): Double = jsonPrimitive.double
}
