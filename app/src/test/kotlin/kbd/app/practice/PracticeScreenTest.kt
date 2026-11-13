package kbd.app.practice

import android.content.ComponentName
import android.media.AudioManager
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.io.File
import kbd.app.feedback.Feedback
import kbd.app.graffiti.GraffitiGuideLayout
import kbd.app.graffiti.GraffitiGuidePanel
import kbd.app.graffiti.GraffitiPadGeometry
import kbd.app.graffiti.GraffitiPadView
import kbd.app.store.ErrorLog
import kbd.app.store.KeyboardAlign
import kbd.app.store.Settings
import kbd.app.store.WidthSetting
import kbd.app.swipe.impl.SwipeGeometry
import kbd.app.swipe.impl.SwipeKeyboardView
import kbd.app.view.Touches
import kbd.app.view.ViewTestSdk
import kbd.graffiti.guide.GuideCatalog
import kbd.graffiti.guide.GuideSection
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.TemplateSet
import kbd.stroke.layout.FunctionKey
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * LLD-11 PR-23 (FSS#54): the practice screen itself — the real topic list, explanation, item lines and swipe board —
 * with the owner's tablet strokes (key 42 dp, right aligned, 711 dp wide). 가 then the space is ✓: the solved item
 * stays on screen with its ✓ for [PracticeFlow.RIGHT_HOLD_MS], taking no input, then the next item's target is shown.
 * PR-28, PR-29 (FSS#72): the graffiti items on the real pad and stroke guide, written with touches.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK], qualifiers = "w711dp-h1138dp-port")
class PracticeScreenTest {

    private val tmp = TemporaryFolder()
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The rule's host `ComponentActivity` is in no manifest of the app (as KeyboardSizeBoxesTest). */
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(tmp).around { base, _ ->
        object : Statement() {
            override fun evaluate() {
                val app = RuntimeEnvironment.getApplication()
                Shadows.shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
                base.evaluate()
            }
        }
    }.around(compose)

    private val settings = Settings.DEFAULT.copy(rowHeightDp = 42f, portraitWidth = WidthSetting(42f, KeyboardAlign.RIGHT))

    private fun <T : View> find(v: View, c: Class<T>): T? {
        if (c.isInstance(v)) return c.cast(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i), c)?.let { return it }
        return null
    }

    @Test
    fun `PR-23 가 then the space is shown right, then the next target (FSS#54)`() {
        val flow = PracticeFlow()
        compose.setContent {
            val fb = Feedback({ null }, compose.activity.getSystemService(AudioManager::class.java), { settings })
            val boards = BoardFactory(settings, fb, ErrorLog(File(tmp.root, "e.log")), TemplateSet(emptyMap()))
            PracticeScreen(flow, flowOf(settings), settings, boards) {}
        }
        compose.onNodeWithText(PracticeTopics.SWIPE.title).performClick()
        compose.onNodeWithText("시작").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("쓸 것: 가").assertExists()

        val sv = checkNotNull(find(compose.activity.window.decorView, SwipeKeyboardView::class.java))
        val frame = sv.parent as View
        val d = sv.resources.displayMetrics.density
        val geo = SwipeGeometry.build(sv.width / d, settings.rowHeightDp)
        val t = Touches(frame)
        fun idle() {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (!compose.mainClock.autoAdvance) compose.mainClock.advanceTimeByFrame()   // one frame: the screen redraws
            compose.waitForIdle()
        }
        fun stroke(x0: Float, y0: Float, x1: Float, y1: Float) {
            t.down(0, sv.left + x0 * d, y0 * d); t.move(0, sv.left + x1 * d, y1 * d, steps = 8); t.up(0); idle()
        }
        fun tap(label: String) = geo.geoms[geo.keys.indexOfFirst { it.label == label }].rect.let {
            t.tap(0, sv.left + (it.left + it.right) / 2 * d, (it.top + it.bottom) / 2 * d); idle()
        }
        compose.mainClock.autoAdvance = false
        tap("ㄱ")
        stroke(250.7f, 67.6f, 262.2f, 152.4f)   // the tablet's ㅣ (FSSStroke, 2026-11-03 20:54:01)
        stroke(264.9f, 105.8f, 302.2f, 95.1f)   // and its dot: 가
        assertEquals(null, flow.verdict)
        tap("스페이스")
        assertEquals(Verdict.RIGHT, flow.verdict)
        compose.onNodeWithText("쓸 것: 가").assertExists()        // the solved item stays, with its ✓
        compose.onNodeWithText("✓").assertExists()
        tap("ㄱ")                                                    // no input while it is shown
        assertEquals(0, flow.index)
        compose.mainClock.advanceTimeBy(PracticeFlow.RIGHT_HOLD_MS + 100)
        idle()
        assertEquals(1, flow.index)
        compose.onNodeWithText("쓸 것: 고").assertExists()        // the next target is drawn (the bug: 가 stayed)
        compose.onNodeWithText("✓").assertDoesNotExist()
    }

    /** FSS#84: the practice board has the keyboard's side margins; a touch that begins on one is no action. */
    @Test
    fun `PR-31 the practice board sits on the keys of the keyboard rectangle, the side margins take touches with no action`() {
        val flow = PracticeFlow()
        compose.setContent {
            val fb = Feedback({ null }, compose.activity.getSystemService(AudioManager::class.java), { settings })
            PracticeScreen(flow, flowOf(settings), settings, BoardFactory(settings, fb, ErrorLog(File(tmp.root, "e.log")), TemplateSet(emptyMap()))) {}
        }
        compose.onNodeWithText(PracticeTopics.SWIPE.title).performClick()
        compose.onNodeWithText("시작").performClick()
        compose.waitForIdle()
        val sv = checkNotNull(find(compose.activity.window.decorView, SwipeKeyboardView::class.java))
        val frame = sv.parent as View
        val d = sv.resources.displayMetrics.density
        // 42 dp keys, right aligned: rectangle 462 dp at the right end, margins 21 dp, keys 420 dp.
        val m = Math.round(21 * d)
        assertEquals(Math.round(420 * d), sv.width)
        assertEquals(frame.width - m, sv.right)
        assertEquals(frame.width - Math.round(462 * d) + m, sv.left)
        assertEquals(m, sv.sideMarginPx)
        assertEquals(m / d, sv.geometrySideDp)
        val t = Touches(frame)
        val tries = flow.tryNo
        for (x in listOf(sv.left - 1f, sv.left - m + 0f, sv.right + 0f, frame.width - 1f)) {
            t.down(0, x, sv.height / 2f); t.move(0, sv.left + sv.width / 2f, sv.height / 2f); t.up(0)
            assertTrue(t.lastHandled)
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        assertEquals("", flow.echo)
        assertEquals(null, flow.verdict)
        assertEquals(tries, flow.tryNo)
    }

    /** FSS#84: the graffiti pad inside the practice column is told the side margin too. */
    @Test
    fun `PR-32 the graffiti practice pad gets the side margin`() {
        openAt(PracticeTopics.GRAFFITI_EN, "a")
        val pad = padNow()
        val d = pad.resources.displayMetrics.density
        assertEquals(Math.round(21 * d), pad.sideMarginPx)
        assertEquals(Math.round(420 * d), pad.width)
    }

    private val templates: TemplateSet by lazy {
        TemplateLoader::class.java.getResourceAsStream("/graffiti/templates.json")!!.use { TemplateLoader.load(it) }
    }

    /** The practice screen at item [target] of [topic], the earlier items solved, the clock held for the ✓ (as PR-23). */
    private fun openAt(topic: PracticeTopic, target: String): PracticeFlow {
        val flow = PracticeFlow()
        compose.setContent {
            val fb = Feedback({ null }, compose.activity.getSystemService(AudioManager::class.java), { settings })
            PracticeScreen(flow, flowOf(settings), settings, BoardFactory(settings, fb, ErrorLog(File(tmp.root, "e.log")), templates)) {}
        }
        compose.runOnIdle {
            flow.open(topic)
            flow.start()
            for (it in topic.items.takeWhile { i -> i.target != target }) { flow.onAction(it.target + " ", it.target); flow.advance() }
        }
        compose.waitForIdle()
        return flow
    }

    private fun padNow() = checkNotNull(find(compose.activity.window.decorView, GraffitiPadView::class.java))

    private fun panelNow() = checkNotNull(find(compose.activity.window.decorView, GraffitiGuidePanel::class.java))

    private fun idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    /** Draws the polyline [pts] (dp in the pad) with one finger on the board frame, as the screen gets it. */
    private fun draw(pts: List<Pair<Float, Float>>) {
        val pad = padNow()
        val column = pad.parent as View
        val d = pad.resources.displayMetrics.density
        val t = Touches(column.parent as View)
        fun x(p: Pair<Float, Float>) = column.left + p.first * d
        fun y(p: Pair<Float, Float>) = pad.top + p.second * d
        t.down(0, x(pts[0]), y(pts[0]))
        for (p in pts.drop(1)) t.move(0, x(p), y(p), steps = 2)
        t.up(0)
        idle()
    }

    /** Stroke 0 of template [id] in a 100 dp box of the letter area (as PracticeGraffitiItemsTest). */
    private fun drawTemplate(id: String) =
        draw(templates.sets.values.flatten().single { it.id == id }.strokes[0].map { (x, y) -> 20f + x * 100f to 20f + y * 100f })

    private fun pressSpace() {
        val pad = padNow()
        val column = pad.parent as View
        val d = pad.resources.displayMetrics.density
        val r = GraffitiPadGeometry(pad.width / d, pad.height / d, pad.letterRatio).keys.first { it.key == FunctionKey.SPACE }.rect
        Touches(column.parent as View).tap(0, column.left + (r.left + r.right) / 2 * d, pad.top + (r.top + r.bottom) / 2 * d)
        idle()
    }

    /** The scroll that brings [label]'s first cell to the top of the panel (LLD-05 §10.2). */
    private fun scrollFor(panel: GraffitiGuidePanel, section: GuideSection, label: String): Float {
        val d = panel.resources.displayMetrics.density
        val l = GraffitiGuideLayout(panel.width / d, GuideCatalog.groups.filter { it.section == section })
        return (l.cells.first { it.cell.label == label }.rect.top - GraffitiGuideLayout.PAD_DP).coerceIn(0f, l.maxScrollDp(panel.height / d))
    }

    @Test
    fun `PR-28 a double consonant on the real pad - without shift wrong and both cells marked, the shift stroke is no action, then right (FSS#72)`() {
        val flow = openAt(PracticeTopics.GRAFFITI_KO, "ㄸ")
        compose.onNodeWithText("쓸 것: ㄸ (시프트 + ㄷ)").assertExists()
        assertEquals(scrollFor(panelNow(), GuideSection.HANGUL, "ㄷ"), panelNow().scrollDp, 0.01f)   // the base cell is in view from the start
        compose.mainClock.autoAdvance = false
        drawTemplate("ko_d")                                              // no shift stroke: ㄷ, closed by the space, is ✗
        pressSpace()
        assertEquals(Verdict.WRONG, flow.verdict)
        assertEquals("ㄸ = 시프트(아래에서 위로) → ㄷ → 스페이스", flow.howTo)
        idle()
        compose.mainClock.advanceTimeByFrame()                            // the new try's keyboard is built
        compose.waitForIdle()
        assertEquals(setOf("ㄷ", "ㄲ"), panelNow().highlight)              // the consonant's cell and the guide's shift cell
        assertEquals(scrollFor(panelNow(), GuideSection.HANGUL, "ㄷ"), panelNow().scrollDp, 0.01f)
        draw(listOf(60f to 120f, 60f to 80f, 60f to 40f))                 // ↑: nothing shows, nothing is judged
        assertEquals(null, flow.verdict)
        assertEquals("", flow.echo)
        drawTemplate("ko_d")
        assertEquals("ㄸ", flow.echo)
        assertEquals(null, flow.verdict)                                  // composing: no result yet
        pressSpace()
        assertEquals(Verdict.RIGHT, flow.verdict)
        assertEquals("ㄸ", flow.echo)
    }

    @Test
    fun `PR-29 a letter deep in the guide is in view from the start, after a wrong one marked, and written it is right (FSS#72)`() {
        val flow = openAt(PracticeTopics.GRAFFITI_EN, "q")
        val want = scrollFor(panelNow(), GuideSection.ENGLISH, "Q")
        assertTrue(want > 0f)
        assertEquals(want, panelNow().scrollDp, 0.01f)
        assertEquals(emptySet<String>(), panelNow().highlight)            // unmarked before a ✗ (SWIPE 10.1 ③)
        compose.mainClock.autoAdvance = false
        val tries = flow.tryNo
        drawTemplate("en_a")
        assertEquals(Verdict.WRONG, flow.verdict)
        assertEquals("q = 안내표의 Q 칸처럼", flow.howTo)
        assertEquals(tries + 1, flow.tryNo)
        idle()
        compose.mainClock.advanceTimeByFrame()                            // the new try's keyboard is built
        compose.waitForIdle()
        assertEquals(setOf("Q"), panelNow().highlight)
        assertEquals(want, panelNow().scrollDp, 0.01f)
        drawTemplate("en_q")
        assertEquals(Verdict.RIGHT, flow.verdict)                         // English commits at once: no space
        assertEquals("q", flow.echo)
    }
}
