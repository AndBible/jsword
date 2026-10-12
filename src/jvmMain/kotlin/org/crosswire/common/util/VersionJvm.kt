package org.crosswire.common.util

import java.util.regex.Pattern

/** Legacy JDK pattern for Java callers; the common version parser uses Regex. */
object VersionJvm {
    @JvmField
    val VERSION_PATTERN: Pattern = Pattern.compile("^(\\d+)(?:.(\\d+))?(?:.(\\d+))?(?:.(\\d+))?$")
}
