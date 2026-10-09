package org.crosswire.common.util

import kotlin.test.Test

/**
 * NSLog is variadic: a Kotlin String passed for `%@` is not an Objective-C object there, and the process
 * dies with SIGSEGV inside NSLog (and-bible CI runs 37990330120 and 37995718911). These calls only have to
 * return; a crash fails the whole iOS test task.
 */
class NSLogLoggerTest {
    private val log = LoggerFactory.getLogger("org.crosswire.jsword.book.sword.SwordUtil")

    @Test fun errorWithPlaceholdersDoesNotCrash() {
        log.error("Nothing to read at offset = {} returning empty because negative size={}", "5", "-3")
    }

    @Test fun nonAsciiAndPercentSignsDoNotCrash() {
        log.warn("äö 100% %@ %s {}", "Johanneksen ilmestys")
    }

    @Test fun throwableIsLogged() {
        log.error("failed", IllegalStateException("boom"))
    }
}
