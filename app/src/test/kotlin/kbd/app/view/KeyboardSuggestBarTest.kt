package kbd.app.view

import android.inputmethodservice.InputMethodService
import android.os.Looper
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import java.io.File
import java.time.Duration
import kbd.app.error.EngineGuard
import kbd.app.error.GuardedState
import kbd.app.feedback.FeedbackPort
import kbd.app.ime.KeyboardInsets
import kbd.app.store.ErrorLog
import kbd.app.store.FloatSetting
import kbd.app.store.FloatingKeyboard
import kbd.app.store.KeyboardAlign
import kbd.app.store.KeyboardPlate
import kbd.app.store.KeyboardSpan
import kbd.app.store.PxRect
import kbd.app.store.WidthSetting
import kbd.graffiti.recognize.TemplateSet
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.TapOutput
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * `KeyboardRootView` with the suggestion bar (LLD-11 §7.12 (차) BR-01~BR-16, LLD-12 §10, SRS FR-SUG-01~03, HLD 5B.4,
 * FSS#73): one row more above the key area, docked and floating; the guide button at the top of the key area; a finger
 * that is not the gesture's first is dropped on the bar. With no bar, everything is the 0.96 keyboard's.
 *
 * The root is [WIDTH] dp wide, the mode view a QWERTY `TapKeyboardView` (as `KeyboardRootViewTest`, `KeyboardFloatViewTest`).
 * The bar's height is added to the [KeyboardRootView.WINDOW_ROWS] rows, which stay 6.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class KeyboardSuggestBarTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val events = ArrayList<String>()

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

    /** The mode view's and the move's feedback. */
    private val feedback = object : FeedbackPort {
        override fun key() {
            events += "key"
        }

        override fun error() {
            events += "error"
        }

        override fun grab() {
            events += "grab"
        }
    }

    /** The bar's own feedback, told apart from the keys'. */
    private val barFeedback = object : FeedbackPort {
        override fun key() {
            events += "bar-key"
        }

        override fun error() {
            events += "bar-error"
        }
    }

    private object NoState : GuardedState {
        override fun snapshot(): Any = Unit
        override fun restore(s: Any) {}
    }

    private val app get() = RuntimeEnvironment.getApplication()
    private val density get() = app.resources.displayMetrics.density
    private val longPressMs = ViewConfiguration.getLongPressTimeout().toLong()

    private fun px(dp: Float): Int = Math.round(dp * density)

    private fun tapView(): TapKeyboardView {
        val guard = EngineGuard(ErrorLog(File(tmp.root, "errors.log")), NoState)
        val v = TapKeyboardView(app, sink, guard, feedback)
        v.setLayout(TapLayouts.QWERTY, ThresholdDefaults.VALUES, ROW)
        return v
    }

    private fun newBar(): SuggestionBarView = SuggestionBarView(app, barFeedback).also { b ->
        b.onPick = { i, r -> events += "pick:$i@$r" }
        b.show(listOf(BarCell("they"), BarCell("there"), BarCell("the")), 1)
    }

    private fun newRoot(
        bar: Boolean,
        rows: Int = KeyboardRootView.WINDOW_ROWS,
        row: Float = ROW,
        width: WidthSetting = WidthSetting.DEFAULT,
        padLeftDp: Float = 0f,
        padRightDp: Float = 0f,
        navDp: Float = 0f,
        guide: Boolean = false,
    ): KeyboardRootView {
        val v = KeyboardRootView(app)
        v.rowHeightDp = row
        v.portraitWidth = width
        v.landscapeWidth = width
        v.setPadding(px(padLeftDp), 0, px(padRightDp), px(navDp))
        v.moveFeedback = feedback
        v.setModeView(tapView(), rows)
        if (guide) v.enableGuide(TemplateSet(emptyMap()), feedback)
        if (bar) v.setSuggestionBar(newBar())
        v.relayout()
        return v
    }

    /** A docked traversal: the height asked with no limit (as the input view's parent does). */
    private fun docked(v: KeyboardRootView, widthDp: Float = WIDTH): KeyboardRootView {
        v.measure(View.MeasureSpec.makeMeasureSpec(px(widthDp), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)
        return v
    }

    /** A traversal with at most [maxH] px of height (the floating keyboard's parent). */
    private fun atMost(v: KeyboardRootView, maxH: Int, widthDp: Float = WIDTH): KeyboardRootView {
        v.measure(View.MeasureSpec.makeMeasureSpec(px(widthDp), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(maxH, View.MeasureSpec.AT_MOST))
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)
        return v
    }

    private fun children(v: ViewGroup): List<View> = (0 until v.childCount).map(v::getChildAt)
    private fun modeView(v: ViewGroup): View = children(v).first { it is TapKeyboardView }
    private fun guideButton(v: ViewGroup): View = children(v).first { it.contentDescription == "획 안내표" && it !is kbd.app.graffiti.GraffitiGuidePanel }
    private fun handle(v: ViewGroup): View = children(v).first { it.contentDescription == KeyboardRootView.HANDLE_NAME }
    private fun panel(v: ViewGroup): View = children(v).first { it is kbd.app.graffiti.GraffitiGuidePanel }
    private fun bar(v: KeyboardRootView): SuggestionBarView = v.suggestionBar!!
    private fun rect(v: View) = PxRect(v.left, v.top, v.right, v.bottom)

    private fun span(v: KeyboardRootView) = KeyboardSpan(v.rectLeft - v.paddingLeft, v.rectWidth, v.sideMargin)

    /** Centre of QWERTY key [id] in the root, px. */
    private fun keyX(v: KeyboardRootView, id: String): Float {
        val k = TapLayouts.QWERTY.keys.first { it.id == id }
        val m = modeView(v)
        return m.left + (k.x + k.w / 2f) * m.width / 360f
    }

    private fun keyY(v: KeyboardRootView, id: String): Float {
        val k = TapLayouts.QWERTY.keys.first { it.id == id }
        val m = modeView(v)
        return m.top + (k.row + k.rowSpan / 2f) * m.height / TapLayouts.QWERTY.rows
    }

    /** Centre of the bar's cell [i] in the root, px. */
    private fun cellX(v: KeyboardRootView, i: Int): Float {
        val b = bar(v)
        return b.left + (Math.round(i * b.width / 3f) + Math.round((i + 1) * b.width / 3f)) / 2f
    }

    private fun cellY(v: KeyboardRootView): Float = bar(v).top + bar(v).height / 2f

    private fun idle(ms: Long) = Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    // --- BR-01: no bar is the keyboard as it was ---

    /** The 0.96 docked keyboard's height, places and plate by its formulas (LLD-08 §7, §7.2, §7.3). */
    private fun assertDockedAsBefore(what: String, v: KeyboardRootView, rows: Int, row: Float, nav: Int) {
        val h = px(KeyboardRootView.WINDOW_ROWS * row) + nav
        assertEquals(what, h, v.height)
        val x = v.keyboardLeft
        val w = v.keyboardWidth
        assertEquals(what, PxRect(x, h - nav - px(rows * row), x + w, h - nav), rect(modeView(v)))
        assertEquals(what, PxRect(x + w - px(row), 0, x + w, px(row)), rect(guideButton(v)))
        assertEquals(what, KeyboardPlate.rect(v.width, h, v.paddingLeft, v.paddingRight, span(v)), v.plate)
        assertNull(what, v.floatPlace)
        assertNull(what, v.suggestionBar)
        assertEquals(what, 0, v.barHeight)
        assertTrue(what, children(v).none { it is SuggestionBarView })
    }

    @Test
    fun `BR-01 no bar - the docked, narrowed and floating keyboards are the 0_96 ones, 6 and 5 rows`() {
        val widths = listOf(WidthSetting.DEFAULT) + KeyboardAlign.values().map { WidthSetting(36f, it) }
        for (rows in listOf(6, 5)) for (w in widths) for (nav in listOf(0f, NAV_DP)) {
            val v = docked(newRoot(bar = false, rows = rows, width = w, navDp = nav, guide = true))
            assertDockedAsBefore("$rows $w $nav", v, rows, ROW, px(nav))
        }
        // Floating: the plate is the handle and six rows, the children on it as in 0.96 (LLD-08 §7.4).
        for (rows in listOf(6, 5)) {
            val v = newRoot(bar = false, rows = rows, width = WidthSetting(40f, KeyboardAlign.CENTER), navDp = NAV_DP, guide = true)
            v.portraitFloat = FloatSetting(true, 0.25f, 0.3f)
            v.relayout()
            atMost(v, px(MAX_H))
            val fp = FloatingKeyboard.place(v.width, px(MAX_H), 0, 0, 0, px(NAV_DP), span(v), px(FloatingKeyboard.HANDLE_DP) + px(KeyboardRootView.WINDOW_ROWS * ROW), v.portraitFloat)
            assertEquals("$rows", fp, v.floatPlace)
            val p = fp!!.plate
            val x = p.left + v.sideMargin
            val rowsTop = p.top + px(FloatingKeyboard.HANDLE_DP)
            assertEquals(PxRect(p.left, p.top, p.right, rowsTop), rect(handle(v)))
            assertEquals(PxRect(x, p.bottom - px(rows * ROW), x + v.keyboardWidth, p.bottom), rect(modeView(v)))
            assertEquals(PxRect(x + v.keyboardWidth - px(ROW), rowsTop, x + v.keyboardWidth, rowsTop + px(ROW)), rect(guideButton(v)))
            assertNull(v.suggestionBar)
        }
    }

    // --- BR-02 ~ BR-07: docked with the bar ---

    @Test
    fun `BR-02 the bar adds one row to the height, at every row height, with and without bottom padding`() {
        for (row in listOf(40f, 48f, 64f)) for (nav in listOf(0f, NAV_DP)) {
            val v = docked(newRoot(bar = true, row = row, navDp = nav))
            assertEquals("$row $nav", px(row), v.barHeight)
            assertEquals("$row $nav", px(row) + px(KeyboardRootView.WINDOW_ROWS * row) + px(nav), v.height)
            assertEquals("$row $nav", px(row), bar(v).height)
        }
        assertEquals(6, KeyboardRootView.WINDOW_ROWS)
    }

    @Test
    fun `BR-03 the height stays when the mode view goes from 6 rows to 5 and back`() {
        val v = docked(newRoot(bar = true, navDp = NAV_DP))
        val h = v.height
        for (rows in listOf(5, 6)) {
            v.setModeView(tapView(), rows)
            v.relayout()
            docked(v)
            assertEquals("$rows", h, v.height)
        }
    }

    @Test
    fun `BR-04 the bar spans the keys of the keyboard rectangle - full, narrowed in each alignment, beside side bars`() {
        val cases = listOf(
            Triple(WidthSetting.DEFAULT, 0f, 0f),
            Triple(WidthSetting(36f, KeyboardAlign.LEFT), 0f, 0f),
            Triple(WidthSetting(36f, KeyboardAlign.CENTER), 0f, 0f),
            Triple(WidthSetting(36f, KeyboardAlign.RIGHT), 0f, 0f),
            Triple(WidthSetting(36f, KeyboardAlign.CENTER), 48f, 0f),   // a side bar at the left
            Triple(WidthSetting.DEFAULT, 0f, 48f),   // and at the right
        )
        for ((w, l, r) in cases) {
            val v = docked(newRoot(bar = true, width = w, padLeftDp = l, padRightDp = r, navDp = NAV_DP))
            val x = v.paddingLeft + span(v).left + v.sideMargin   // x = l + x + m
            assertEquals("$w $l $r", x, v.keyboardLeft)
            assertEquals("$w $l $r", PxRect(x, 0, x + v.keyboardWidth, px(ROW)), rect(bar(v)))
            assertEquals("$w $l $r", rect(modeView(v)).let { it.left to it.right }, rect(bar(v)).let { it.left to it.right })
        }
    }

    @Test
    fun `BR-05 the mode view sits on the bottom of the rows - 6 rows right under the bar, 5 rows with a band`() {
        for (rows in listOf(6, 5)) {
            val v = docked(newRoot(bar = true, rows = rows, navDp = NAV_DP))
            val m = modeView(v)
            assertEquals("$rows", v.height - px(NAV_DP), m.bottom)
            assertEquals("$rows", m.bottom - px(rows * ROW), m.top)
            if (rows == 6) assertEquals(px(ROW), m.top)
        }
    }

    @Test
    fun `BR-06 graffiti - the guide button at the top of the key area, the open panel over the mode view only`() {
        val v = docked(newRoot(bar = true, rows = 5, guide = true, navDp = NAV_DP))
        val x = v.keyboardLeft + v.keyboardWidth
        assertEquals(PxRect(x - px(ROW), px(ROW), x, 2 * px(ROW)), rect(guideButton(v)))
        val barRect = rect(bar(v))
        assertTrue(rect(guideButton(v)).top >= barRect.bottom)   // not over the bar
        v.openGuide()
        docked(v)
        assertTrue(v.guideOpen)
        assertEquals(rect(modeView(v)), rect(panel(v)))
        assertTrue(rect(panel(v)).top >= barRect.bottom)
        assertEquals(barRect, rect(bar(v)))
    }

    @Test
    fun `BR-07 docked - the plate is the view's whole height with the bar in it, and it is the touchable region`() {
        val v = docked(newRoot(bar = true, width = WidthSetting(40f, KeyboardAlign.CENTER), navDp = NAV_DP))
        assertEquals(0, v.plate.top)
        assertEquals(v.height, v.plate.bottom)
        assertTrue(v.plate.left <= bar(v).left && bar(v).right <= v.plate.right)
        val insets = InputMethodService.Insets()
        KeyboardInsets.compute(v, insets)
        assertEquals(InputMethodService.Insets.TOUCHABLE_INSETS_REGION, insets.touchableInsets)
        val b = insets.touchableRegion.bounds
        assertEquals(v.plate, PxRect(b.left, b.top, b.right, b.bottom))
    }

    // --- BR-08, BR-09, BR-12: floating ---

    private fun floatingRoot(fy: Float? = null): KeyboardRootView {
        val v = newRoot(bar = true, width = WidthSetting(40f, KeyboardAlign.CENTER), navDp = NAV_DP, guide = true)
        v.portraitFloat = FloatSetting(true, null, fy)
        v.relayout()
        return v
    }

    private val plateH: Int get() = px(FloatingKeyboard.HANDLE_DP) + px(ROW) + px(KeyboardRootView.WINDOW_ROWS * ROW)

    @Test
    fun `BR-08 floating - the plate is handle, bar and rows, it fits or docks by that height, top to bottom in that order`() {
        val need = plateH + 0 + px(NAV_DP)   // t = no top inset given here, b = the bottom padding
        val fits = atMost(floatingRoot(), need)
        assertTrue(fits.floated)
        assertEquals(plateH, fits.plate.height)
        val low = atMost(floatingRoot(), need - 1)
        assertFalse(low.floated)   // does not fit: docked, with the bar
        assertEquals(px(ROW) + px(KeyboardRootView.WINDOW_ROWS * ROW) + px(NAV_DP), low.height)
        assertEquals(PxRect(low.keyboardLeft, 0, low.keyboardLeft + low.keyboardWidth, px(ROW)), rect(bar(low)))

        val v = atMost(floatingRoot(fy = 0.3f), px(MAX_H))
        assertTrue(v.floated)
        val fp = v.floatPlace!!
        assertEquals(plateH, fp.plate.height)
        assertEquals(px(MAX_H) - px(NAV_DP) - plateH, fp.rangeY)   // Y = S height − the sum
        assertEquals(FloatingKeyboard.place(v.width, px(MAX_H), 0, 0, 0, px(NAV_DP), span(v), plateH, v.portraitFloat), fp)
        val p = fp.plate
        val handleBottom = p.top + px(FloatingKeyboard.HANDLE_DP)
        val x = p.left + v.sideMargin
        assertEquals(PxRect(p.left, p.top, p.right, handleBottom), rect(handle(v)))
        assertEquals(PxRect(x, handleBottom, x + v.keyboardWidth, handleBottom + px(ROW)), rect(bar(v)))
        assertEquals(PxRect(x, handleBottom + px(ROW), x + v.keyboardWidth, p.bottom), rect(modeView(v)))
        assertEquals(handleBottom + px(ROW), guideButton(v).top)
    }

    @Test
    fun `BR-09 the bar moves with the plate, and a finger on it while the plate moves does nothing`() {
        val v = atMost(floatingRoot(), px(MAX_H))
        val touch = Touches(v)
        val before = rect(bar(v))
        val hx = v.plate.left + v.plate.width / 2f
        val hy = v.plate.top + px(FloatingKeyboard.HANDLE_DP) / 2f
        touch.down(0, hx, hy)
        idle(longPressMs)
        assertEquals(listOf("grab"), events)
        touch.move(0, hx - px(50f), hy - px(200f), steps = 4)
        val dx = -px(50f)
        val dy = -px(200f)
        assertEquals(PxRect(before.left + dx, before.top + dy, before.right + dx, before.bottom + dy), rect(bar(v)))
        assertEquals(rect(bar(v)).bottom, modeView(v).top)
        touch.down(1, cellX(v, 0), cellY(v))
        touch.up(1)
        touch.up(0)
        assertEquals(listOf("grab"), events)   // no bar feedback, no pick, no key
    }

    @Test
    fun `BR-12 floating - a finger on the bar while one waits on the handle drops the move, and the bar does nothing`() {
        val v = atMost(floatingRoot(), px(MAX_H))
        val touch = Touches(v)
        val plate = v.plate
        touch.down(0, plate.left + plate.width / 2f, plate.top + px(FloatingKeyboard.HANDLE_DP) / 2f)
        idle(longPressMs / 2)
        touch.down(1, cellX(v, 1), cellY(v))
        idle(2 * longPressMs)
        touch.up(1)
        touch.up(0)
        assertEquals(emptyList<String>(), events)   // no grab, nothing from the bar
        assertEquals(plate, v.plate)
    }

    // --- BR-10, BR-11, BR-13: touches ---

    @Test
    fun `BR-10 a second finger on the bar is dropped - the first finger's key goes on as it was`() {
        val v = docked(newRoot(bar = true))
        val touch = Touches(v)
        touch.down(0, keyX(v, "g"), keyY(v, "g"))
        assertEquals(listOf("key"), events)
        touch.down(1, cellX(v, 0), cellY(v))
        touch.up(1)
        touch.up(0)
        assertEquals(listOf("key", "out:g"), events)
    }

    @Test
    fun `BR-11 the bar first, then a key - the key gets its own press and gives its input`() {
        val v = docked(newRoot(bar = true))
        val touch = Touches(v)
        touch.down(0, cellX(v, 0), cellY(v))
        assertEquals(listOf("bar-key"), events)
        touch.down(1, keyX(v, "g"), keyY(v, "g"))
        assertEquals(listOf("bar-key", "key"), events)
        touch.up(1)
        assertTrue("$events", events.contains("out:g"))
        touch.up(0)
        assertEquals(listOf("bar-key", "key", "out:g", "pick:0@1"), events)
    }

    @Test
    fun `BR-13 a stroke up from a top key out over the bar is read as with no bar`() {
        fun strokeUp(bar: Boolean, id: String): List<String> {
            events.clear()
            val v = docked(newRoot(bar = bar))
            val touch = Touches(v)
            val x = keyX(v, id)
            val y = keyY(v, id)
            touch.down(0, x, y)
            touch.move(0, x, y - 2.5f * px(ROW), steps = 6)   // past the top of the key area, into the bar's place
            touch.up(0)
            return events.toList()
        }
        for (id in listOf("2", "esc")) {
            val off = strokeUp(false, id)
            val on = strokeUp(true, id)
            assertTrue(id, off.isNotEmpty())
            assertEquals(id, off, on)
        }
        assertTrue(strokeUp(true, "2").contains("out:@"))   // ↑ of 2
    }

    // --- BR-14 ~ BR-16: changes ---

    @Test
    fun `BR-14 the bar taken away is BR-01's keyboard again, a new row height carries the bar along`() {
        val v = docked(newRoot(bar = true, guide = true, navDp = NAV_DP))
        v.setSuggestionBar(null)
        v.relayout()
        docked(v)
        assertDockedAsBefore("off", v, KeyboardRootView.WINDOW_ROWS, ROW, px(NAV_DP))

        val w = docked(newRoot(bar = true, navDp = NAV_DP))
        w.rowHeightDp = 56f
        w.setModeView(modeView(w), KeyboardRootView.WINDOW_ROWS)
        w.relayout()
        docked(w)
        assertEquals(px(56f), bar(w).height)
        assertEquals(px(56f) + px(KeyboardRootView.WINDOW_ROWS * 56f) + px(NAV_DP), w.height)
    }

    @Test
    fun `BR-15 relayout keeps the mode view and the bar as they are`() {
        val v = docked(newRoot(bar = true))
        val m = modeView(v)
        val b = bar(v)
        val before = children(v)
        v.relayout()
        docked(v)
        assertSame(m, modeView(v))
        assertSame(b, v.suggestionBar)
        assertEquals(before, children(v))
    }

    @Test
    fun `BR-16 the bar stays where it is when the mode view changes and the guide opens and closes`() {
        val v = docked(newRoot(bar = true, guide = true, navDp = NAV_DP))
        val r = rect(bar(v))
        v.setModeView(tapView(), 5)
        docked(v)
        assertEquals(r, rect(bar(v)))
        v.openGuide()
        docked(v)
        assertEquals(r, rect(bar(v)))
        v.closeGuide()
        v.setModeView(tapView(), 6)
        docked(v)
        assertEquals(r, rect(bar(v)))
    }

    private companion object {
        const val ROW = 48f
        const val WIDTH = 800f
        const val NAV_DP = 24f
        const val MAX_H = 1000f
    }
}
