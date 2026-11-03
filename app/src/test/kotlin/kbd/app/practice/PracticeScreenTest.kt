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
import kbd.app.store.ErrorLog
import kbd.app.store.KeyboardAlign
import kbd.app.store.Settings
import kbd.app.store.WidthSetting
import kbd.app.swipe.impl.SwipeGeometry
import kbd.app.swipe.impl.SwipeKeyboardView
import kbd.app.view.Touches
import kbd.app.view.ViewTestSdk
import kbd.graffiti.recognize.TemplateSet
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
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
}
