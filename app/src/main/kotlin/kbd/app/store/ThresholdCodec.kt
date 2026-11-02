package kbd.app.store

import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The threshold file is unreadable, of an unknown version, or has a value out of range (LLD-07 §4, D-23). */
class ThresholdFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * One tutor boundary as recorded, never judged with (FR-TUT-13, HLD 7.2 `calibration`, issue #173): the
 * short side's 90th percentile, the long side's 10th, their gap (≤ 0 = overlap), both sample counts and
 * the extra rounds the tutor asked for.
 */
data class BoundaryRecord(val shortP90: Float, val longP10: Float, val gap: Float,
                          val nShort: Int, val nLong: Int, val extraRounds: Int)

/** How far 1-2's second strokes started from their reference place (FR-TUT-13, SWIPE 10): P90, P95, count. */
data class PositionRecord(val p90: Float, val p95: Float, val n: Int)

/** The tutor's record kept with the thresholds (FR-TUT-13): tap ↔ dot, dot ↔ long stroke, the position errors, and when. */
data class Calibration(val tapDot: BoundaryRecord?, val dotLong: BoundaryRecord?, val position: PositionRecord?, val at: String)

/**
 * Threshold JSON (LLD-07 §5, HLD 7.2). Every field of format 5 except `calibration` is required,
 * unknown fields are ignored, numbers are JSON numbers. [decode] also checks the value ranges of
 * LLD-07 §4 (D-23). [encode] writes format 5. [decode] also reads formats 1–4 (HLD 7.2, issues #150,
 * #168, #173): `minCrossDp`, `keySizeDp` and `guideDelayMs` are kept; `longDp` meant something else
 * then (the first exit's length past the edge), so the default is used; the other fields
 * (`dwellMs`, the break-gap fields, `doubleTapWindowMs`, `addIDp`, `returnLongDp`, `turnAngleDeg`) are
 * not read. `calibration` is a record only: [decodeCalibration] reads it leniently, [decode] never.
 */
object ThresholdCodec {

    const val VERSION = 5

    /** Older formats, read only (1: `dwellMs`, 2: break-gap fields, 3: `doubleTapWindowMs`, 4: one-stroke vowel fields). */
    val OLD_VERSIONS = setOf(1L, 2L, 3L, 4L)

    private val json = Json { prettyPrint = true }

    fun encode(t: Thresholds, calibration: Calibration? = null): String = json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("version", VERSION)
            put("keySizeDp", buildJsonObject {
                put("w", number(t.keySizeWDp))
                put("h", number(t.keySizeHDp))
            })
            put("minCrossDp", number(t.minCrossDp))
            put("longDp", number(t.longDp))
            put("guideDelayMs", t.guideDelayMs)
            put("posTolDp", number(t.posTolDp))
            calibration?.let { c ->
                put("calibration", buildJsonObject {
                    c.tapDot?.let { put("tapDot", record(it)) }
                    c.dotLong?.let { put("dotLong", record(it)) }
                    c.position?.let { pos -> put("position", buildJsonObject {
                        put("p90", number(pos.p90)); put("p95", number(pos.p95)); put("n", pos.n)
                    }) }
                    put("at", c.at)
                })
            }
        },
    )

    private fun record(r: BoundaryRecord): JsonObject = buildJsonObject {
        put("shortP90", number(r.shortP90))
        put("longP10", number(r.longP10))
        put("gap", number(r.gap))
        put("n", JsonArray(listOf(JsonPrimitive(r.nShort), JsonPrimitive(r.nLong))))
        put("extraRounds", r.extraRounds)
    }

    /** The `calibration` record of [json], or null when there is none or it cannot be read (it is never judged with). */
    fun decodeCalibration(json: String): Calibration? = try {
        val c = Json.parseToJsonElement(json).asObject("root")["calibration"] as? JsonObject
        c?.let {
            Calibration(
                tapDot = (it["tapDot"] as? JsonObject)?.let(::readRecord),
                dotLong = (it["dotLong"] as? JsonObject)?.let(::readRecord),
                position = (it["position"] as? JsonObject)?.let { o ->
                    PositionRecord(o.number("p90").toFloat(), o.number("p95").toFloat(), o.long("n").toInt())
                },
                at = (it["at"] as? JsonPrimitive)?.content ?: "",
            )
        }
    } catch (e: Exception) {
        null
    }

    private fun readRecord(o: JsonObject): BoundaryRecord {
        val n = o.field("n") as? JsonArray ?: throw ThresholdFormatException("n is not an array")
        return BoundaryRecord(
            shortP90 = o.number("shortP90").toFloat(), longP10 = o.number("longP10").toFloat(),
            gap = o.number("gap").toFloat(),
            nShort = (n[0] as JsonPrimitive).content.toInt(), nLong = (n[1] as JsonPrimitive).content.toInt(),
            extraRounds = o.long("extraRounds").toInt(),
        )
    }

    /** @throws ThresholdFormatException on bad JSON, a missing field, an unknown version or a value out of range. */
    fun decode(json: String): Thresholds {
        val root = try {
            Json.parseToJsonElement(json)
        } catch (e: Exception) {
            throw ThresholdFormatException("not JSON", e)
        }
        val obj = root.asObject("root")
        val version = obj.long("version")
        val old = version in OLD_VERSIONS
        if (version != VERSION.toLong() && !old) throw ThresholdFormatException("unknown version $version")
        val keySize = obj.field("keySizeDp").asObject("keySizeDp")
        return Thresholds(
            // Fixed since issue #217: a stored (tutor-measured) value is ignored, so it need not be there or valid.
            minCrossDp = ThresholdDefaults.MIN_CROSS_DP,
            // Formats 1–4: `longDp` measured the first exit past the key's edge; the default starts over (HLD 7.2).
            longDp = if (old) ThresholdDefaults.VALUES.longDp else obj.float("longDp", 10f, 300f),
            guideDelayMs = obj.long("guideDelayMs", 0L, 2000L),
            posTolDp = if (old) ThresholdDefaults.VALUES.posTolDp else obj.float("posTolDp", 1f, 100f),
            keySizeWDp = keySize.float("w", 10f, 300f),
            keySizeHDp = keySize.float("h", 10f, 300f),
        )
    }

    /** Whole values are written without a fraction, as in the HLD 7.2 example. */
    private fun number(v: Float): JsonPrimitive {
        val l = v.toLong()
        return if (l.toFloat() == v) JsonPrimitive(l) else JsonPrimitive(v)
    }

    private fun JsonElement.asObject(name: String): JsonObject =
        this as? JsonObject ?: throw ThresholdFormatException("$name is not an object")

    private fun JsonObject.field(name: String): JsonElement =
        this[name] ?: throw ThresholdFormatException("missing $name")

    private fun JsonObject.number(name: String): Double {
        val p = field(name) as? JsonPrimitive
        val v = if (p == null || p.isString) null else p.content.toDoubleOrNull()
        if (v == null || !v.isFinite()) throw ThresholdFormatException("$name is not a number")
        return v
    }

    private fun JsonObject.float(name: String, min: Float, max: Float): Float {
        val v = number(name)
        if (v < min || v > max) throw ThresholdFormatException("$name out of range")
        return v.toFloat()
    }

    private fun JsonObject.long(name: String, min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): Long {
        val v = number(name)
        if (v != Math.floor(v)) throw ThresholdFormatException("$name is not a whole number")
        if (v < min || v > max) throw ThresholdFormatException("$name out of range")
        return v.toLong()
    }
}
