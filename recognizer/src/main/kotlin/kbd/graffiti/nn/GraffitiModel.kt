package kbd.graffiti.nn

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kbd.graffiti.recognize.SetName
import kotlin.math.sqrt

/**
 * One network body (LLD-05 §3.5): conv 9 → 16 (3 × 3, padding 1), ReLU, conv 16 → 32 (3 × 3, padding 1), ReLU,
 * 2 × 2 max pooling, a dense layer 512 → [EMBEDDING], ReLU. Weights are 32-bit floats in the training framework's
 * order (`[out][in][row][column]`, dense `[out][in]`) and never change on the device (SRS FR-GRF-11 ③).
 */
class NnBody internal constructor(
    private val conv1W: FloatArray,
    private val conv1B: FloatArray,
    private val conv2W: FloatArray,
    private val conv2B: FloatArray,
    private val denseW: FloatArray,
    private val denseB: FloatArray,
) {
    /** The unit-length embedding of [features] ([StrokeFeatures.SIZE] values); all zeros when every output is zero. */
    fun embed(features: FloatArray): FloatArray {
        require(features.size == StrokeFeatures.SIZE) { "feature size ${features.size}" }
        val g = StrokeFeatures.GRID
        val h1 = conv3x3(features, StrokeFeatures.CHANNELS, g, conv1W, conv1B, CONV1)
        relu(h1)
        val h2 = conv3x3(h1, CONV1, g, conv2W, conv2B, CONV2)
        relu(h2)
        val p = pool2(h2, CONV2, g)
        val out = FloatArray(EMBEDDING)
        var sq = 0.0
        for (o in 0 until EMBEDDING) {
            var s = denseB[o]
            val w = o * DENSE_IN
            for (i in 0 until DENSE_IN) s += denseW[w + i] * p[i]
            if (s < 0f) s = 0f
            out[o] = s
            sq += s.toDouble() * s
        }
        val norm = maxOf(sqrt(sq), NORM_MIN).toFloat()
        for (o in 0 until EMBEDDING) out[o] /= norm
        return out
    }

    internal companion object {
        const val CONV1 = 16
        const val CONV2 = 32
        const val EMBEDDING = 32
        const val DENSE_IN = CONV2 * (StrokeFeatures.GRID / 2) * (StrokeFeatures.GRID / 2)

        /** Number of floats of one body in the model file, in file order. */
        val SIZES = intArrayOf(CONV1 * StrokeFeatures.CHANNELS * 9, CONV1, CONV2 * CONV1 * 9, CONV2, EMBEDDING * DENSE_IN, EMBEDDING)

        /** A zero embedding stays zero instead of dividing by zero (the training side's normalisation floor). */
        private const val NORM_MIN = 1e-12

        /** 3 × 3 convolution with zero padding 1 over [c] planes of [g] × [g]. */
        private fun conv3x3(x: FloatArray, c: Int, g: Int, w: FloatArray, b: FloatArray, outs: Int): FloatArray {
            val plane = g * g
            val y = FloatArray(outs * plane)
            for (o in 0 until outs) {
                val yo = o * plane
                for (ci in 0 until c) {
                    val wo = (o * c + ci) * 9
                    val xo = ci * plane
                    for (kh in 0 until 3) for (kw in 0 until 3) {
                        val k = w[wo + kh * 3 + kw]
                        val dh = kh - 1
                        val dw = kw - 1
                        for (r in maxOf(0, -dh) until minOf(g, g - dh)) {
                            val a = yo + r * g
                            val s = xo + (r + dh) * g + dw
                            for (i in maxOf(0, -dw) until minOf(g, g - dw)) y[a + i] += k * x[s + i]
                        }
                    }
                }
                for (i in 0 until plane) y[yo + i] += b[o]
            }
            return y
        }

        private fun relu(x: FloatArray) {
            for (i in x.indices) if (x[i] < 0f) x[i] = 0f
        }

        /** 2 × 2 max pooling of [c] planes of [g] × [g], flattened `[plane][row][column]`. */
        private fun pool2(x: FloatArray, c: Int, g: Int): FloatArray {
            val h = g / 2
            val y = FloatArray(c * h * h)
            for (ci in 0 until c) for (r in 0 until h) for (i in 0 until h) {
                val a = (ci * g + 2 * r) * g + 2 * i
                y[(ci * h + r) * h + i] = maxOf(maxOf(x[a], x[a + 1]), maxOf(x[a + g], x[a + g + 1]))
            }
            return y
        }
    }
}

/**
 * One classification set of the model (SRS FR-GRF-13 ①): its body, its letters ([classes], a template's
 * `output.value`) and the shipped exemplars — unit-length embeddings, [exemplarClass]`[i]` the letter of
 * exemplar `i`, whose values are [exemplars]`[i × EMBEDDING ..]`.
 */
class ModelSet internal constructor(
    val body: NnBody,
    val bodyIndex: Int,
    val classes: List<String>,
    val exemplarClass: IntArray,
    val exemplars: FloatArray,
)

/** The shipped model (LLD-05 §7.1): one body and one exemplar list per template set. */
class GraffitiModel internal constructor(val sets: Map<SetName, ModelSet>, val bodyCount: Int)

/**
 * Reads the model file `graffiti/graffiti-nn.bin` (LLD-05 §7.1). A wrong magic, version or dimension, a set name
 * that is no [SetName] or comes twice, a letter with no exemplar, an exemplar of no letter, a file that ends early
 * or goes on after the last set → [IllegalStateException]; the caller then recognizes with the templates alone
 * (LLD-09 §4). The stream is not closed.
 */
object GraffitiModelLoader {
    const val RESOURCE = "/graffiti/graffiti-nn.bin"
    const val VERSION = 1
    private val MAGIC = byteArrayOf('F'.code.toByte(), 'S'.code.toByte(), 'G'.code.toByte(), 'N'.code.toByte())

    fun load(stream: InputStream): GraffitiModel {
        val buf = ByteBuffer.wrap(stream.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        try {
            return read(buf)
        } catch (e: java.nio.BufferUnderflowException) {
            throw IllegalStateException("model file ends early", e)
        }
    }

    private fun read(b: ByteBuffer): GraffitiModel {
        val magic = ByteArray(4).also { b.get(it) }
        check(magic.contentEquals(MAGIC)) { "not a graffiti model file" }
        val version = b.int
        check(version == VERSION) { "unsupported model version $version" }
        val grid = b.int
        val dirs = b.int
        val centre = b.int
        val stepDp = b.float
        val gain = b.float
        val emb = b.int
        check(grid == StrokeFeatures.GRID && dirs == StrokeFeatures.DIRECTIONS && centre == 1) { "feature layout $grid x $dirs + $centre" }
        check(stepDp.toDouble() == StrokeFeatures.CENTRE_STEP_DP && gain == StrokeFeatures.CENTRE_GAIN) { "centre channel $stepDp / $gain" }
        check(emb == NnBody.EMBEDDING) { "embedding size $emb" }
        val bodyCount = b.int
        check(bodyCount in 1..SetName.entries.size) { "body count $bodyCount" }
        val bodies = List(bodyCount) {
            val a = NnBody.SIZES.map { n -> FloatArray(n).also { f -> b.asFloatBuffer().get(f); b.position(b.position() + n * 4) } }
            NnBody(a[0], a[1], a[2], a[3], a[4], a[5])
        }
        val setCount = b.int
        check(setCount in 1..SetName.entries.size) { "set count $setCount" }
        val sets = LinkedHashMap<SetName, ModelSet>()
        repeat(setCount) {
            val raw = ByteArray(b.get().toInt() and 0xFF).also { b.get(it) }.toString(Charsets.US_ASCII)
            val name = SetName.entries.firstOrNull { it.name == raw } ?: error("unknown model set $raw")
            check(name !in sets) { "second model set $raw" }
            val bodyIndex = b.int
            check(bodyIndex in 0 until bodyCount) { "body index $bodyIndex" }
            val classCount = b.int
            check(classCount in 1..MAX_CLASSES) { "class count $classCount" }
            val classes = List(classCount) { ByteArray(b.short.toInt() and 0xFFFF).also { v -> b.get(v) }.toString(Charsets.UTF_8) }
            check(classes.toSet().size == classCount) { "duplicate letter in $raw" }
            val n = b.int
            check(n >= 1 && n.toLong() * EXEMPLAR_BYTES <= b.remaining()) { "exemplar count $n" }
            val cls = IntArray(n)
            val values = FloatArray(n * NnBody.EMBEDDING)
            val seen = BooleanArray(classCount)
            for (i in 0 until n) {
                val c = b.short.toInt() and 0xFFFF
                check(c < classCount) { "exemplar of no letter in $raw" }
                cls[i] = c
                seen[c] = true
                for (k in 0 until NnBody.EMBEDDING) values[i * NnBody.EMBEDDING + k] = b.float
            }
            check(seen.all { it }) { "letter without exemplar in $raw" }
            sets[name] = ModelSet(bodies[bodyIndex], bodyIndex, classes, cls, values)
        }
        check(!b.hasRemaining()) { "bytes after the last model set" }
        return GraffitiModel(sets, bodyCount)
    }

    private const val MAX_CLASSES = 1024

    /** One exemplar in the file: its letter (u16) and its embedding. */
    private const val EXEMPLAR_BYTES = 2 + NnBody.EMBEDDING * 4
}
