package kbd.suggest.en

import java.io.Reader
import java.util.Locale

/**
 * Array trie over a rank-ordered word list (LLD-12 §3, §4, D-79).
 *
 * Nodes are parallel arrays: `label` (0..25 = a..z, 26 = '), `firstChild` / `nextSibling` (−1 = none)
 * and `top` (K slots per node, −1 = empty). Node 0 is the root. A word id is its accepted-line number.
 */
class WordDictionary private constructor(
    private val label: ByteArray,
    private val firstChild: IntArray,
    private val nextSibling: IntArray,
    private val top: IntArray,
    private val forms: Array<String>,
) : WordIndex {

    override val size: Int get() = forms.size

    override fun form(id: Int): String = forms[id]

    override fun top(prefixLower: String): IntArray? {
        if (prefixLower.isEmpty()) return null
        var node = ROOT
        for (c in prefixLower) {
            val l = labelOf(c)
            if (l < 0) return null
            node = child(firstChild, nextSibling, label, node, l)
            if (node < 0) return null
        }
        val base = node * WordIndex.K
        var n = 0
        while (n < WordIndex.K && top[base + n] >= 0) n++
        return top.copyOfRange(base, base + n) // a copy: callers cannot change the dictionary
    }

    companion object {
        private const val ROOT = 0
        private const val INITIAL_NODES = 1 shl 17
        private const val APOSTROPHE = 26
        private val LINE = Regex("^[A-Za-z]+(?:'[A-Za-z]+)*$")

        /** Reads with the line rules of LLD-12 §3 and builds the trie. I/O exceptions go to the caller. */
        fun read(reader: Reader): WordDictionary {
            val builder = Builder()
            forEachLine(reader) { builder.accept(it) }
            return builder.build()
        }

        /** 0..25 for a..z, 26 for ', −1 otherwise. */
        private fun labelOf(c: Char): Int = when (c) {
            in 'a'..'z' -> c - 'a'
            '\'' -> APOSTROPHE
            else -> -1
        }

        /** Child of [node] with label [l], or −1 (scans the sibling list, at most 27). */
        private fun child(firstChild: IntArray, nextSibling: IntArray, label: ByteArray, node: Int, l: Int): Int {
            var c = firstChild[node]
            while (c >= 0 && label[c].toInt() != l) c = nextSibling[c]
            return c
        }

        /** Splits on LF only and strips one trailing CR (LLD-12 §3). The last line may lack its LF. */
        private inline fun forEachLine(reader: Reader, each: (String) -> Unit) {
            val buf = CharArray(8192)
            val line = StringBuilder()
            var open = false
            while (true) {
                val n = reader.read(buf)
                if (n < 0) break
                for (i in 0 until n) {
                    val ch = buf[i]
                    if (ch == '\n') {
                        each(stripCr(line))
                        line.setLength(0)
                        open = false
                    } else {
                        line.append(ch)
                        open = true
                    }
                }
            }
            if (open) each(stripCr(line))
        }

        private fun stripCr(line: StringBuilder): String =
            if (line.isNotEmpty() && line[line.length - 1] == '\r') line.substring(0, line.length - 1) else line.toString()
    }

    /** Grows the node arrays by doubling, starting at 2^17 nodes (LLD-12 §4). */
    private class Builder {
        private var label = ByteArray(INITIAL_NODES)
        private var firstChild = IntArray(INITIAL_NODES)
        private var nextSibling = IntArray(INITIAL_NODES)
        /** Word id ending at the node, −1 = none. Only used to skip a repeated lowercase key. */
        private var end = IntArray(INITIAL_NODES)
        private var top = IntArray(INITIAL_NODES * WordIndex.K)
        private var nodes = 0
        private val forms = ArrayList<String>()

        init {
            newNode(0) // root; its label is never read
        }

        fun accept(line: String) {
            if (line.startsWith("#")) return
            if (line.length > WordChars.MAX_WORD || !LINE.matches(line)) return
            val key = line.lowercase(Locale.ROOT)
            val found = find(key)
            if (found >= 0 && end[found] >= 0) return // same lowercase key as an earlier line: keep the earlier
            val id = forms.size
            forms += line
            var node = ROOT
            for (c in key) {
                val l = labelOf(c)
                var next = child(firstChild, nextSibling, label, node, l)
                if (next < 0) {
                    next = newNode(l)
                    nextSibling[next] = firstChild[node] // new child goes to the front of the sibling list
                    firstChild[node] = next
                }
                node = next
                // Lines arrive in rank order, so the first empty slot keeps the K slots in rank order.
                val base = node * WordIndex.K
                for (k in 0 until WordIndex.K) {
                    if (top[base + k] < 0) {
                        top[base + k] = id
                        break
                    }
                }
            }
            end[node] = id
        }

        fun build(): WordDictionary = WordDictionary(label, firstChild, nextSibling, top, forms.toTypedArray())

        /** The node for [key], or −1. */
        private fun find(key: String): Int {
            var node = ROOT
            for (c in key) {
                node = child(firstChild, nextSibling, label, node, labelOf(c))
                if (node < 0) return -1
            }
            return node
        }

        private fun newNode(l: Int): Int {
            if (nodes == label.size) grow()
            val n = nodes++
            label[n] = l.toByte()
            firstChild[n] = -1
            nextSibling[n] = -1
            end[n] = -1
            top.fill(-1, n * WordIndex.K, (n + 1) * WordIndex.K)
            return n
        }

        private fun grow() {
            val cap = label.size * 2
            label = label.copyOf(cap)
            firstChild = firstChild.copyOf(cap)
            nextSibling = nextSibling.copyOf(cap)
            end = end.copyOf(cap)
            top = top.copyOf(cap * WordIndex.K)
        }
    }
}
