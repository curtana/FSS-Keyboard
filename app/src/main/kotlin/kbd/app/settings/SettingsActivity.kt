package kbd.app.settings

import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kbd.app.R
import kbd.app.store.KeyboardAlign
import kbd.app.store.KeyboardWidth
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

            Section(R.string.settings_section_modes)   // FR-SET-08: the first group
            Text(
                stringResource(R.string.settings_modes_summary), style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            // Mode-key cycle order (FR-SET-08, FR-CMN-03 ②)
            CheckRow(R.string.settings_mode_dubeolsik, checked = true, onChange = null)
            CheckRow(R.string.settings_mode_qwerty, checked = true, onChange = null)
            CheckRow(R.string.settings_mode_swipe, settings.swipeEnabled) { v -> update { it.copy(swipeEnabled = v) } }
            CheckRow(R.string.settings_mode_graffiti, settings.graffitiEnabled) { v -> update { it.copy(graffitiEnabled = v) } }

            Section(R.string.settings_section_input)
            ActionRow(R.string.settings_ime, R.string.settings_ime_summary, actions.openImeSettings)

            Section(R.string.settings_section_display)   // every build has the swipe guides (issue #251)
            SwitchRow(R.string.settings_direction_guide, settings.showDirectionGuide) { v -> update { it.copy(showDirectionGuide = v) } }
            SwitchRow(R.string.settings_next_char, settings.showNextChar) { v -> update { it.copy(showNextChar = v) } }
            SwitchRow(R.string.settings_current_char, settings.showCurrentChar) { v -> update { it.copy(showCurrentChar = v) } }   // FR-DSP-08, #135
            SwitchRow(R.string.settings_stroke_trail, settings.showStrokeTrail) { v -> update { it.copy(showStrokeTrail = v) } }   // FR-DSP-09, FSS#57

            Section(R.string.settings_section_feedback)
            SwitchRow(R.string.settings_haptic, settings.haptic) { v -> update { it.copy(haptic = v) } }
            SwitchRow(R.string.settings_sound, settings.sound) { v -> update { it.copy(sound = v) } }

            Section(R.string.settings_section_layout)
            RowHeightRow(settings.rowHeightDp) { v -> update { it.copy(rowHeightDp = v) } }
            KeyboardSizeBoxes(settings, update)   // FR-SET-09, FSS#25, FSS#44
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

/** A mode check box (FR-SET-08); [onChange] null = always on, checked and disabled with "늘 켜짐". */
@Composable
private fun CheckRow(title: Int, checked: Boolean, onChange: ((Boolean) -> Unit)?) {
    val base = Modifier.fillMaxWidth()
    val row = if (onChange == null) base else base.toggleable(checked, role = Role.Checkbox) { onChange(it) }
    Row(row.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = onChange != null)
        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp).weight(1f))
        if (onChange == null) Text(stringResource(R.string.settings_mode_always_on), style = MaterialTheme.typography.bodySmall)
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

private val ALIGNS = listOf(
    KeyboardAlign.LEFT to R.string.settings_align_left,
    KeyboardAlign.CENTER to R.string.settings_align_center,
    KeyboardAlign.RIGHT to R.string.settings_align_right,
)

/**
 * Key size and alignment of the portrait and the landscape screen (FR-SET-09, LLD-07 §7, FSS#25), in two boxes:
 * "세로 화면" and "가로 화면", each with its key size and its alignment (owner decision, FSS#44). The box of the screen's
 * present orientation is marked "지금 화면"; both are always shown and can be changed. Each slider goes from
 * 32 dp in 1 dp steps to the first key size whose keyboard rectangle (11 cells: the keys and the two side margins,
 * FSS#84) fills about this screen's width in that orientation (its short or
 * long side; the keyboard computes the exact width, FR-CMN-14 ④); that top stop is "꽉 채움": no key size on the
 * portrait screen, [KeyboardWidth.FULL] on the landscape one. Until it is moved the landscape slider sits at the
 * portrait key size (FR-CMN-14 ③). The alignment is greyed out when the keyboard fills (no rest to place).
 * Under the alignment each box has its "띄우기" switch (FR-CMN-15, FSS#86, default off); the alignment can be chosen
 * whether it is on or off.
 */
@Composable
internal fun KeyboardSizeBoxes(settings: Settings, update: ((Settings) -> Settings) -> Unit) {
    val landscapeNow = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val dm = LocalContext.current.resources.displayMetrics
    val shortDp = minOf(dm.widthPixels, dm.heightPixels) / dm.density
    val longDp = maxOf(dm.widthPixels, dm.heightPixels) / dm.density
    val pTop = KeyboardWidth.fullKeyDp(shortDp)
    val lTop = KeyboardWidth.fullKeyDp(longDp)
    val p = settings.portraitWidth
    val l = settings.landscapeWidth
    // The slider position of a key size: a whole dp, the full mark and anything that fills at the top.
    fun stop(k: Float?, top: Int): Int = if (k == null || k == KeyboardWidth.FULL) top else k.roundToInt().coerceAtMost(top)
    val pKey = stop(p.keyDp, pTop)
    val lKey = when {
        l.keyDp != null -> stop(l.keyDp, lTop)
        p.keyDp == null || p.keyDp == KeyboardWidth.FULL -> (shortDp / KeyboardWidth.RECT_UNITS).roundToInt().coerceIn(KeyboardWidth.MIN_KEY_DP.toInt(), lTop)
        else -> stop(p.keyDp, lTop)
    }
    OrientationBox(R.string.settings_box_portrait, now = !landscapeNow, BOX_PORTRAIT) {
        KeyRow(pKey, pTop, shortDp) { k -> update { it.copy(portraitWidth = it.portraitWidth.copy(keyDp = if (k >= pTop) null else k.toFloat())) } }
        AlignRow(p.align, enabled = pKey < pTop) { a -> update { it.copy(portraitWidth = it.portraitWidth.copy(align = a)) } }
        FloatRow(settings.portraitFloat.on, FLOAT_PORTRAIT) { v -> update { it.copy(portraitFloat = it.portraitFloat.copy(on = v)) } }
    }
    OrientationBox(R.string.settings_box_landscape, now = landscapeNow, BOX_LANDSCAPE) {
        KeyRow(lKey, lTop, longDp) { k -> update { it.copy(landscapeWidth = it.landscapeWidth.copy(keyDp = if (k >= lTop) KeyboardWidth.FULL else k.toFloat())) } }
        AlignRow(l.align, enabled = lKey < lTop) { a -> update { it.copy(landscapeWidth = it.landscapeWidth.copy(align = a)) } }
        FloatRow(settings.landscapeFloat.on, FLOAT_LANDSCAPE) { v -> update { it.copy(landscapeFloat = it.landscapeFloat.copy(on = v)) } }
    }
}

/** Test tags of the two boxes (FSS#44). */
internal const val BOX_PORTRAIT = "box_portrait"
internal const val BOX_LANDSCAPE = "box_landscape"

/** Test tags of the two "띄우기" switches (FSS#86). */
internal const val FLOAT_PORTRAIT = "float_portrait"
internal const val FLOAT_LANDSCAPE = "float_landscape"

/** One orientation's box: its title, "지금 화면" at the right end when [now], then [content] (FSS#44). */
@Composable
private fun OrientationBox(title: Int, now: Boolean, tag: String, content: @Composable () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).testTag(tag)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (now) Text(stringResource(R.string.settings_now), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        content()
    }
}

/**
 * A key size slider from 32 dp to [top] (= fills about [availDp]) with the width of the keyboard rectangle (side
 * margins included, 11 × the key size) beside it: "키 36 dp · 전체 396 dp", or at the top "꽉 채움 · 키 약 65 dp · 711 dp".
 * Stored when the slider is released.
 */
@Composable
private fun KeyRow(stored: Int, top: Int, availDp: Float, onChange: (Int) -> Unit) {
    var dragging by remember { mutableStateOf<Int?>(null) }
    val shown = dragging ?: stored
    val min = KeyboardWidth.MIN_KEY_DP.toInt()
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_key), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                if (shown >= top) stringResource(R.string.settings_key_full, (availDp / KeyboardWidth.RECT_UNITS).roundToInt(), availDp.roundToInt())
                else stringResource(R.string.settings_key_value, shown, shown * KeyboardWidth.RECT_UNITS),
            )
        }
        Slider(
            value = shown.toFloat(),
            onValueChange = { dragging = it.roundToInt().coerceIn(min, top) },
            onValueChangeFinished = { dragging?.let(onChange); dragging = null },
            valueRange = min.toFloat()..maxOf(top, min + 1).toFloat(),
            steps = maxOf(top - min - 1, 0),
            enabled = top > min,   // a screen of 320 dp or less: the keyboard fills anyway
        )
    }
}

/** 왼손모드 · 중앙모드 · 오른손모드 in one row. */
@Composable
private fun AlignRow(stored: KeyboardAlign, enabled: Boolean, onChange: (KeyboardAlign) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.settings_align), style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA),
        )
        Row(Modifier.fillMaxWidth()) {
            ALIGNS.forEach { (a, label) ->
                val selected = stored == a
                Row(
                    Modifier.weight(1f).selectable(selected, enabled = enabled, role = Role.RadioButton) { onChange(a) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected, onClick = null, enabled = enabled)
                    Text(
                        stringResource(label), modifier = Modifier.padding(start = 4.dp),
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA),
                    )
                }
            }
        }
    }
}

/**
 * The "띄우기" switch of one orientation's box, under its alignment, with a line on how the keyboard is moved and what
 * the alignment then means (FR-SET-09, FR-CMN-15 ①⑥, FSS#86). Only the switch is stored here: the place stays as it is.
 */
@Composable
private fun FloatRow(checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Column(
        Modifier.fillMaxWidth().testTag(tag).selectable(checked, role = Role.Switch) { onChange(!checked) }
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_float), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = null)
        }
        Text(stringResource(R.string.settings_float_summary), style = MaterialTheme.typography.bodySmall)
    }
}

/** Material's disabled content alpha. */
private const val DISABLED_ALPHA = 0.38f

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
