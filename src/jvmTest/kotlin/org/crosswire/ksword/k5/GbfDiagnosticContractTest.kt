package org.crosswire.ksword.k5

import fi.sykero.kdom.Content
import fi.sykero.kdom.Text
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.LinkedList
import org.crosswire.common.util.ClassUtil
import org.crosswire.jsword.book.DataPolice
import org.crosswire.jsword.book.filter.filterTestBook
import org.crosswire.jsword.book.filter.gbf.GBFTags
import org.crosswire.jsword.passage.DefaultLeafKeyList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GbfDiagnosticContractTest {
    private class NestedText : Text("nested")

    @Test fun nestedContentDiagnosticRetainsEnclosingClassName() = verifyDiagnostic(NestedText())

    @Test fun anonymousContentDiagnosticRetainsBinaryNameSegments() = verifyDiagnostic(object : Text("anonymous") {})

    private fun verifyDiagnostic(content: Content) {
        val originalReporting = DataPolice.isReporting
        // slf4j-simple 1.7.6 caches its target at initialization; System.setErr cannot capture it.
        org.slf4j.LoggerFactory.getLogger(DataPolice::class.java)
        val streamField = org.slf4j.impl.SimpleLogger::class.java.getDeclaredField("TARGET_STREAM").apply { isAccessible = true }
        val originalStream = streamField.get(null)
        val bytes = ByteArrayOutputStream()
        val captured = PrintStream(bytes, true, "UTF-8")
        try {
            streamField.set(null, captured)
            DataPolice.isReporting = true
            val stack = LinkedList<Content>().apply { addFirst(content) }
            GBFTags.FootnoteEndTag("Rf").updateOsisStack(filterTestBook("K5Diagnostic"), DefaultLeafKeyList("probe"), stack)
            assertTrue("The actual footnote handler must consume the custom content", stack.isEmpty())
            val expected = "K5Diagnostic:probe: expected to pop a Note, but found " +
                ClassUtil.getShortClassName(content.javaClass)
            val lines = bytes.toString("UTF-8").lineSequence().filter { "expected to pop a Note" in it }.toList()
            assertEquals("Capture must observe exactly one real DataPolice log call", 1, lines.size)
            val line = lines.single()
            assertTrue("Real logger must identify DataPolice: $line", "org.crosswire.jsword.book.DataPolice" in line)
            assertEquals("The actual logger payload must preserve the complete legacy class name", expected, line.substringAfter(" - "))
        } finally {
            DataPolice.isReporting = originalReporting
            streamField.set(null, originalStream)
            captured.close()
        }
    }
}
