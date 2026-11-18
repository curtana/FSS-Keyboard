package kbd.app.settings

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

/** FSS#140, LLD-11 BI-05: the "버전" item is the settings screen's last item, read only, named once. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
class SettingsVersionItemTest {

    val compose = createComposeRule()

    /** The rule's host `ComponentActivity` is in no manifest of the app (no test manifest library in the APK). */
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
    fun `BI-05 the version item is last, read only, and named once`() {
        val text = "0.1.0-beta (4)\n개발 · 47a8398 · 2026-11-18"
        compose.setContent { SettingsScreen(Settings.DEFAULT, {}, SettingsActions({}, {}), versionText = text) }

        val node = compose.onNode(hasTestTag(VERSION_TAG) and hasText(text))
        node.performScrollTo().assertIsDisplayed()
        compose.onNode(hasTestTag(VERSION_TAG) and hasClickAction()).assertDoesNotExist()
        assertEquals(1, compose.onAllNodesWithText("버전").fetchSemanticsNodes().size)

        // Nothing below it: its bottom is the lowest of all text on the screen.
        val all = compose.onAllNodes(hasText("", substring = true)).fetchSemanticsNodes()
        val bottom = node.fetchSemanticsNode().boundsInRoot.bottom
        assertTrue(all.all { it.boundsInRoot.bottom <= bottom })
    }
}
