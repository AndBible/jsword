package org.crosswire.common.xml

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class XmlWriterNativeContractTest {
    @Test fun nullTextIsRejectedWithoutChangingBufferedContent() {
        val writer = stringXmlWriter()
        writer.write("before")
        assertFailsWith<NullPointerException> { writer.write(null) }
        assertEquals("before", writer.toString())
        writer.write("after")
        assertEquals("beforeafter", writer.toString())
    }
}
