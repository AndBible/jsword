package org.crosswire.common.util

import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

/** JVM stream boundary retaining Properties enumeration, escaping and timestamp semantics. */
object PropertyMapJvm {
    @JvmStatic
    @Throws(java.io.IOException::class)
    fun load(map: PropertyMap, input: InputStream) = map.load(input.readBytes())

    @JvmStatic
    @Throws(java.io.IOException::class)
    fun store(map: PropertyMap, output: OutputStream, comments: String?) {
        Properties().apply { putAll(map) }.store(output, comments)
    }
}

internal actual fun legacyPropertyEntries(bytes: ByteArray): List<Pair<String, String>> {
    val properties = Properties().apply { load(bytes.inputStream()) }
    return properties.keys().toList().map { it as String to properties[it] as String }
}

internal actual fun legacyPropertyBytes(map: PropertyMap, comments: String?): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    PropertyMapJvm.store(map, output, comments)
    return output.toByteArray()
}
