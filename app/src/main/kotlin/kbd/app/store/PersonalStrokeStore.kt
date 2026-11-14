package kbd.app.store

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kbd.graffiti.recognize.PersonalStroke
import kbd.graffiti.recognize.Recognizer
import kbd.graffiti.recognize.SetName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The strokes the user stored in "my strokes" (LLD-07 §8.2, SRS FR-TUT-15): per letter (its set and its
 * `output.value`) a list of strokes, the oldest first, at most [MAX_PER_LETTER]. Immutable; Android-free.
 */
class PersonalStrokeBook(val letters: Map<Pair<SetName, String>, List<PersonalStroke>>) {

    /** Every stroke, as [Recognizer.withPersonal] takes them. */
    val strokes: List<PersonalStroke> get() = letters.values.flatten()

    /** Letters with at least one stroke (the topic line of the practice list, TBD-30 ⑩). */
    val taughtLetters: Int get() = letters.count { it.value.isNotEmpty() }

    fun count(set: SetName, value: String): Int = letters[set to value]?.size ?: 0

    /** With one more stroke of the letter; past [MAX_PER_LETTER] the oldest ones go (FR-TUT-15 ②). */
    fun add(set: SetName, value: String, points: List<Pair<Float, Float>>): PersonalStrokeBook {
        require(set in Recognizer.PERSONAL_SETS) { "no personal strokes in $set" }
        require(points.isNotEmpty()) { "empty stroke" }
        require(points.all { it.first.isFinite() && it.second.isFinite() }) { "coordinate is no finite number" }
        val list = (letters[set to value].orEmpty() + PersonalStroke(set, value, points.toList())).takeLast(MAX_PER_LETTER)
        return PersonalStrokeBook(letters + ((set to value) to list))
    }

    /** Without the strokes of the letter (FR-TUT-16 ①②). */
    fun clear(set: SetName, value: String): PersonalStrokeBook = PersonalStrokeBook(letters - (set to value))

    companion object {
        const val MAX_PER_LETTER = 8
        val EMPTY = PersonalStrokeBook(emptyMap())
    }
}

/**
 * The personal stroke file (LLD-07 §8.2): `{"version":1,"letters":[{"set":…,"value":…,"strokes":[[[x,y],…],…]}]}`,
 * coordinates in dp written so that the same 32-bit values are read back. Embeddings and personal templates are not
 * stored: the recognizer computes them from the strokes (FR-TUT-15 ①).
 */
object PersonalStrokeCodec {
    const val VERSION = 1

    fun encode(book: PersonalStrokeBook): String = buildString {
        append("{\"version\":").append(VERSION).append(",\"letters\":[")
        var first = true
        for ((key, list) in book.letters) {
            if (list.isEmpty()) continue
            if (!first) append(',')
            first = false
            append("{\"set\":\"").append(key.first.name).append("\",\"value\":").append(JsonPrimitive(key.second).toString())
            append(",\"strokes\":[")
            list.forEachIndexed { i, s ->
                if (i > 0) append(',')
                append('[')
                s.points.forEachIndexed { k, (x, y) ->
                    if (k > 0) append(',')
                    append('[').append(x.toString()).append(',').append(y.toString()).append(']')
                }
                append(']')
            }
            append("]}")
        }
        append("]}")
    }

    /**
     * The book of [text]. Malformed JSON, a missing or wrongly typed field, a version other than [VERSION] or a
     * coordinate that is no finite number → [IllegalStateException]. Left out without an error: letters of a set that
     * takes no personal strokes, strokes without points, and the oldest strokes past the limit.
     */
    fun decode(text: String): PersonalStrokeBook = try {
        val root = Json.parseToJsonElement(text).jsonObject
        val version = field(root, "version").jsonPrimitive.int
        check(version == VERSION) { "unsupported personal stroke version $version" }
        val letters = LinkedHashMap<Pair<SetName, String>, List<PersonalStroke>>()
        for (e in field(root, "letters").jsonArray) {
            val o = e.jsonObject
            val setName = field(o, "set").jsonPrimitive.also { check(it.isString) { "set is no string" } }.content
            val value = field(o, "value").jsonPrimitive.also { check(it.isString) { "value is no string" } }.content
            val strokes = field(o, "strokes").jsonArray.map { points(it.jsonArray) }
            val set = SetName.entries.firstOrNull { it.name == setName } ?: continue
            if (set !in Recognizer.PERSONAL_SETS) continue
            val list = (letters[set to value].orEmpty() + strokes.filter { it.isNotEmpty() }.map { PersonalStroke(set, value, it) })
                .takeLast(PersonalStrokeBook.MAX_PER_LETTER)
            if (list.isNotEmpty()) letters[set to value] = list
        }
        PersonalStrokeBook(letters)
    } catch (e: IllegalArgumentException) {   // malformed JSON or wrong JSON kind (SerializationException included)
        throw IllegalStateException("malformed personal stroke file", e)
    }

    private fun points(a: JsonArray): List<Pair<Float, Float>> = a.map { p ->
        val xy = p.jsonArray
        check(xy.size == 2) { "point must be [x, y]" }
        val x = xy[0].jsonPrimitive.float
        val y = xy[1].jsonPrimitive.float
        check(x.isFinite() && y.isFinite()) { "coordinate is no finite number" }
        x to y
    }

    private fun field(o: JsonObject, key: String): JsonElement = o[key] ?: error("missing \"$key\"")
}

/**
 * The personal stroke file and its content in memory (LLD-07 §8.2). One object per process is shared by the keyboard
 * and the practice screen (`AppStores.personalStrokes`), so [revision] tells the recognizers when to rebuild
 * (LLD-05 §11). Only the "my strokes" screen calls [add], [clear] and [clearAll] (FR-TUT-17 ①); everything else reads.
 *
 * A file that is missing or cannot be read is an empty book (FR-TUT-15 ④): the failure is logged by site and
 * exception kind only (NFR-05) and the file is left as it is until the next write, which first moves it to
 * `<name>.bak` (one, the latest unreadable file). A write goes to a temporary file, synced to the disk, that then
 * replaces the real one in one step, so a write that stops half-way or a power cut leaves the old file.
 */
class PersonalStrokeStore(val file: File, private val log: ErrorLog) {

    private var loaded: PersonalStrokeBook? = null

    /** The file was there but could not be read: the next write keeps it as [BAK_SUFFIX] before replacing it. */
    private var unreadable = false

    /** Goes up by one with every successful write. */
    @Volatile
    var revision: Int = 0
        private set

    @Synchronized
    fun book(): PersonalStrokeBook = loaded ?: read().also { loaded = it }

    /** Stores one more stroke of the letter; false when the file could not be written (nothing changes then). */
    @Synchronized
    fun add(set: SetName, value: String, points: List<Pair<Float, Float>>): Boolean = write(book().add(set, value, points))

    @Synchronized
    fun clear(set: SetName, value: String): Boolean = write(book().clear(set, value))

    /** Read first all the same, so that an unreadable file is kept as [BAK_SUFFIX] rather than replaced unseen. */
    @Synchronized
    fun clearAll(): Boolean {
        book()
        return write(PersonalStrokeBook.EMPTY)
    }

    private fun read(): PersonalStrokeBook = try {
        if (file.isFile) PersonalStrokeCodec.decode(file.readText(Charsets.UTF_8)) else PersonalStrokeBook.EMPTY
    } catch (e: Exception) {
        log.record(SITE_READ, e)
        unreadable = true
        PersonalStrokeBook.EMPTY
    }

    /** False, logged, when anything fails; the book in memory and [revision] stay then as they were. */
    private fun write(book: PersonalStrokeBook): Boolean = try {
        val tmp = File(file.parentFile, file.name + TMP_SUFFIX)
        FileOutputStream(tmp).use { out ->
            out.write(PersonalStrokeCodec.encode(book).toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        if (unreadable) {
            if (file.exists()) {
                Files.move(file.toPath(), File(file.parentFile, file.name + BAK_SUFFIX).toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            unreadable = false
        }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        loaded = book
        revision++
        true
    } catch (e: Exception) {
        log.record(SITE_WRITE, e)
        false
    }

    companion object {
        const val FILE_NAME = "personal-strokes.json"
        const val TMP_SUFFIX = ".tmp"
        const val BAK_SUFFIX = ".bak"
        const val SITE_READ = "PersonalStrokeStore.read"
        const val SITE_WRITE = "PersonalStrokeStore.write"
    }
}
