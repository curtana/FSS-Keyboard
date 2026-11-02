package kbd.app.swipe

import kbd.app.swipe.impl.SwipeEngineModule

/** The swipe engine (LLD-03 §2.2, HLD 3.1); one edition since issue #251. */
object SwipeModuleProvider {
    fun get(): SwipeModule = SwipeEngineModule
}
