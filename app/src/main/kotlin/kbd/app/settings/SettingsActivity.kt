package kbd.app.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings as SystemSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kbd.app.R
import kbd.app.store.RowHeight
import kbd.app.store.Settings
import kbd.app.practice.PracticeActivity
import kbd.app.practice.PracticeLaunchPolicy
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Light or dark Material colours following the system (no colour rule in the LLD; UI self-check). */
@Composable
fun KeyboardAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(), content = content)
}

/**
 * The only choices (FR-SET-05, owner decision #108 A, LLD-07 §6.1): letters : numbers 1 : 1, 2 : 1 (default 2/3), 3 : 1,
 * stored as the letter share of the pad width.
 */
internal val GRAFFITI_RATIOS = listOf(
    1f / 2f to R.string.settings_graffiti_ratio_1_1,
    2f / 3f to R.string.settings_graffiti_ratio_2_1,
    3f / 4f to R.string.settings_graffiti_ratio_3_1,
)

/**
 * The launcher screen (LLD-07 §7). On the first open it opens the input practice's topic list once (FR-TUT-01, LLD-07
 * §2.5, D-34); afterwards "입력 연습" is the only way to it (FR-TUT-06). No threshold status, no threshold menus
 * (FSS#17: the values are fixed code constants).
 */
class SettingsActivity : ComponentActivity() {

    private lateinit var stores: AppStores

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stores = AppStores(this)
        lifecycleScope.launch {
            // The flag is stored before the list opens (D-34): closing it never opens it again.
            PracticeLaunchPolicy.onAppOpen(stores.settings.flow.first(), { stores.settings.setPracticeListShown(true) }, ::openPractice)
        }
        setContent {
            KeyboardAppTheme {
                SettingsScreen(
                    settings = stores.settings.flow.collectAsState(Settings.DEFAULT).value,
                    update = { f -> lifecycleScope.launch { stores.settings.update(f) } },
                    actions = SettingsActions(
                        openImeSettings = { startActivity(Intent(SystemSettings.ACTION_INPUT_METHOD_SETTINGS)) },
                        openPractice = ::openPractice,
                    ),
                )
            }
        }
    }

    private fun openPractice() {
        startActivity(Intent(this, PracticeActivity::class.java))
    }
}

/** Buttons of the settings screen that leave it. */
class SettingsActions(
    val openImeSettings: () -> Unit,
    val openPractice: () -> Unit,
)

/** The LLD-07 §7 items in order. */
@Composable
private fun SettingsScreen(settings: Settings, update: ((Settings) -> Settings) -> Unit, actions: SettingsActions) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))

            Section(R.string.settings_section_input)
            ActionRow(R.string.settings_ime, R.string.settings_ime_summary, actions.openImeSettings)

            Section(R.string.settings_section_display)   // every build has the swipe guides (issue #251)
            SwitchRow(R.string.settings_direction_guide, settings.showDirectionGuide) { v -> update { it.copy(showDirectionGuide = v) } }
            SwitchRow(R.string.settings_next_char, settings.showNextChar) { v -> update { it.copy(showNextChar = v) } }
            SwitchRow(R.string.settings_current_char, settings.showCurrentChar) { v -> update { it.copy(showCurrentChar = v) } }   // FR-DSP-08, #135

            Section(R.string.settings_section_feedback)
            SwitchRow(R.string.settings_haptic, settings.haptic) { v -> update { it.copy(haptic = v) } }
            SwitchRow(R.string.settings_sound, settings.sound) { v -> update { it.copy(sound = v) } }

            Section(R.string.settings_section_layout)
            RowHeightRow(settings.rowHeightDp) { v -> update { it.copy(rowHeightDp = v) } }
            GraffitiRatioRow(settings.graffitiLetterRatio) { v -> update { it.copy(graffitiLetterRatio = v) } }

            Section(R.string.settings_section_practice)
            ActionRow(R.string.settings_practice_open, R.string.settings_practice_open_summary, actions.openPractice)   // FR-TUT-06, FR-SET-04
        }
    }
}

@Composable
private fun Section(title: Int) {
    HorizontalDivider(Modifier.padding(top = 8.dp))
    Text(
        stringResource(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ActionRow(title: Int, summary: Int?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
        summary?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun SwitchRow(title: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(checked, role = Role.Switch) { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** 40–64 dp in 1 dp steps (FR-SET-06, TBD-16); stored when the slider is released. */
@Composable
private fun RowHeightRow(stored: Float, onChange: (Float) -> Unit) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging ?: stored
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_row_height), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.settings_row_height_value, shown.roundToInt()))
        }
        Slider(
            value = shown,
            onValueChange = { dragging = it.roundToInt().toFloat() },
            onValueChangeFinished = { dragging?.let { onChange(RowHeight.clamp(it)) }; dragging = null },
            valueRange = RowHeight.MIN_DP..RowHeight.MAX_DP,
            steps = (RowHeight.MAX_DP - RowHeight.MIN_DP).toInt() - 1,
        )
    }
}

@Composable
private fun GraffitiRatioRow(stored: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(R.string.settings_graffiti_ratio), style = MaterialTheme.typography.bodyLarge)
        GRAFFITI_RATIOS.forEach { (ratio, label) ->
            val selected = kotlin.math.abs(stored - ratio) < RATIO_EPS
            Row(
                Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton) { onChange(ratio) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected, onClick = null)
                Text(stringResource(label), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

private const val RATIO_EPS = 1e-3f
