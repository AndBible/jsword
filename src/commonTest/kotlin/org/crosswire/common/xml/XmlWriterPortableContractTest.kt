package org.crosswire.common.xml

import kotlin.test.Test
import kotlin.test.assertEquals

class XmlWriterPortableContractTest {
    @Test fun stringAndCharacterWritesPreserveOrderAndLowSixteenBits() {
        val writer = stringXmlWriter()
        writer.write("prefix")
        writer.write(':'.code)
        writer.write(0x10041)
        writer.write(-1)
        writer.write("")
        assertEquals("prefix:A￿", writer.toString())
    }

    @Test fun defaultWritersHaveIndependentBuffers() {
        val first = stringXmlWriter()
        val second = stringXmlWriter()
        first.write("first")
        second.write("second")
        assertEquals("first", first.toString())
        assertEquals("second", second.toString())
    }
}
