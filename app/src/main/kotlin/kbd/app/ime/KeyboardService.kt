package kbd.app.ime

import android.content.Context
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import kbd.app.error.EngineGuard
import kbd.app.feedback.Feedback
import kbd.app.feedback.FeedbackPort
import kbd.app.graffiti.GraffitiInputHandler
import kbd.app.graffiti.GraffitiPadGeometry
import kbd.app.graffiti.GraffitiPadView
import kbd.app.store.ErrorLog
import kbd.app.store.Settings
import kbd.app.store.SettingsStore
import kbd.app.swipe.SwipeEngineState
import kbd.app.swipe.SwipeHost
import kbd.app.swipe.SwipeModule
import kbd.app.swipe.SwipeModuleProvider
import kbd.app.view.CenterText
import kbd.app.view.KeyboardRootView
import kbd.app.view.TapKeyboardView
import kbd.hangul.cheonjiin.CheonjiinAssembler
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.TemplateLoader
import kbd.graffiti.recognize.TemplateSet
import kbd.graffiti.state.GraffitiState
import kbd.hangul.compose.ComposePolicy
import kbd.hangul.compose.HangulComposer
import kbd.hangul.event.JamoEvent
import kbd.hangul.jamo.Jamo
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapLayouts
import kbd.stroke.threshold.ThresholdDefaults
import kbd.stroke.threshold.Thresholds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** The settings DataStore, one per process (LLD-07 §6.1). */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * The input method (LLD-08 §3, HLD 8): swipe, QWERTY, Dubeolsik and graffiti views.
 */
class KeyboardService : InputMethodService(), SwipeHost {
    lateinit var dispatcher: InputDispatcher
    lateinit var modes: ModeController
    lateinit var root: KeyboardRootView

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var errorLog: ErrorLog
    private lateinit var settingsStore: SettingsStore
    private lateinit var swipe: SwipeModule
    private lateinit var composing: ComposingController

    /** Graffiti handler, one per service: language and caps lock survive field changes (FR-GRF-05a, LLD-05 §6). */
    private lateinit var graffiti: GraffitiInputHandler
    /** The graffiti pad on screen, null in the other modes; target of [GraffitiInputHandler]'s flash. */
    private var pad: GraffitiPadView? = null
    /** The built-in templates, also drawn by the stroke guide (FR-GRF-09c, LLD-05 §10). */
    private var templates: TemplateSet = TemplateSet(emptyMap())

    /** Fixed code constants (HLD 7.2, FSS#17): no stored file, nothing measured. */
    override val thresholds: Thresholds = ThresholdDefaults.VALUES
    override var settings: Settings = Settings.DEFAULT
        private set
    override val fieldInfo: FieldInfo get() = dispatcher.field
    override val centerConsonant: Jamo? get() = if (::dispatcher.isInitialized) dispatcher.composer?.lastConsonant() else null
    override val centerText: CenterText? get() = if (::dispatcher.isInitialized) dispatcher.centerText() else null
    override lateinit var guard: EngineGuard
    override lateinit var feedback: FeedbackPort

    override fun onCreate() {
        super.onCreate()
        errorLog = ErrorLog(File(filesDir, ERROR_LOG_FILE))
        settingsStore = SettingsStore(settingsDataStore, errorLog)
        settings = settingsStore.loadBlocking()   // once at start (D-24)
        swipe = SwipeModuleProvider.get()
        // Every mode in every build (issue #251); the settings turn graffiti and swipe off (FR-SET-08).
        modes = ModeController(InputMode.entries.toList(), settings.lastMode, settings.enabledModes)
        modes.fixedStart?.let(::saveLastMode)   // the last mode was off (FR-CMN-03f ③)
        feedback = Feedback({ if (::root.isInitialized) root else null }, getSystemService(AudioManager::class.java), { settings })
        composing = ComposingController(AndroidEditorPort(this))
        dispatcher = InputDispatcher(modes, composing, feedback, ::saveLastMode)
        dispatcher.onShowMode = { m -> showModeView(m, dispatcher.field) }
        dispatcher.onOpenSettings = {   // mode key up stroke, all modes (issue #205)
            startActivity(android.content.Intent(this, kbd.app.settings.SettingsActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        guard = EngineGuard(errorLog, dispatcher)
        createGraffiti()
        scope.launch {
            settingsStore.flow.collect { s -> handler.post { applySettings(s) } }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        handler.removeCallbacksAndMessages(null)   // pending applySettings
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        root = KeyboardRootView(this).also {
            it.rowHeightDp = settings.rowHeightDp
            it.portraitWidth = settings.portraitWidth   // FR-CMN-14
            it.landscapeWidth = settings.landscapeWidth
        }
        showModeView(modes.current, dispatcher.field)
        return root
    }

    /** LLD-08 §3.1; the order matters. */
    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val f = FieldInfo.from(info.inputType, info.imeOptions)
        dispatcher.field = f
        dispatcher.finishAll()   // HLD 8
        dispatcher.resetParsers()
        modes.onFieldChanged()   // FR-CMN-03e
        val before = modes.current
        if (f.numericLike) modes.enterNumericField() else modes.leaveNumericField()   // FR-CMN-04, 04b
        modes.applyEnabled(settings.enabledModes)?.let(::saveLastMode)   // FR-CMN-03f; committed above
        errorLog.enabled = !f.noRecord   // NFR-05, FR-CMN-08 ① ②
        composing.onStart(info.initialSelStart, info.initialSelEnd)
        showModeView(modes.current, f)
        if (modes.current == InputMode.GRAFFITI && before != InputMode.GRAFFITI) dispatcher.graffiti?.enterMode()   // FR-GRF-05a
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        when (composing.onUpdateSelection(newSelStart, newSelEnd, candidatesStart, candidatesEnd)) {
            SelectionVerdict.USER_MOVED -> dispatcher.onCursorMoved(newSelStart, newSelEnd, candidatesStart, candidatesEnd)
            SelectionVerdict.OTHER_MOVED -> dispatcher.onCursorMovedNoComposing()
            SelectionVerdict.OWN -> Unit
        }
    }

    /**
     * FR-CMN-06. The cancel reaches the mode view, which drops its guide and repeat timer. The stroke guide
     * closes when the keyboard goes down (`finishingInput == false`) and stays open on a field change (FR-GRF-09).
     */
    override fun onFinishInputView(finishingInput: Boolean) {
        dispatcher.finishAll()
        if (::root.isInitialized) {
            root.cancelTouches()
            if (!finishingInput) root.closeGuide()
        }
        super.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        dispatcher.finishAll()
        super.onFinishInput()
    }

    /** D-27: the touch in progress and its timers are dropped. */
    override fun onWindowHidden() {
        if (::root.isInitialized) {
            root.cancelTouches()
            root.closeGuide()   // the keyboard went down (FR-GRF-09)
        }
        super.onWindowHidden()
    }

    /** FR-CMN-12. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    /** Same layout with the new orientation's keyboard width (FR-CMN-12, FR-CMN-14 ⑩), same row height. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::root.isInitialized) root.relayout()
    }

    /**
     * Content and visible insets at the top of the input view; the whole input view, blank band
     * included, takes touches (SRS 3.3). See the PR note on `TOUCHABLE_INSETS_VISIBLE`.
     */
    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        if (!::root.isInitialized || !root.isAttachedToWindow) return
        val loc = IntArray(2)
        root.getLocationInWindow(loc)
        outInsets.contentTopInsets = loc[1]
        outInsets.visibleTopInsets = loc[1]
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_VISIBLE
    }

    /**
     * Templates are read once at start (LLD-05 §7). A failure is logged and leaves an empty set: rule
     * strokes still work, template strokes are recognition failures (LLD-09 §4).
     */
    private fun createGraffiti() {
        templates = guard.run(SITE_TEMPLATES) {
            val stream = checkNotNull(TemplateLoader::class.java.getResourceAsStream(TEMPLATES_RESOURCE)) { "missing $TEMPLATES_RESOURCE" }
            stream.use { TemplateLoader.load(it) }
        } ?: TemplateSet(emptyMap())
        graffiti = GraffitiInputHandler(
            recognizer = Recognizer(templates),
            state = GraffitiState(templates),
            asm = CheonjiinAssembler(),
            composer = { checkNotNull(dispatcher.composer) },   // graffiti policy while the pad is shown (LLD-08 §6)
            out = composing,
            sink = dispatcher,
            flash = { label -> pad?.flash(label) },
            redraw = { pad?.invalidate() },   // pad status display (FR-GRF-05d, LLD-05 §8.1)
        )
    }

    /** LLD-08 §3.1 `showModeView`; the composer is re-created for the mode (D-10). */
    private fun showModeView(mode: InputMode, f: FieldInfo) {
        dispatcher.composer = when (mode) {
            InputMode.SWIPE -> HangulComposer(ComposePolicy.SWIPE, ::onInvalidComposerEvent)
            InputMode.DUBEOLSIK -> HangulComposer(ComposePolicy.DUBEOLSIK, ::onInvalidComposerEvent)
            InputMode.GRAFFITI -> HangulComposer(ComposePolicy.GRAFFITI, ::onInvalidComposerEvent)
            InputMode.QWERTY -> null
        }
        // Attached before enterMode() (G6) and before the view exists; null outside graffiti (LLD-08 §6).
        dispatcher.graffiti = if (mode == InputMode.GRAFFITI) graffiti else null
        pad = null
        if (!::root.isInitialized) return
        if (mode == InputMode.GRAFFITI) root.enableGuide(templates, feedback) else root.disableGuide()   // FR-GRF-09
        if (mode == InputMode.SWIPE) {
            root.setModeView(swipe.createSwipeView(this, this), SWIPE_ROWS)
            return
        }
        val tapLayout = when (mode) {
            InputMode.QWERTY -> TapLayouts.QWERTY   // numeric-like fields too (FR-CMN-04, LLD-05A §3.5)
            InputMode.DUBEOLSIK -> TapLayouts.DUBEOLSIK
            else -> null
        }
        if (tapLayout != null) {
            val view = TapKeyboardView(this, dispatcher, guard, feedback)
            view.setLayout(tapLayout, thresholds, settings.rowHeightDp)
            root.setModeView(view, tapLayout.rows)
            return
        }
        val view = GraffitiPadView(this, dispatcher, graffiti, guard, feedback)
        view.letterRatio = settings.graffitiLetterRatio   // FR-SET-05
        view.minCrossDp = thresholds.minCrossDp   // the mode key's up stroke (issue #205), as on the tap keyboards
        pad = view
        root.setModeView(view, GraffitiPadGeometry.ROWS)
    }

    /** Settings flow → current settings and window size (LLD-07 §6.1). */
    private fun applySettings(s: Settings) {
        val rowChanged = s.rowHeightDp != settings.rowHeightDp
        val widthChanged = s.portraitWidth != settings.portraitWidth || s.landscapeWidth != settings.landscapeWidth
        val trailChanged = s.showStrokeTrail != settings.showStrokeTrail
        settings = s
        if (trailChanged && ::root.isInitialized) root.redrawModeView()   // FSS#57: the swipe view reads it when it draws
        pad?.let { if (it.letterRatio != s.graffitiLetterRatio) it.letterRatio = s.graffitiLetterRatio }
        if ((rowChanged || widthChanged) && ::root.isInitialized) {
            root.rowHeightDp = s.rowHeightDp
            root.portraitWidth = s.portraitWidth   // FR-CMN-14 ⑩: placed again at once
            root.landscapeWidth = s.landscapeWidth
            root.relayout()
        }
        applyEnabledModes(s.enabledModes)
    }

    /**
     * FR-CMN-03f ④, HLD 8.1: the mode in use was turned off. While the keyboard is up it switches at once
     * (commit first, then the new view, as on the mode key); otherwise the next `onStartInputView` shows it.
     */
    private fun applyEnabledModes(on: Set<InputMode>) {
        val before = modes.current
        if (before !in on && isInputViewShown) {
            dispatcher.finishAll()   // FR-CMN-03d
            dispatcher.resetParsers()
        }
        modes.applyEnabled(on)?.let(::saveLastMode)
        if (modes.current == before || !isInputViewShown) return
        showModeView(modes.current, dispatcher.field)
        if (modes.current == InputMode.GRAFFITI) dispatcher.graffiti?.enterMode()   // FR-GRF-05a
    }

    private fun saveLastMode(m: InputMode) {
        scope.launch { settingsStore.setLastMode(m) }
    }

    /** D-09 via the composer callback (G5). The text is not logged (NFR-05). */
    private fun onInvalidComposerEvent(@Suppress("UNUSED_PARAMETER") detail: String) {
        errorLog.record(SITE_COMPOSER, InvalidComposerEvent())
    }

    override fun onSwipeEvents(events: List<JamoEvent>, errorVibe: Boolean) = dispatcher.onJamoEvents(events, errorVibe)

    override fun onSwipeText(text: String, errorVibe: Boolean) = dispatcher.onSwipeText(text, errorVibe)

    override fun onFunctionKey(key: FunctionKey) = dispatcher.onFunctionDown(key)

    override fun onFunctionKeyUp(key: FunctionKey) = dispatcher.onFunctionUp(key)

    override fun onSettingsStroke() = dispatcher.onSettingsStroke()

    override fun registerSwipeState(state: SwipeEngineState?) {
        dispatcher.swipeState = state
    }

    /** Marker for D-09 reports; only its class name reaches the log. */
    private class InvalidComposerEvent : Exception()

    private companion object {
        const val ERROR_LOG_FILE = "errors.log"
        const val SITE_COMPOSER = "HangulComposer.invalid"
        const val SITE_TEMPLATES = "TemplateLoader.load"
        const val TEMPLATES_RESOURCE = "/graffiti/templates.json"
        const val SWIPE_ROWS = 6   // the double-consonant row (layout A, issue #168) fills the window
    }
}
