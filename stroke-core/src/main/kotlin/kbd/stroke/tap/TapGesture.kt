package kbd.stroke.tap

import kbd.stroke.geom.Way

/**
 * The physical gesture on one tap-keyboard key (LLD-05A §2.1), named by how many times the
 * stroke crossed its first exit edge. Mapping a gesture to output is `TapKeyResolver`'s job (§2.2).
 * [Exit.dir] and the others are a [kbd.stroke.geom.Dir] — the first exit edge — except on an 8-way key
 * (a key with a diagonal assignment, FSS#109), where the angle at the first exit decides and may give a
 * [kbd.stroke.geom.Diag].
 */
sealed interface TapGesture {
    data object Tap : TapGesture

    /** The first exit edge crossed once. */
    data class Exit(val dir: Way) : TapGesture

    /** Crossed twice (out and back in); resolves like [Exit] (§2.2). */
    data class ExitReturn(val dir: Way) : TapGesture

    /** Crossed three or more times (rubbing). */
    data class Rub(val dir: Way) : TapGesture
}
