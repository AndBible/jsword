package org.crosswire.jsword.passage;

import org.junit.Assert;
import org.junit.Test;

/** Exercises the Java boundary where Kotlin nullable metadata cannot reject a call. */
public class PassageEventConstructorTest {
    @Test
    public void nullSourceKeepsEventObjectExceptionContract() {
        IllegalArgumentException error = Assert.assertThrows(IllegalArgumentException.class,
                () -> new PassageEvent(null, PassageEvent.EventType.ADDED, null, null));
        Assert.assertEquals("null source", error.getMessage());
    }

    @Test
    public void validSourceRetainsIdentityAndNullableBounds() {
        Object source = new Object();
        PassageEvent event = new PassageEvent(source, PassageEvent.EventType.REMOVED, null, null);
        Assert.assertSame(source, event.getSource());
        Assert.assertSame(PassageEvent.EventType.REMOVED, event.getType());
        Assert.assertNull(event.getLowerIndex());
        Assert.assertNull(event.getUpperIndex());
    }
}
