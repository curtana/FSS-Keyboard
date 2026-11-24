package kbd.app.settings

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import kbd.app.store.Settings
import kbd.app.view.ViewTestSdk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/** LLD-11 §7.12 (카) SE-01 (SRS FR-SET-11, LLD-12 §11, LLD-07 §7, FSS#73): the "단어 후보" group of the settings screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
class SettingsSuggestTest {

    val compose = createComposeRule()

    /** The rule's host `ComponentActivity` is in no manifest of the app (as SettingsVersionItemTest). */
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                val app = RuntimeEnvironment.getApplication()
                Shadows.shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
                base.evaluate()
            }
        }
    }.around(compose)

    @Test
    fun `SE-01 the word suggestion group right after the modes - one switch, off, and its summary, a press turns it on`() {
        val updates = ArrayList<(Settings) -> Settings>()
        compose.setContent { SettingsScreen(Settings.DEFAULT, { updates += it }, SettingsActions({}, {})) }

        fun top(text: String): Float = compose.onNodeWithText(text).fetchSemanticsNode().positionInRoot.y
        val switch = compose.onNode(hasTestTag(WORD_SUGGEST_TAG))
        switch.assertIsNotSelected()
        val switchTop = switch.fetchSemanticsNode().positionInRoot.y
        val summary = "영문 쿼티 자판 위에 단어 후보 한 줄을 둡니다. 켜면 키보드가 한 줄 높아집니다."
        // In order: the modes group and its last row, then this group, its switch and summary, then "입력기".
        val order = listOf(
            top("입력 모드"), top("그래피티(한 획 손글씨)"), top("단어 후보"), switchTop, top(summary), top("입력기"),
        )
        assertEquals(order.sorted(), order)
        assertEquals(order.size, order.toSet().size)
        compose.onNodeWithTag(WORD_SUGGEST_TAG).assertExists()
        compose.onNodeWithText("영문 단어 후보").assertExists()

        switch.performScrollTo().performClick()
        assertEquals(1, updates.size)
        assertTrue(updates[0](Settings.DEFAULT).wordSuggest)
        assertEquals(Settings.DEFAULT.copy(wordSuggest = true), updates[0](Settings.DEFAULT))
    }
}
