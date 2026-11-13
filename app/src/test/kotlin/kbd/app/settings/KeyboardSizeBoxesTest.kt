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
import kbd.app.store.KeyboardAlign
import kbd.app.store.WidthSetting
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

    private fun show(settings: Settings = Settings.DEFAULT) {
        compose.setContent {
            val config = Configuration(LocalConfiguration.current).also { it.orientation = orientation }
            CompositionLocalProvider(LocalConfiguration provides config) {
                Column { KeyboardSizeBoxes(settings) {} }
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

    /** FSS#84: the slider's top and the width beside it count the keyboard rectangle, 11 cells. */
    @Test
    @Config(qualifiers = "w411dp-h891dp-port")
    fun `OB-03 the width shown is the keyboard rectangle - 11 keys, the full mark at the screen width over 11`() {
        show()
        compose.onNode(inBox(BOX_PORTRAIT, "꽉 채움 · 키 약 37 dp · 411 dp")).assertExists()   // 411 / 11 = 37.4
        compose.onNode(inBox(BOX_LANDSCAPE, "키 37 dp · 전체 407 dp")).assertExists()          // the portrait key size, 11 × 37
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port")
    fun `OB-04 a stored key size shows 11 times its width, one that no longer fits shows the full mark`() {
        show(Settings.DEFAULT.copy(portraitWidth = WidthSetting(34f, KeyboardAlign.CENTER)))
        compose.onNode(inBox(BOX_PORTRAIT, "키 34 dp · 전체 374 dp")).assertExists()
        compose.onNode(inBox(BOX_LANDSCAPE, "키 34 dp · 전체 374 dp")).assertExists()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port")
    fun `OB-05 a key size stored when ten keys filled the screen shows the full mark`() {
        show(Settings.DEFAULT.copy(portraitWidth = WidthSetting(41f, KeyboardAlign.CENTER)))   // 11 × 41 > 411
        compose.onNode(inBox(BOX_PORTRAIT, "꽉 채움 · 키 약 37 dp · 411 dp")).assertExists()
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
