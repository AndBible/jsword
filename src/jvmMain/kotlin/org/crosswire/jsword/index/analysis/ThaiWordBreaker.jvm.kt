package org.crosswire.jsword.index.analysis

import java.text.BreakIterator
import java.util.Locale

internal actual object ThaiWordBreaker {
    /** A fresh `BreakIterator` per call: it is not thread safe and analyzers are shared. */
    actual fun words(text: String): List<IntRange> {
        val breaker = BreakIterator.getWordInstance(Locale("th"))
        breaker.setText(text)
        val out = ArrayList<IntRange>()
        var start = breaker.first()
        var end = breaker.next()
        while (end != BreakIterator.DONE) {
            if (end > start) out.add(start until end)
            start = end
            end = breaker.next()
        }
        return out
    }
}
