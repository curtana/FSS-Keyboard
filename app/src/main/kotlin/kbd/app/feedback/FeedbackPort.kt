package kbd.app.feedback

/** Key-press vibration and sound boundary used by the views and `InputDispatcher` (LLD-08 §8). */
interface FeedbackPort {
    /** On key DOWN only (FR-CMN-11a ②, FR-SET-03). */
    fun key()

    /** Unsupported super/subscript (FR-TAP-01b), graffiti recognition failure (FR-GRF-08). */
    fun error()

    /**
     * The floating keyboard's move began (FR-CMN-15 ⑦ 나, FSS#86): one vibration, no sound; not key feedback. Ports that
     * have no keyboard to move leave it empty.
     */
    fun grab() {}
}
