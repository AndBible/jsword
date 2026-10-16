package org.crosswire.jsword.index.analysis

import org.crosswire.common.util.IosKswordResources
import org.crosswire.jsword.index.analysis.thai.ThaiBreakPort

internal actual object ThaiWordBreaker {
    private val port: ThaiBreakPort by lazy {
        ThaiBreakPort(
            checkNotNull(IosKswordResources.bytes("search/openjdk17/WordBreakIteratorData_th")) { "Thai rule data is not embedded" },
            checkNotNull(IosKswordResources.bytes("search/openjdk17/thai_dict")) { "Thai dictionary is not embedded" },
        )
    }

    actual fun session(): ThaiWordSplitter = ThaiWordSplitter { words(it) }

    actual fun words(text: String): List<IntRange> {
        val b = port.boundaries(text)
        val out = ArrayList<IntRange>()
        for (i in 0 until b.size - 1) if (b[i] < b[i + 1]) out.add(b[i] until b[i + 1])
        return out
    }
}
