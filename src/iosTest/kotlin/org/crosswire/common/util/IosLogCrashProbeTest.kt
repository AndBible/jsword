package org.crosswire.common.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLog
import platform.posix.fflush
import platform.posix.fputs
import platform.posix.stderr
import kotlin.test.Test

/**
 * Temporary diagnostic (CI run 37990330120: the iOS test process SIGSEGVs on the first NSLogLogger.error call).
 * Steps run in order of increasing suspicion; each prints a marker to stderr first, so the last marker in the
 * CI log names the step that crashed. Delete once the root cause is fixed.
 */
@OptIn(ExperimentalForeignApi::class)
class IosLogCrashProbeTest {
    private fun mark(step: String) {
        fputs("PROBE $step\n", stderr)
        fflush(stderr)
    }

    @Test fun probe() {
        mark("1 formatForLog")
        val r = Slf4jFormat.formatForLog("a={} b={}", arrayOf("1", "-3"))
        mark("1 ok: ${r.message}")

        mark("2 getLogger")
        val log = LoggerFactory.getLogger("org.crosswire.jsword.book.sword.SwordUtil")
        mark("2 ok: ${log.name} errorEnabled=${log.isErrorEnabled}")

        mark("3 NSLog literal, no varargs")
        NSLog("probe literal")
        mark("3 ok")

        mark("4 NSLog %@ with a literal String")
        NSLog("%@", "probe literal arg")
        mark("4 ok")

        mark("5 NSLog %@ with a built String")
        NSLog("%@", buildString { append("ERROR ").append(log.name).append(" - ").append(r.message) })
        mark("5 ok")

        mark("6 log.error")
        log.error("Nothing to read at offset = {} returning empty because negative size={}", "5", "-3")
        mark("6 ok")
    }
}
