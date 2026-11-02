package kbd.app.error

import kbd.app.store.ErrorLog

/** State rolled back when a guarded action fails (LLD-09 §2). Implemented by `InputDispatcher`. */
interface GuardedState {
    fun snapshot(): Any
    fun restore(s: Any)
}

/**
 * Wraps engine calls so one failing stroke is dropped instead of crashing the keyboard (LLD-09 §2,
 * NFR-03). Only [Exception] is caught; [Error] (OOM and the like) propagates (D-31).
 */
class EngineGuard(private val log: ErrorLog, private val state: GuardedState) {

    /** Calculation that changes no state. Returns null on failure. */
    fun <T> run(site: String, block: () -> T): T? = try {
        block()
    } catch (e: Exception) {
        log.record(site, e)
        null
    }

    /** One state-changing action: snapshot → run → restore on failure. Returns null on failure. */
    fun <T> stroke(site: String, block: () -> T): T? {
        val s = state.snapshot()
        return try {
            block()
        } catch (e: Exception) {
            state.restore(s)
            log.record(site, e)
            null
        }
    }
}
