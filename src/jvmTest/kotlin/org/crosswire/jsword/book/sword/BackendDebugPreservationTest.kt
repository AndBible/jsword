package org.crosswire.jsword.book.sword

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.sword.state.RawLDBackendState
import org.crosswire.jsword.book.sword.state.ZLDBackendState
import org.junit.Assert.*
import org.junit.Test

class BackendDebugPreservationTest {
    private fun metadata(path: String) = SwordBookMetaData(
        "[DebugProbe]\nModDrv=RawLD\nDataPath=$path\nEncoding=UTF-8\n".toByteArray(), "debug.conf"
    ).apply { library = java.io.File("/").toURI() }

    private fun capture(action: () -> Unit): String {
        val old = System.out
        val bytes = ByteArrayOutputStream()
        try { System.setOut(PrintStream(bytes, true, "UTF-8")); action() }
        finally { System.setOut(old) }
        return bytes.toString("UTF-8")
    }

    @Test fun populatedRawDumpAndImpMatchFrozenPrintStreamLoops() {
        val dir = Files.createTempDirectory("debug-raw").toFile()
        try {
            val path = java.io.File(dir, "entries").path
            val texts = listOf("", "short\nline", "x".repeat(38), "y".repeat(39))
            val data = ByteArrayOutputStream()
            val indexes = texts.mapIndexed { i, raw ->
                val bytes = if (i == 0) byteArrayOf() else "key$i\n$raw".toByteArray()
                DataIndex(data.size(), bytes.size).also { data.write(bytes) }
            }
            java.io.File("$path.idx").writeBytes(byteArrayOf())
            java.io.File("$path.dat").writeBytes(data.toByteArray())
            val md = metadata(path)
            var releases = 0
            val state = object : RawLDBackendState(md) {
                override fun setLastAccess(lastAccess: Long) { releases++; super.setLastAccess(lastAccess) }
            }
            val backend = object : RawLDBackend<RawLDBackendState>(md, 2) {
                override fun initState() = state
                override fun getCardinality() = indexes.size
                override fun getIndex(state: RawLDBackendState, entry: Long) = indexes[entry.toInt()]
            }
            // Frozen pre-port loops used PrintStream.println and DataEntry decoding.
            val dumpReference = capture {
                System.out.println("index\toffset\tsize\tkey\tvalue")
                indexes.forEachIndexed { i, index ->
                    val buf = StringBuilder("$i\t${index.getOffset()}\t${index.getSize()}")
                    if (index.getSize() > 0) {
                        val entry = DataEntry(i.toString(), data.toByteArray().copyOfRange(index.getOffset(), index.getOffset() + index.getSize()), md.bookCharset)
                        val raw = entry.getRawText(null)
                        buf.append('\t').append(entry.getKey()).append('\t')
                        if (raw.length > 43) buf.append(raw.substring(0, 40).replace('\n', ' ')).append("...")
                        else buf.append(raw.replace('\n', ' '))
                    } else buf.append("\t\t")
                    System.out.println(buf.toString())
                }
            }
            val impReference = capture {
                indexes.forEachIndexed { i, index ->
                    val buf = StringBuilder("$$$")
                    if (index.getSize() > 0) {
                        val entry = DataEntry(i.toString(), data.toByteArray().copyOfRange(index.getOffset(), index.getOffset() + index.getSize()), md.bookCharset)
                        buf.append(entry.getKey()).append('\n').append(entry.getRawText(null))
                    }
                    System.out.println(buf.toString())
                }
            }
            val dump = StringBuilder(); backend.dumpIdxRaf(dump)
            val imp = StringBuilder(); backend.toIMP(imp)
            assertEquals(dumpReference, dump.toString())
            assertEquals(impReference, imp.toString())
            assertTrue(dump.contains("y".repeat(35) + "..."))
            assertTrue(dump.contains("x".repeat(38)))
            assertEquals(2, releases)
            val err = ByteArrayOutputStream(); val oldErr = System.err
            try {
                System.setErr(PrintStream(err, true, "UTF-8"))
                val broken = object : Appendable {
                    override fun append(csq: CharSequence?): Appendable = throw java.io.IOException("append-probe")
                    override fun append(csq: CharSequence?, start: Int, end: Int): Appendable = append(csq)
                    override fun append(c: Char): Appendable = append(c.toString())
                }
                backend.dumpIdxRaf(broken)
                backend.toIMP(broken)
            } finally { System.setErr(oldErr) }
            assertTrue(err.toString("UTF-8").contains("java.io.IOException: append-probe"))
            assertEquals(4, releases)
            state.releaseResources()
        } finally { dir.deleteRecursively() }
    }

    @Test fun populatedZldDumpMatchesFrozenPrintStreamLoop() {
        val dir = Files.createTempDirectory("debug-zld").toFile()
        try {
            val path = java.io.File(dir, "entries").path
            val raws = listOf("short\nline", "x".repeat(43), "y".repeat(44), "target\nkey")
            val blocks = raws.indices.map { i ->
                "key$i\n".toByteArray() + ByteArray(8).also {
                    SwordUtil.encodeLittleEndian32(i + 10, it, 0)
                    SwordUtil.encodeLittleEndian32(i + 20, it, 4)
                }
            }
            val data = ByteArrayOutputStream()
            val indexes = listOf(DataIndex(0, 0)) + blocks.map { bytes ->
                DataIndex(data.size(), bytes.size).also { data.write(bytes) }
            }
            java.io.File("$path.idx").writeBytes(byteArrayOf())
            java.io.File("$path.dat").writeBytes(data.toByteArray())
            val md = metadata(path)
            var releases = 0
            var failRead = false
            val state = object : ZLDBackendState(md) {
                override fun setLastAccess(lastAccess: Long) { releases++; super.setLastAccess(lastAccess) }
            }
            val backend = object : ZLDBackend(md) {
                override fun initState() = state
                override fun getCardinality() = indexes.size
                override fun getIndex(state: RawLDBackendState, entry: Long): DataIndex {
                    if (failRead) throw java.io.IOException("zld-read-probe")
                    return indexes[entry.toInt()]
                }
                override fun getEntry(fileState: RawLDBackendState?, entry: DataEntry): DataEntry =
                    object : DataEntry(entry.getName(), byteArrayOf(), "UTF-8") {
                        override fun getRawText(cipherKey: ByteArray?) = raws[entry.getBlockIndex().getOffset() - 10]
                        override fun isLinkEntry() = entry.getBlockIndex().getOffset() == 13
                        override fun getLinkTarget() = "target\nkey"
                    }
            }
            val reference = capture {
                System.out.println("index\toffset\tsize\tkey\tvalue")
                indexes.forEachIndexed { i, index ->
                    val buf = StringBuilder("$i\t${index.getOffset()}\t${index.getSize()}")
                    if (index.getSize() > 0) {
                        val entry = DataEntry(i.toString(), blocks[i - 1], "UTF-8")
                        val block = entry.getBlockIndex(); val raw = raws[i - 1]
                        buf.append('\t').append(entry.getKey()).append('\t').append(block.getOffset())
                            .append('\t').append(block.getSize()).append('\t')
                        if (i == 4) buf.append("Linked to: ").append(raw.replace('\n', ' '))
                        else if (raw.length > 43) buf.append(raw.substring(0, 40).replace('\n', ' ')).append("...")
                        else buf.append(raw)
                    }
                    System.out.println(buf.toString())
                }
            }
            val output = StringBuilder(); backend.dumpIdxRaf(output)
            assertEquals(reference, output.toString())
            assertTrue(output.contains("short\nline"))
            assertEquals(1, releases)
            failRead = true
            val oldErr = System.err; val err = ByteArrayOutputStream()
            val failedOutput = StringBuilder()
            try {
                System.setErr(PrintStream(err, true, "UTF-8"))
                backend.dumpIdxRaf(failedOutput)
            } finally { System.setErr(oldErr) }
            assertEquals("index\toffset\tsize\tkey\tvalue\n", failedOutput.toString())
            assertTrue(err.toString("UTF-8").contains("java.io.IOException: zld-read-probe"))
            assertEquals(2, releases)
            state.releaseResources()
        } finally { dir.deleteRecursively() }
    }

    @Test fun caughtBookFailureUsesStderrRatherThanOutput() {
        val backend = object : RawLDBackend<RawLDBackendState>(metadata("missing"), 2) {
            override fun initState(): RawLDBackendState = throw BookException("book-probe")
        }
        val old = System.err; val bytes = ByteArrayOutputStream(); val output = StringBuilder()
        try {
            System.setErr(PrintStream(bytes, true, "UTF-8"))
            backend.dumpIdxRaf(output); backend.toIMP(output)
        } finally { System.setErr(old) }
        assertEquals("", output.toString())
        assertTrue(bytes.toString("UTF-8").contains("BookException: book-probe"))
    }
}
