package org.crosswire.common.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLog
import platform.Foundation.NSString
import platform.posix.fflush
import platform.posix.fputs
import platform.posix.stderr
import kotlin.test.Test

/**
 * Temporary diagnostic. CI run 37990330120: the iOS test process SIGSEGVs on the first NSLogLogger.error call;
 * the I0 app's crash report shows the same class of crash inside NSLog("bridge[%@]: %@", ...) with Kotlin
 * Strings (objc_opt_respondsToSelector on a bad pointer). CI runs each probe in its own process, because a
 * crash ends the process; the stderr markers name the step that crashed. Delete once the root cause is fixed.
 */
@OptIn(ExperimentalForeignApi::class)
class IosLogCrashProbeTest {
    private val text = "ERROR org.crosswire.jsword.book.sword.SwordUtil - a=1 b=-3 äö"

    private fun mark(step: String) {
        fputs("PROBE $step\n", stderr)
        fflush(stderr)
    }

    @Test fun probe1CommonCode() {
        mark("1 formatForLog")
        val r = Slf4jFormat.formatForLog("a={} b={}", arrayOf("1", "-3"))
        mark("1 ok: ${r.message}")
        mark("1 getLogger")
        val log = LoggerFactory.getLogger("org.crosswire.jsword.book.sword.SwordUtil")
        mark("1 ok: ${log.name} errorEnabled=${log.isErrorEnabled}")
        mark("1 NSLog literal, no varargs")
        NSLog("probe literal")
        mark("1 ok")
    }

    @Test fun probe2PercentS() {
        mark("2 NSLog %s with a Kotlin String (candidate fix)")
        NSLog("%s", text)
        mark("2 ok")
    }

    @Test fun probe3ExplicitNSString() {
        mark("3 NSLog %@ with an explicit NSString (candidate fix)")
        NSLog("%@", text as NSString)
        mark("3 ok")
    }

    @Test fun probe4PercentAtString() {
        mark("4 NSLog %@ with a Kotlin String (suspected crash)")
        NSLog("%@", text)
        mark("4 ok")
    }

    @Test fun probe5LogError() {
        mark("5 NSLogLogger.error")
        LoggerFactory.getLogger("org.crosswire.jsword.book.sword.SwordUtil")
            .error("Nothing to read at offset = {} returning empty because negative size={}", "5", "-3")
        mark("5 ok")
    }
}
