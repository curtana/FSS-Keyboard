package kbd.app.practice

import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kbd.app.R
import kbd.app.feedback.Feedback
import kbd.app.graffiti.GraffitiGuidePanel
import kbd.app.graffiti.GraffitiPadGeometry
import kbd.app.graffiti.GraffitiPadView
import kbd.app.ime.InputMode
import kbd.app.settings.AppStores
import kbd.app.settings.KeyboardAppTheme
import kbd.app.store.ErrorLog
import kbd.app.store.KeyboardSpan
import kbd.app.store.KeyboardWidth
import kbd.app.store.Settings
import kbd.app.store.WidthSetting
import kbd.app.swipe.SwipeModuleProvider
import kbd.app.view.KeyboardRootView
import kbd.app.view.OffKeyboardPointers
import kbd.app.view.TapKeyboardView
import kbd.graffiti.guide.GuideCatalog
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.TemplateSet
import kbd.stroke.layout.TapLayouts
import kbd.stroke.threshold.ThresholdDefaults
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The input practice screen (LLD-07 §2.3, SWIPE 10.0, HLD 7.1, FSS#17): topic list → explanation → items → end, with
 * the real keyboard views below. [PracticeFlow] holds the state; every try gets a fresh [PracticeInput] and view.
 */
class PracticeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stores = AppStores(this)
        val settings = stores.settings.loadBlocking()
        val feedback = Feedback({ window?.decorView }, getSystemService(AudioManager::class.java), { settings })
        val templates = loadTemplates(stores.errorLog)
        val flow = PracticeFlow(PracticeTopics.ALL) { id -> lifecycleScope.launch { stores.settings.markPracticeDone(id) } }
        val boards = BoardFactory(settings, feedback, stores.errorLog, templates)
        setContent {
            KeyboardAppTheme {
                PracticeScreen(flow, stores.settings.flow, settings, boards, ::finish)
            }
        }
    }

    /** The recognizer's built-in templates, as the keyboard reads them (LLD-05 §7); none on failure (logged). */
    private fun loadTemplates(log: ErrorLog): TemplateSet = try {
        checkNotNull(TemplateLoader::class.java.getResourceAsStream(TEMPLATES_RESOURCE)) { "missing $TEMPLATES_RESOURCE" }
            .use { TemplateLoader.load(it) }
    } catch (e: Exception) {
        log.record(SITE_TEMPLATES, e)
        TemplateSet(emptyMap())
    }

    private companion object {
        const val TEMPLATES_RESOURCE = "/graffiti/templates.json"
        const val SITE_TEMPLATES = "PracticeActivity.templates"
    }
}

/**
 * Builds one try's practice keyboard (LLD-07 §2.3): the real view of the item's board, wrapped in a [BoardFrame] that
 * tells the flow when a touch is pressed and when the engine is done with a released one.
 */
internal class BoardFactory(
    private val settings: Settings,
    private val feedback: Feedback,
    private val errorLog: ErrorLog,
    private val templates: TemplateSet,
) {
    fun build(context: Context, flow: PracticeFlow, item: PracticeItem): View {
        val input = PracticeInput(item.board, feedback, errorLog, settings)
        val density = context.resources.displayMetrics.density
        fun px(dp: Float) = Math.round(dp * density)
        val row = settings.rowHeightDp
        var before = ""
        var beforeComposing = ""
        val frame = BoardFrame(context, ThresholdDefaults.MIN_CROSS_DP * density, settings.portraitWidth, settings.landscapeWidth, held = { flow.holding }, onDown = {
            input.errored = false
            before = input.editor.text
            beforeComposing = input.editor.composing
            flow.onPress()
        }) { reversed ->
            val text = input.editor.text
            val composing = input.editor.composing
            // Off the tap keyboard an action counts when it typed something, committed something (the graffiti finish
            // stroke commits without typing, FSS#43) or vibrated (a touch on a key that does nothing here, or a scroll
            // of the guide, is not an action).
            if (item.board == PracticeBoard.TAP_QWERTY || text != before || composing != beforeComposing || input.errored) {
                flow.onAction(text, input.editor.display, input.errored, reversed, composing)
            }
        }
        when (item.board) {
            PracticeBoard.TAP_QWERTY -> {
                val layout = TapLayouts.QWERTY   // the real English QWERTY keyboard, edit row included (owner decision A)
                val v = TapKeyboardView(context, input.tapSink, input.guard, input.feedback)
                v.setLayout(layout, input.thresholds, row)
                frame.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, px(layout.rows * row)))
            }
            PracticeBoard.SWIPE -> {
                val v = SwipeModuleProvider.get().createSwipeView(context, input)   // with its guides (FR-TUT-11)
                frame.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
            }
            PracticeBoard.GRAFFITI -> {
                var pad: GraffitiPadView? = null
                val handler = input.graffiti(templates, item.lang ?: GraffitiLang.KO, { pad?.flash(it) }, { pad?.invalidate() })
                val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                // The stroke guide, always open above the pad (not over it), only the item's section (SWIPE 10.1 ③).
                val panel = GraffitiGuidePanel(context, templates, input.feedback, GuideCatalog.groups.filter { it.section == item.guideSection() }, showClose = false) {}
                panel.reveal = item.guideCells.toSet()   // the target's cell is in view from the start (FSS#72)
                panel.highlight = flow.highlight.toSet()
                column.addView(panel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(GUIDE_DP)))
                val p = GraffitiPadView(context, input.tapSink, handler, input.guard, input.feedback)
                p.letterRatio = settings.graffitiLetterRatio
                p.minCrossDp = input.thresholds.minCrossDp
                pad = p
                column.addView(p, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(GraffitiPadGeometry.ROWS * row)))
                frame.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
            }
        }
        return frame
    }

    private companion object {
        /** Height of the always-open stroke guide in the practice (LLD-07 §2.3): about two rows of cells. */
        const val GUIDE_DP = 200f
    }
}

/**
 * Sees every touch of the practice keyboard without taking it (LLD-07 §2.3): [onDown] on a press with no other finger
 * down; on each release,
 * after the view has handled it, [onUp] is posted (so an engine that posts its own work is done too). [onUp] gets
 * whether the released touch went up or left by more than [slopPx] (a reversed swipe stroke, SWIPE 10.2 덧붙임).
 *
 * The board has the keyboard width and alignment of the screen's orientation (SRS FR-CMN-14 ⑥, LLD-07 §2.3, FSS#25):
 * the frame's width is the available width, the board sits in its [KeyboardWidth.place] span, and the rest is
 * keyboard-coloured margin whose touches are taken with no input and are no action of the practice.
 *
 * A gesture that begins while [held] (a right item on show, FSS#54) is taken whole with no input and no action.
 */
private class BoardFrame(
    context: Context,
    private val slopPx: Float,
    private val portraitWidth: WidthSetting,
    private val landscapeWidth: WidthSetting,
    private val held: () -> Boolean,
    private val onDown: () -> Unit,
    private val onUp: (reversed: Boolean) -> Unit,
) : FrameLayout(context) {

    private val starts = HashMap<Int, Pair<Float, Float>>()
    private var span = KeyboardSpan(0, 0)
    /** The gesture began while [held]: none of it reaches the board. */
    private var heldGesture = false
    private val offBoard = OffKeyboardPointers { x, y -> x >= span.left && x < span.left + span.width && y >= 0 && y < height }

    init {
        setBackgroundColor(KeyboardRootView.BACKGROUND_COLOR)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val avail = MeasureSpec.getSize(widthMeasureSpec)
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val dm = resources.displayMetrics
        span = KeyboardWidth.place(avail, minOf(dm.widthPixels, dm.heightPixels), dm.density, portraitWidth, landscapeWidth, landscape)
        super.onMeasure(MeasureSpec.makeMeasureSpec(span.width, MeasureSpec.EXACTLY), heightMeasureSpec)
        setMeasuredDimension(avail, measuredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            c.layout(span.left, 0, span.left + c.measuredWidth, c.measuredHeight)
        }
    }

    /** The margin beside the board takes its touches with no input (FR-CMN-14 ⑤). */
    override fun onTouchEvent(event: MotionEvent): Boolean = true

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) heldGesture = held()
        if (heldGesture) return true   // a right item on show takes no input (FSS#54)
        val ev = offBoard.filter(event) ?: return true   // a finger on the margin: no input, no practice action
        try {
            return dispatchOnBoard(ev) || offBoard.active
        } finally {
            if (ev !== event) ev.recycle()
        }
    }

    private fun dispatchOnBoard(ev: MotionEvent): Boolean {
        val i = ev.actionIndex
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Only the first finger starts an action: a second one pressed meanwhile must not reset the first
                // one's text and error mark before it is released (one-finger practice; review PR #27).
                val first = starts.isEmpty()
                starts[ev.getPointerId(i)] = ev.getX(i) to ev.getY(i)
                if (first) onDown()
            }
        }
        val handled = super.dispatchTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val s = starts.remove(ev.getPointerId(i))
                val dx = if (s == null) 0f else ev.getX(i) - s.first
                val dy = if (s == null) 0f else ev.getY(i) - s.second
                val reversed = maxOf(abs(dx), abs(dy)) > slopPx && (if (abs(dy) > abs(dx)) dy < 0 else dx < 0)
                post { onUp(reversed) }
            }
            MotionEvent.ACTION_CANCEL -> starts.clear()
        }
        return handled
    }
}

@Composable
internal fun PracticeScreen(
    flow: PracticeFlow,
    settingsFlow: kotlinx.coroutines.flow.Flow<Settings>,
    start: Settings,
    boards: BoardFactory,
    close: () -> Unit,
) {
    val now by settingsFlow.collectAsState(start)   // the topic ✓ marks as stored (SWIPE 10.0)
    val done = now.practiceDone
    val enabled = now.enabledModes   // "키보드에서 꺼져 있음" (FR-CMN-03f ⑦)
    // A right item stays on screen with its ✓, then the next one comes (FSS#54); [그만] meanwhile ends the hold.
    LaunchedEffect(flow.holding) {
        if (flow.holding) {
            delay(PracticeFlow.RIGHT_HOLD_MS)
            flow.advance()
        }
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.practice_title), style = MaterialTheme.typography.headlineSmall)
                when (flow.screen) {
                    PracticeFlow.Screen.LIST -> TopicList(flow, done, enabled, close)
                    PracticeFlow.Screen.INTRO -> Intro(flow)
                    PracticeFlow.Screen.ITEM -> Item(flow)
                    PracticeFlow.Screen.DONE -> Finished(flow)
                }
            }
            val item = flow.item
            if (flow.screen == PracticeFlow.Screen.ITEM && item != null) {
                key(flow.topic?.id, flow.index, flow.tryNo) {
                    AndroidView(
                        factory = { ctx -> boards.build(ctx, flow, item) },
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.TopicList(flow: PracticeFlow, done: Set<String>, enabled: Set<InputMode>, close: () -> Unit) {
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.practice_list_hint), style = MaterialTheme.typography.bodyMedium)
        for (t in flow.topics) {
            HorizontalDivider(Modifier.padding(top = 8.dp))
            Column(Modifier.fillMaxWidth().clickable { flow.open(t) }.padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (t.id in done) Text(MARK_RIGHT, color = RIGHT_COLOR, style = MaterialTheme.typography.titleMedium)
                    Text(t.time, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 12.dp))
                }
                Text(t.summary, style = MaterialTheme.typography.bodyMedium)
                if (t.offInKeyboard(enabled)) {
                    Text(stringResource(R.string.practice_mode_off), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
    Button(onClick = close) { Text(stringResource(R.string.practice_close)) }
}

@Composable
private fun ColumnScope.Intro(flow: PracticeFlow) {
    val t = flow.topic ?: return
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t.title, style = MaterialTheme.typography.titleLarge)
        for (line in t.intro) Text(line, style = MaterialTheme.typography.bodyLarge)
    }
    Buttons(flow) { Button(onClick = { flow.start() }) { Text(stringResource(R.string.practice_start)) } }
}

@Composable
private fun ColumnScope.Item(flow: PracticeFlow) {
    val t = flow.topic ?: return
    val item = flow.item ?: return
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${t.title}  " + stringResource(R.string.practice_item, flow.index + 1, t.items.size), style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(R.string.practice_target, item.target) + (item.note?.let { " ($it)" } ?: ""),
            style = MaterialTheme.typography.headlineMedium,
        )
        Echo(flow.echo, flow.verdict)
        flow.howTo?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        flow.hint?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
    Buttons(flow) {}
}

@Composable
private fun ColumnScope.Finished(flow: PracticeFlow) {
    Column(Modifier.weight(1f).fillMaxWidth()) {
        Text(flow.topic?.title ?: "", style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.practice_finished), style = MaterialTheme.typography.headlineMedium)
    }
    Button(onClick = { flow.stop() }) { Text(stringResource(R.string.practice_back)) }
}

/** [그만] on the left (back to the topic list at any time, SWIPE 10.0), [extra] on the right. */
@Composable
private fun Buttons(flow: PracticeFlow, extra: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { flow.stop() }) { Text(stringResource(R.string.practice_stop)) }
        extra()
    }
}

/**
 * The input display, large (FR-TUT-09), with the ✓ (green) or ✗ (red) beside it (FR-TUT-12). The box keeps its
 * height when empty so the screen does not jump.
 */
@Composable
private fun Echo(c: String, verdict: Verdict?) {
    Row(Modifier.fillMaxWidth().height(ECHO_BOX), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(c, fontSize = ECHO_SIZE, lineHeight = ECHO_SIZE, maxLines = 1)
        when (verdict) {
            Verdict.RIGHT -> Text(MARK_RIGHT, fontSize = MARK_SIZE, color = RIGHT_COLOR, modifier = Modifier.padding(start = 16.dp))
            Verdict.WRONG -> Text(MARK_WRONG, fontSize = MARK_SIZE, color = WRONG_COLOR, modifier = Modifier.padding(start = 16.dp))
            else -> Unit
        }
    }
}

/** ✓ / ✗ and their colours (LLD-07 §2.4: Material green 700 / red 700, readable on light and dark). */
private const val MARK_RIGHT = "✓"
private const val MARK_WRONG = "✗"
private val RIGHT_COLOR = Color(0xFF388E3C)
private val WRONG_COLOR = Color(0xFFD32F2F)
private val MARK_SIZE = 48.sp

/** The input display: clearly larger than the target line (headlineMedium), still small enough not to cover the screen. */
private val ECHO_SIZE = 64.sp
private val ECHO_BOX = 80.dp
