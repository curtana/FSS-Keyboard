package kbd.hangul.event

/** Composer output (LLD-03 §3.2). */
data class ComposeResult(
    val committed: String,   // text newly committed by this call, "" if none
    val composing: String,   // whole composing text after this call, "" if none
)
