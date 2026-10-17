package org.crosswire.jsword.passage

import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Exercises the Java boundary where Kotlin nullable metadata cannot reject a call. */
class PassageEventConstructorTest {
    @Test
    fun nullSourceKeepsEventObjectExceptionContract() {
        val error = assertIs<IllegalArgumentException>(
            thrownByJavaConstructor(null, PassageEvent.EventType.ADDED, null, null),
        )
        assertEquals("null source", error.message)
    }

    @Test
    fun validSourceRetainsIdentityAndNullableBounds() {
        val source = Any()
        val event = PassageEvent(source, PassageEvent.EventType.REMOVED, null, null)
        assertSame(source, event.getSource())
        assertSame(PassageEvent.EventType.REMOVED, event.getType())
        assertNull(event.getLowerIndex())
        assertNull(event.getUpperIndex())
    }

    /**
     * Calls the constructor the way a Java caller would, through reflection because
     * Kotlin cannot pass null to the non-null source parameter. Returns what the
     * constructor threw, or null if it did not throw.
     */
    private fun thrownByJavaConstructor(vararg args: Any?): Throwable? {
        val ctor = PassageEvent::class.java.getConstructor(
            Any::class.java,
            PassageEvent.EventType::class.java,
            Verse::class.java,
            Verse::class.java,
        )
        return try {
            ctor.newInstance(*args)
            null
        } catch (e: InvocationTargetException) {
            e.cause
        }
    }
}
