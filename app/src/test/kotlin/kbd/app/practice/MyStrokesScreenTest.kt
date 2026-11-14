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
import java.util.concurrent.Executor
import kbd.app.feedback.Feedback
import kbd.app.graffiti.GraffitiGuidePanel
import kbd.app.graffiti.GraffitiRecognizers
import kbd.app.graffiti.GraffitiPadView
import kbd.app.store.ErrorLog
import kbd.app.store.KeyboardAlign
import kbd.app.store.PersonalStrokeStore
import kbd.app.store.Settings
import kbd.app.store.WidthSetting
import kbd.app.view.Touches
import kbd.app.view.ViewTestSdk
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.TemplateSet
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

/** LLD-11 §7.10 MV-01..MV-04 (LLD-07 §8.3, SRS FR-TUT-14·16): the "my strokes" screens in the real practice screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK], qualifiers = "w711dp-h1138dp-port")
class MyStrokesScreenTest {

    private val tmp = TemporaryFolder()
    val compose = createAndroidComposeRule<ComponentActivity>()

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
    private val templates: TemplateSet by lazy {
        TemplateLoader::class.java.getResourceAsStream("/graffiti/templates.json")!!.use { TemplateLoader.load(it) }
    }
    private lateinit var store: PersonalStrokeStore
    private lateinit var my: MyStrokesModel

    private fun open() {
        val log = ErrorLog(File(tmp.root, "e.log"))
        store = PersonalStrokeStore(File(tmp.root, PersonalStrokeStore.FILE_NAME), log)
        val sup = GraffitiRecognizers(templates, log, { store.revision }, { store.book().strokes }, executor = Executor { it.run() })
        my = MyStrokesModel(templates, store, sup)
        compose.setContent {
            val fb = Feedback({ null }, compose.activity.getSystemService(AudioManager::class.java), { settings })
            PracticeScreen(PracticeFlow(), flowOf(settings), settings, BoardFactory(settings, fb, log, templates, sup::get),
                my, MyStrokesBoard(settings, fb, log, templates, sup::get)) {}
        }
        compose.waitForIdle()
    }

    private fun <T : View> find(v: View, c: Class<T>): T? {
        if (c.isInstance(v)) return c.cast(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i), c)?.let { return it }
        return null
    }

    private fun idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    private fun padNow() = checkNotNull(find(compose.activity.window.decorView, GraffitiPadView::class.java))

    /** Draws the polyline [pts] (dp in the pad) with one finger on the board frame. */
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

    private fun drawTemplate(id: String) =
        draw(templates.sets.values.flatten().single { it.id == id }.strokes[0].map { (x, y) -> 20f + x * 100f to 20f + y * 100f })

    @Test
    fun `MV-01 the topic line is below the practice topics and opens the letter list`() {
        open()
        compose.onNodeWithText("내 획 학습").assertExists()
        compose.onNodeWithText("0글자").assertExists()
        compose.onNodeWithText("내 획 학습").performClick()
        idle()
        compose.onNodeWithText("한글 자음").assertExists()
        compose.onNodeWithText("영문").assertExists()
        compose.onNodeWithText("전체 초기화").assertExists()
        assertEquals(MyStrokesModel.Screen.LETTERS, my.screen)
    }

    @Test
    fun `MV-02 a letter's write screen has its guide cells and the real pad, a stroke there is stored and types nothing`() {
        open()
        compose.runOnIdle { my.open(); my.pick(my.letters.first { it.label == "ㄱ" }) }
        idle()
        compose.onNodeWithText("쓸 글자: ㄱ").assertExists()
        compose.onNodeWithText("저장된 획: 0 / 8").assertExists()
        assertTrue(find(compose.activity.window.decorView, GraffitiGuidePanel::class.java) != null)
        drawTemplate("ko_g")
        compose.onNodeWithText("저장된 획: 1 / 8").assertExists()
        compose.onNodeWithText("저장했어요. 저장하기 전 인식: ㄱ").assertExists()
        assertEquals(1, store.book().count(SetName.KO_LETTER, "ㄱ"))
    }

    @Test
    fun `MV-03 a straight line on the write screen is not stored and the screen says why`() {
        open()
        compose.runOnIdle { my.open(); my.pick(my.letters.first { it.label == "A" }) }
        idle()
        draw((0..10).map { 50f to 20f + it * 15f })
        compose.onNodeWithText("탭이나 직선 획은 저장하지 않아요. 이런 획은 글자보다 먼저 규칙으로 판정해요.").assertExists()
        assertEquals(0, store.book().strokes.size)
    }

    @Test
    fun `MV-04 relearn, reset and clear everything with its question`() {
        open()
        val g = my.letters.first { it.label == "ㄱ" }
        compose.runOnIdle { my.open(); my.pick(g) }
        idle()
        drawTemplate("ko_g")
        compose.onNodeWithText("다시 학습").performClick()
        idle()
        compose.onNodeWithText("이 글자의 획을 지웠어요. 새로 써 주세요.").assertExists()
        assertEquals(0, store.book().strokes.size)
        drawTemplate("ko_g")
        compose.onNodeWithText("초기화").performClick()
        idle()
        assertEquals(MyStrokesModel.Screen.LETTERS, my.screen)
        assertEquals(0, store.book().strokes.size)
        compose.runOnIdle { my.pick(g) }
        idle()
        drawTemplate("ko_g")
        compose.onNodeWithText("글자 목록").performClick()
        idle()
        compose.onNodeWithText("전체 초기화").performClick()
        idle()
        compose.onNodeWithText("저장한 내 획을 모두 지울까요? 기본 인식은 그대로예요.").assertExists()
        compose.onNodeWithText("취소").performClick()
        idle()
        assertEquals(1, store.book().strokes.size)
        compose.onNodeWithText("전체 초기화").performClick()
        idle()
        compose.onNodeWithText("지우기").performClick()
        idle()
        assertEquals(0, store.book().strokes.size)
    }
}
