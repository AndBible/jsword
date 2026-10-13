package org.crosswire.jsword.book.install

import org.crosswire.common.util.KUri

open class DownloadCancelledException(uri: KUri?) : InstallException("Downloading $uri was cancelled")
