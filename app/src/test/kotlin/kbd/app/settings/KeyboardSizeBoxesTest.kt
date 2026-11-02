package kbd.app.settings

import android.content.ComponentName
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import kbd.app.store.Settings
import kbd.app.view.ViewTestSdk
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * The settings screen's key size and alignment, one box per orientation (LLD-07 §7, FSS#44 owner decision): each box
 * holds its own key size and alignment, both boxes are shown, and "지금 화면" marks the present orientation's box.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
class KeyboardSizeBoxesTest {

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

    private var orientation by mutableIntStateOf(Configuration.ORIENTATION_PORTRAIT)

    private fun show() {
        compose.setContent {
            val config = Configuration(LocalConfiguration.current).also { it.orientation = orientation }
            CompositionLocalProvider(LocalConfiguration provides config) {
                Column { KeyboardSizeBoxes(Settings.DEFAULT) {} }
            }
        }
    }

    private fun inBox(tag: String, text: String) = hasText(text) and hasAnyAncestor(hasTestTag(tag))

    @Test
    fun `OB-01 each box holds its own key size and alignment, both shown`() {
        show()
        for (tag in listOf(BOX_PORTRAIT, BOX_LANDSCAPE)) {
            compose.onNode(hasTestTag(tag)).assertExists()
            compose.onNode(inBox(tag, "키 크기")).assertExists()
            compose.onNode(inBox(tag, "정렬")).assertExists()
            compose.onNode(inBox(tag, "왼손모드")).assertExists()
        }
        compose.onNode(inBox(BOX_PORTRAIT, "세로 화면")).assertExists()
        compose.onNode(inBox(BOX_LANDSCAPE, "가로 화면")).assertExists()
    }

    @Test
    fun `OB-02 "지금 화면" marks the present orientation's box and follows a rotation`() {
        show()
        compose.onNode(inBox(BOX_PORTRAIT, "지금 화면")).assertExists()
        compose.onNode(inBox(BOX_LANDSCAPE, "지금 화면")).assertDoesNotExist()

        orientation = Configuration.ORIENTATION_LANDSCAPE
        compose.waitForIdle()
        compose.onNode(inBox(BOX_LANDSCAPE, "지금 화면")).assertExists()
        compose.onNode(inBox(BOX_PORTRAIT, "지금 화면")).assertDoesNotExist()

        orientation = Configuration.ORIENTATION_PORTRAIT
        compose.waitForIdle()
        compose.onNode(inBox(BOX_PORTRAIT, "지금 화면")).assertExists()
        compose.onNode(inBox(BOX_LANDSCAPE, "지금 화면")).assertDoesNotExist()
    }
}
