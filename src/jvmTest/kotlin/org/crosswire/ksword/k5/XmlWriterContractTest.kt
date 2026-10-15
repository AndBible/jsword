package org.crosswire.ksword.k5

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import java.io.Writer
import org.crosswire.common.xml.FormatType
import org.crosswire.common.xml.PrettySerializingContentHandler
import org.junit.Assert.*
import org.junit.Test

class XmlWriterContractTest {
    private class FailingWriter : Writer() {
        var writes = 0
        var flushes = 0
        var closes = 0
        override fun write(chars: CharArray, offset: Int, length: Int) {
            writes++
            throw IOException("Supplied writer failed")
        }
        override fun flush() { flushes++ }
        override fun close() { closes++ }
    }

    @Test fun suppliedWriterErrorsAreDiagnosedAndSwallowedWithoutTakingOwnership() {
        val writer = FailingWriter()
        val handler = PrettySerializingContentHandler(FormatType.AS_IS, writer)
        val original = System.err
        val bytes = ByteArrayOutputStream()
        val captured = PrintStream(bytes)
        try {
            System.setErr(captured)
            handler.startDocument()
            // PI writes both char and String paths; each failure is swallowed and subsequent writes continue.
            handler.processingInstruction("target", "data")
            handler.endDocument()
        } finally {
            System.setErr(original)
            captured.close()
        }
        assertTrue("Writer failures must not stop subsequent writes", writer.writes > 1)
        assertTrue(bytes.toString(Charsets.UTF_8).contains("java.io.IOException: Supplied writer failed"))
        assertEquals(0, writer.flushes)
        assertEquals(0, writer.closes)
    }
}
