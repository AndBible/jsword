/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
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
package org.crosswire.jsword.book.install;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.crosswire.common.util.CWClassLoader;
import org.crosswire.common.util.CWProject;
import org.crosswire.common.util.NetUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Characterization of {@link InstallManager}: factory lookup, adding and
 * removing installers with listener events, and the save/reload round trip.
 * Runs against a temporary CWProject home that is restored afterwards. No
 * network access is made.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class InstallManagerCharacterizationTest {
    /** Installer definition: host,packageDir,catalogDir,indexDir,proxyHost,proxyPort. */
    private static final String DEFINITION = "test.invalid,/pub,/idx,,,";

    private Object savedInstance;
    private String savedHomeProperty;
    private File tempHome;

    @Before
    public void setUp() throws Exception {
        Field instanceField = CWProject.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        savedInstance = instanceField.get(null);
        savedHomeProperty = System.getProperty("jsword.home");

        tempHome = Files.createTempDirectory("installmanager-test").toFile();
        System.setProperty("jsword.home", tempHome.getAbsolutePath());

        // Fresh, not yet established CWProject so that the temporary home is used
        java.lang.reflect.Method reset = CWProject.class.getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(null);
        CWProject.instance().setFrontendName("jsword-test");
    }

    @After
    public void tearDown() throws Exception {
        Field instanceField = CWProject.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, savedInstance);
        if (savedHomeProperty == null) {
            System.clearProperty("jsword.home");
        } else {
            System.setProperty("jsword.home", savedHomeProperty);
        }

        // Re-point the statics that establishing the temporary home changed
        CWProject restored = (CWProject) savedInstance;
        CWClassLoader.setHome(restored.getProjectResourceDirs());
        URI writable = restored.getWritableProjectDir();
        NetUtil.setURICacheDir(new File(new File(writable), "netcache"));

        deleteRecursively(tempHome);
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private static class Recorder implements InstallerListener {
        final List<String> events = new ArrayList<String>();
        final List<Installer> installers = new ArrayList<Installer>();

        public void installerAdded(InstallerEvent ev) {
            Assert.assertTrue(ev.isAddition());
            events.add("added");
            installers.add(ev.getInstaller());
        }

        public void installerRemoved(InstallerEvent ev) {
            Assert.assertFalse(ev.isAddition());
            events.add("removed");
            installers.add(ev.getInstaller());
        }
    }

    private static Installer createTestInstaller(InstallManager manager, String definition) {
        return manager.getInstallerFactory("sword-http").createInstaller(definition);
    }

    @Test
    public void testFactoryNames() {
        InstallManager manager = new InstallManager();
        // InstallerFactory.plugin defines sword-http and sword-https (sword-ftp is commented out)
        Set<String> expected = new TreeSet<String>(Arrays.asList("sword-http", "sword-https"));
        Assert.assertEquals(expected, new TreeSet<String>(manager.getInstallerFactoryNames()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFactoryNamesUnmodifiable() {
        new InstallManager().getInstallerFactoryNames().add("x");
    }

    @Test
    public void testGetInstallerFactory() {
        InstallManager manager = new InstallManager();
        Assert.assertNotNull(manager.getInstallerFactory("sword-http"));
        Assert.assertNotNull(manager.getInstallerFactory("sword-https"));
    }

    /**
     * Quirk: the factory class for an unknown name is null and is passed on to
     * ReflectionUtil.construct, so the lookup throws instead of returning null
     * (the Javadoc promises null).
     */
    @Test(expected = NullPointerException.class)
    public void testGetInstallerFactoryUnknownNameThrows() {
        new InstallManager().getInstallerFactory("nope");
    }

    @Test
    public void testDefaultSiteMapInstallers() {
        // InstallManager.plugin defines seven sword-https installers, named by the second field
        InstallManager manager = new InstallManager();
        Assert.assertEquals(7, manager.getInstallers().size());
        Assert.assertTrue(manager.getInstallers().containsKey("CrossWire"));
        Assert.assertEquals("sword-https", manager.getInstaller("CrossWire").getType());
        Assert.assertNull(manager.getInstaller("Test"));
    }

    @Test
    public void testAddInstallerLookupAndNames() {
        InstallManager manager = new InstallManager();
        Installer installer = createTestInstaller(manager, DEFINITION);
        Assert.assertEquals("sword-http", installer.getType());
        Assert.assertEquals(DEFINITION, installer.getInstallerDefinition());

        Recorder recorder = new Recorder();
        manager.addInstallerListener(recorder);
        manager.addInstaller("Test", installer);

        Assert.assertSame(installer, manager.getInstaller("Test"));
        Assert.assertSame(installer, manager.getInstallers().get("Test"));
        Assert.assertEquals("Test", manager.getInstallerNameForInstaller(installer));
        Assert.assertEquals("sword-http", manager.getFactoryNameForInstaller(installer));
        Assert.assertEquals(Arrays.asList("added"), recorder.events);
        Assert.assertSame(installer, recorder.installers.get(0));
    }

    @Test
    public void testRemoveInstaller() {
        InstallManager manager = new InstallManager();
        Installer installer = createTestInstaller(manager, DEFINITION);
        manager.addInstaller("Test", installer);

        Recorder recorder = new Recorder();
        manager.addInstallerListener(recorder);
        manager.removeInstaller("Test");

        Assert.assertNull(manager.getInstaller("Test"));
        Assert.assertEquals(Arrays.asList("removed"), recorder.events);
        Assert.assertSame(installer, recorder.installers.get(0));

        // Removing an unknown name is silent
        manager.removeInstaller("Test");
        Assert.assertEquals(1, recorder.events.size());

        // After removing the listener no events arrive
        manager.removeBooksListener(recorder);
        manager.addInstaller("Test", installer);
        Assert.assertEquals(1, recorder.events.size());
    }

    @Test
    public void testReplaceSameNameFiresRemovedThenAdded() {
        InstallManager manager = new InstallManager();
        Installer first = createTestInstaller(manager, DEFINITION);
        Installer second = createTestInstaller(manager, "other.invalid,/pub,/idx,,,");
        manager.addInstaller("Test", first);

        Recorder recorder = new Recorder();
        manager.addInstallerListener(recorder);
        manager.addInstaller("Test", second);

        Assert.assertSame(second, manager.getInstaller("Test"));
        Assert.assertEquals(Arrays.asList("removed", "added"), recorder.events);
        Assert.assertSame(first, recorder.installers.get(0));
        Assert.assertSame(second, recorder.installers.get(1));
    }

    @Test
    public void testEqualInstallerUnderNewNameDropsOldName() {
        InstallManager manager = new InstallManager();
        Installer first = createTestInstaller(manager, DEFINITION);
        Installer equal = createTestInstaller(manager, DEFINITION);
        Assert.assertEquals(first, equal);
        manager.addInstaller("One", first);

        Recorder recorder = new Recorder();
        manager.addInstallerListener(recorder);
        manager.addInstaller("Two", equal);

        // names and installers are both unique: the old name goes away
        Assert.assertNull(manager.getInstaller("One"));
        Assert.assertSame(equal, manager.getInstaller("Two"));
        Assert.assertEquals(Arrays.asList("removed", "added"), recorder.events);
        Assert.assertEquals("Two", manager.getInstallerNameForInstaller(first));
    }

    @Test
    public void testUnknownInstallerHasNoName() {
        InstallManager manager = new InstallManager();
        Installer stranger = createTestInstaller(manager, "stranger.invalid,/a,/b,,,");
        Assert.assertNull(manager.getInstallerNameForInstaller(stranger));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInstallersMapUnmodifiable() {
        new InstallManager().getInstallers().remove("CrossWire");
    }

    @Test
    public void testSaveThenReload() {
        InstallManager manager = new InstallManager();
        manager.addInstaller("Test", createTestInstaller(manager, DEFINITION));
        manager.save();

        // save() wrote <home>/<fqn>.plugin
        File saved = new File(tempHome, InstallManager.class.getName() + ".plugin");
        Assert.assertTrue(saved.isFile());

        InstallManager reloaded = new InstallManager();
        Installer installer = reloaded.getInstaller("Test");
        Assert.assertNotNull(installer);
        Assert.assertEquals("sword-http", installer.getType());
        Assert.assertEquals(DEFINITION, installer.getInstallerDefinition());
        Assert.assertEquals(new HashSet<String>(manager.getInstallers().keySet()), new HashSet<String>(reloaded.getInstallers().keySet()));
    }

    @Test
    public void testNullNames() {
        InstallManager manager = new InstallManager();
        // Map lookups with a null key: no such installer, nothing removed, no event
        Assert.assertNull(manager.getInstaller(null));
        Recorder recorder = new Recorder();
        manager.addInstallerListener(recorder);
        int before = manager.getInstallers().size();
        manager.removeInstaller(null);
        Assert.assertEquals(before, manager.getInstallers().size());
        Assert.assertTrue(recorder.events.isEmpty());
    }

    /** Null name: the HashMap lookup gives a null class, which ReflectionUtil.construct rejects with an NPE. */
    @Test(expected = NullPointerException.class)
    public void testGetInstallerFactoryNullNameThrows() {
        new InstallManager().getInstallerFactory(null);
    }
}
