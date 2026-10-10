package org.crosswire.common.util

actual open class MissingResourceException actual constructor(
    s: String?,
    val className: String?,
    val key: String?,
) : RuntimeException(s)
