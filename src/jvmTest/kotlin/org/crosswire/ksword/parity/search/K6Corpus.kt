package org.crosswire.ksword.parity.search

import java.io.File

/**
 * The K6 search-parity corpus (SWORD layout), fetched by `jsword/scripts/fetch-k6-corpus.sh` into
 * `.local/k6-corpus`. Its location comes from the system property `ksword.k6.corpus`. A missing or empty corpus
 * fails the test (never skips): a skipped parity gate proves nothing.
 */
object K6Corpus {
    /** Modules of the corpus, as fetched by the script (conf names, case as in the packages). */
    val modules: List<String> = listOf(
        "ChiUns", "ChiUn", "JapKougo", "ThaiKJV", "GerBoLut", "GreVamvas", "Byz", "HebModern", "WLC", "AraSVD",
        "FarOPV", "CzeBKR", "RusSynodal", "FreCrampon", "MHC", "KJV", "FinPR",
    )

    val root: File
        get() {
            val path = System.getProperty("ksword.k6.corpus")
                ?: error("ksword.k6.corpus is not set; run the searchParityTest task (and jsword/scripts/fetch-k6-corpus.sh)")
            val dir = File(path)
            val confs = File(dir, "mods.d").list().orEmpty()
            check(confs.isNotEmpty()) { "K6 corpus missing or empty at $dir; run jsword/scripts/fetch-k6-corpus.sh" }
            return dir
        }
}
