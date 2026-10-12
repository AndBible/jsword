package org.crosswire.ksword.fixtures

/** The raw strings are public-domain KJV text; punctuation/combining variants exercise decoding. */
data class FixtureModule(val initials: String, val driver: String, val entries: List<Pair<String, String>>,
                         val extras: List<String> = emptyList()) {
    val dictionary get() = driver in setOf("RawLD", "RawLD4", "zLD")
    val dataPath get() = "modules/$initials" + if (dictionary || driver == "RawGenBook") "/$initials" else ""
    val confLines get() = listOf("[$initials]", "DataPath=./$dataPath", "ModDrv=$driver",
        "Description=K4 public-domain KJV fixture", "DistributionLicense=Public Domain", "SourceType=OSIS",
        "Lang=en", "Versification=KJV") +
        (if (extras.any { it.startsWith("Encoding=") }) emptyList() else listOf("Encoding=UTF-8")) + extras
}

object FixtureSet {
    private val genesis = listOf(
        "Gen.1.1" to "In the beginning God created the heaven and the earth.",
        "Gen.1.2" to "And the earth was without form, and void; and darkness was upon the face of the deep. And the Spirit of God moved upon the face of the waters.",
        "Gen.1.3" to "And God said, Let there be light: and there was light.")
    private val john = listOf(
        "John.1.1" to "In the beginning was the Word, and the Word was with God, and the Word was God.",
        "John.1.2" to "The same was in the beginning with God.",
        "John.1.3" to "All things were made by him; and without him was not any thing made that was made.")
    private val john3 = listOf(
        "John.3.16" to "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
        "John.3.17" to "For God sent not his Son into the world to condemn the world; but that the world through him might be saved.")
    val MODULES: List<FixtureModule> = listOf(
        FixtureModule("K4RawText", "RawText", genesis + john),
        FixtureModule("K4RawText4", "RawText4", john3),
        FixtureModule("K4ZTextBook", "zText", genesis + john, listOf("CompressType=ZIP", "BlockType=BOOK")),
        FixtureModule("K4ZTextChapterLzss", "zText", john + john3.take(1), listOf("CompressType=LZSS", "BlockType=CHAPTER")),
        FixtureModule("K4ZText4Verse", "zText4", john.take(2), listOf("CompressType=ZIP", "BlockType=VERSE")),
        FixtureModule("K4Cipher", "zText", john, listOf("CompressType=ZIP", "BlockType=BOOK", "CipherKey=k4-Test-key")),
        FixtureModule("K4Latin1", "RawText", listOf("John.1.1" to "“In the beginning” – the Word — God… é"), listOf("Encoding=Latin-1")),
        FixtureModule("K4Combining", "RawText", listOf("John.1.1" to "In the beginning é בְּרֵאשִׁית λόγος")),
        FixtureModule("K4RawCom", "RawCom", listOf("John.1.1" to "<note>In the beginning was the Word.</note>")),
        FixtureModule("K4ZComChapter", "zCom", john3.take(1), listOf("CompressType=ZIP", "BlockType=CHAPTER")),
        FixtureModule("K4RawLD", "RawLD", listOf("G0025" to "God so loved the world.", "G0026" to "God is love.", "G3056" to "In the beginning was the Word."), listOf("Feature=GreekDef")),
        FixtureModule("K4RawLD4", "RawLD4", listOf("ALPHA" to "I am Alpha and Omega.", "OMEGA" to "The beginning and the end.")),
        FixtureModule("K4ZLD", "zLD", listOf("AARON" to "Aaron thy brother.", "ABBA" to "Abba, Father.", "ZION" to "The mount Zion."), listOf("CompressType=ZIP")),
        FixtureModule("K4Devotion", "RawLD", listOf("01.01" to genesis[0].second, "02.29" to john[0].second, "12.31" to john3[0].second), listOf("Category=Daily Devotional")),
        FixtureModule("K4GenBook", "RawGenBook", listOf("/Part 1/Chapter 1" to genesis[0].second, "/Part 1/Chapter 2" to genesis[1].second, "/Part 2" to john[0].second))
    )
}
