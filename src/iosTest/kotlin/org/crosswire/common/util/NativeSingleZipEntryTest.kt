@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.common.util

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.*
import org.crosswire.jsword.book.sword.SwordBookMetaData

class NativeSingleZipEntryTest {
    @Test fun storedAndLegacyDeflatedCacheSupportOrdinaryMetadataReload() = memScoped {
        val template = (platform.Foundation.NSTemporaryDirectory() + "ksword-zip-XXXXXX").cstr.getPointer(this)
        val root = mkdtemp(template)!!.toKString()
        val path = "$root/mods.d.zip"
        try {
            for (fixture in listOf(stored, deflated)) {
                NativeLexicalFiles.sink(path).use { sink ->
                    val buffer = okio.Buffer().write(hexBytes(fixture))
                    sink.write(buffer, buffer.size)
                }
                val spec = "$path!mods.d/demo.conf"
                val bytes = IOUtil.getZipEntry(spec)
                assertEquals(conf, bytes.decodeToString())
                val metadata = SwordBookMetaData(bytes, spec)
                metadata.reload()
                assertEquals("Retained only after reload", metadata.getProperty("About"))
                assertFailsWith<NullPointerException> { IOUtil.getZipEntry("$path!missing") }
                assertFailsWith<NullPointerException> { IOUtil.getZipEntry("$path!./mods.d/demo.conf") }
                assertFailsWith<IndexOutOfBoundsException> { IOUtil.getZipEntry(path) }
            }
        } finally { unlink(path); rmdir(root) }
    }

    private fun hexBytes(text: String) = ByteArray(text.length / 2) {
        text.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }

    private val conf = "[Demo]\nDescription=Catalog demo\nModDrv=RawText\nDataPath=./modules/texts/rawtext/demo/\nLang=en\nAbout=Retained only after reload\n"
    // Independently generated Python zipfile central archives, not production writer output.
    private val stored = "504b030414000000000000002150c60a45907f0000007f000000100000006d6f64732e642f64656d6f2e636f6e665b44656d6f5d0a4465736372697074696f6e3d436174616c6f672064656d6f0a4d6f644472763d526177546578740a44617461506174683d2e2f6d6f64756c65732f74657874732f726177746578742f64656d6f2f0a4c616e673d656e0a41626f75743d52657461696e6564206f6e6c792061667465722072656c6f61640a504b0102140314000000000000002150c60a45907f0000007f0000001000000000000000000000008001000000006d6f64732e642f64656d6f2e636f6e66504b050600000000010001003e000000ad0000000000"
    private val deflated = "504b030414000000080000002150c60a4590700000007f000000100000006d6f64732e642f64656d6f2e636f6e66158b310ec2300c0077bf222fc02ff080c8582454b1210683dd12298d51e252f83de976bad3dda22e7687a8ed59d3db93153ab173b639482f703689f543236f57fd3ac4de2eec2f3ae062b2666de8dd37acbced80fb84307099490b1c1fb63a8dea9c8a4ab0927f8127d71aaa6663813f504b0102140314000000080000002150c60a4590700000007f0000001000000000000000000000008001000000006d6f64732e642f64656d6f2e636f6e66504b050600000000010001003e0000009e0000000000"
}
