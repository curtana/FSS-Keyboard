package kbd.app.swipe

import org.junit.Assert.assertTrue
import org.junit.Test

/** MOD-04 (LLD-11 §7.5, LLD-03 §2.2): one edition (issue #251), so the provider always gives the swipe engine. */
class SwipeModuleProviderTest {

    @Test
    fun `MOD-04 provider gives the swipe engine module`() {
        val m = SwipeModuleProvider.get()
        assertTrue(m === kbd.app.swipe.impl.SwipeEngineModule)
        assertTrue(m === SwipeModuleProvider.get())   // singleton
        assertTrue(m.tutorSteps().isNotEmpty())
    }
}
