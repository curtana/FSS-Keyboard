package kbd.suggest

/** Text around the cursor a source may ask for. null = could not read. */
interface TextPeek {
    fun before(n: Int): String?

    fun after(n: Int): String?

    fun hasSelection(): Boolean?
}

/**
 * Language-neutral suggestion provider (HLD 5B.1, LLD-12 §6). Called on the input-method main thread only;
 * implementations need not be thread-safe. Call order: LLD-12 §6.1.
 */
interface SuggestionSource {
    /** Cell contents in rank order; empty when there is nothing to suggest. No length limit. */
    val candidates: List<Candidate>

    /** Asked **before** the normal path. The only place a source may peek. */
    fun onKey(key: SuggestKey, peek: TextPeek): KeyAnswer

    /** Told **after** the normal path ran (Pass, WriteThenPass, and a Consume that fell back with OnFail.PASS). */
    fun afterKey(key: SuggestKey)

    /** Cell [index] was picked. The plan to write, or null (state unchanged). A plan counts as written. */
    fun pick(index: Int): EditPlan?

    /** The plan of the preceding onKey or pick could not be written. */
    fun onWriteFailed()

    fun reset(reason: ResetReason)
}
