package org.crosswire.jsword.book.filter

import org.crosswire.jsword.book.OSISUtil

internal actual typealias FilterOsisFactory = OSISUtil.OSISFactory
internal actual fun filterOsisFactory(): FilterOsisFactory = OSISUtil.factory()
