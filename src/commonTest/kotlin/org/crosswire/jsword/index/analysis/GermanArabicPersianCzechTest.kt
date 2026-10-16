package org.crosswire.jsword.index.analysis

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Expected values are the baseline (Lucene 3.6.2, JSword analyzers with stemming on and stop words off) outputs of the
 * oracle probe `analyze`, captured once; see the cases marked "oracle" in each test.
 */
class GermanArabicPersianCzechTest {
    private fun terms(analyzer: TextAnalyzer, text: String): List<String> = analyzer.analyze(text).map { it.text }

    // German: LowerCaseTokenizer + GermanStemFilter

    @Test fun germanStemsUmlautsAndSharpS() {
        // oracle: "Häuser" -> hau; "Straße Straßen" -> strass strass; "schön Schönheit" -> schon schonhei
        assertEquals(listOf("hau"), terms(GermanAnalyzer(), "Häuser"))
        assertEquals(listOf("strass", "strass"), terms(GermanAnalyzer(), "Straße Straßen"))
        assertEquals(listOf("schon", "schonhei"), terms(GermanAnalyzer(), "schön Schönheit"))
    }

    @Test fun germanStemsIrregularPluralsAndParticles() {
        // oracle: "Matrizen Lehrerinnen" -> matrix lehreri (z becomes x, final -in/-en/-er stripped)
        assertEquals(listOf("matrix", "lehreri"), terms(GermanAnalyzer(), "Matrizen Lehrerinnen"))
        // oracle: "gegeben gegangen" -> geb gegang; "Gott Gottes Gottheiten" -> gott gott gotthei
        assertEquals(listOf("geb", "gegang"), terms(GermanAnalyzer(), "gegeben gegangen"))
        assertEquals(listOf("gott", "gott", "gotthei"), terms(GermanAnalyzer(), "Gott Gottes Gottheiten"))
    }

    @Test fun germanKeepsStopWordsBecauseTheBaselineHasThemOff() {
        // oracle: "der die das und ist" -> der die das und ist (no stop filter, no stemming change)
        assertEquals(listOf("der", "die", "das", "und", "ist"), terms(GermanAnalyzer(), "der die das und ist"))
    }

    @Test fun germanSwiftOracleCases() {
        SWIFT_ORACLE_ANALYZER_CASES.filter { it.languageCode == "de" }.forEach {
            assertEquals(it.tokens, terms(GermanAnalyzer(), it.input), it.name)
        }
    }

    // Arabic: ArabicLetterTokenizer + LowerCaseFilter + ArabicNormalizationFilter + ArabicStemFilter

    @Test fun arabicRemovesHarakatAndTatweelAndStemsPrefixesAndSuffixes() {
        // oracle: "الْكِتَابُ" -> كتاب (harakat removed, ال prefix and ُ removed)
        assertEquals(listOf("كتاب"), terms(ArabicAnalyzer(), "الْكِتَابُ"))
        // oracle: "أحمد إبراهيم إلى آمنوا علي" -> احمد ابراهيم ال امنوا عل (alef hamza folded, dotless yeh folded, ي suffix stripped)
        assertEquals(listOf("احمد", "ابراهيم", "ال", "امنوا", "عل"), terms(ArabicAnalyzer(), "أحمد إبراهيم إلى آمنوا علي"))
    }

    @Test fun arabicPrefixLengthRulesFollowLucene() {
        // oracle: "ب ال وال" -> ب ال وال (too short to strip a prefix; the wa- prefix needs at least 4 units)
        assertEquals(listOf("ب", "ال", "وال"), terms(ArabicAnalyzer(), "ب ال وال"))
    }

    @Test fun arabicWordMadeOnlyOfDiacriticsBecomesEmptyTerms() {
        // oracle: "ّ َ ِ" -> three empty tokens; "ـــ" (three tatweels) -> one empty token. Lucene keeps them.
        assertEquals(listOf("", "", ""), terms(ArabicAnalyzer(), "ّ َ ِ"))
        assertEquals(listOf(""), terms(ArabicAnalyzer(), "ـــ"))
    }

    @Test fun arabicLowercasesLatinLettersAndDropsDigitsAndTanwin() {
        // oracle: "Test العربي 123 مؤمنٌ" -> test عرب مؤمن (the trailing ي is a stemmed suffix)
        assertEquals(listOf("test", "عرب", "مؤمن"), terms(ArabicAnalyzer(), "Test العربي 123 مؤمنٌ"))
    }

    @Test fun arabicSwiftOracleCases() {
        SWIFT_ORACLE_ANALYZER_CASES.filter { it.languageCode == "ar" }.forEach {
            assertEquals(it.tokens, terms(ArabicAnalyzer(), it.input), it.name)
        }
    }

    // Persian: ArabicLetterTokenizer + LowerCaseFilter + ArabicNormalizationFilter + PersianNormalizationFilter, no stemmer

    @Test fun persianZeroWidthNonJoinerSeparatesTokens() {
        // oracle: "كتاب‌ها" -> كتاب ها (ZWNJ is neither a letter nor a combining mark)
        assertEquals(listOf("كتاب", "ها"), terms(PersianAnalyzer(), "كتاب‌ها"))
    }

    @Test fun persianMapsFarsiLettersToArabicForms() {
        // oracle: "کتابها" (Farsi kaf and yeh) -> كتابها (Arabic kaf and yeh); "ۀ" -> ه
        assertEquals(listOf("كتابها"), terms(PersianAnalyzer(), "کتابها"))
        assertEquals(listOf("ه"), terms(PersianAnalyzer(), "ۀ"))
    }

    @Test fun persianDoesNotStem() {
        // oracle: "مدرسه‌ها ساده‌ترین" -> مدرسه ها ساده ترين (the Arabic ه suffix is kept; ی becomes ي)
        assertEquals(
            listOf("مدرسه", "ها", "ساده", "ترين"),
            terms(PersianAnalyzer(), "مدرسه‌ها ساده‌ترین"),
        )
    }

    @Test fun persianSwiftOracleCases() {
        SWIFT_ORACLE_ANALYZER_CASES.filter { it.languageCode == "fa" }.forEach {
            assertEquals(it.tokens, terms(PersianAnalyzer(), it.input), it.name)
        }
    }

    // Czech: LowerCaseTokenizer only (LUCENE_29 has no Czech stemmer; the baseline has none either)

    @Test fun czechLowercasesWithDiacriticsAndDoesNotStem() {
        // oracle: "PŘÍLIŠ žluťoučký kůň" -> příliš žluťoučký kůň; "žena ženy ženách" -> unchanged
        assertEquals(listOf("příliš", "žluťoučký", "kůň"), terms(CzechAnalyzer(), "PŘÍLIŠ žluťoučký kůň"))
        assertEquals(listOf("žena", "ženy", "ženách"), terms(CzechAnalyzer(), "žena ženy ženách"))
    }

    @Test fun czechSplitsOnNonLettersLikeTheBaseline() {
        // oracle: "Hello WORLD ľudia" -> hello world ľudia
        assertEquals(listOf("hello", "world", "ľudia"), terms(CzechAnalyzer(), "Hello WORLD ľudia"))
    }

    @Test fun czechSwiftOracleCases() {
        SWIFT_ORACLE_ANALYZER_CASES.filter { it.languageCode == "cs" }.forEach {
            assertEquals(it.tokens, terms(CzechAnalyzer(), it.input), it.name)
        }
    }
}
