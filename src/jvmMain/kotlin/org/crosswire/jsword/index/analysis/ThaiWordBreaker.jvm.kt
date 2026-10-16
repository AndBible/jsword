package org.crosswire.jsword.index.analysis

import java.text.BreakIterator
import java.util.Locale

internal actual object ThaiWordBreaker {
    /** A fresh `BreakIterator` per session, created on first use: it is not thread safe and analyzers are shared. */
    actual fun session(): ThaiWordSplitter = object : ThaiWordSplitter {
        private var breaker: BreakIterator? = null

        override fun words(text: String): List<IntRange> {
            val b = breaker ?: BreakIterator.getWordInstance(Locale("th")).also { breaker = it }
            b.setText(text)
            val out = ArrayList<IntRange>()
            var start = b.first()
            var end = b.next()
            while (end != BreakIterator.DONE) {
                if (end > start) out.add(start until end)
                start = end
                end = b.next()
            }
            return out
        }
    }

    actual fun words(text: String): List<IntRange> = session().words(text)
}
