package org.crosswire.common.util

import org.junit.Test
import java.io.ObjectStreamClass
import kotlin.test.*

class ReporterCharacterizationTest {
    @Test fun eventsRejectNullAndKeepSourceNamesAndSerialIds() {
        assertFailsWith<IllegalArgumentException> { ReporterEvent(null, "message") }
        val event = ReporterEvent(String::class.java, "message")
        assertSame(String::class.java, event.getSource())
        assertEquals("String", event.sourceName)
        assertEquals("message", event.message)
        assertEquals(4121978048640988213L, ObjectStreamClass.lookup(ReporterEvent::class.java).serialVersionUID)
        assertEquals(3976736990807011378L, ObjectStreamClass.lookup(org.crosswire.common.progress.WorkEvent::class.java).serialVersionUID)
    }

    @Test fun listenerChangesUseSnapshotAndExceptionsStopDelivery() {
        val calls = mutableListOf<String>()
        val error = IllegalStateException("report")
        fun listener(action: () -> Unit) = object : ReporterListener {
            override fun reportMessage(ev: ReporterEvent) = action()
            override fun reportException(ev: ReporterEvent) = action()
        }
        val second = listener { calls.add("second") }
        val first = listener { calls.add("first"); Reporter.removeReporterListener(second) }
        val throwing = listener { throw error }
        try {
            Reporter.addReporterListener(first); Reporter.addReporterListener(second)
            Reporter.informUser(this, "hello")
            assertEquals(listOf("first", "second"), calls)
            calls.clear()
            Reporter.addReporterListener(throwing); Reporter.addReporterListener(second)
            assertSame(error, assertFailsWith<IllegalStateException> { Reporter.informUser(this, error as Throwable) })
            assertEquals(listOf("first"), calls)
        } finally {
            Reporter.removeReporterListener(first); Reporter.removeReporterListener(second); Reporter.removeReporterListener(throwing)
        }
    }
}
