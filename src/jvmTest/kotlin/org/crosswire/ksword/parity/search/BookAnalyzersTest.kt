package org.crosswire.ksword.parity.search

import org.crosswire.common.util.Language
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.crosswire.jsword.index.analysis.AnalyzedToken
import org.crosswire.jsword.index.analysis.BookAnalyzers
import kotlin.test.assertSame
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [BookAnalyzers] against the baseline: every key of `AnalyzerFactory.properties` must select an analyzer whose token
 * stream equals the baseline analyzer's stream for that language (the oracle's `analyze` op asks the baseline
 * `AnalyzerFactory` for the language spec). Each key has a sample text that the analyzers tokenize differently.
 * Region and script subtags are resolved by the baseline `Language` parsing, so `zh-Hans` behaves as `zh`.
 */
class BookAnalyzersTest {
    /** Every key of the baseline table, with the analyzer id the table selects and a sample text for it. */
    private val table = listOf(
        Row("ar", "ar", "الكتابات الجديدة للمؤمنين"),
        Row("cs", "cs", "Hospodinovi slavná díla"),
        Row("de", "de", "Die Häuser der Männer wurden gebaut"),
        Row("el", "el", "Ἐν ἀρχῇ ἦν ὁ λόγος"),
        Row("fa", "fa", "کتابهای مقدس"),
        Row("grc", "el", "Ἐν ἀρχῇ ἦν ὁ λόγος"),
        Row("he", "he", "בְּרֵאשִׁית בָּרָא אֱלֹהִים"),
        Row("ja", "mmseg", "神は光があれと言われた"),
        Row("zh", "mmseg", "起初神创造天地"),
        Row("th", "th", "พระเจ้าทรงสร้างโลก"),
        Row("da", "snowball-Danish", "Huset og hestene løb hurtigt"),
        Row("es", "snowball-Spanish", "Las casas grandes de los hombres"),
        Row("fi", "snowball-Finnish", "Talojen ja kirjojen lukeminen"),
        Row("fr", "snowball-French", "Les maisons des hommes sont grandes"),
        Row("it", "snowball-Italian", "Le case degli uomini sono grandi"),
        Row("nl", "snowball-Dutch", "De huizen van de mannen zijn groot"),
        Row("no", "snowball-Norwegian", "Husene til mennene er store"),
        Row("pt", "snowball-Portuguese", "As casas dos homens são grandes"),
        Row("ru", "snowball-Russian", "Господь пастырь мой и ничего не лишусь"),
        Row("sv", "snowball-Swedish", "Husen och männens hästar springer"),
    )

    private class Row(val code: String, val id: String, val sample: String)

    /** The baseline stream for [spec] as the oracle's `analyze` op prints it: `term<TAB>increment` per line. */
    private fun baseline(spec: String?, text: String): String = OracleSearch.call("analyze", spec ?: "en", text)

    /** The same format, from our tokens (the first increment is measured from -1, as Lucene does). */
    private fun rendered(tokens: List<AnalyzedToken>): String {
        var previous = -1
        return tokens.joinToString("\n") { t ->
            val increment = t.position - previous
            previous = t.position
            "${t.text}\t$increment"
        }
    }

    @Test fun everyBaselineKeySelectsTheBaselineAnalyzer() {
        for (row in table) {
            assertEquals(row.id, BookAnalyzers.analyzerId(row.code), "analyzer id for ${row.code}")
            assertEquals(baseline(row.code, row.sample), rendered(BookAnalyzers.forLanguage(row.code).analyze(row.sample)),
                "tokens for ${row.code}")
        }
    }

    @Test fun regionAndScriptSubtagsResolveLikeTheBaseline() {
        for (spec in listOf("zh-Hans", "zh-Hant", "pt-BR")) {
            val code = Language(spec).code
            val sample = if (code == "zh") "起初神创造天地" else "As casas dos homens são grandes"
            assertEquals(baseline(spec, sample), rendered(BookAnalyzers.forLanguage(code).analyze(sample)), "tokens for $spec")
        }
        assertEquals("mmseg", BookAnalyzers.analyzerId(Language("zh-Hans").code))
        assertEquals("mmseg", BookAnalyzers.analyzerId(Language("zh-Hant").code))
        assertEquals("snowball-Portuguese", BookAnalyzers.analyzerId(Language("pt-BR").code))
    }

    @Test fun ancientHebrewIsUnmappedAndUsesTheSimpleAnalyzer() {
        val sample = "בְּרֵאשִׁית בָּרָא אֱלֹהִים"
        assertEquals("simple", BookAnalyzers.analyzerId("hbo"))
        assertEquals(baseline("hbo", sample), rendered(BookAnalyzers.forLanguage("hbo").analyze(sample)))
    }

    @Test fun englishNullAndUnknownUseTheSimpleAnalyzer() {
        val sample = "Sûrely Café, the Lord's words"
        for (code in listOf<String?>("en", null, "xx", "")) {
            assertEquals("simple", BookAnalyzers.analyzerId(code), "analyzer id for $code")
            assertEquals(baseline("en", sample), rendered(BookAnalyzers.forLanguage(code).analyze(sample)), "tokens for $code")
        }
    }

    @Test fun instancesAreCachedPerAnalyzer() {
        assertEquals(true, BookAnalyzers.forLanguage("zh") === BookAnalyzers.forLanguage("ja"))
        assertEquals(true, BookAnalyzers.forLanguage("el") === BookAnalyzers.forLanguage("grc"))
        assertEquals(true, BookAnalyzers.forLanguage("fr") === BookAnalyzers.forLanguage("fr"))
    }

    /** forBook over installed corpus books: the analyzer is the one of the book's language (Lang of the module). */
    @Test fun forBookSelectsTheAnalyzerOfTheInstalledBookLanguage() {
        SwordBookPathJvm.setAugmentPath(arrayOf(K6Corpus.root))
        val expected = mapOf("KJV" to "simple", "GerBoLut" to "de", "ChiUn" to "mmseg", "WLC" to "simple", "RusSynodal" to "snowball-Russian")
        for ((initials, id) in expected) {
            val book = requireNotNull(Books.installed().getBook(initials)) { "no such book $initials" }
            assertEquals(id, BookAnalyzers.analyzerId(book.language.code), "analyzer id for $initials")
            assertSame(BookAnalyzers.forLanguage(book.language.code), BookAnalyzers.forBook(book), "forBook for $initials")
        }
    }
}
