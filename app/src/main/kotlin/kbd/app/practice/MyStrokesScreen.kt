package kbd.app.practice

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kbd.app.R
import kbd.app.feedback.FeedbackPort
import kbd.app.graffiti.GraffitiGuidePanel
import kbd.app.graffiti.GraffitiPadGeometry
import kbd.app.graffiti.GraffitiPadView
import kbd.app.ime.InputMode
import kbd.app.store.ErrorLog
import kbd.app.store.Settings
import kbd.app.view.TapSink
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.SetName
import kbd.graffiti.recognize.TemplateSet
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.tap.TapOutput
import kbd.stroke.threshold.ThresholdDefaults

/**
 * The "my strokes" line of the practice topic list (LLD-07 §8.3, SRS FR-TUT-14 ⑥): below the practice topics, with
 * the number of letters that have personal strokes in place of a ✓ (TBD-30 ⑩). It can be opened with graffiti turned
 * off in the settings, like a practice topic.
 */
@Composable
internal fun MyStrokesTopicRow(my: MyStrokesModel, enabled: Set<InputMode>) {
    HorizontalDivider(Modifier.padding(top = 8.dp))
    Column(Modifier.fillMaxWidth().clickable { my.open() }.padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.my_strokes_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.my_strokes_taught, my.book.taughtLetters), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 12.dp))
        }
        Text(stringResource(R.string.my_strokes_summary), style = MaterialTheme.typography.bodyMedium)
        if (InputMode.GRAFFITI !in enabled) {
            Text(stringResource(R.string.practice_mode_off), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

/** The letter list or the write screen of "my strokes", in the practice screen's upper part (LLD-07 §8.3). */
@Composable
internal fun ColumnScope.MyStrokesPanel(my: MyStrokesModel) {
    when (my.screen) {
        MyStrokesModel.Screen.WRITE -> Write(my)
        else -> Letters(my)
    }
}

@Composable
private fun ColumnScope.Letters(my: MyStrokesModel) {
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.my_strokes_title), style = MaterialTheme.typography.titleLarge)
        if (my.letters.isEmpty()) {
            Text(stringResource(R.string.my_strokes_no_letters), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(stringResource(R.string.my_strokes_hint), style = MaterialTheme.typography.bodyMedium)
            for ((set, title) in listOf(SetName.KO_LETTER to R.string.my_strokes_section_ko, SetName.EN_LETTER to R.string.my_strokes_section_en)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                for (row in my.letters.filter { it.set == set }.chunked(LETTERS_PER_ROW)) {
                    Row(Modifier.fillMaxWidth()) {
                        for (l in row) {
                            Column(
                                Modifier.weight(1f).clickable { my.pick(l) }.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(l.label, style = MaterialTheme.typography.headlineSmall)
                                Text(my.count(l).toString(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        repeat(LETTERS_PER_ROW - row.size) { Column(Modifier.weight(1f)) {} }
                    }
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { my.close() }) { Text(stringResource(R.string.practice_stop)) }
        if (my.letters.isNotEmpty()) OutlinedButton(onClick = { my.askClearAll() }) { Text(stringResource(R.string.my_strokes_clear_all)) }
    }
    if (my.askingClearAll) {
        AlertDialog(
            onDismissRequest = { my.answerClearAll(false) },
            text = { Text(stringResource(R.string.my_strokes_clear_all_ask)) },
            confirmButton = { TextButton(onClick = { my.answerClearAll(true) }) { Text(stringResource(R.string.my_strokes_clear_all_yes)) } },
            dismissButton = { TextButton(onClick = { my.answerClearAll(false) }) { Text(stringResource(R.string.my_strokes_cancel)) } },
        )
    }
}

@Composable
private fun ColumnScope.Write(my: MyStrokesModel) {
    val l = my.letter ?: return
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.my_strokes_title), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(R.string.my_strokes_target, l.label), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.my_strokes_count, my.count(l), MyStrokesModel.MAX_PER_LETTER), style = MaterialTheme.typography.titleMedium)
        Text(
            when (val m = my.message) {
                null -> stringResource(R.string.my_strokes_write_hint)
                is MyStrokeMessage.Saved -> m.recognized?.let { stringResource(R.string.my_strokes_saved, it) } ?: stringResource(R.string.my_strokes_saved_unknown)
                MyStrokeMessage.RuleStroke -> stringResource(R.string.my_strokes_rule_stroke)
                MyStrokeMessage.NumberArea -> stringResource(R.string.my_strokes_number_area)
                MyStrokeMessage.Cleared -> stringResource(R.string.my_strokes_cleared)
                MyStrokeMessage.WriteFailed -> stringResource(R.string.my_strokes_write_failed)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { my.backToLetters() }) { Text(stringResource(R.string.my_strokes_letters)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { my.reset() }) { Text(stringResource(R.string.my_strokes_reset)) }
            Button(onClick = { my.relearn() }) { Text(stringResource(R.string.my_strokes_relearn)) }
        }
    }
}

/**
 * Builds the write screen's board (LLD-07 §8.3): the letter's stroke guide cells over the real pad view, in the
 * practice's [BoardFrame] so the keyboard width and alignment are the keyboard's. The pad hands every stroke to
 * [MyStrokesModel.onStroke] ([GraffitiPadView.onInk]); its auxiliary bar keys do nothing here.
 */
internal class MyStrokesBoard(
    private val settings: Settings,
    private val feedback: FeedbackPort,
    private val errorLog: ErrorLog,
    private val templates: TemplateSet,
    private val recognizer: () -> Recognizer,
) {
    fun build(context: Context, my: MyStrokesModel, letter: MyLetter): View {
        val density = context.resources.displayMetrics.density
        fun px(dp: Float) = Math.round(dp * density)
        // The pad draws its status (the language letter) from a handler's state; with onInk set no stroke reaches the handler.
        val input = PracticeInput(PracticeBoard.GRAFFITI, feedback, errorLog, settings)
        val handler = input.graffiti(templates, letter.lang, {}, {}, recognizer)
        val frame = BoardFrame(context, ThresholdDefaults.MIN_CROSS_DP * density, settings.portraitWidth, settings.landscapeWidth, held = { false }, onDown = {}) {}
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val panel = GraffitiGuidePanel(context, templates, input.feedback, MyStrokes.guideGroups(templates, letter), showClose = false) {}
        column.addView(panel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(GUIDE_DP)))
        val pad = GraffitiPadView(context, NoKeys, handler, input.guard, input.feedback)
        pad.letterRatio = settings.graffitiLetterRatio
        pad.minCrossDp = input.thresholds.minCrossDp
        pad.onInk = my::onStroke
        column.addView(pad, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(GraffitiPadGeometry.ROWS * settings.rowHeightDp)))
        frame.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        return frame
    }

    /** The pad's auxiliary bar on the write screen: shown as on the keyboard, doing nothing. */
    private object NoKeys : TapSink {
        override fun onTapOutput(key: TapKeyDef, out: TapOutput) = Unit
        override fun onFunctionDown(key: FunctionKey) = Unit
        override fun onFunctionUp(key: FunctionKey) = Unit
        override fun onSettingsStroke() = Unit
    }

    private companion object {
        /** Height of the letter's guide cells: one row of cells. */
        const val GUIDE_DP = 120f
    }
}

private const val LETTERS_PER_ROW = 7
