package kbd.suggest.en

import java.io.InputStreamReader

/** The bundled English word list (LLD-12 §3, §4). */
object EnglishDictionary {
    const val RESOURCE = "/suggest/en_words.txt"

    /** Reads [RESOURCE] as UTF-8 into a [WordDictionary]. A missing resource throws IllegalStateException (no words in messages, NFR-05). */
    fun load(): WordDictionary {
        val stream = EnglishDictionary::class.java.getResourceAsStream(RESOURCE)
            ?: throw IllegalStateException("word list resource missing")
        return InputStreamReader(stream, Charsets.UTF_8).buffered().use { WordDictionary.read(it) }
    }
}
