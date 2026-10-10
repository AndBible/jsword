package org.crosswire.jsword.passage;

import org.crosswire.jsword.versification.Versification;
import org.crosswire.jsword.versification.system.Versifications;
import org.junit.Assert;
import org.junit.Test;

/** Preserves the Java caller's null-token ABI guard without throwing in the test itself. */
public class AccuracyNullTokensJvmTest {
    @Test
    public void testNullTokensFromJava() throws Exception {
        Versification v11n = Versifications.instance().getVersification("KJV");
        Verse verse = new Verse(v11n, org.crosswire.jsword.versification.BibleBook.GEN, 1, 1);
        VerseRange range = new VerseRange(v11n, verse, verse);
        try {
            AccuracyType.fromText(v11n, null, (String[]) null, range);
            Assert.fail();
        } catch (NullPointerException expected) {
            // The Kotlin ABI rejects this Java-only input.
        }
    }
}
