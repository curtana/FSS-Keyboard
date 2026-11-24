package kbd.suggest

/** Editor commands (HLD 5B.1). Step 1 has two. */
sealed interface EditCommand {
    /** Delete [count] characters before the cursor. */
    data class DeleteBefore(val count: Int) : EditCommand

    /** Insert committed [text] at the cursor. */
    data class Insert(val text: String) : EditCommand
}

/** Commands to write when the text right before the cursor is [expectBefore] (empty = no check). */
data class EditPlan(val expectBefore: String, val commands: List<EditCommand>) {
    companion object {
        val NONE = EditPlan("", emptyList())
    }
}

/** What the app does when a [KeyAnswer.Consume] plan cannot be written: nothing, or the normal path. */
enum class OnFail { NOTHING, PASS }

/** A source's answer to a key, asked before the normal path (LLD-12 §6.1). */
sealed interface KeyAnswer {
    /** Normal path only. */
    data object Pass : KeyAnswer

    /** Write [plan], then the normal path inside the same batch. */
    data class WriteThenPass(val plan: EditPlan) : KeyAnswer

    /** Write [plan] only; the normal path is not taken (unless the write fails and [onFail] is PASS). */
    data class Consume(val plan: EditPlan, val onFail: OnFail) : KeyAnswer
}
