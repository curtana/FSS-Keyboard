package kbd.app.swipe.impl

import android.os.Looper
import java.time.Duration
import kbd.app.ime.FieldClass
import kbd.app.ime.FieldInfo
import kbd.app.store.Settings
import kbd.app.tutor.PracticeKeyboardView
import kbd.app.tutor.PracticeLayout
import kbd.app.tutor.PracticeLayouts
import kbd.app.tutor.RawStroke
import kbd.app.view.Touches
import kbd.app.view.ViewTestSdk
import kbd.app.view.layoutExactly
import kbd.gesture.layout.SwipeLayout
import kbd.gesture.progress.ProgressState
import kbd.stroke.threshold.ThresholdDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * The swipe guide on the tutor practice keyboard (FR-TUT-11, issue #135): [PracticeKeyboardView] with the
 * personal module's guide, synthetic touches. Before #135 the practice keyboard had no guide at all.
 *
 * View: 357 × 288 dp → 51 × 48 dp cells (row height = the default setting, 6 rows); ㄱ = col 4 row 2, ㄴ = col 2 row 3.
 * The guide shows on every consonant key (the anchor-key rule went with issue #168).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
@LooperMode(LooperMode.Mode.PAUSED)
class SwipePracticeGuideTest {

    private val th = ThresholdDefaults.VALUES
    private val looper get() = shadowOf(Looper.getMainLooper())
    private val strokes = ArrayList<RawStroke>()
    private lateinit var view: PracticeKeyboardView
    private lateinit var touch: Touches
    private var density = 1f

    private fun open(layout: PracticeLayout, settings: Settings = Settings.DEFAULT) {
        val keys = PracticeLayouts.of(layout, SwipeEngineModule)
        view = PracticeKeyboardView(RuntimeEnvironment.getApplication(), keys, settings.rowHeightDp, { raw, _ -> strokes += raw }) {
            PracticeLayouts.guide(layout, SwipeEngineModule, it, th, settings) { v -> if (v.span >= th.longDp) "ㅣ" else "ㆍ" }
        }
        density = view.resources.displayMetrics.density
        layoutExactly(view, Math.round(WIDTH * density), Math.round(SwipeLayout.ROWS * ROW * density))
        touch = Touches(view)
    }

    private fun cx(col: Int) = (col + 0.5f) * COL * density
    private fun cy(row: Int) = (row + 0.5f) * ROW * density
    private fun dp(v: Float) = v * density

    private fun overlay(): GuideOverlay = (checkNotNull(view.guide) as SwipePracticeGuide).overlay

    private fun phase(): GuidePhase =
        (GuideOverlay::class.java.getDeclaredField("logic").apply { isAccessible = true }.get(overlay()) as GuideTimerLogic).phase

    private fun progress(): ProgressState? =
        GuideOverlay::class.java.getDeclaredField("ps").apply { isAccessible = true }.get(overlay()) as ProgressState?

    private val all = GuideView(showDirection = true, showCurrent = true, showNext = true)
    private val none = GuideView(showDirection = false, showCurrent = false, showNext = false)

    @Test
    fun `PG-01 a practice stroke shows the keyboard's guide and the stroke still reaches the tutor`() {
        open(PracticeLayout.SWIPE_CONSONANT_KEY)
        assertNotNull(view.guide)
        touch.down(0, cx(N_COL), cy(N_ROW))
        assertEquals(GuidePhase.PRESSED_WAITING, phase())
        touch.move(0, cx(N_COL), cy(N_ROW) + dp(60f), steps = 9)   // moved past the slop: shown at once (FR-DSP-01)
        assertEquals(GuidePhase.SHOWING, phase())
        assertEquals(all, overlay().view)
        assertEquals("ㅣ", progress()?.current)   // ↓ long: what the stroke would give (SWIPE 9.2, issue #173)
        touch.up(0)
        assertEquals(GuidePhase.IDLE, phase())
        assertEquals(none, overlay().view)
        assertEquals(1, strokes.size)   // samples and the "입력: …" line are unchanged (FR-TUT-09)
    }

    @Test
    fun `PG-02 resting on a double-consonant key keeps the plain consonant (no hold, issue #142)`() {
        open(PracticeLayout.SWIPE_CONSONANT_KEY)
        touch.down(0, cx(G_COL), cy(G_ROW))
        looper.idleFor(Duration.ofMillis(th.guideDelayMs + 1))   // show delay: shown without a move
        assertEquals(GuidePhase.SHOWING, phase())
        assertEquals("ㄱ", progress()?.current)
        looper.idleFor(Duration.ofMillis(900))   // a long rest: no hold timer
        assertEquals("ㄱ", progress()?.current)
        touch.up(0)
        assertEquals(none, overlay().view)
    }

    @Test
    fun `PG-03 a short tap shows nothing (FR-DSP-07)`() {
        open(PracticeLayout.SWIPE_SENTENCE)
        touch.tap(0, cx(N_COL), cy(N_ROW))
        looper.idleFor(Duration.ofMillis(2_000))
        assertEquals(GuidePhase.IDLE, phase())
        assertEquals(none, overlay().view)
    }

    @Test
    fun `PG-04 all three guide settings off show no guide, the stroke still counts (FR-DSP-08)`() {
        open(PracticeLayout.SWIPE_CONSONANT_KEY, Settings.DEFAULT.copy(showDirectionGuide = false, showNextChar = false, showCurrentChar = false))
        touch.down(0, cx(N_COL), cy(N_ROW))
        touch.move(0, cx(N_COL), cy(N_ROW) - dp(30f), steps = 6)
        assertEquals(none, overlay().view)
        touch.up(0)
        assertEquals(1, strokes.size)
    }

    @Test
    fun `PG-05 each guide follows its own setting`() {
        open(PracticeLayout.SWIPE_CONSONANT_KEY, Settings.DEFAULT.copy(showCurrentChar = false))
        touch.down(0, cx(N_COL), cy(N_ROW))
        touch.move(0, cx(N_COL), cy(N_ROW) - dp(30f), steps = 6)
        assertEquals(GuideView(showDirection = true, showCurrent = false, showNext = true), overlay().view)
        touch.up(0)
    }

    @Test
    fun `PG-07 every consonant key and the double keys show the guide (FR-DSP-01, issue #168)`() {
        open(PracticeLayout.SWIPE_CONSONANT_KEY)
        touch.down(0, cx(G_COL), cy(G_ROW - 1))   // ㄲ, above ㄱ
        looper.idleFor(Duration.ofMillis(th.guideDelayMs + 1))
        assertEquals(GuidePhase.SHOWING, phase())
        assertEquals("ㄲ", progress()?.current)
        touch.up(0)
        assertEquals(1, strokes.size)
    }

    @Test
    fun `GT-10 GuideFlags - settings switch each guide, a password field also turns the current character off, a no-learning field does not`() {
        val text = FieldInfo(FieldClass.TEXT, isPassword = false, noLearning = false, multiLine = false, enterAction = null)
        assertEquals(GuideFlags(true, true, true), GuideFlags.of(Settings.DEFAULT, text))
        assertEquals(GuideFlags(true, true, true), GuideFlags.of(Settings.DEFAULT, null))   // the tutor
        val off = Settings.DEFAULT.copy(showDirectionGuide = false, showNextChar = false, showCurrentChar = false)
        assertEquals(GuideFlags(false, false, false), GuideFlags.of(off, text))
        assertEquals(GuideFlags(false, false, false), GuideFlags.of(off, null))
        assertFalse(GuideFlags.of(Settings.DEFAULT, text.copy(isPassword = true)).bigCurrentOn)   // FR-CMN-08 ①, whatever the setting
        assertTrue(GuideFlags.of(Settings.DEFAULT, text.copy(noLearning = true)).bigCurrentOn)   // ② shown as usual (owner 2026-11-01)
        assertTrue(GuideFlags.of(Settings.DEFAULT, text.copy(noLearning = true)).nextCharOn)
    }

    private companion object {
        const val WIDTH = 357f
        const val ROW = 48f
        const val COL = WIDTH / SwipeLayout.COLS
        const val G_COL = 4
        const val G_ROW = 2
        const val N_COL = 2
        const val N_ROW = 3
    }
}
