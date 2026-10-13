package org.crosswire.ksword.parity

import org.crosswire.common.util.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URI
import java.net.MalformedURLException
import java.util.Properties

/** Concrete utility boundaries compared with the isolated frozen Java implementation. */
class UtilityBoundaryParityTest {
    private val old by lazy { Oracle.loader.loadClass("org.crosswire.common.util.NetUtil") }
    private fun oldCall(name: String, types: Array<Class<*>>, vararg args: Any?): Any? =
        try { old.getMethod(name, *types).invoke(null, *args) }
        catch (error: InvocationTargetException) { throw error.targetException }
    private fun outcome(call: () -> Any?): String = try { "value:${call()}" }
        catch (error: Throwable) { "error:${error.javaClass.name}:${error.message}" }

    @Test fun literalFileUriAndStreamOpenMatchesFrozenJava() {
        val root = kotlin.io.path.createTempDirectory("k4-utility-").toFile()
        try {
            val file = File(root, "literal\\name")
            val uri = file.toURI()
            val spelling = File(root, "./literal\\name")
            assertEquals(oldCall("getURI", arrayOf(File::class.java), spelling), NetUtilJvm.getURI(spelling))
            assertEquals(oldCall("getAsFile", arrayOf(URI::class.java), uri), NetUtilJvm.getAsFile(uri))
            NetUtilJvm.getOutputStream(uri).use { it.write(byteArrayOf(1, 2, 3)) }
            val expected = (oldCall("getInputStream", arrayOf(URI::class.java), uri) as java.io.InputStream).use { it.readBytes() }
            assertArrayEquals(expected, NetUtilJvm.getInputStream(uri).use { it.readBytes() })
            assertTrue(file.isFile)
            assertFalse(File(root, "literal/name").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun predictionPropertiesRetainParsingAndOpenBeforeNullFailure() {
        val root = kotlin.io.path.createTempDirectory("k4-properties-").toFile()
        try {
            val file = File(root, "predictions\\literal")
            file.writeText("b=second\na=first\nunicode=\\u00e4\n")
            val expected = oldCall("loadProperties", arrayOf(URI::class.java), file.toURI()) as Map<*, *>
            val actual = NetUtil.loadProperties(file.toURI())
            assertEquals(expected, actual)
            assertEquals(expected.keys.toList(), actual.keys.toList())
            val oldMap = Oracle.loader.loadClass("org.crosswire.common.util.PropertyMap")
            val oldFile = File(root, "old")
            val newFile = File(root, "new")
            oldFile.writeText("must truncate")
            newFile.writeText("must truncate")
            val oldFailure = runCatching { oldCall("storeProperties", arrayOf(oldMap, URI::class.java, String::class.java), null, oldFile.toURI(), "title") }.exceptionOrNull()
            val newFailure = runCatching { NetUtil.storeProperties(null, newFile.toURI(), "title") }.exceptionOrNull()
            assertTrue(oldFailure is NullPointerException)
            assertTrue(newFailure is NullPointerException)
            assertEquals(0L, oldFile.length())
            assertEquals(0L, newFile.length())
            NetUtil.storeProperties(actual, file.toURI(), "title")
            assertEquals(actual, Properties().apply { file.inputStream().use { load(it) } })
        } finally { root.deleteRecursively() }
    }

    @Test fun invalidUriAndUnsupportedProtocolRetainExceptionPolicy() {
        val uri = URI("http://example.org/path")
        assertEquals(outcome { oldCall("makeDirectory", arrayOf(URI::class.java), uri) }, outcome { NetUtil.makeDirectory(uri) })
        assertEquals(outcome { oldCall("shortenURI", arrayOf(URI::class.java, String::class.java), uri, "missing") }, outcome { NetUtil.shortenURI(uri, "missing") })
        val opaque = URI("urn:value")
        assertTrue(runCatching { NetUtil.lengthenURI(opaque, "x") }.exceptionOrNull() is NullPointerException)
        assertTrue(runCatching { oldCall("lengthenURI", arrayOf(URI::class.java, String::class.java), opaque, "x") }.exceptionOrNull() is NullPointerException)
        assertEquals(java.net.URISyntaxException::class.java, runCatching { URI("bad uri") }.exceptionOrNull()!!.javaClass)
        assertTrue(netUriSyntaxFailure(java.net.URISyntaxException("bad", "reason")))
        assertFalse(netUriSyntaxFailure(IllegalArgumentException("unrelated")))
        assertEquals(listOf(MalformedURLException::class.java), NetUtil::class.java.getMethod("makeDirectory", URI::class.java).exceptionTypes.toList())
    }
}
