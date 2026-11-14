package kbd.app.graffiti

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import kbd.app.store.ErrorLog
import kbd.app.store.PersonalStrokeStore
import kbd.graffiti.nn.GraffitiModel
import kbd.graffiti.nn.GraffitiModelLoader
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.TemplateSet

/**
 * Supplies the graffiti recognizer (LLD-05 §11, HLD 5.1): the shipped model plus the personal strokes of [store].
 * Android-free.
 *
 * Reading the model and preparing the recognizer take tens of milliseconds, the personal strokes up to a few hundred
 * (LLD-11 §6.7), so they run on one background thread: [warm] starts the work and returns, and the keyboard's first
 * display does not wait for it. [get] gives the recognizer for the store's current content, waiting for work in
 * progress; a stroke is recognized whole by the recognizer [get] returned for it.
 *
 * When the model cannot be read or does not fit the templates, the recognizer goes by the templates alone
 * ([Recognizer] without a model); the failure is logged once. The input method never stops for it (NFR-03).
 */
class GraffitiRecognizers(
    private val templates: TemplateSet,
    private val store: PersonalStrokeStore,
    private val log: ErrorLog,
    private val model: () -> GraffitiModel? = { sharedModel(log) },
    private val executor: Executor = background,
) {
    private class Built(val revision: Int, val recognizer: Recognizer)

    private var built: Built? = null
    private var pending: FutureTask<Built>? = null
    private var pendingRevision = -1

    /** Starts building the recognizer for the store's current content unless it is there or on its way. Does not wait. */
    fun warm() {
        task()
    }

    /** The recognizer for the store's current content; waits for the build when it is not done yet. */
    fun get(): Recognizer = task()?.get()?.recognizer ?: synchronized(this) { checkNotNull(built).recognizer }

    /** The build in progress for the current revision, started if needed; null when [built] is already current. */
    @Synchronized
    private fun task(): FutureTask<Built>? {
        val revision = store.revision
        if (built?.revision == revision) return null
        pending?.takeIf { pendingRevision == revision }?.let { return it }
        val before = built?.recognizer
        val t = FutureTask {
            Built(revision, build(before)).also { done -> synchronized(this) { if ((built?.revision ?: -1) <= done.revision) built = done } }
        }
        pending = t
        pendingRevision = revision
        executor.execute(t)
        return t
    }

    /** [before] with the store's strokes, or a new recognizer; never throws. */
    private fun build(before: Recognizer?): Recognizer {
        val personal = store.book().strokes
        return try {
            (before ?: Recognizer(templates, model())).withPersonal(personal)
        } catch (e: Exception) {
            log.record(SITE_BUILD, e)
            try {
                Recognizer(templates).withPersonal(personal)   // the templates alone (LLD-05 §3.6)
            } catch (e2: Exception) {
                log.record(SITE_BUILD, e2)
                Recognizer(TemplateSet(emptyMap()))             // rule strokes only (LLD-09 §4)
            }
        }
    }

    companion object {
        const val SITE_MODEL = "GraffitiModelLoader.load"
        const val SITE_BUILD = "GraffitiRecognizers.build"

        /** One daemon thread for every build of the process: builds never run side by side. */
        val background: Executor = Executors.newSingleThreadExecutor { r -> Thread(r, "graffiti-recognizer").apply { isDaemon = true } }

        private val modelLock = Any()
        private var modelRead = false
        private var sharedModel: GraffitiModel? = null

        /** The shipped model, read once per process (LLD-05 §7.1); null, logged once, when it cannot be read. */
        fun sharedModel(log: ErrorLog): GraffitiModel? = synchronized(modelLock) {
            if (!modelRead) {
                modelRead = true
                sharedModel = try {
                    checkNotNull(GraffitiModelLoader::class.java.getResourceAsStream(GraffitiModelLoader.RESOURCE)) { "missing ${GraffitiModelLoader.RESOURCE}" }
                        .use { GraffitiModelLoader.load(it) }
                } catch (e: Exception) {
                    log.record(SITE_MODEL, e)
                    null
                }
            }
            sharedModel
        }
    }
}
