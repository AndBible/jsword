package org.crosswire.common.util

/** Java `assert` semantics: checked only when the platform enables assertions (JVM `-ea`); no-op on native. */
expect fun jswordAssert(value: Boolean, lazyMessage: () -> Any)
