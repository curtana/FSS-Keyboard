package kbd.app.swipe.impl

import android.content.Context
import android.view.View
import kbd.app.swipe.SwipeHost
import kbd.app.swipe.SwipeModule

/**
 * The swipe module (LLD-03 §2.2): the swipe keyboard view. The input practice (FSS#17) shows this same view
 * through a practice [SwipeHost], so it has no practice keyboards or lessons of its own any more.
 */
object SwipeEngineModule : SwipeModule {
    override fun createSwipeView(context: Context, host: SwipeHost): View = SwipeKeyboardView(context, host)
}
