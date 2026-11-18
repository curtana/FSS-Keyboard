package kbd.app.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import java.io.File
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.store.ErrorLog
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayout
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.TapOutput
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * LLD-11 §7.5 BV-01..BV-04: the two-row ↵ on `TapKeyboardView` (FSS#105, SRS FR-TAP-03d ②⑤⑦, LLD-05A §4, §7) — one
 * rectangle drawn over both rows with the label in its middle at the size of the other labels, pressed as a whole,
 * one accessibility node over both rows, listed after l.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class TallEnterViewTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val events = ArrayList<String>()
    private lateinit var view: TapKeyboardView

    private val sink = object : TapSink {
        override fun onTapOutput(key: TapKeyDef, out: TapOutput) {
            events += "out:" + (out.text ?: out.jamo.toString())
        }

        override fun onFunctionDown(key: FunctionKey) {
            events += "down:$key"
        }

        override fun onFunctionUp(key: FunctionKey) {
            events += "up:$key"
        }
    }

    private object NoFeedback : FeedbackPort {
        override fun key() = Unit
        override fun error() = Unit
    }

    private object NoState : GuardedState {
        override fun snapshot(): Any = Unit
        override fun restore(s: Any) {}
    }

    private var density = 1f
    private lateinit var touch: Touches

    private fun show(layout: TapLayout = TapLayouts.QWERTY) {
        val guard = EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState)
        view = TapKeyboardView(RuntimeEnvironment.getApplication(), sink, guard, NoFeedback)
        view.setLayout(layout, ThresholdDefaults.VALUES, ROW)
        density = view.resources.displayMetrics.density
        layoutExactly(view, Math.round(360f * density), Math.round(6 * ROW * density))
        touch = Touches(view)
    }

    private fun index(id: String) = TapLayouts.QWERTY.keys.indexOfFirst { it.id == id }

    /** The keys as drawn, in drawing order: each rounded rectangle with its colour, each label with its place and size. */
    private class Drawn : Canvas() {
        val rects = ArrayList<Pair<RectF, Int>>()
        val labels = ArrayList<Triple<String, Pair<Float, Float>, Float>>()
        /** The alignment each label was drawn with, in the order of [labels]. */
        val aligns = ArrayList<Paint.Align>()

        override fun drawRoundRect(rect: RectF, rx: Float, ry: Float, paint: Paint) {
            rects += RectF(rect) to paint.color
        }

        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            labels += Triple(text, x to y, paint.textSize)
            aligns += paint.textAlign
        }
    }

    private fun draw() = Drawn().also { view.draw(it) }

    @Test
    fun `BV-01 enter is drawn as one rectangle over rows 3 and 4 with its label in the middle, as big as the other labels`() {
        for (layout in listOf(TapLayouts.QWERTY, TapLayouts.DUBEOLSIK)) {
            show(layout)
            val d = draw()
            assertEquals(layout.name, layout.keys.size, d.rects.size)   // one rectangle per key, ↵ once
            val (r, _) = d.rects[index("enter")]
            val inset = 2f * density
            assertEquals(layout.name, 324f * density + inset, r.left, 0.5f)
            assertEquals(layout.name, 360f * density - inset, r.right, 0.5f)
            assertEquals(layout.name, 3 * ROW * density + inset, r.top, 0.5f)
            assertEquals(layout.name, 5 * ROW * density - inset, r.bottom, 0.5f)
            val (text, at, size) = d.labels.single { it.first == "↵" }
            assertEquals(layout.name, "↵", text)
            assertEquals(layout.name, r.centerX(), at.first, 0.01f)
            // Vertically centred on the rectangle (= the boundary of its two rows), as the view centres every label.
            val p = Paint().apply { textSize = size }
            assertEquals(layout.name, r.centerY() - (p.ascent() + p.descent()) / 2, at.second, 1f)
            assertEquals(layout.name, 4 * ROW * density, r.centerY(), 0.5f)
            assertEquals(layout.name, view.labelTextSizePx, size, 0f)
            assertEquals(layout.name, 0.4f * ROW * density, size, 0.01f)
            assertEquals(layout.name, d.labels.first { it.first == layout.keys[index("q")].label }.third, size, 0f)
        }
    }

    @Test
    fun `BV-02 a held enter is pressed as a whole, whichever row it was pressed on`() {
        show()
        for (y in listOf(3.2f, 4.8f)) {
            touch.down(0, 342f * density, y * ROW * density)
            val d = draw()
            assertEquals(PRESSED_COLOR, d.rects[index("enter")].second)
            assertEquals(1, d.rects.count { it.second == PRESSED_COLOR })
            touch.up(0)
        }
        assertEquals(listOf("down:ENTER", "up:ENTER", "down:ENTER", "up:ENTER"), events)
    }

    @Test
    fun `BV-03 one accessibility node for enter - bounds over both rows, both rows lead to it, after l and before z`() {
        show()
        val i = index("enter")
        val b = Rect()
        view.keyBoundsPx(i, b)
        assertEquals(Rect(px(324f), px(3 * ROW), px(360f), px(5 * ROW)), b)
        @Suppress("DEPRECATION")
        val nodeBounds = Rect().also { view.accessibilityNodeProvider.createAccessibilityNodeInfo(i)!!.getBoundsInParent(it) }
        assertEquals(b, nodeBounds)
        assertEquals(i, view.keyIndexAtPx(342f * density, 3.2f * ROW * density))
        assertEquals(i, view.keyIndexAtPx(342f * density, 4.8f * ROW * density))
        assertEquals(index("l") + 1, i)
        assertEquals(index("z") - 1, i)
        assertEquals(TapLayouts.QWERTY.keys.size, view.keyCount)
        assertEquals(1, TapLayouts.QWERTY.keys.count { it.function == FunctionKey.ENTER })
    }

    @Test
    fun `BV-04 the moved slash key is drawn in row 4 left of enter and the space bar 216 wide`() {
        show()
        val d = draw()
        val inset = 2f * density
        val slash = d.rects[index("slash")].first
        assertEquals(288f * density + inset, slash.left, 0.5f)
        assertEquals(4 * ROW * density + inset, slash.top, 0.5f)
        val space = d.rects[index("space")].first
        assertEquals(216f * density - 2 * inset, space.width(), 0.5f)
    }

    @Test
    fun `GJ-02 consonant keys draw only their labels, corners only the vowel diagonals, the number row its tense consonants and 1 its tilde on the left (FSS 109, 124, 126)`() {
        show(TapLayouts.DUBEOLSIK)
        val d = draw()
        val inset = 2f * density
        val pad = 2f * density
        fun inside(d: Drawn, x0: Float, row: Int): Map<String, Pair<Pair<Float, Float>, Paint.Align>> {
            val l = x0 * density + inset
            val r = (x0 + 36f) * density - inset
            val t = row * ROW * density + inset
            val b = (row + 1) * ROW * density - inset
            return d.labels.indices.filter { i -> d.labels[i].second.let { (x, y) -> x in l..r && y in t..b } }
                .associate { d.labels[it].first to (d.labels[it].second to d.aligns[it]) }
        }
        // Every consonant key, ㄹ too: the label alone (a consonant + vowel is not drawn, owner; FSS#124 took the
        // compound finals and the ↑ tense consonants off).
        for (k in TapLayouts.DUBEOLSIK.keys.filter { it.jamo != null && it.jamo in 'ㄱ'..'ㅎ' }) {
            assertEquals(k.label, setOf(k.label), inside(d, k.x, k.row).keys)
        }
        // Number row: 1 has ! ↑, ~ ←, ` →, ㅃ ↓, each in the middle of its edge; 2..5 their tense consonant at the bottom.
        val one = inside(d, 0f, 1)
        assertEquals(setOf("1", "!", "~", "`", "ㅃ"), one.keys)
        assertEquals(Paint.Align.LEFT, one.getValue("~").second)
        assertEquals(inset + pad, one.getValue("~").first.first, 0.01f)
        assertEquals(Paint.Align.CENTER, one.getValue("ㅃ").second)
        assertTrue(one.getValue("ㅃ").first.second > one.getValue("1").first.second)
        assertTrue(one.getValue("!").first.second < one.getValue("1").first.second)
        "ㅉㄸㄲㅆ".forEachIndexed { i, c ->
            val n = inside(d, (i + 1) * 36f, 1)
            assertEquals("$c", setOf("${i + 2}", "@#$%"[i].toString(), "$c"), n.keys)
            assertEquals(Paint.Align.CENTER, n.getValue("$c").second)
        }
        assertEquals(setOf("6", "^"), inside(d, 5 * 36f, 1).keys)
        // Corner hints: only the vowel diagonals, ㅙ ㅞ of ㅗ ㅜ (FSS#110) and ㅔ ㅖ ㅐ ㅒ of ㅓ ㅏ (FSS#126); every other
        // LEFT/RIGHT hint is mid-row.
        val hintSize = d.labels.first { it.first == "ㅙ" }.third
        val hp = Paint().apply { textSize = hintSize }
        val corners = d.labels.indices.filter { i ->
            val (_, p, size) = d.labels[i]
            val row = Math.floor((p.second / (ROW * density)).toDouble()).toInt()
            val mid = (row + 0.5f) * ROW * density - (hp.ascent() + hp.descent()) / 2
            size == hintSize && d.aligns[i] != Paint.Align.CENTER && Math.abs(p.second - mid) > 1f
        }.map { d.labels[it].first }
        assertEquals(listOf("ㅙ", "ㅞ", "ㅔ", "ㅖ", "ㅐ", "ㅒ").sorted(), corners.sorted())
        // QWERTY: 1 has ~ on the left too, and nothing at the bottom.
        show(TapLayouts.QWERTY)
        assertEquals(setOf("1", "!", "~", "`"), inside(draw(), 0f, 1).keys)
    }

    @Test
    fun `GV-03 ㅗ ㅜ ㅡ draw their compound vowels mid-edge and ㅙ ㅞ in the bottom corners (FSS 110, FR-TAP-02g ⑧)`() {
        show(TapLayouts.DUBEOLSIK)
        val d = draw()
        val inset = 2f * density
        val pad = 2f * density
        fun inside(x0: Float, row: Int): Map<String, Pair<Pair<Float, Float>, Paint.Align>> {
            val l = x0 * density + inset
            val r = (x0 + 36f) * density - inset
            val t = row * ROW * density + inset
            val b = (row + 1) * ROW * density - inset
            return d.labels.indices.filter { i -> d.labels[i].second.let { (x, y) -> x in l..r && y in t..b } }
                .associate { d.labels[it].first to (d.labels[it].second to d.aligns[it]) }
        }
        // ㅗ (h): x 180..216, row 3. ㅜ (n): x 180..216, row 4. ㅡ (m): x 216..252, row 4.
        val o = inside(180f, 3)
        assertEquals(setOf("ㅗ", "ㅛ", "ㅚ", "ㅘ", "ㅙ"), o.keys)
        val u = inside(180f, 4)
        assertEquals(setOf("ㅜ", "ㅟ", "ㅝ", "ㅠ", "ㅞ"), u.keys)
        assertEquals(setOf("ㅡ", "ㅢ"), inside(216f, 4).keys)
        // ㅙ bottom-right of ㅗ on ㅚ's line, ㅞ bottom-left of ㅜ on ㅠ's line; the others mid-edge.
        assertEquals(Paint.Align.RIGHT, o.getValue("ㅙ").second)
        assertEquals(216f * density - inset - pad, o.getValue("ㅙ").first.first, 0.01f)
        assertEquals(o.getValue("ㅚ").first.second, o.getValue("ㅙ").first.second, 0.01f)
        assertEquals(Paint.Align.LEFT, u.getValue("ㅞ").second)
        assertEquals(180f * density + inset + pad, u.getValue("ㅞ").first.first, 0.01f)
        assertEquals(u.getValue("ㅠ").first.second, u.getValue("ㅞ").first.second, 0.01f)
        assertEquals(Paint.Align.CENTER, o.getValue("ㅛ").second)
        assertEquals(Paint.Align.CENTER, o.getValue("ㅚ").second)
        assertEquals(Paint.Align.RIGHT, o.getValue("ㅘ").second)
        assertEquals(Paint.Align.CENTER, u.getValue("ㅟ").second)
        assertEquals(Paint.Align.LEFT, u.getValue("ㅝ").second)
        assertEquals(Paint.Align.CENTER, u.getValue("ㅠ").second)
    }

    @Test
    fun `GV-05 ㅓ ㅏ draw their diagonal vowels small in the corners, ㅔ ㅐ at the bottom and ㅖ ㅒ at the top (FSS 126, FR-TAP-02j)`() {
        show(TapLayouts.DUBEOLSIK)
        val d = draw()
        val inset = 2f * density
        val pad = 2f * density
        fun inside(x0: Float, row: Int): Map<String, Pair<Pair<Float, Float>, Paint.Align>> {
            val l = x0 * density + inset
            val r = (x0 + 36f) * density - inset
            val t = row * ROW * density + inset
            val b = (row + 1) * ROW * density - inset
            return d.labels.indices.filter { i -> d.labels[i].second.let { (x, y) -> x in l..r && y in t..b } }
                .associate { d.labels[it].first to (d.labels[it].second to d.aligns[it]) }
        }
        // ㅓ (j): x 216..252, row 3. ㅏ (k): x 252..288, row 3. ㅗ (h) for the lines of the top and bottom hints.
        val eo = inside(216f, 3)
        assertEquals(setOf("ㅓ", "ㅕ", "ㅔ", "ㅖ"), eo.keys)
        val a = inside(252f, 3)
        assertEquals(setOf("ㅏ", "ㅑ", "ㅐ", "ㅒ"), a.keys)
        val o = inside(180f, 3)
        // Left corners of ㅓ, right corners of ㅏ, as ㅞ and ㅙ (GV-03).
        for (c in listOf("ㅔ", "ㅖ")) {
            assertEquals(c, Paint.Align.LEFT, eo.getValue(c).second)
            assertEquals(c, 216f * density + inset + pad, eo.getValue(c).first.first, 0.01f)
        }
        for (c in listOf("ㅐ", "ㅒ")) {
            assertEquals(c, Paint.Align.RIGHT, a.getValue(c).second)
            assertEquals(c, 288f * density - inset - pad, a.getValue(c).first.first, 0.01f)
        }
        // Bottom on ㅚ's line, top on ㅛ's line; ㅕ ㅑ stay mid-edge.
        assertEquals(o.getValue("ㅚ").first.second, eo.getValue("ㅔ").first.second, 0.01f)
        assertEquals(o.getValue("ㅚ").first.second, a.getValue("ㅐ").first.second, 0.01f)
        assertEquals(o.getValue("ㅛ").first.second, eo.getValue("ㅖ").first.second, 0.01f)
        assertEquals(o.getValue("ㅛ").first.second, a.getValue("ㅒ").first.second, 0.01f)
        assertEquals(Paint.Align.LEFT, eo.getValue("ㅕ").second)
        assertEquals(Paint.Align.RIGHT, a.getValue("ㅑ").second)
        // Small: the hint size (ㅙ's), not the key label's.
        val hintSize = d.labels.first { it.first == "ㅙ" }.third
        fun size(c: String, at: Pair<Float, Float>) = d.labels.single { it.first == c && it.second == at }.third
        for (c in listOf("ㅔ", "ㅖ")) assertEquals(c, hintSize, size(c, eo.getValue(c).first), 0.01f)
        for (c in listOf("ㅐ", "ㅒ")) assertEquals(c, hintSize, size(c, a.getValue(c).first), 0.01f)
        assertTrue(size("ㅓ", eo.getValue("ㅓ").first) > hintSize)
    }

    private fun px(dp: Float): Int = Math.round(dp * density)

    private companion object {
        const val ROW = 48f
        const val PRESSED_COLOR = 0xFF5A5A5A.toInt()
    }
}
