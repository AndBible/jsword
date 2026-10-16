package org.crosswire.jsword.index.analysis

import org.crosswire.jsword.index.analysis.snowball.snowballStemmer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SnowballStemmerTest {
    /** Stems captured from the baseline probe (Lucene 3.6.2 `org.tartarus.snowball.ext.*Stemmer`), five words per language. */
    private val oracleStems: Map<String, List<Pair<String, String>>> = mapOf(
        "Danish" to listOf("kærligheden" to "kær", "bøgerne" to "bøg", "gudernes" to "gud", "elskende" to "elsk", "hellige" to "hel"),
        "Dutch" to listOf("liefhebbende" to "liefhebb", "boeken" to "boek", "koninkrijken" to "koninkrijk", "heiligheid" to "heilig", "gelovigen" to "gelov"),
        "Finnish" to listOf("kirjoissa" to "kirj", "rakkaudessa" to "rakkaud", "jumalalta" to "jumal", "ihmisten" to "ihmist", "sanoistansa" to "sano"),
        "French" to listOf("royaumes" to "royaum", "bénédictions" to "bénédict", "aimerions" to "aim", "continuellement" to "continuel", "éternelle" to "éternel"),
        "Italian" to listOf("fedelmente" to "fedel", "benedizioni" to "benedizion", "amorevolmente" to "amorevol", "profeti" to "profet", "signore" to "signor"),
        "Norwegian" to listOf("kjærligheten" to "kjær", "bøkene" to "bøk", "hellige" to "hel", "elskende" to "elsk", "menneskene" to "mennesk"),
        "Portuguese" to listOf("bênçãos" to "bênçã", "corações" to "coraçõ", "amorosamente" to "amor", "profetas" to "profet", "santidade" to "santidad"),
        "Russian" to listOf("возлюбленные" to "возлюблен", "царствами" to "царств", "благословение" to "благословен", "господа" to "господ", "святостью" to "святост"),
        "Spanish" to listOf("bendiciones" to "bendicion", "corazones" to "corazon", "amorosamente" to "amor", "profetas" to "profet", "santidad" to "santid"),
        "Swedish" to listOf("kärleken" to "kärlek", "böckerna" to "böck", "heliga" to "hel", "älskande" to "älsk", "människorna" to "människ"),
    )

    private val languageNames = mapOf(
        "da" to "Danish", "nl" to "Dutch", "fi" to "Finnish", "fr" to "French", "it" to "Italian",
        "no" to "Norwegian", "pt" to "Portuguese", "ru" to "Russian", "es" to "Spanish", "sv" to "Swedish",
    )

    @Test
    fun handWordsMatchOracle() {
        assertEquals(languageNames.values.toSet(), oracleStems.keys)
        for ((name, pairs) in oracleStems) {
            val stemmer = assertNotNull(snowballStemmer(name), name)
            for ((word, stem) in pairs) assertEquals(stem, stemmer.stem(word), "$name $word")
        }
    }

    @Test
    fun swiftCasesMatchOracle() {
        for (case in SWIFT_ORACLE_ANALYZER_CASES) {
            val name = languageNames[case.languageCode] ?: continue
            assertEquals(case.tokens, SnowballAnalyzer(name).analyze(case.input).map { it.text }, case.name)
        }
    }

    @Test
    fun stemmerIsReusableAcrossWords() {
        val s = assertNotNull(snowballStemmer("Finnish"))
        // The second call must not see state of the first (buffer growth, region marks).
        assertEquals("kirj", s.stem("kirjoissa"))
        assertEquals("rakkaud", s.stem("rakkaudessa"))
        assertEquals("kirj", s.stem("kirjoissa"))
    }

    @Test
    fun emptyAndShortWordsDoNotThrow() {
        for (name in oracleStems.keys) {
            val s = assertNotNull(snowballStemmer(name))
            assertEquals("", s.stem(""), name)
            s.stem("a")
            s.stem("ab")
        }
    }

    @Test
    fun finnishConditionalEntriesAreNeverTakenAsInLucene362() {
        // Oracle: Lucene 3.6.2 reflects on private r_VI/r_LONG, gets IllegalAccessException and treats it as false, so
        // the "den"/"siin" entries do not fire and the shorter "n" entry does ("aarteiden" -> "aarteid", not "aart").
        val s = assertNotNull(snowballStemmer("Finnish"))
        assertEquals("aarteid", s.stem("aarteiden"))
        assertEquals("alhais", s.stem("alhaisiin"))
    }

    @Test
    fun unknownNameHasNoStemmer() {
        assertNull(snowballStemmer("English"))
        assertNull(snowballStemmer("finnish"))
    }

    @Test
    fun analyzerLowercasesThenStemsWithoutFoldingOrStopWords() {
        val tokens = SnowballAnalyzer("French").analyze("Les Royaumes éternelle")
        assertEquals(listOf("le", "royaum", "éternel"), tokens.map { it.text })
        assertEquals(listOf(0, 1, 2), tokens.map { it.position })
    }
}
