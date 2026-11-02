package kbd.app.store

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kbd.stroke.geom.Dir
import kbd.stroke.geom.KeyGeom
import kbd.stroke.geom.RectDp
import kbd.stroke.geom.StrokePoint
import kbd.stroke.tap.TapGesture
import kbd.stroke.tap.TapGestureClassifier
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** TS-01..TS-09 (LLD-11 §7.5, LLD-07 §4·§5, LLD-09 §3; format 3, issue #150). */
class ThresholdStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** HLD 7.2 format 5 (issue #173: no one-stroke vowel fields, `posTolDp`, the `calibration` record). */
    private val example = """
        {
          "version": 5,
          "keySizeDp": { "w": 51, "h": 48 },
          "minCrossDp": 14,
          "longDp": 51,
          "guideDelayMs": 120,
          "posTolDp": 20
        }
    """.trimIndent()

    /** HLD 7.2 format 4 (issue #168): the one-stroke vowel fields, read only. */
    private val exampleV4 = """
        {
          "version": 4,
          "keySizeDp": { "w": 51, "h": 48 },
          "minCrossDp": 15,
          "longDp": 70,
          "addIDp": 48,
          "returnLongDp": 48,
          "turnAngleDeg": 60,
          "guideDelayMs": 130
        }
    """.trimIndent()

    /** HLD 7.2 format 3 (issues #150–#160): format 4 plus `doubleTapWindowMs`, read only. */
    private val exampleV3 = exampleV4.replace("\"version\": 4", "\"version\": 3").replace("\"guideDelayMs\": 130", "\"doubleTapWindowMs\": 250,\n  \"guideDelayMs\": 130")

    /** HLD 7.2 format 2 (issues #127–#150): the merge window, reference gaps and break values. */
    private val exampleV2 = """
        {
          "version": 2,
          "keySizeDp": { "w": 51, "h": 48 },
          "minCrossDp": 15,
          "longDp": 52,
          "addIDp": 49,
          "returnLongDp": 47,
          "turnAngleDeg": 61,
          "mergeWindowMs": 450,
          "doubleTapWindowMs": 380,
          "mergeRefGapMs": 200,
          "doubleRefGapMs": 150,
          "incomingRefGapMs": 250,
          "breakRatio": 1.5,
          "breakMinDeltaMs": 80,
          "guideDelayMs": 130
        }
    """.trimIndent()

    /** HLD 7.2 format 1 (before issue #127): `dwellMs`, no reference gaps or break values. */
    private val exampleV1 = """
        {
          "version": 1,
          "keySizeDp": { "w": 51, "h": 48 },
          "minCrossDp": 14,
          "longDp": 51,
          "addIDp": 48,
          "returnLongDp": 48,
          "turnAngleDeg": 60,
          "mergeWindowMs": 450,
          "doubleTapWindowMs": 380,
          "dwellMs": 300,
          "guideDelayMs": 120
        }
    """.trimIndent()

    private fun errorLog() = ErrorLog(File(tmp.root, "errors.log"))

    private fun store(log: ErrorLog = errorLog()) = ThresholdStore(tmp.root, log)

    private fun thresholdFile() = File(tmp.root, ThresholdStore.FILE_NAME)

    private fun lines(log: ErrorLog) = log.readAll().lines().filter { it.isNotEmpty() }

    private fun withField(name: String, value: String) = example.replace(Regex("\"$name\": [0-9.]+"), "\"$name\": $value")

    @Test
    fun `TS-01 example JSON round-trips to the same values`() {
        val t = ThresholdCodec.decode(example)
        assertEquals(ThresholdDefaults.VALUES, t)
        assertEquals(t, ThresholdCodec.decode(ThresholdCodec.encode(t)))
    }

    @Test
    fun `TS-01 fractional values round-trip through save and load`() {
        val t = ThresholdDefaults.VALUES.copy(longDp = 52.5f, posTolDp = 17.25f, keySizeWDp = 36f)
        store().save(t)
        assertEquals(Loaded(t, LoadStatus.OK), store().load())
    }

    @Test
    fun `TS-01 unknown fields are ignored`() {
        val json = example.replace("\"version\": 5,", "\"version\": 5, \"future\": { \"x\": [1, 2] },")
        assertEquals(ThresholdDefaults.VALUES, ThresholdCodec.decode(json))
    }

    @Test
    fun `TS-02 missing file gives defaults and DEFAULT_MISSING`() {
        val log = errorLog()
        assertEquals(Loaded(ThresholdDefaults.VALUES, LoadStatus.DEFAULT_MISSING), store(log).load())
        assertEquals(0, lines(log).size)
    }

    @Test
    fun `TS-03 broken JSON gives defaults, DEFAULT_CORRUPT and one error record`() {
        val log = errorLog()
        thresholdFile().writeText("{ \"version\": 1, \"minCrossDp\": ")
        assertEquals(Loaded(ThresholdDefaults.VALUES, LoadStatus.DEFAULT_CORRUPT), store(log).load())
        val recorded = lines(log)
        assertEquals(1, recorded.size)
        assertTrue(recorded[0], recorded[0].startsWith("ThresholdStore.load\tThresholdFormatException\t"))
        // LLD-09 §3: the corrupt file stays for the next save to overwrite.
        assertTrue(thresholdFile().isFile)
    }

    @Test
    fun `TS-03 missing field or non-number value is DEFAULT_CORRUPT`() {
        thresholdFile().writeText(example.replace("\"longDp\": 51,", ""))
        assertEquals(LoadStatus.DEFAULT_CORRUPT, store().load().status)
        thresholdFile().writeText(exampleV2.replace("\"keySizeDp\": { \"w\": 51, \"h\": 48 },", ""))   // a common field is still required in format 2
        assertEquals(LoadStatus.DEFAULT_CORRUPT, store().load().status)
        thresholdFile().writeText(withField("longDp", "\"51\""))
        assertEquals(LoadStatus.DEFAULT_CORRUPT, store().load().status)
    }

    @Test
    fun `TS-04 version 6 is DEFAULT_CORRUPT`() {
        val log = errorLog()
        thresholdFile().writeText(withField("version", "6"))
        assertEquals(Loaded(ThresholdDefaults.VALUES, LoadStatus.DEFAULT_CORRUPT), store(log).load())
        assertEquals(1, lines(log).size)
    }

    @Test
    fun `TS-05 a stored minCrossDp is ignored - fixed at 8 dp (issue 217), whatever the file says or if it is missing`() {
        for (v in listOf("0", "20", "61", "14")) {
            thresholdFile().writeText(withField("minCrossDp", v))
            assertEquals(v, Loaded(ThresholdDefaults.VALUES, LoadStatus.OK), store().load())
        }
        thresholdFile().writeText(example.replace("\"minCrossDp\": 14,", ""))
        assertEquals(Loaded(ThresholdDefaults.VALUES, LoadStatus.OK), store().load())
        assertEquals(8f, ThresholdDefaults.VALUES.minCrossDp)
        assertTrue(ThresholdCodec.encode(ThresholdDefaults.VALUES).contains("\"minCrossDp\": 8"))   // still written: older builds read it
    }

    @Test
    fun `TS-05 range bounds are inclusive and one step outside is rejected`() {
        val cases = listOf(
            Triple("longDp", "10", "9"), Triple("longDp", "300", "301"),
            Triple("posTolDp", "1", "0.5"), Triple("posTolDp", "100", "100.5"),
            Triple("guideDelayMs", "0", "-1"), Triple("guideDelayMs", "2000", "2001"),
            Triple("w", "10", "9"), Triple("h", "300", "301"),
        )
        for ((name, ok, bad) in cases) {
            ThresholdCodec.decode(withField(name, ok))
            try {
                ThresholdCodec.decode(withField(name, bad))
                throw AssertionError("$name = $bad accepted")
            } catch (_: ThresholdFormatException) {
            }
        }
    }

    /**
     * Formats 1–4 read (HLD 7.2, issues #150, #168, #173): `minCrossDp`, `keySizeDp`, `guideDelayMs` kept; `longDp`
     * meant the first exit past the key's edge then, so the default (51) starts over; the other old fields are not read.
     */
    private val fromOld = ThresholdDefaults.VALUES.copy(guideDelayMs = 130L)   // minCrossDp 15 in the files is ignored (issue 217)

    @Test
    fun `TS-09 format 2 is read without the dropped fields`() {
        val t = ThresholdCodec.decode(exampleV2)
        assertEquals(fromOld, t)
        // Re-encoding writes format 5 without the dropped fields.
        val out = ThresholdCodec.encode(t)
        assertTrue(out.contains("\"version\": 5"))
        for (gone in listOf("mergeWindowMs", "mergeRefGapMs", "doubleRefGapMs", "incomingRefGapMs", "breakRatio", "breakMinDeltaMs", "dwellMs",
            "doubleTapWindowMs", "addIDp", "returnLongDp", "turnAngleDeg")) {
            assertTrue(gone, !out.contains(gone))
        }
        assertEquals(t, ThresholdCodec.decode(out))
    }

    @Test
    fun `TS-10 formats 3 and 4 - the old longDp and the dropped fields are not read or checked (issues 168, 173)`() {
        assertEquals(fromOld, ThresholdCodec.decode(exampleV4))
        assertEquals(fromOld, ThresholdCodec.decode(exampleV4.replace("\"turnAngleDeg\": 60", "\"turnAngleDeg\": 999").replace("\"longDp\": 70", "\"longDp\": 9999")))
        assertEquals(fromOld, ThresholdCodec.decode(exampleV3))
        assertEquals(fromOld, ThresholdCodec.decode(exampleV3.replace("250", "9999")))
        assertTrue(!ThresholdCodec.encode(ThresholdCodec.decode(exampleV3)).contains("doubleTapWindowMs"))
    }

    @Test
    fun `TS-11 the calibration record is written and read back, never judged with, and goes along on import (FR-TUT-13)`() {
        val cal = Calibration(
            tapDot = BoundaryRecord(6.1f, 18.4f, 12.3f, 24, 30, 0),
            dotLong = BoundaryRecord(31f, 62.5f, 31.5f, 30, 30, 1),
            position = PositionRecord(11.2f, 14.8f, 40),
            at = "2026-11-01T10:00:00+09:00",
        )
        val json = ThresholdCodec.encode(ThresholdDefaults.VALUES, cal)
        assertTrue(json.contains("\"calibration\""))
        assertEquals(ThresholdDefaults.VALUES, ThresholdCodec.decode(json))
        assertEquals(cal, ThresholdCodec.decodeCalibration(json))
        assertEquals(null, ThresholdCodec.decodeCalibration(example))
        assertEquals(null, ThresholdCodec.decodeCalibration(json.replace("\"shortP90\"", "\"x\"")))   // unreadable: just no record
        val s = store()
        assertEquals(ImportResult.Ok, s.importFrom(ByteArrayInputStream(json.toByteArray())))
        assertEquals(cal, ThresholdCodec.decodeCalibration(thresholdFile().readText()))
    }

    @Test
    fun `TS-09 format 1 is read the same way`() {
        val v1 = exampleV1.replace("\"minCrossDp\": 14", "\"minCrossDp\": 15").replace("\"longDp\": 51", "\"longDp\": 52")
            .replace("\"guideDelayMs\": 120", "\"guideDelayMs\": 130")
        assertEquals(fromOld, ThresholdCodec.decode(v1))
    }

    @Test
    fun `TS-09 old files load OK and their dropped fields are not checked`() {
        thresholdFile().writeText(exampleV1)
        assertEquals(LoadStatus.OK, store().load().status)
        thresholdFile().writeText(exampleV1.replace("\"dwellMs\": 300", "\"dwellMs\": 3001").replace("\"doubleTapWindowMs\": 380,", ""))
        assertEquals(LoadStatus.OK, store().load().status)
        thresholdFile().writeText(exampleV2.replace("\"breakRatio\": 1.5", "\"breakRatio\": 1"))
        assertEquals(Loaded(fromOld, LoadStatus.OK), store().load())
        thresholdFile().writeText(exampleV2.replace("\"guideDelayMs\": 130", "\"guideDelayMs\": 9999"))   // a common field out of range
        assertEquals(LoadStatus.DEFAULT_CORRUPT, store().load().status)
    }

    @Test
    fun `TS-06 importing a broken file returns Invalid and keeps the existing file`() {
        val s = store()
        val saved = ThresholdDefaults.VALUES.copy(longDp = 100f)
        s.save(saved)
        val before = thresholdFile().readBytes()
        for (bad in listOf("not json", withField("version", "6"), withField("longDp", "0"))) {
            assertSame(ImportResult.Invalid, s.importFrom(ByteArrayInputStream(bad.toByteArray())))
        }
        assertArrayEquals(before, thresholdFile().readBytes())
        assertEquals(Loaded(saved, LoadStatus.OK), s.load())
    }

    @Test
    fun `TS-06 importing a valid file saves it and export writes it back`() {
        val s = store()
        val json = withField("longDp", "100")
        assertSame(ImportResult.Ok, s.importFrom(ByteArrayInputStream(json.toByteArray())))
        assertEquals(100f, s.load().thresholds.longDp)
        val out = ByteArrayOutputStream()
        s.exportTo(out)
        assertEquals(ThresholdCodec.decode(json), ThresholdCodec.decode(out.toString(Charsets.UTF_8.name())))
    }

    @Test
    fun `TS-06 save replaces the file without leaving a temp file, reset writes defaults`() {
        val s = store()
        s.save(ThresholdDefaults.VALUES.copy(longDp = 100f))
        s.save(ThresholdDefaults.VALUES.copy(longDp = 120f))
        assertEquals(120f, s.load().thresholds.longDp)
        assertTrue(s.lastModified() > 0L)
        s.resetToDefault()
        assertEquals(Loaded(ThresholdDefaults.VALUES, LoadStatus.OK), s.load())
        assertEquals(listOf(ThresholdStore.FILE_NAME), tmp.root.list()!!.filter { it.startsWith("thresholds") })
    }

    // TS-07: key 36×48, strokes start at the centre (LLD-11 §5.1 conventions).
    private val key = KeyGeom("k", RectDp(0f, 0f, 36f, 48f), emptySet())

    /** Straight 1 dp steps from the centre by (dx, dy) per leg. */
    private fun stroke(vararg legs: Pair<Int, Int>): List<StrokePoint> {
        var x = 18f
        var y = 24f
        var t = 0L
        val points = mutableListOf(StrokePoint(x, y, t))
        for ((dx, dy) in legs) {
            val n = maxOf(Math.abs(dx), Math.abs(dy))
            repeat(n) {
                x += dx.toFloat() / n
                y += dy.toFloat() / n
                t += 8L
                points += StrokePoint(x, y, t)
            }
        }
        return points
    }

    private fun classify(t: Thresholds, points: List<StrokePoint>): TapGesture =
        TapGestureClassifier(key, t.minCrossDp).run {
            begin(points.first())
            points.drop(1).dropLast(1).forEach { add(it) }
            finish(points.last())
        }

    @Test
    fun `TS-07 changing longDp in the saved file leaves tap judgement unchanged`() {
        val strokes = listOf(
            stroke(),
            stroke(0 to -40),
            stroke(0 to 40),
            stroke(-35 to 0),
            stroke(0 to -40, 0 to 40),
            stroke(0 to -40, 0 to 40, 0 to -40),
            stroke(0 to -30),
            stroke(30 to 0, -30 to 0),
        )
        val s = store()
        s.save(ThresholdDefaults.VALUES.copy(longDp = 10f))
        val shortLong = s.load()
        s.save(ThresholdDefaults.VALUES.copy(longDp = 300f))
        val longLong = s.load()
        assertEquals(LoadStatus.OK, shortLong.status)
        assertNotEquals(shortLong.thresholds.longDp, longLong.thresholds.longDp)
        for (p in strokes) {
            val expected = classify(ThresholdDefaults.VALUES, p)
            assertEquals(expected, classify(shortLong.thresholds, p))
            assertEquals(expected, classify(longLong.thresholds, p))
        }
        // minCrossDp, by contrast, does change the result: 16 dp past the top edge exits at 8, not at 30.
        val up = stroke(0 to -40)
        assertEquals(TapGesture.Exit(Dir.UP), classify(ThresholdDefaults.VALUES, up))
        assertEquals(TapGesture.Tap, classify(ThresholdDefaults.VALUES.copy(minCrossDp = 30f), up))
    }
}
