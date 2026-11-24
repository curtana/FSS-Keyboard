package kbd.app.ime

import android.content.Context
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Looper
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.lang.reflect.Proxy
import kbd.app.store.Settings
import kbd.app.store.SettingsStore
import kbd.app.view.SuggestionBarView
import kbd.app.view.ViewTestSdk
import kbd.stroke.layout.FunctionKey
import kbd.stroke.layout.TapLayouts
import kbd.stroke.tap.TapGesture
import kbd.stroke.tap.TapKeyResolver
import kbd.suggest.en.WordIndex
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * LLD-11 §7.12 (하) SV-01..SV-08: `KeyboardService` wiring of the word suggestions (LLD-12 §14). The real service,
 * created by Robolectric as in `KeyboardServiceChordTest`, with an input connection that keeps the field's text (cursor
 * at its end) and records every call. The dictionary comes from [KeyboardService.dictionaryLoader]: the test dictionary,
 * or one that throws.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ViewTestSdk.SDK])
class KeyboardServiceSuggestTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** Every connection call as name(args). */
    private val calls = ArrayList<String>()
    private val text = StringBuilder()

    private val connection = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(InputConnection::class.java)) { _, method, args ->
        val a = args.orEmpty()
        calls += "${method.name}(${a.joinToString(",")})"
        when (method.name) {
            "commitText" -> text.append(a[0] as CharSequence)
            "deleteSurroundingText" -> text.setLength(maxOf(0, text.length - a[0] as Int))
            "sendKeyEvent" -> (a[0] as KeyEvent).let { e ->
                if (e.action == KeyEvent.ACTION_DOWN && e.keyCode == KeyEvent.KEYCODE_DEL && text.isNotEmpty()) text.setLength(text.length - 1)
            }
        }
        when {
            method.name == "getTextBeforeCursor" -> text.substring(maxOf(0, text.length - a[0] as Int))
            method.name == "getTextAfterCursor" -> ""
            method.name == "getSelectedText" -> null
            method.returnType == Boolean::class.javaPrimitiveType -> true
            method.returnType == Int::class.javaPrimitiveType -> 0
            else -> null
        }
    } as InputConnection

    private var loads = 0
    private lateinit var service: KeyboardService

    private var ctl: org.robolectric.android.controller.ServiceController<KeyboardService>? = null

    @After
    fun tearDown() {
        ctl?.destroy()   // no more writes of the last mode
        // The settings DataStore is one per process: leave the defaults for the next test.
        store { Settings.DEFAULT }
    }

    /** Writes the settings through the store, as the settings screen does; the service sees them through its flow. */
    private fun store(f: (Settings) -> Settings) = runBlocking { SettingsStore(context.settingsDataStore).update(f) }

    /** Starts the service with [wordSuggest] stored, its input view made, and the given dictionary loader. */
    private fun start(wordSuggest: Boolean, base: Settings = Settings.DEFAULT, loader: () -> WordIndex = { MiniWords.load() }) {
        store { base.copy(wordSuggest = wordSuggest) }
        val c = Robolectric.buildService(KeyboardService::class.java)
        ctl = c
        c.get().dictionaryLoader = {
            loads++
            loader()
        }
        service = c.create().get()
        connect()
        service.onCreateInputView()
    }

    /** The connection the editor port reads: the one a started input would have. */
    private fun connect() {
        val f = InputMethodService::class.java.getDeclaredField("mStartedInputConnection")
        f.isAccessible = true
        f.set(service, connection)
    }

    private fun field(type: Int = InputType.TYPE_CLASS_TEXT) = EditorInfo().apply {
        inputType = type
        initialSelStart = text.length
        initialSelEnd = text.length
    }

    /** A field shown, then the mode key until QWERTY. */
    private fun startInput(info: EditorInfo = field()) {
        service.onStartInputView(info, false)
        while (service.modes.current != InputMode.QWERTY) service.dispatcher.onModeKey()
    }

    /** Runs the main looper until [done] or 5 s (the dictionary is read on a background thread). */
    private fun waitFor(done: () -> Boolean) {
        val looper = Shadows.shadowOf(Looper.getMainLooper())
        val end = System.currentTimeMillis() + 5000
        while (true) {
            looper.idle()
            if (done()) return
            check(System.currentTimeMillis() < end) { "timed out" }
            Thread.sleep(5)
        }
    }

    private fun type(s: String) {
        for (ch in s) {
            if (ch == ' ') {
                press(FunctionKey.SPACE)
                continue
            }
            val k = TapLayouts.QWERTY.keys.single { it.id == if (ch == '.') "period" else ch.toString() }
            service.dispatcher.onTapResult(k, TapKeyResolver.resolve(k, TapGesture.Tap), TapGesture.Tap)
            notifyCursor()
        }
    }

    private fun press(f: FunctionKey) {
        service.dispatcher.onFunctionDown(f)
        service.dispatcher.onFunctionUp(f)
        notifyCursor()
    }

    private var cursor = 0

    /** The editor's notice for where the cursor now is, as it comes after the keyboard's writes. */
    private fun notifyCursor() {
        val old = cursor
        cursor = text.length
        service.onUpdateSelection(old, old, cursor, cursor, -1, -1)
    }

    private val bar: SuggestionBarView? get() = service.root.suggestionBar
    private fun cells(): List<String> = bar?.let { b -> (0 until b.filledCount).map { b.cellText(it)!!.text } } ?: emptyList()

    private fun reads() = calls.filter { it.startsWith("getTextBeforeCursor") || it.startsWith("getTextAfterCursor") || it.startsWith("getSelectedText") || it.startsWith("deleteSurroundingText") }

    private fun barChildren() = (0 until service.root.childCount).count { service.root.getChildAt(it) is SuggestionBarView }

    private fun height(): Int {
        val root = service.root
        root.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(4000, View.MeasureSpec.AT_MOST))
        return root.measuredHeight
    }

    private fun rowPx(): Int = Math.round(service.root.rowHeightDp * service.resources.displayMetrics.density)

    private fun waitAttached() = waitFor { service.suggestion.active }

    @Test
    fun `SV-01 setting off - no bar child, same height, no reads, no dictionary read`() {
        start(wordSuggest = false)
        startInput()
        val h = height()
        type("the ")
        press(FunctionKey.BACKSPACE)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals("the", text.toString())
        assertEquals(0, barChildren())
        assertNull(bar)
        assertEquals(0, service.root.barHeight)
        assertEquals(h, height())
        assertEquals(emptyList<String>(), reads())
        assertEquals(0, loads)
        assertFalse(service.suggestion.active)
    }

    @Test
    fun `SV-02 setting on at start - the dictionary attaches and the bar shows they, there, then`() {
        start(wordSuggest = true)
        assertNotNull(bar)
        assertEquals(1, barChildren())
        startInput()
        waitAttached()
        assertEquals(1, loads)
        type("the")
        assertEquals(listOf("they", "there", "then"), cells())
    }

    @Test
    fun `SV-03 setting on, password, URI and number fields - empty bar, no reads`() {
        start(wordSuggest = true)
        startInput()
        waitAttached()
        val fields = listOf(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_CLASS_NUMBER,
        )
        for (t in fields) {
            text.setLength(0)
            cursor = 0
            service.onStartInputView(field(t), false)
            while (service.modes.current != InputMode.QWERTY) service.dispatcher.onModeKey()
            calls.clear()
            if (t == InputType.TYPE_CLASS_NUMBER) {
                type("1")
            } else {
                type("the")
            }
            assertEquals("$t", emptyList<String>(), cells())
            assertEquals("$t", emptyList<String>(), reads())
        }
    }

    @Test
    fun `SV-04 notices at the keyboard's own positions keep the word, a touch elsewhere drops it`() {
        start(wordSuggest = true)
        startInput()
        waitAttached()
        type(" the")
        assertEquals(listOf("they", "there", "then"), cells())
        press(FunctionKey.BACKSPACE)   // " th"
        assertEquals(listOf("the", "that", "they"), cells())
        repeat(2) { service.dispatcher.onFunctionDown(FunctionKey.BACKSPACE) }   // a repeat: " "
        service.dispatcher.onFunctionUp(FunctionKey.BACKSPACE)
        notifyCursor()
        type("the")
        assertEquals(listOf("they", "there", "then"), cells())
        service.suggestion.pick(0, service.suggestion.revision)   // " they "
        notifyCursor()
        assertEquals(" they ", text.toString())
        press(FunctionKey.BACKSPACE)   // the undo: " the"
        assertEquals(" the", text.toString())
        assertEquals(listOf("they", "there", "then"), cells())
        service.suggestion.pick(1, service.suggestion.revision)   // " there "
        notifyCursor()
        type(".")   // the auto space goes: " there."
        assertEquals(" there.", text.toString())
        type(" mon")
        assertEquals(listOf("Monday", "money", "month"), cells())

        // The user touches somewhere else.
        service.onUpdateSelection(cursor, cursor, 2, 2, -1, -1)
        cursor = 2
        assertEquals(emptyList<String>(), cells())
    }

    @Test
    fun `SV-05 turned on and off while shown - the bar comes and goes with one row of height, the dictionary is read`() {
        start(wordSuggest = false)
        startInput()
        val h = height()
        store { it.copy(wordSuggest = true) }
        waitFor { barChildren() == 1 }
        assertEquals(h + rowPx(), height())
        waitAttached()
        assertEquals(1, loads)
        type("the")
        assertEquals(listOf("they", "there", "then"), cells())

        store { it.copy(wordSuggest = false) }
        waitFor { barChildren() == 0 }
        assertNull(service.bar)
        assertEquals(h, height())
        assertFalse(service.suggestion.active)
        calls.clear()
        type("a")
        assertEquals(emptyList<String>(), reads())   // no provider
    }

    @Test
    fun `SV-06 th, then Dubeolsik - empty bar and the same height - back in QWERTY still empty`() {
        // Graffiti off, so that the mode key goes QWERTY ↔ Dubeolsik.
        start(wordSuggest = true, base = Settings.DEFAULT.copy(graffitiEnabled = false))
        startInput()
        waitAttached()
        type("th")
        assertEquals(listOf("the", "that", "they"), cells())
        val h = height()
        service.dispatcher.onModeKey()
        assertEquals(InputMode.DUBEOLSIK, service.modes.current)
        assertEquals(emptyList<String>(), cells())
        assertEquals(h, height())
        while (service.modes.current != InputMode.QWERTY) service.dispatcher.onModeKey()
        assertEquals(emptyList<String>(), cells())
        type("e")   // no word from before: a new one after "th" is blocked (letters before it)
        assertEquals(emptyList<String>(), cells())
    }

    @Test
    fun `SV-07 th, then each lifecycle event - empty bar`() {
        val events: List<Pair<String, () -> Unit>> = listOf(
            "onFinishInputView" to { service.onFinishInputView(false) },
            "onFinishInput" to { service.onFinishInput() },
            "onConfigurationChanged" to {
                service.onConfigurationChanged(Configuration(service.resources.configuration).apply { orientation = Configuration.ORIENTATION_LANDSCAPE })
            },
            "onStartInputView restarting" to { service.onStartInputView(field(), true) },
        )
        start(wordSuggest = true)
        startInput()
        waitAttached()
        for ((name, event) in events) {
            type(" th")
            assertEquals(name, listOf("the", "that", "they"), cells())
            event()
            assertEquals(name, emptyList<String>(), cells())
            if (service.modes.current != InputMode.QWERTY || !service.suggestion.fieldAllowed) startInput()
        }
    }

    @Test
    fun `SV-08 the dictionary read throws - site and class logged only, the service goes on, empty bar, input as usual`() {
        start(wordSuggest = true) { throw IllegalStateException("word list resource missing") }
        startInput()
        val log = File(service.filesDir, "errors.log")
        waitFor { log.isFile && log.readText().contains("EnglishDictionary.load") }
        val line = log.readText().lines().single { it.startsWith("EnglishDictionary.load") }
        assertEquals("EnglishDictionary.load", line.split('\t')[0])
        assertEquals("IllegalStateException", line.split('\t')[1])
        assertFalse(line.contains("missing"))
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertFalse(service.suggestion.active)
        type("the")
        assertEquals("the", text.toString())
        assertEquals(emptyList<String>(), cells())
        assertTrue(reads().isEmpty())
    }
}
