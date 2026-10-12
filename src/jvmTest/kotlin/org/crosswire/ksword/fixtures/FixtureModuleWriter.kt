package org.crosswire.ksword.fixtures

import okio.Path
import org.crosswire.common.compress.CompressorType
import org.crosswire.common.crypt.Sapphire
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.versification.Testament
import org.crosswire.jsword.versification.system.Versifications
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

/** Writes the actual reader layouts, including byte offsets (not node ordinals) in GenBook links. */
object FixtureModuleWriter {
    private val v11n = Versifications.instance().getVersification("KJV")!!
    private fun ints(vararg values: Int) = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        .apply { values.forEach { putInt(it) } }.array()
    private fun size(value: Int, ds: Int) = if (ds == 4) ints(value) else
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array()
    private fun bytes(module: FixtureModule, text: String) = text.toByteArray(Charset.forName(
        if ("Encoding=Latin-1" in module.extras) "windows-1252" else "UTF-8"))
    private fun compressed(module: FixtureModule, data: ByteArray) = CompressorType.fromString(
        module.extras.first { it.startsWith("CompressType=") }.substringAfter('='))!!.getCompressor(data).compress().toByteArray()
    private fun cipher(module: FixtureModule, data: ByteArray): ByteArray {
        val key = module.extras.firstOrNull { it.startsWith("CipherKey=") }?.substringAfter('=') ?: return data
        val engine = Sapphire(bytes(module, key))
        // Sapphire.cipher is a decipher operation with plaintext/ciphertext feedback, not its inverse.
        // Derive each encryption byte from a zero-byte trial, restoring state before the real decipher step.
        val fields = Sapphire::class.java.declaredFields.filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .onEach { it.isAccessible = true }
        try {
            data.indices.forEach { i ->
                val snapshot = fields.map { it.get(engine).let { value -> if (value is IntArray) value.copyOf() else value } }
                val mask = engine.cipher(0)
                fields.zip(snapshot).forEach { (field, value) -> field.set(engine, value) }
                val encrypted = (data[i].toInt() xor mask.toInt()).toByte()
                check(engine.cipher(encrypted) == data[i])
                data[i] = encrypted
            }
        } finally { engine.burn() }
        return data
    }
    private fun save(file: File, data: ByteArray) { file.parentFile.mkdirs(); file.writeBytes(data) }
    private fun verse(ref: String): Verse {
        val p = ref.split('.')
        return Verse(v11n, org.crosswire.jsword.versification.BibleBook.fromOSIS(p[0]), p[1].toInt(), p[2].toInt())
    }

    /** Returns expected non-empty global-list names and raw strings independently of any backend reads. */
    fun write(root: Path, module: FixtureModule): List<Pair<String, String>> {
        val directory = File(root.toString())
        val conf = File(directory, "mods.d/${module.initials}.conf")
        save(conf, (module.confLines.joinToString("\n") + "\n").toByteArray())
        val path = File(directory, module.dataPath)
        return when {
            module.dictionary -> writeDictionary(path, module)
            module.driver == "RawGenBook" -> writeGenBook(path, module)
            else -> writeVerses(path, module)
        }
    }

    private fun writeVerses(path: File, module: FixtureModule): List<Pair<String, String>> {
        val ds = if (module.driver.endsWith('4')) 4 else 2
        val zipped = module.driver.startsWith('z')
        val entries = module.entries.map { verse(it.first) to it.second }.sortedBy { it.first.ordinal }
        for ((testament, name) in listOf(Testament.OLD to "ot", Testament.NEW to "nt")) {
            val selected = entries.filter { v11n.getTestament(it.first.ordinal) == testament }
            // Sparse tail may be omitted: readers return empty for EOF. Cover all ordinals through the last entry.
            val count = (selected.maxOfOrNull { v11n.getTestamentOrdinal(it.first.ordinal) } ?: 0) + 1
            val index = ByteBuffer.allocate(count * (if (zipped) 8 + ds else 4 + ds)).order(ByteOrder.LITTLE_ENDIAN)
            val text = ByteArrayOutputStream()
            if (!zipped) {
                selected.forEach { (verse, raw) ->
                    val data = cipher(module, bytes(module, raw))
                    index.position(v11n.getTestamentOrdinal(verse.ordinal) * (4 + ds))
                    index.putInt(text.size()).put(size(data.size, ds)); text.write(data)
                }
                save(File(path, "$name.vss"), index.array()); save(File(path, name), text.toByteArray())
            } else {
                val blockType = module.extras.first { it.startsWith("BlockType=") }.substringAfter('=')
                val indicator = when (blockType) { "BOOK" -> "b"; "CHAPTER" -> "c"; else -> "v" }
                val blocks = selected.groupBy { (verse, _) -> when (blockType) {
                    "BOOK" -> verse.book.osis; "CHAPTER" -> "${verse.book.osis}.${verse.chapter}"; else -> verse.getOsisID()
                } }
                val blockIndex = ByteArrayOutputStream()
                blocks.values.forEachIndexed { number, block ->
                    val rawBlock = ByteArrayOutputStream()
                    block.forEach { (verse, raw) ->
                        val data = bytes(module, raw)
                        index.position(v11n.getTestamentOrdinal(verse.ordinal) * (8 + ds))
                        index.putInt(number).putInt(rawBlock.size()).put(size(data.size, ds)); rawBlock.write(data)
                    }
                    val data = cipher(module, compressed(module, rawBlock.toByteArray()))
                    blockIndex.write(ints(text.size(), data.size, rawBlock.size())); text.write(data)
                }
                save(File(path, "$name.${indicator}zv"), index.array())
                save(File(path, "$name.${indicator}zs"), blockIndex.toByteArray())
                save(File(path, "$name.${indicator}zz"), text.toByteArray())
            }
        }
        return entries.map { it.first.getName() to it.second }
    }

    private fun writeDictionary(path: File, module: FixtureModule): List<Pair<String, String>> {
        val ds = if (module.driver == "RawLD") 2 else 4
        val entries = module.entries.sortedBy { it.first }
        val index = ByteArrayOutputStream(); val data = ByteArrayOutputStream()
        val block = ByteArrayOutputStream()
        if (module.driver == "zLD") {
            val texts = entries.map { bytes(module, it.second) + byteArrayOf(0) }
            block.write(ints(entries.size))
            var offset = 4 + entries.size * 8
            texts.forEach { block.write(ints(offset, it.size)); offset += it.size }
            texts.forEach { block.write(it) }
            val compressed = compressed(module, block.toByteArray())
            save(File("$path.zdx"), ints(0, compressed.size)); save(File("$path.zdt"), compressed)
        }
        entries.forEachIndexed { i, (key, raw) ->
            val payload = bytes(module, "$key\n") + if (module.driver == "zLD") ints(0, i) else bytes(module, raw)
            index.write(ints(data.size())); index.write(size(payload.size, ds)); data.write(payload)
        }
        save(File("$path.idx"), index.toByteArray()); save(File("$path.dat"), data.toByteArray())
        return entries.map { (key, raw) ->
            val display = key
            // RawLD's DataEntry deliberately returns KEY + newline + payload; zLD returns payload alone.
            display to if (module.driver == "zLD") raw else "$key\n$raw"
        }
    }

    private fun writeGenBook(path: File, module: FixtureModule): List<Pair<String, String>> {
        val names = listOf("", "Part 1", "Chapter 1", "Chapter 2", "Part 2")
        val parents = listOf(-1, 0, 1, 1, 0); val siblings = listOf(-1, 4, 3, -1, -1); val children = listOf(1, 2, -1, -1, -1)
        val texts = listOf("", "", module.entries[0].second, module.entries[1].second, module.entries[2].second)
        val index = ByteArrayOutputStream(); val nodes = ByteArrayOutputStream(); val data = ByteArrayOutputStream()
        names.indices.forEach { i ->
            index.write(ints(nodes.size()))
            fun link(n: Int) = if (n < 0) -1 else n * 4
            nodes.write(ints(link(parents[i]), link(siblings[i]), link(children[i])))
            nodes.write(bytes(module, names[i])); nodes.write(0)
            val text = bytes(module, texts[i])
            nodes.write(size(if (text.isEmpty()) 0 else 8, 2))
            if (text.isNotEmpty()) { nodes.write(ints(data.size(), text.size)); data.write(text) }
        }
        save(File("$path.idx"), index.toByteArray()); save(File("$path.dat"), nodes.toByteArray()); save(File("$path.bdt"), data.toByteArray())
        return listOf("Chapter 1" to texts[2], "Chapter 2" to texts[3], "Part 2" to texts[4])
    }
}
