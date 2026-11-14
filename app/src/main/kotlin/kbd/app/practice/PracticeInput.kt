package kbd.app.practice

import kbd.app.error.EngineGuard
import kbd.app.feedback.FeedbackPort
import kbd.app.graffiti.GraffitiInputHandler
import kbd.app.ime.ComposingController
import kbd.app.ime.EditorKey
import kbd.app.ime.EditorPort
import kbd.app.ime.InputDispatcher
import kbd.app.ime.InputMode
import kbd.app.ime.ModeController
import kbd.app.store.ErrorLog
import kbd.app.store.Settings
import kbd.app.view.TapSink
import kbd.graffiti.ink.GraffitiLang
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.TemplateSet
import kbd.graffiti.state.GraffitiState
import kbd.hangul.cheonjiin.CheonjiinAssembler
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapKeyDef
import kbd.stroke.tap.TapOutput
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds

/**
 * The practice keyboard's editor (LLD-07 §2.3): an in-memory text in place of the app's input field. Like
 * `InputConnection`, a commit replaces the composing text.
 */
class PracticeEditor : EditorPort {
    private val committed = StringBuilder()

    /** The composing text as shown, temporary ㆍ / ‥ included (FR-HGL-04e). */
    var composing: String = ""
        private set

    /** Everything the item typed so far: what the judgement compares (SRS appendix A.3). */
    val text: String get() = committed.toString() + composing

    /** The input display (FR-TUT-09): the composing syllable as shown ("ㄱㆍ" too), else the last typed character (a space is none). */
    val display: String get() = composing.ifEmpty { PracticeItemJudge.lastChar(committed.toString().trimEnd()) }

    override fun commitText(text: String) {
        composing = ""
        committed.append(text)
    }

    override fun setComposingText(text: String) {
        composing = text
    }

    override fun finishComposingText() {
        committed.append(composing)
        composing = ""
    }

    /** ⌫ with nothing composing: one character goes (a code point; the practice targets have no surrogate pairs). */
    override fun deleteBefore() {
        if (committed.isNotEmpty()) committed.setLength(committed.offsetByCodePoints(committed.length, -1))
    }

    override fun sendKey(key: EditorKey) = Unit
    override fun performEditorAction(actionId: Int) = Unit
    override fun setSelection(start: Int, end: Int) = Unit
    override fun beginBatch() = Unit
    override fun endBatch() = Unit
}

/**
 * One try of one practice item (LLD-07 §2.3, HLD 7.1): the real `InputDispatcher`, composer and graffiti
 * engine writing into a [PracticeEditor], at the fixed thresholds. Android-free; the practice screen gives
 * [tapSink] to the tap keyboard and graffiti views.
 *
 * Only what the item practises reaches the dispatcher: letters, digits, symbols, graffiti strokes, on
 * the tap keyboard ⌫ (owner decision A, FSS#17), and on the graffiti pad the space, which
 * commits the composing syllable for judging (PM decision A, FSS#43). The other function keys (↵, arrows, the mode
 * key, the tap keyboard's space, the pad's ⌫) are shown as on the keyboard but do nothing
 * here (SRS appendix A.3: the pad has no edit keys in practice; the edit row's other keys are not
 * judged).
 */
class PracticeInput(
    val board: PracticeBoard,
    private val out: FeedbackPort,
    errorLog: ErrorLog,
    val settings: Settings,
) {

    val editor = PracticeEditor()

    /** The current action vibrated as an error (SRS appendix A.3 오류 진동); the screen clears it at each touch. */
    var errored: Boolean = false

    val feedback: FeedbackPort = object : FeedbackPort {
        override fun key() = out.key()
        override fun error() {
            errored = true
            out.error()
        }
    }

    private val mode = when (board) {
        PracticeBoard.TAP_QWERTY -> InputMode.QWERTY
        PracticeBoard.GRAFFITI -> InputMode.GRAFFITI
    }

    private val composing = ComposingController(editor)

    val dispatcher = InputDispatcher(ModeController(listOf(mode), mode), composing, feedback) {}.also {
        it.composer = when (board) {
            PracticeBoard.TAP_QWERTY -> null   // QWERTY commits at once (LLD-08 §3.1)

            PracticeBoard.GRAFFITI -> HangulComposer(ComposePolicy.GRAFFITI)
        }
    }

    val guard = EngineGuard(errorLog, dispatcher)

    val thresholds: Thresholds = ThresholdDefaults.VALUES

    /** What the tap keyboard and the graffiti pad report: see the class comment for the keys that do nothing. */
    val tapSink: TapSink = object : TapSink {
        override fun onTapOutput(key: TapKeyDef, out: TapOutput) = dispatcher.onTapOutput(key, out)

        override fun onFunctionDown(key: FunctionKey) {
            if (key == FunctionKey.BACKSPACE && board == PracticeBoard.TAP_QWERTY) dispatcher.onFunctionDown(key)
            if (key == FunctionKey.SPACE && board == PracticeBoard.GRAFFITI) dispatcher.onFunctionDown(key)   // commits (FSS#43)
        }

        override fun onFunctionUp(key: FunctionKey) = Unit
        override fun onSettingsStroke() = Unit
    }

    /** The graffiti engine of this try, the pad's language set to [lang] (SRS appendix A.2 ③: the item sets it, no switching stroke). */
    fun graffiti(templates: TemplateSet, lang: GraffitiLang, flash: (String) -> Unit, redraw: () -> Unit): GraffitiInputHandler {
        check(board == PracticeBoard.GRAFFITI) { "graffiti engine on $board" }
        val state = GraffitiState(templates).also { it.lang = lang }
        return GraffitiInputHandler(
            recognizer = Recognizer(templates),
            state = state,
            asm = CheonjiinAssembler(),
            composer = { checkNotNull(dispatcher.composer) },
            out = composing,
            sink = dispatcher,
            flash = flash,
            redraw = redraw,
        ).also { dispatcher.graffiti = it }
    }
}
