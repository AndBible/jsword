package org.crosswire.jsword.passage

import java.lang.reflect.InvocationTargetException
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.Test
import kotlin.test.assertIs

/** Preserves the Java caller's null-token ABI guard without throwing in the test itself. */
class AccuracyNullTokensJvmTest {
    @Test
    fun testNullTokensFromJava() {
        val v11n = Versifications.instance().getVersification("KJV")
        val verse = Verse(v11n, BibleBook.GEN, 1, 1)
        val range = VerseRange(v11n, verse, verse)
        // Kotlin cannot pass null for the non-null String array, so the Java-visible
        // overload taking a VerseRange is invoked reflectively with null for the tokens.
        val method = AccuracyType::class.java.getMethod(
            "fromText",
            Versification::class.java,
            String::class.java,
            Array<String>::class.java,
            VerseRange::class.java,
        )
        val cause = try {
            method.invoke(null, v11n, null, null, range)
            null
        } catch (e: InvocationTargetException) {
            e.cause
        }
        assertIs<NullPointerException>(cause)
    }
}
