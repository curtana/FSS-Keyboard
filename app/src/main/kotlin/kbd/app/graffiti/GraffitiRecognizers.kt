package kbd.app.graffiti

import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import kbd.app.store.ErrorLog
import kbd.graffiti.nn.GraffitiModel
import kbd.graffiti.nn.GraffitiModelLoader
import kbd.graffiti.recognize.PersonalStroke
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.TemplateSet

/**
 * Supplies the graffiti recognizer (LLD-05 §11, HLD 5.1): the shipped model with the templates, plus the personal
 * strokes [personal] gives. Android-free.
 *
 * Reading the model and preparing the recognizer take tens of milliseconds, the personal strokes up to a few hundred
 * (LLD-11 §6.7), so they run on one background thread: [warm] starts the work and returns, and the keyboard's first
 * display does not wait for it. [get] gives the recognizer for the current [revision], waiting for work in progress;
 * a stroke is recognized whole by the recognizer [get] returned for it.
 *
 * When the model cannot be read or does not fit the templates, the recognizer goes by the templates alone
 * ([Recognizer] without a model); the failure is logged. The input method never stops for it (NFR-03).
 */
class GraffitiRecognizers(
    private val templates: TemplateSet,
    private val log: ErrorLog,
    private val revision: () -> Int = { 0 },
    private val personal: () -> List<PersonalStroke> = { emptyList() },
    private val model: () -> GraffitiModel? = { sharedModel(log) },
    private val executor: Executor = background,
) {
    /** [fallback]: a stand-in after a build failed past [build]; the next revision builds afresh instead of on it. */
    private class Built(val revision: Int, val recognizer: Recognizer, val fallback: Boolean = false)

    private var built: Built? = null
    private var pending: FutureTask<Built>? = null
    private var pendingRevision = -1

    /** Starts building the recognizer for the current revision unless it is there or on its way. Does not wait. */
    fun warm() {
        task()
    }

    /** The recognizer for the current revision; waits for the build when it is not done yet. Never throws (NFR-03). */
    fun get(): Recognizer {
        val t = task() ?: return synchronized(this) { checkNotNull(built).recognizer }
        return try {
            t.get().recognizer
        } catch (e: ExecutionException) {
            failed(t, e.cause ?: e)
        }
    }

    /**
     * A build that threw past [build] — an [Error] such as [OutOfMemoryError], which [build] does not catch. The failed
     * task is dropped so later strokes do not get the same failure, and the templates alone, else the rule strokes
     * alone, stand in for its revision.
     */
    private fun failed(t: FutureTask<Built>, cause: Throwable): Recognizer {
        log.record(SITE_BUILD, cause)
        val stand = try {
            Recognizer(templates)                  // the templates alone (LLD-05 §3.6)
        } catch (e: Throwable) {
            log.record(SITE_BUILD, e)
            Recognizer(TemplateSet(emptyMap()))    // rule strokes only (LLD-09 §4)
        }
        return synchronized(this) {
            if (pending !== t) return@synchronized built?.recognizer ?: stand
            val rev = pendingRevision
            pending = null
            pendingRevision = -1
            if ((built?.revision ?: -1) <= rev) built = Built(rev, stand, fallback = true)
            stand
        }
    }

    /** The build in progress for the current revision, started if needed; null when [built] is already current. */
    @Synchronized
    private fun task(): FutureTask<Built>? {
        val rev = revision()
        if (built?.revision == rev) return null
        pending?.takeIf { pendingRevision == rev }?.let { return it }
        val before = built?.takeUnless { it.fallback }?.recognizer
        val t = FutureTask {
            Built(rev, build(before)).also { done -> synchronized(this) { if ((built?.revision ?: -1) <= done.revision) built = done } }
        }
        pending = t
        pendingRevision = rev
        executor.execute(t)
        return t
    }

    /** [before] with the current personal strokes, or a new recognizer; never throws. */
    private fun build(before: Recognizer?): Recognizer {
        val strokes = try {
            personal()
        } catch (e: Exception) {
            log.record(SITE_BUILD, e)
            emptyList()
        }
        return try {
            (before ?: Recognizer(templates, model())).withPersonal(strokes)
        } catch (e: Exception) {
            log.record(SITE_BUILD, e)
            try {
                Recognizer(templates).withPersonal(strokes)   // the templates alone (LLD-05 §3.6)
            } catch (e2: Exception) {
                log.record(SITE_BUILD, e2)
                Recognizer(TemplateSet(emptyMap()))           // rule strokes only (LLD-09 §4)
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
                sharedModel = readModel(log)
            }
            sharedModel
        }

        /** Reads the model resource; null, logged, when it is missing or malformed. */
        internal fun readModel(log: ErrorLog, resource: String = GraffitiModelLoader.RESOURCE): GraffitiModel? = try {
            checkNotNull(GraffitiModelLoader::class.java.getResourceAsStream(resource)) { "missing $resource" }
                .use { GraffitiModelLoader.load(it) }
        } catch (e: Exception) {
            log.record(SITE_MODEL, e)
            null
        }
    }
}
