package org.crosswire.common.util

import org.crosswire.jsword.book.filter.SourceFilter
import org.crosswire.jsword.book.filter.gbf.GBFFilter
import org.crosswire.jsword.book.filter.osis.OSISFilter
import org.crosswire.jsword.book.filter.plaintext.PlainTextFilter
import org.crosswire.jsword.book.filter.thml.THMLFilter

internal fun registerSourceFilters(builder: PluginRegistryBuilder) {
    builder.register(SourceFilter::class, "default", PlainTextFilter::class) { PlainTextFilter() }
    builder.register(SourceFilter::class, "gbf", GBFFilter::class) { GBFFilter() }
    builder.register(SourceFilter::class, "osis", OSISFilter::class) { OSISFilter() }
    builder.register(SourceFilter::class, "plaintext", PlainTextFilter::class) { PlainTextFilter() }
    builder.register(SourceFilter::class, "tei", OSISFilter::class) { OSISFilter() }
    builder.register(SourceFilter::class, "thml", THMLFilter::class) { THMLFilter() }
}
