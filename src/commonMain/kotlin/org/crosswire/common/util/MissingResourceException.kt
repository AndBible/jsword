package org.crosswire.common.util

/**
 * `java.util.MissingResourceException` for common code. On the JVM it is the JDK class, so existing
 * catch sites keep working.
 */
expect open class MissingResourceException(s: String?, className: String?, key: String?) : RuntimeException
