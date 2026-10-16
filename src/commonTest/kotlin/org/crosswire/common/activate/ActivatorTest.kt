/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.common.activate;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that the Activator tracks activation per instance rather than per
 * equal value.
 *
 * Books compare equal by (category, name, initials), so a replacement instance
 * for the same module equals the instance it replaced. Activation state lives
 * in the instance, so tracking by value would skip activating the replacement
 * and leave it permanently unusable.
 */
public class ActivatorTest {

    /**
     * Activatable whose equality is by name, mimicking AbstractBook.
     */
    private static final class NamedActivatable implements Activatable {
        private final String name;
        private int activateCount;
        private int deactivateCount;

        NamedActivatable(String name) {
            this.name = name;
        }

        public void activate(Lock lock) {
            activateCount++;
        }

        public void deactivate(Lock lock) {
            deactivateCount++;
        }

        boolean isActivated() {
            return activateCount > deactivateCount;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NamedActivatable)) {
                return false;
            }
            return name.equals(((NamedActivatable) obj).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    @Before
    public void setUp() {
        Activator.deactivateAll();
    }

    @After
    public void tearDown() {
        Activator.deactivateAll();
    }

    @Test
    public void testActivatesEachInstanceEvenWhenTheyCompareEqual() {
        NamedActivatable original = new NamedActivatable("KJV");
        NamedActivatable replacement = new NamedActivatable("KJV");
        Assert.assertEquals("precondition: the two instances compare equal", original, replacement);

        Activator.activate(original);
        Activator.activate(replacement);

        Assert.assertEquals(1, original.activateCount);
        Assert.assertEquals("replacement instance must be activated in its own right",
                1, replacement.activateCount);
    }

    @Test
    public void testReplacementIsActivatedAfterTheOriginalIsDeactivated() {
        NamedActivatable original = new NamedActivatable("KJV");
        Activator.activate(original);
        Activator.deactivate(original);

        NamedActivatable replacement = new NamedActivatable("KJV");
        Activator.activate(replacement);

        Assert.assertTrue(replacement.isActivated());
    }

    @Test
    public void testDeactivatingOneInstanceLeavesAnEqualInstanceActive() {
        NamedActivatable first = new NamedActivatable("KJV");
        NamedActivatable second = new NamedActivatable("KJV");
        Activator.activate(first);
        Activator.activate(second);

        Activator.deactivate(first);

        Assert.assertFalse(first.isActivated());
        Assert.assertTrue("deactivating one instance must not deactivate an equal one",
                second.isActivated());
        Assert.assertEquals(0, second.deactivateCount);
    }

    @Test
    public void testActivateIsNotRepeatedForTheSameInstance() {
        NamedActivatable subject = new NamedActivatable("KJV");

        Activator.activate(subject);
        Activator.activate(subject);

        Assert.assertEquals(1, subject.activateCount);
    }

    @Test
    public void testDeactivateAllDeactivatesEveryTrackedInstance() {
        NamedActivatable first = new NamedActivatable("KJV");
        NamedActivatable second = new NamedActivatable("KJV");
        NamedActivatable other = new NamedActivatable("ESV");
        Activator.activate(first);
        Activator.activate(second);
        Activator.activate(other);

        Activator.deactivateAll();

        Assert.assertFalse(first.isActivated());
        Assert.assertFalse(second.isActivated());
        Assert.assertFalse(other.isActivated());
    }

    @Test
    public void testDeactivateOfAnUntrackedInstanceIsANoOp() {
        NamedActivatable activated = new NamedActivatable("KJV");
        NamedActivatable neverActivated = new NamedActivatable("KJV");
        Activator.activate(activated);

        Activator.deactivate(neverActivated);

        Assert.assertEquals(0, neverActivated.deactivateCount);
        Assert.assertTrue(activated.isActivated());
    }
}
