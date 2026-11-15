package kbd.graffiti.eval

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LLD-11 §6.7 CE-01 (SRS FR-GRF-10 acceptance, HLD 11.1 다): every stroke set of the repository read by the
 * templates alone (recognition before the network score) and by the weighted sum, as a table. The synthetic sets come
 * from the generator the network was trained with, so they are a regression check, not evidence; `owner_en` and
 * most of `device/ko` are strokes that are templates and exemplars themselves.
 */
class CombinedEvalTest {

    @Test
    fun `CE-01 templates alone against the weighted sum on every stroke set`() {
        val lines = ArrayList<String>()
        val got = LinkedHashMap<String, Pair<Int, Int>>()
        for (split in listOf("eval", "dev", "device", "device_held", "owner_en")) for ((folder, list) in GraffitiEval.load(split)) {
            val alone = GraffitiEval.score(folder, list, GraffitiEval.templateOnly)
            val sum = GraffitiEval.score(folder, list, GraffitiEval.recognizer)
            got["$split/$folder"] = alone.correct to sum.correct
            lines += "CE-01 | %-14s | %4d | %4d | %4d | %s".format("$split/$folder", list.size, alone.correct, sum.correct, sum.misses)
        }
        println("CE-01 | set | strokes | templates alone | weighted sum | misses of the sum")
        lines.forEach(::println)
        for ((name, want) in SUM_MIN) {
            val (alone, sum) = got.getValue(name)
            assertTrue("$name: weighted sum $sum, templates alone $alone", sum >= alone)
            assertTrue("$name: weighted sum $sum under $want", sum >= want)
        }
        assertEquals(SUM_MIN.keys, got.keys)
    }

    private companion object {
        /** The weighted sum's count on each set when the model went in (LLD-11 §6.7): every stroke of every set. */
        val SUM_MIN = linkedMapOf(
            "eval/en" to 328, "eval/ext" to 208, "eval/ko" to 312, "eval/num" to 128, "eval/punct" to 272,
            "dev/en" to 164, "dev/ext" to 104, "dev/ko" to 156, "dev/num" to 64, "dev/punct" to 136,
            "device/en" to 32, "device/ko" to 103, "device_held/ko" to 74, "owner_en/day1" to 83, "owner_en/day2" to 85,
        )
    }
}
