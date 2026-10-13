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
/**
 * An implementation of an installer that can handle SWORD Books (Modules).
 */
package org.crosswire.jsword.book.install.sword

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.HashMap

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.crosswire.common.progress.JobManager
import org.crosswire.common.progress.Progress
import org.crosswire.common.util.CWProject
import org.crosswire.common.util.CollectionUtil
import org.crosswire.common.util.FileUtil
import org.crosswire.common.util.IOUtil
import org.crosswire.common.util.NetUtil
import org.crosswire.common.util.StringUtil
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.AbstractBookList
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookFilter
import org.crosswire.jsword.book.BookFilterIterator
import org.crosswire.jsword.book.BookSet
import org.crosswire.jsword.book.install.InstallException
import org.crosswire.jsword.book.install.Installer
import org.crosswire.jsword.book.sword.Backend
import org.crosswire.jsword.book.sword.NullBackend
import org.crosswire.jsword.book.sword.SwordBook
import org.crosswire.jsword.book.sword.SwordBookDriver
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.book.sword.SwordBookMetaDataJvm
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.crosswire.jsword.book.sword.SwordConstants
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * The AbstractSwordInstaller provides for the common implementation of derived classes.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
abstract class AbstractSwordInstaller : AbstractBookList(), Installer, Comparable<AbstractSwordInstaller> {
    /**
     * Utility to download a file from a remote site
     * 
     * @param job
     *            The way of noting progress
     * @param dir
     *            The directory from which to download the file
     * @param file
     *            The file to download
     * @throws InstallException
     */
    @Throws(InstallException::class)
    protected abstract fun download(job: Progress?, dir: String?, file: String?, dest: URI?)

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#getInstallerDefinition()
     */
    override fun getInstallerDefinition(): String {
        val buf = StringBuilder(host!!)
        buf.append(',')
        buf.append(packageDirectory)
        buf.append(',')
        buf.append(catalogDirectory)
        buf.append(',')
        buf.append(indexDirectory)
        buf.append(',')
        if (proxyHost != null) {
            buf.append(proxyHost)
        }
        buf.append(',')
        if (proxyPort != null) {
            buf.append(proxyPort)
        }
        return buf.toString()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#isNewer(org.crosswire.jsword.book.Book)
     */
    override fun isNewer(book: Book): Boolean {
        val sbmd = book.bookMetaData as SwordBookMetaData
        val conf = org.crosswire.jsword.book.sword.SwordBookMetaDataJvm.getConfigFile(sbmd)

        // The conf may not exist in our download dir.
        // In this case we say that it should not be downloaded again.
        if (conf == null || !conf.exists()) {
            return false
        }

        val configURI = NetUtil.getURI(conf)

        val remote = toRemoteURI(book)
        return NetUtil.isNewer(remote!!, configURI, proxyHost, proxyPort)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookList#getBooks()
     */
    override fun getBooks(): MutableList<Book> {
        try {
            if (!loaded) {
                loadCachedIndex()
            }

            // We need to create a List from the Set returned by
            // entries.values() so the underlying list is not modified.
            return ArrayList<Book>(entries.values)
        } catch (ex: InstallException) {
            log.error("Failed to reload cached index file", ex)
            return ArrayList<Book>()
        } catch (ex: IOException) {
            log.error("Failed to reload cached index file", ex)
            return ArrayList<Book>()
        } catch (ex: BookException) {
            log.error("Failed to reload cached index file", ex)
            return ArrayList<Book>()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBookList#getBooks(org.crosswire.jsword.book.BookFilter)
     */
    override fun getBooks(filter: BookFilter?): MutableList<Book> {
        val books: List<Book> = synchronized(this) {
            getBooks()
        }
        val temp = CollectionUtil.createList(BookFilterIterator(books, filter))
        return BookSet(temp)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#getBook(java.lang.String)
     */
    override fun getBook(book: String): Book? {
        val name = book
        val books: List<Book> = synchronized(this) {
            getBooks()
        }
        // Check name first
        // First check for exact matches
        for (candidate in books) {
            if (name == candidate.name) {
                return candidate
            }
        }

        // Next check for case-insensitive matches
        for (candidate in books) {
            if (name.equals(candidate.name, ignoreCase = true)) {
                return candidate
            }
        }

        // Then check initials
        // First check for exact matches
        for (candidate in books) {
            val bmd = candidate.bookMetaData
            if (name == bmd.initials) {
                return candidate
            }
        }

        // Next check for case-insensitive matches
        for (candidate in books) {
            val bmd = candidate.bookMetaData
            if (name.equals(bmd.initials, ignoreCase = true)) {
                return candidate
            }
        }
        return null
    }

    @Throws(InstallException::class)
    override fun install(book: Book) {
        install(book, null)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#install(org.crosswire.jsword.book.Book)
     */
    @Throws(InstallException::class)
    override fun install(book: Book, jobId: String?) {
        var jobId = jobId
        // // Is the book already installed? Then nothing to do.
        // if (Books.installed().getBook(book.getName()) != null)
        // {
        // return;
        // }
        //
        val sbmd = book.bookMetaData as SwordBookMetaData

        // TRANSLATOR: Progress label indicating the installation of a book. {0} is a placeholder for the name of the book.
        val jobName = JSMsg.gettext("Installing book: {0}", sbmd.name)
        if (jobId == null) {
            jobId = String.format(Progress.INSTALL_BOOK, book.initials)
        }
        val job = org.crosswire.common.progress.JobManagerJvm.createJob(jobId, jobName, Thread.currentThread())

        var temp: URI? = null
        var fileName: String? = null
        try {
            // Don't bother setting a size, we'll do it later.
            job.beginJob(jobName)

            Thread.yield()

            // TRANSLATOR: Progress label indicating the Initialization of installing of a book.
            job.sectionName = JSMsg.gettext("Initializing")

            temp = NetUtil.getTemporaryURI("swd", ZIP_SUFFIX)

            fileName = packageDirectory + "/" + sbmd.initials + ZIP_SUFFIX

            download(job, packageDirectory, sbmd.initials + ZIP_SUFFIX, temp)

            // Once the unzipping is started, we need to continue
            job.isCancelable = false
            if (!job.isFinished) {
                //copy into mods.d and modules under SWORD_HOME
                val dldir = SwordBookPathJvm.getSwordDownloadDir()
                IOUtil.unpackZip(NetUtil.getAsFile(temp), dldir, true, SwordConstants.DIR_CONF, SwordConstants.DIR_DATA)

                //copy everything else into JSWORD_HOME
                val jswordHome = NetUtil.getAsFile(CWProject.instance().writableProjectDir!!)
                IOUtil.unpackZip(NetUtil.getAsFile(temp), jswordHome, false, SwordConstants.DIR_CONF, SwordConstants.DIR_DATA)
                // TRANSLATOR: Progress label for installing the conf file for a book.
                job.sectionName = JSMsg.gettext("Copying config file")
                sbmd.library = NetUtil.getURI(dldir)
                SwordBookDriver.registerNewBook(sbmd)
            }

        } catch (e: IOException) {
            log.error("Error in downloading " + fileName)
            job.cancel()
            throw InstallException("Error in downloading " + fileName, e)
        } catch (e: InstallException) {
            job.cancel()
            throw e
        } catch (e: BookException) {
            job.cancel()
            throw InstallException("Something went wrong with the book ", e)
        } finally {
            job.done()
            // tidy up after ourselves
            // This is a best effort. If for some reason it does not delete now
            // it will automatically be deleted when the JVM exits normally.
            if (temp != null) {
                try {
                    NetUtil.delete(temp)
                } catch (e: IOException) {
                    log.warn("Error deleting temp download file.", e)
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#reloadBookList()
     */
    @Throws(InstallException::class)
    override fun reloadBookList() {
        // TRANSLATOR: Progress label for downloading one or more files.
        val jobName = JSMsg.gettext("Downloading files")
        val job = org.crosswire.common.progress.JobManagerJvm.createJob(Progress.RELOAD_BOOK_LIST, jobName, null)

        job.isNotifyUser = false
        job.beginJob(jobName)

        var errors: List<File>? = null
        try {
            val cacheDir = getCachedIndexDir()
            val confDir = NetUtil.lengthenURI(cacheDir!!, "mods.d.zip")
            val cache = getCachedIndexFile()
            download(job, catalogDirectory, FILE_LIST_GZ, cache)
            // It cannot be cancelled from this point forward
            job.isCancelable = false
            if (NetUtil.isFile(confDir!!)) {
                val confDirPath = confDir.path
                val confDirPathOld = confDirPath + ".old"

                val dirConf = File(confDirPath)
                val dirConfOld = File(confDirPathOld)
                // Get rid of the old. It shouldn't be there, but just in case
                if (dirConfOld.exists()) {
                    FileUtil.delete(dirConfOld)
                }
                if (!dirConf.renameTo(dirConfOld)) {
                    errors = FileUtil.delete(File(confDirPath))
                }
                // TODO(DM): list all that failed
                if (errors != null && !errors.isEmpty()) {
                    // TRANSLATOR: Common error condition: The file could not be deleted. There can be many reasons.
                    // {0} is a placeholder for the file.
                    throw InstallException(JSMsg.gettext("Unable to delete: {0}", errors[0]))
                }

                unpack(cacheDir, cache)
                // Best effort to delete
                FileUtil.delete(dirConfOld)
            }

            loaded = false
        } catch (ex: InstallException) {
            job.cancel()
            throw ex
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } finally {
            job.done()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#downloadSearchIndex(org.crosswire.jsword.book.Book, java.net.URI)
     */
    @Throws(InstallException::class)
    override fun downloadSearchIndex(book: Book, tempDest: URI) {
        // TRANSLATOR: Progress label for downloading one or more files.
        val jobName = JSMsg.gettext("Downloading files")
        val job = org.crosswire.common.progress.JobManagerJvm.createJob(String.format(Progress.DOWNLOAD_SEARCH_INDEX, book.initials), jobName, Thread.currentThread())
        job.beginJob(jobName)

        try {
            download(job, packageDirectory + '/' + SEARCH_DIR, book.initials + ZIP_SUFFIX, tempDest)
        } catch (ex: InstallException) {
            job.cancel()
            throw ex
        } finally {
            job.done()
        }
    }

    /**
     * Unpack the cached index file to disk
     */
    @Throws(IOException::class)
    protected open fun unpack(cacheDir: URI?, cache: URI?) {
        var fin: InputStream? = null
        var gin: GzipCompressorInputStream? = null
        var tin: ArchiveInputStream? = null
        var zout: ArchiveOutputStream? = null

        try {
            // The following is faster if it is passed a file.
            zout = ZipArchiveOutputStream(File(cacheDir!!.path, "mods.d.zip"))
            fin = NetUtil.getInputStream(cache!!)
            gin = GzipCompressorInputStream(fin)
            tin = TarArchiveInputStream(gin)
            while (true) {
                val entry: ArchiveEntry? = tin.nextEntry
                if (entry == null) {
                    break
                }

                // We'll create the mods.d directory elsewhere
                if (entry.isDirectory) {
                    continue
                }

                val path = entry.name

                if (!path.endsWith(SwordConstants.EXTENSION_CONF)) {
                    log.error("Not a SWORD config file: {}", path)
                    continue
                }

                val size = entry.size.toInt()
                // Every now and then an empty entry sneaks in
                if (size == 0) {
                    log.error("Empty entry: {}", path)
                    continue
                }

                val zipEntry: ArchiveEntry = ZipArchiveEntry(path)
                zout.putArchiveEntry(zipEntry)
                val buffer = ByteArray(size)
                val n = tin.read(buffer)
                if (n != size) {
                    log.error("Error: Could not read {} bytes for {} from {}", Integer.toString(size), path, cache.path)
                }
                zout.write(buffer, 0, n)
                zout.closeArchiveEntry()
            }
        } finally {
            IOUtil.close(tin)
            IOUtil.close(gin)
            IOUtil.close(fin)
            IOUtil.close(zout)
        }
    }

    /**
     * Load the cached index file into memory
     */
    @Throws(IOException::class, InstallException::class, BookException::class)
    protected open fun loadCachedIndex() {
        // We need a sword book driver so the installer can use the driver
        // name to use in deciding where to put the index.
        val fake: BookDriver = SwordBookDriver.instance()
        val nullBackend: Backend<*> = NullBackend()

        entries.clear()

        val cacheDir = getCachedIndexDir()
        val confDir = NetUtil.lengthenURI(cacheDir!!, "mods.d.zip")
        val cache = getCachedIndexFile()
        // If the cache file is missing, get a fresh copy
        if (!NetUtil.isFile(cache!!)) {
            reloadBookList()
        }

        // If it is not missing and mods.d doesn't exist
        // expand it
        // For backward compatibility.
        // It used to be that downloading the file didn't unpack it.
        // Now it does.
        if (!NetUtil.isFile(confDir!!)) {
            unpack(cacheDir, cache)
        }

        var fin: InputStream? = null
        var zin: ZipArchiveInputStream? = null
        try {
            fin = NetUtil.getInputStream(confDir)
            zin = ZipArchiveInputStream(fin)
            while (true) {
                val entry: ArchiveEntry? = zin.nextZipEntry
                if (entry == null) {
                    break
                }

                // We'll create the mods.d directory elsewhere
                if (entry.isDirectory) {
                    continue
                }

                val path = entry.name

                if (!path.endsWith(SwordConstants.EXTENSION_CONF)) {
                    log.error("Not a SWORD config file: {}", path)
                    continue
                }

                val size = entry.size.toInt()
                // Every now and then an empty entry sneaks in
                if (size == 0) {
                    log.error("Empty entry: {}", path)
                    continue
                }

                // Create a buffer of the right size
                val buffer = ByteArray(size)
                // Repeatedly read until all has been read
                var offset = 0
                while (offset < size) {
                    offset += zin.read(buffer, offset, size - offset)
                }

                if (offset != size) {
                    log.error("Error: Could not read {} bytes, instead {}, for {} from {}", Integer.toString(size), Integer.toString(offset), path, cache.path)
                }

                // Set the path to something that gives the path to the zip and the entry in the zip
                val sbmd = SwordBookMetaData(buffer, confDir.path + '!' + path)
                sbmd.setDriver(fake)

                // skip any book that is not supported.
                if (!sbmd.isSupported) {
                    continue
                }

                val book: Book = SwordBook(sbmd, nullBackend)
                entries[book.initials + book.name] = book
            }
        } finally {
            IOUtil.close(fin)
            IOUtil.close(zin)
        }
        loaded = true
    }

    /** remove the cached book list to clear memory
     */
    override fun close() {
        entries.clear()
        loaded = false
    }

    /**
     * @return the catalogDirectory
     */
    open fun getCatalogDirectory(): String? {
        return catalogDirectory
    }

    /**
     * @param catologDirectory
     *            the catologDirectory to set
     */
    open fun setCatalogDirectory(catologDirectory: String?) {
        this.catalogDirectory = catologDirectory
    }

    /**
     * @return Returns the directory.
     */
    open fun getPackageDirectory(): String? {
        return packageDirectory
    }

    /**
     * @param newDirectory
     *            The directory to set.
     */
    open fun setPackageDirectory(newDirectory: String?) {
        if (packageDirectory == null || packageDirectory != newDirectory) {
            packageDirectory = newDirectory
            loaded = false
        }
    }

    /**
     * @return the indexDirectory
     */
    open fun getIndexDirectory(): String? {
        return indexDirectory
    }

    /**
     * @param indexDirectory
     *            the indexDirectory to set
     */
    open fun setIndexDirectory(indexDirectory: String?) {
        this.indexDirectory = indexDirectory
    }

    /**
     * @return Returns the host.
     */
    open fun getHost(): String? {
        return host
    }

    /**
     * @param newHost
     *            The host to set.
     */
    open fun setHost(newHost: String?) {
        if (host == null || host != newHost) {
            host = newHost
            loaded = false
        }
    }

    /**
     * @return Returns the proxyHost.
     */
    open fun getProxyHost(): String? {
        return proxyHost
    }

    /**
     * @param newProxyHost
     *            The proxyHost to set.
     */
    open fun setProxyHost(newProxyHost: String?) {
        var pHost: String? = null
        if (newProxyHost != null && newProxyHost.length > 0) {
            pHost = newProxyHost
        }
        if (proxyHost == null || proxyHost != pHost) {
            proxyHost = pHost
            loaded = false
        }
    }

    /**
     * @return Returns the proxyPort.
     */
    open fun getProxyPort(): Int? {
        return proxyPort
    }

    /**
     * @param newProxyPort
     *            The proxyPort to set.
     */
    open fun setProxyPort(newProxyPort: Int?) {
        if (proxyPort == null || proxyPort != newProxyPort) {
            proxyPort = newProxyPort
            loaded = false
        }
    }

    /**
     * The URL for the cached index file for this installer
     */
    @Throws(InstallException::class)
    protected open fun getCachedIndexDir(): URI? {
        try {
            return CWProject.instance().getWritableProjectSubdir(getTempFileExtension(host, catalogDirectory), true)
        } catch (ex: IOException) {
            throw InstallException(JSOtherMsg.lookupText("URL manipulation failed"), ex)
        }
    }

    /**
     * The URL for the cached index file for this installer
     */
    @Throws(InstallException::class)
    protected open fun getCachedIndexFile(): URI? {
        return NetUtil.lengthenURI(getCachedIndexDir()!!, FILE_LIST_GZ)
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    override fun equals(other: Any?): Boolean {
        if (other !is AbstractSwordInstaller) {
            return false
        }
        val that: AbstractSwordInstaller = other

        if (!StringUtil.equals(this.host, that.host)) {
            return false
        }

        if (!StringUtil.equals(this.packageDirectory, that.packageDirectory)) {
            return false
        }

        return true
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: AbstractSwordInstaller): Int {

        var ret = host!!.compareTo(other.host!!)
        if (ret != 0) {
            ret = packageDirectory!!.compareTo(other.packageDirectory!!)
        }
        return ret
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    override fun hashCode(): Int {
        return host!!.hashCode() + packageDirectory!!.hashCode()
    }

    /**
     * A map of the books in this download area
     */
    @JvmField
    protected var entries: MutableMap<String, Book> = HashMap<String, Book>()

    /**
     * The remote hostname.
     */
    @JvmField
    protected var host: String? = null

    /**
     * The remote proxy hostname.
     */
    @JvmField
    protected var proxyHost: String? = null

    /**
     * The remote proxy port.
     */
    @JvmField
    protected var proxyPort: Int? = null

    /**
     * The directory containing zipped books on the <code>host</code>.
     */
    @JvmField
    protected var packageDirectory: String? = ""

    /**
     * The directory containing the catalog of all books on the
     * <code>host</code>.
     */
    @JvmField
    protected var catalogDirectory: String? = ""

    /**
     * The directory containing the catalog of all books on the
     * <code>host</code>.
     */
    @JvmField
    protected var indexDirectory: String? = ""

    /**
     * Do we need to reload the index file
     */
    @JvmField
    protected var loaded: Boolean = false

    companion object {
        /**
         * The sword index file
         */
        protected const val FILE_LIST_GZ: String = "mods.d.tar.gz"

        /**
         * The sword conf directory
         */
        protected const val CONF_DIR: String = "mods.d"

        /**
         * The suffix of zip books on this server
         */
        protected const val ZIP_SUFFIX: String = ".zip"

        /**
         * The relative path of the dir holding the search index files
         */
        protected const val SEARCH_DIR: String = "search/jsword/L1"

        /**
         * When we cache a download index
         */
        protected const val DOWNLOAD_PREFIX: String = "download-"

        /**
         * The log stream
         */
        @JvmField
        protected val log: Logger = LoggerFactory.getLogger(AbstractSwordInstaller::class)

        /**
         * What are we using as a temp filename?
         */
        private fun getTempFileExtension(host: String?, catalogDir: String?): String {
            return DOWNLOAD_PREFIX + host + catalogDir!!.replace('/', '_')
        }
    }
}
