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
 * \u00A9 CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.jsword.book.install.sword;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.crosswire.common.progress.Progress;
import org.crosswire.common.util.CWClassLoader;
import org.crosswire.common.util.CWProject;
import org.crosswire.common.util.NetUtil;
import org.crosswire.common.util.NetUtilJvm;
import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookFilters;
import org.crosswire.jsword.book.Books;
import org.crosswire.jsword.book.install.DownloadCancelledException;
import org.crosswire.jsword.book.install.InstallException;
import org.crosswire.jsword.book.install.Installer;
import org.crosswire.jsword.book.install.InstallerFactory;
import org.crosswire.jsword.book.sword.SwordBookMetaData;
import org.crosswire.jsword.book.sword.SwordBookMetaDataJvm;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Characterization of {@link AbstractSwordInstaller}, {@link HttpSwordInstaller},
 * {@link HttpsSwordInstaller} and their factories: pins the behaviour of the Java
 * classes before they are converted to Kotlin. A catalog (mods.d.tar.gz) is built
 * locally from two conf files of ~/.sword and put where the installer expects its
 * cached index, or handed out by a subclass that overrides the protected
 * download method, so no network is needed. Runs against a temporary CWProject
 * home that is restored afterwards. Expected values were derived by reading the Java.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class SwordInstallerCharacterizationTest {
    private Object savedInstance;
    private String savedHomeProperty;
    private File tempHome;
    private File work;

    @Before
    public void setUp() throws Exception {
        Field instanceField = CWProject.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        savedInstance = instanceField.get(null);
        savedHomeProperty = System.getProperty("jsword.home");

        tempHome = Files.createTempDirectory("swordinstaller-test").toFile();
        System.setProperty("jsword.home", tempHome.getAbsolutePath());

        // Fresh, not yet established CWProject so that the temporary home is used
        java.lang.reflect.Method reset = CWProject.class.getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(null);
        CWProject.instance().setFrontendName("jsword-test");

        work = Files.createTempDirectory("swordinstaller-work").toFile();
    }

    @After
    public void tearDown() throws Exception {
        // A cancelled job may have interrupted this thread
        Thread.interrupted();
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
        NetUtilJvm.setURICacheDir(new File(new File(writable), "netcache"));

        deleteRecursively(tempHome);
        deleteRecursively(work);
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

    private static byte[] conf(String fileName) throws IOException {
        File f = new File(System.getProperty("user.home"), ".sword/mods.d/" + fileName);
        Assume.assumeTrue("missing test module conf " + fileName, f.isFile());
        return Files.readAllBytes(f.toPath());
    }

    /** The value of a key=value line of a conf. */
    private static String confValue(byte[] conf, String key) {
        for (String line : new String(conf, StandardCharsets.UTF_8).split("\r?\n")) {
            if (line.startsWith(key + "=")) {
                return line.substring(key.length() + 1).trim();
            }
        }
        return null;
    }

    private static void addEntry(TarArchiveOutputStream tar, String name, byte[] data) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(data.length);
        tar.putArchiveEntry(entry);
        tar.write(data);
        tar.closeArchiveEntry();
    }

    /**
     * A catalog with the given confs, plus a directory entry, an entry that is not a conf and
     * an empty conf, which have to be skipped.
     */
    private static byte[] catalog(String... confFiles) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gz = new GZIPOutputStream(bos);
        TarArchiveOutputStream tar = new TarArchiveOutputStream(gz);
        TarArchiveEntry dir = new TarArchiveEntry("mods.d/");
        tar.putArchiveEntry(dir);
        tar.closeArchiveEntry();
        addEntry(tar, "mods.d/readme.txt", "not a conf".getBytes(StandardCharsets.UTF_8));
        addEntry(tar, "mods.d/empty.conf", new byte[0]);
        for (String confFile : confFiles) {
            addEntry(tar, "mods.d/" + confFile, conf(confFile));
        }
        tar.close();
        return bos.toByteArray();
    }

    private static HttpSwordInstaller installer(String host) {
        HttpSwordInstaller installer = new HttpSwordInstaller();
        installer.setHost(host);
        installer.setPackageDirectory("/pkg");
        installer.setCatalogDirectory("/cat");
        return installer;
    }

    private static void putCache(AbstractSwordInstaller installer, byte[] catalog) throws Exception {
        File cache = new File(installer.getCachedIndexFile().getPath());
        Files.write(cache.toPath(), catalog);
    }

    private static List<String> initials(List<Book> books) {
        List<String> result = new ArrayList<String>();
        for (Book book : books) {
            result.add(book.getInitials());
        }
        Collections.sort(result);
        return result;
    }

    /** An installer whose "download" copies the catalog from a local file and records the requests. */
    private class LocalInstaller extends HttpSwordInstaller {
        final List<String> requests = new ArrayList<String>();
        byte[] catalog;
        InstallException failure;
        URI remote;

        @Override
        protected void download(Progress job, String dir, String file, URI dest) throws InstallException {
            requests.add(dir + "|" + file);
            if (failure != null) {
                throw failure;
            }
            try {
                Files.write(new File(dest.getPath()).toPath(), catalog);
            } catch (IOException e) {
                throw new InstallException("local copy failed", e);
            }
        }

        @Override
        public URI toRemoteURI(Book book) {
            return remote != null ? remote : super.toRemoteURI(book);
        }
    }

    @Test
    public void testCachedIndexListsBothBooksSorted() throws Exception {
        HttpSwordInstaller installer = installer("cache.invalid");
        putCache(installer, catalog("kjv.conf", "strongsgreek.conf"));

        List<Book> books = installer.getBooks();
        Assert.assertEquals(Arrays.asList("KJV", "StrongsGreek"), initials(books));
        Assert.assertTrue(installer.loaded);
        // A fresh list each time
        Assert.assertNotSame(books, installer.getBooks());
        books.clear();
        Assert.assertEquals(2, installer.getBooks().size());

        Book kjv = installer.getBook("KJV");
        Assert.assertEquals(confValue(conf("kjv.conf"), "Version"), kjv.getBookMetaData().getProperty("Version"));
        Assert.assertEquals("2.3", kjv.getBookMetaData().getProperty("Version"));
        Assert.assertEquals("StrongsGreek", installer.getBook("StrongsGreek").getInitials());
    }

    @Test
    public void testCachedIndexLocation() throws Exception {
        HttpSwordInstaller installer = installer("cache.invalid");
        File dir = new File(installer.getCachedIndexDir().getPath());
        Assert.assertEquals("download-cache.invalid_cat", dir.getName());
        Assert.assertTrue(dir.isDirectory());
        Assert.assertTrue(dir.getPath().startsWith(tempHome.getCanonicalPath())
                || dir.getPath().startsWith(tempHome.getAbsolutePath()));
        Assert.assertEquals(new File(dir, "mods.d.tar.gz").getPath(), installer.getCachedIndexFile().getPath());
    }

    @Test
    public void testGetBookLookup() throws Exception {
        HttpSwordInstaller installer = installer("lookup.invalid");
        putCache(installer, catalog("kjv.conf", "strongsgreek.conf"));

        Assert.assertNull(installer.getBook("NOPE"));
        Assert.assertNull(installer.getBook(""));
        Book kjv = installer.getBook("KJV");
        Assert.assertEquals("KJV", kjv.getInitials());
        // case insensitive initials
        Assert.assertEquals("KJV", installer.getBook("kjv").getInitials());
        // by name, exact and case insensitive
        Assert.assertEquals("KJV", installer.getBook(kjv.getName()).getInitials());
        Assert.assertEquals("KJV", installer.getBook(kjv.getName().toUpperCase()).getInitials());
        Assert.assertEquals("StrongsGreek", installer.getBook("strongsgreek").getInitials());
    }

    @Test
    public void testFilteredBooks() throws Exception {
        HttpSwordInstaller installer = installer("filter.invalid");
        putCache(installer, catalog("kjv.conf", "strongsgreek.conf"));

        List<Book> bibles = installer.getBooks(BookFilters.getBibles());
        Assert.assertEquals(Arrays.asList("KJV"), initials(bibles));
        Assert.assertEquals(2, installer.getBooks(BookFilters.getAll()).size());
    }

    @Test
    public void testBooksOfACatalogHaveNoConfFile() throws Exception {
        HttpSwordInstaller installer = installer("noconf.invalid");
        putCache(installer, catalog("kjv.conf"));
        Book kjv = installer.getBook("KJV");
        Assert.assertNull(((SwordBookMetaData) kjv.getBookMetaData()).getConfigFile());
        // Without a conf file in the download dir the book is never newer; no remote is asked
        Assert.assertFalse(installer.isNewer(kjv));
    }

    @Test
    public void testMissingCacheDownloadsTheCatalog() throws Exception {
        LocalInstaller installer = new LocalInstaller();
        installer.setHost("missing.invalid");
        installer.setPackageDirectory("/pkg");
        installer.setCatalogDirectory("/cat");
        installer.catalog = catalog("kjv.conf", "strongsgreek.conf");

        Assert.assertEquals(Arrays.asList("KJV", "StrongsGreek"), initials(installer.getBooks()));
        Assert.assertEquals(Arrays.asList("/cat|mods.d.tar.gz"), installer.requests);
        // Loaded now: no second download
        installer.getBooks();
        Assert.assertEquals(1, installer.requests.size());
    }

    @Test
    public void testReloadBookListReplacesTheList() throws Exception {
        LocalInstaller installer = new LocalInstaller();
        installer.setHost("reload.invalid");
        installer.setPackageDirectory("/pkg");
        installer.setCatalogDirectory("/cat");
        installer.catalog = catalog("kjv.conf", "strongsgreek.conf");
        Assert.assertEquals(2, installer.getBooks().size());
        File zip = new File(new File(installer.getCachedIndexDir().getPath()), "mods.d.zip");
        Assert.assertTrue(zip.isFile());

        installer.catalog = catalog("kjv.conf");
        installer.reloadBookList();
        Assert.assertFalse(installer.loaded);
        Assert.assertEquals(2, installer.requests.size());
        Assert.assertTrue(zip.isFile());
        Assert.assertFalse(new File(zip.getPath() + ".old").exists());
        Assert.assertEquals(Arrays.asList("KJV"), initials(installer.getBooks()));
    }

    @Test
    public void testFailureOfTheDownloadPropagatesFromReloadAndIsLoggedByGetBooks() throws Exception {
        LocalInstaller installer = new LocalInstaller();
        installer.setHost("fail.invalid");
        installer.setPackageDirectory("/pkg");
        installer.setCatalogDirectory("/cat");
        installer.failure = new InstallException("boom");
        try {
            installer.reloadBookList();
            Assert.fail();
        } catch (InstallException expected) {
            Assert.assertSame(installer.failure, expected);
        }
        // getBooks swallows it and answers an empty (modifiable) list
        List<Book> books = installer.getBooks();
        Assert.assertTrue(books.isEmpty());
        books.add(null);
        Assert.assertFalse(installer.loaded);
    }

    @Test
    public void testCancelledDownloadKeepsItsType() throws Exception {
        LocalInstaller installer = new LocalInstaller();
        installer.setHost("cancel.invalid");
        installer.setPackageDirectory("/pkg");
        installer.setCatalogDirectory("/cat");
        DownloadCancelledException cancelled = new DownloadCancelledException(URI.create("http://x/y"));
        installer.failure = cancelled;
        try {
            installer.reloadBookList();
            Assert.fail();
        } catch (DownloadCancelledException expected) {
            Assert.assertSame(cancelled, expected);
            Assert.assertEquals("Downloading http://x/y was cancelled", expected.getMessage());
        }

        // The same for install: the exception is rethrown as it is
        LocalInstaller other = new LocalInstaller();
        other.setHost("cancel2.invalid");
        other.setPackageDirectory("/pkg");
        other.setCatalogDirectory("/cat");
        other.catalog = catalog("kjv.conf");
        Book kjv = other.getBook("KJV");
        other.failure = cancelled;
        try {
            other.install(kjv);
            Assert.fail();
        } catch (DownloadCancelledException expected) {
            Assert.assertSame(cancelled, expected);
        }
        // The download of the package was asked for with the initials and the zip suffix
        Assert.assertEquals("/pkg|KJV.zip", other.requests.get(other.requests.size() - 1));
    }

    @Test
    public void testInstallFailureRethrowsTheInstallException() throws Exception {
        LocalInstaller installer = new LocalInstaller();
        installer.setHost("install.invalid");
        installer.setPackageDirectory("/pkg");
        installer.setCatalogDirectory("/cat");
        installer.catalog = catalog("strongsgreek.conf");
        Book book = installer.getBook("StrongsGreek");
        installer.failure = new InstallException("no way");
        try {
            installer.install(book, "my-job-id");
            Assert.fail();
        } catch (InstallException expected) {
            Assert.assertSame(installer.failure, expected);
        }
        Assert.assertEquals("/pkg|StrongsGreek.zip", installer.requests.get(installer.requests.size() - 1));
    }

    @Test
    public void testIsNewerComparesTheRemoteWithTheInstalledConf() throws Exception {
        Book installed = Books.installed().getBook("KJV");
        Assume.assumeNotNull(installed);
        File conf = org.crosswire.jsword.book.sword.SwordBookMetaDataJvm.getConfigFile((SwordBookMetaData) installed.getBookMetaData());
        Assume.assumeTrue(conf != null && conf.exists());

        File remote = new File(work, "KJV.zip");
        Files.write(remote.toPath(), new byte[] {1});
        LocalInstaller installer = new LocalInstaller();
        installer.setHost("newer.invalid");
        installer.remote = remote.toURI();

        Assert.assertTrue(remote.setLastModified(conf.lastModified() + 3600 * 1000L));
        Assert.assertTrue(installer.isNewer(installed));
        Assert.assertTrue(remote.setLastModified(conf.lastModified() - 3600 * 1000L));
        Assert.assertFalse(installer.isNewer(installed));
        Assert.assertTrue(remote.setLastModified(conf.lastModified()));
        Assert.assertFalse(installer.isNewer(installed));
    }

    @Test
    public void testInstallerDefinition() {
        HttpSwordInstaller installer = new HttpSwordInstaller();
        installer.setHost("h.invalid");
        installer.setPackageDirectory("/p");
        installer.setCatalogDirectory("/c");
        installer.setIndexDirectory("/i");
        Assert.assertEquals("h.invalid,/p,/c,/i,,", installer.getInstallerDefinition());

        installer.setProxyHost("px");
        installer.setProxyPort(Integer.valueOf(8080));
        Assert.assertEquals("h.invalid,/p,/c,/i,px,8080", installer.getInstallerDefinition());
        Assert.assertEquals("h.invalid", installer.getHost());
        Assert.assertEquals("/p", installer.getPackageDirectory());
        Assert.assertEquals("/c", installer.getCatalogDirectory());
        Assert.assertEquals("/i", installer.getIndexDirectory());
        Assert.assertEquals("px", installer.getProxyHost());
        Assert.assertEquals(Integer.valueOf(8080), installer.getProxyPort());

        // An empty proxy host is stored as null
        installer.setProxyHost("");
        Assert.assertNull(installer.getProxyHost());
        installer.setProxyPort(null);
        Assert.assertNull(installer.getProxyPort());
        Assert.assertEquals("h.invalid,/p,/c,/i,,", installer.getInstallerDefinition());
    }

    @Test
    public void testDefaults() {
        HttpSwordInstaller installer = new HttpSwordInstaller();
        Assert.assertNull(installer.getHost());
        Assert.assertEquals("", installer.getPackageDirectory());
        Assert.assertEquals("", installer.getCatalogDirectory());
        Assert.assertEquals("", installer.getIndexDirectory());
        Assert.assertNull(installer.getProxyHost());
        Assert.assertNull(installer.getProxyPort());
        Assert.assertFalse(installer.loaded);
        Assert.assertEquals("sword-http", installer.getType());
        Assert.assertEquals(Long.valueOf(0L), installer.indexLastUpdated());
    }

    @Test
    public void testSettersInvalidateTheLoadedFlagOnlyOnChange() throws Exception {
        LocalInstaller installer = new LocalInstaller();
        installer.setHost("flag.invalid");
        installer.setPackageDirectory("/pkg");
        installer.setCatalogDirectory("/cat");
        installer.catalog = catalog("kjv.conf");
        installer.setProxyHost("px");
        installer.setProxyPort(Integer.valueOf(3128));
        installer.getBooks();
        Assert.assertTrue(installer.loaded);

        installer.setHost("flag.invalid");
        installer.setPackageDirectory("/pkg");
        installer.setProxyHost("px");
        installer.setProxyPort(Integer.valueOf(3128));
        // the catalog and index directories never reset the flag
        installer.setCatalogDirectory("/other");
        installer.setIndexDirectory("/index");
        Assert.assertTrue(installer.loaded);

        installer.setProxyPort(Integer.valueOf(1));
        Assert.assertFalse(installer.loaded);
        installer.loaded = true;
        installer.setProxyHost("");
        Assert.assertFalse(installer.loaded);
        installer.loaded = true;
        installer.setPackageDirectory("/pkg2");
        Assert.assertFalse(installer.loaded);
        installer.loaded = true;
        installer.setHost("flag2.invalid");
        Assert.assertFalse(installer.loaded);
    }

    @Test
    public void testCloseDropsTheLoadedList() throws Exception {
        HttpSwordInstaller installer = installer("close.invalid");
        putCache(installer, catalog("kjv.conf"));
        Assert.assertEquals(1, installer.getBooks().size());
        Assert.assertEquals(1, installer.entries.size());
        installer.close();
        Assert.assertTrue(installer.entries.isEmpty());
        Assert.assertFalse(installer.loaded);
        // and it is simply reloaded from the cache
        Assert.assertEquals(1, installer.getBooks().size());
    }

    @Test
    public void testEqualsHashCodeAndCompareTo() {
        HttpSwordInstaller a = installer("a.invalid");
        HttpSwordInstaller a2 = installer("a.invalid");
        HttpSwordInstaller b = installer("b.invalid");
        HttpSwordInstaller aOtherPackage = installer("a.invalid");
        aOtherPackage.setPackageDirectory("/zzz");

        Assert.assertEquals(a, a);
        Assert.assertEquals(a, a2);
        Assert.assertEquals(a.hashCode(), a2.hashCode());
        Assert.assertEquals("a.invalid".hashCode() + "/pkg".hashCode(), a.hashCode());
        Assert.assertNotEquals(a, b);
        Assert.assertNotEquals(a, aOtherPackage);
        Assert.assertNotEquals(a, null);
        Assert.assertNotEquals(a, "a.invalid");
        // the catalog directory does not take part
        a2.setCatalogDirectory("/else");
        Assert.assertEquals(a, a2);

        // The same host and package are equal for the Https subclass too, but the two classes never are
        HttpsSwordInstaller https = new HttpsSwordInstaller();
        https.setHost("a.invalid");
        https.setPackageDirectory("/pkg");
        Assert.assertNotEquals(a, https);
        Assert.assertNotEquals(https, a);
        Assert.assertEquals(a.hashCode(), https.hashCode());

        Assert.assertEquals(0, a.compareTo(a2));
        // compareTo only looks at the package directory when the hosts differ (and answers 0 for the same host)
        Assert.assertEquals(0, a.compareTo(b));
        Assert.assertEquals(0, a.compareTo(aOtherPackage));
        b.setPackageDirectory("/aaa");
        Assert.assertTrue(a.compareTo(b) > 0);
        Assert.assertTrue(b.compareTo(a) < 0);
    }

    @Test
    public void testRemoteUriAndType() throws Exception {
        Book kjv = Books.installed().getBook("KJV");
        Assume.assumeNotNull(kjv);
        HttpSwordInstaller http = installer("h.invalid");
        Assert.assertEquals("sword-http", http.getType());
        Assert.assertEquals(URI.create("http://h.invalid/pkg/KJV.zip"), http.toRemoteURI(kjv));

        HttpsSwordInstaller https = new HttpsSwordInstaller();
        https.setHost("h.invalid");
        https.setPackageDirectory("/pkg");
        Assert.assertEquals("sword-https", https.getType());
        Assert.assertEquals(URI.create("https://h.invalid/pkg/KJV.zip"), https.toRemoteURI(kjv));

        // an unusable host gives no uri
        HttpSwordInstaller bad = installer("bad host");
        Assert.assertNull(bad.toRemoteURI(kjv));
    }

    @Test
    public void testHttpsDownloadOfAnUnreachableServerIsAnInstallException() throws Exception {
        HttpsSwordInstaller https = new HttpsSwordInstaller();
        https.setHost("localhost");
        https.setPackageDirectory("/pkg");
        // nobody is expected to listen on the default https port of the loopback interface
        Assert.assertEquals(Long.valueOf(0L), https.indexLastUpdated());
        File dest = new File(work, "x.zip");
        try {
            https.download(null, "/dir", "file.zip", dest.toURI());
            Assert.fail();
        } catch (InstallException expected) {
            Assert.assertEquals("Unable to find: https://localhost/dir/file.zip", expected.getMessage());
        }
    }

    @Test
    public void testFactories() {
        InstallerFactory http = new HttpSwordInstallerFactory();
        InstallerFactory https = new HttpsSwordInstallerFactory();
        Assert.assertEquals(HttpSwordInstaller.class, http.createInstaller().getClass());
        Assert.assertEquals(HttpsSwordInstaller.class, https.createInstaller().getClass());

        // host,package,catalog,proxyHost,proxyPort,(unused)
        Installer six = http.createInstaller("h.invalid,/pub,/idx,px,3128,");
        Assert.assertEquals(HttpSwordInstaller.class, six.getClass());
        Assert.assertEquals("h.invalid,/pub,/idx,,px,3128", six.getInstallerDefinition());
        Installer sixHttps = https.createInstaller("h.invalid,/pub,/idx,,,");
        Assert.assertEquals(HttpsSwordInstaller.class, sixHttps.getClass());
        Assert.assertEquals("h.invalid,/pub,/idx,,,", sixHttps.getInstallerDefinition());
        // a proxy host without a port
        Assert.assertEquals("h.invalid,/pub,/idx,,px,", http.createInstaller("h.invalid,/pub,/idx,px,,").getInstallerDefinition());

        // the old format: host,directory,proxyHost,proxyPort
        Installer old = https.createInstaller("h.invalid,/dir,px,8080");
        Assert.assertEquals(HttpsSwordInstaller.class, old.getClass());
        Assert.assertEquals("h.invalid,/dir/packages/rawzip,/dir/raw,,px,8080", old.getInstallerDefinition());
        Assert.assertEquals("h.invalid,/dir/packages/rawzip,/dir/raw,,,", http.createInstaller("h.invalid,/dir,,").getInstallerDefinition());

        try {
            http.createInstaller("a,b,c");
            Assert.fail();
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("a,b,c"));
        }
        try {
            https.createInstaller("h,/p,/c,px,notanumber,");
            Assert.fail();
        } catch (NumberFormatException expected) {
            // expected
        }
    }
}
