package kbd.stroke.tap

import kbd.stroke.geom.Dir

/**
 * The physical gesture on one tap-keyboard key (LLD-05A §2.1), named by how many times the
 * stroke crossed its first exit edge. Mapping a gesture to output is `TapKeyResolver`'s job (§2.2).
 */
sealed interface TapGesture {
    data object Tap : TapGesture

    /** The first exit edge crossed once. */
    data class Exit(val dir: Dir) : TapGesture

    /** Crossed twice (out and back in); resolves like [Exit] (§2.2). */
    data class ExitReturn(val dir: Dir) : TapGesture

    /** Crossed three or more times (rubbing). */
    data class Rub(val dir: Dir) : TapGesture
}
