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

import kotlin.jvm.JvmField
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import okio.IOException

import org.crosswire.common.progress.JobManager
import org.crosswire.common.progress.Progress
import org.crosswire.common.util.CWProject
import org.crosswire.common.util.KFiles
import org.crosswire.common.util.KUri
import org.crosswire.common.util.NetUtil
import org.crosswire.common.util.StringUtil
import org.crosswire.common.util.pathUri
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.AbstractBookList
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookFilter
import org.crosswire.jsword.book.BookFilterIterator
import org.crosswire.jsword.book.BookSet
import org.crosswire.jsword.book.install.DownloadCancelledException
import org.crosswire.jsword.book.install.InstallException
import org.crosswire.jsword.book.install.Installer
import org.crosswire.jsword.book.install.SwordArchive
import org.crosswire.jsword.book.sword.Backend
import org.crosswire.jsword.book.sword.NullBackend
import org.crosswire.jsword.book.sword.SwordBook
import org.crosswire.jsword.book.sword.SwordBookDriver
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.book.sword.SwordBookPath
import org.crosswire.jsword.book.sword.SwordConstants
import org.crosswire.jsword.book.sword.StorageLocation
import org.crosswire.jsword.book.sword.storageChild
import org.crosswire.jsword.book.sword.storageExists
import org.crosswire.jsword.book.sword.storageMove
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
    @Throws(InstallException::class, kotlin.coroutines.cancellation.CancellationException::class)
    protected abstract suspend fun download(job: Progress?, dir: String?, file: String?, dest: KUri?)

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
    override suspend fun isNewer(book: Book): Boolean {
        val sbmd = book.bookMetaData as SwordBookMetaData
        val conf = sbmd.configFile

        // The conf may not exist in our download dir.
        // In this case we say that it should not be downloaded again.
        if (conf == null || !KFiles.exists(conf)) {
            return false
        }

        val configURI = pathUri(conf)

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
        val books: List<Book> = synchronized(lock) {
            getBooks()
        }
        val temp = ArrayList<Book>()
        for (book in BookFilterIterator(books, filter)) {
            temp.add(book)
        }
        return BookSet(temp)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#getBook(java.lang.String)
     */
    override fun getBook(book: String): Book? {
        val name = book
        val books: List<Book> = synchronized(lock) {
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

    @Throws(InstallException::class, kotlin.coroutines.cancellation.CancellationException::class)
    override suspend fun install(book: Book) {
        install(book, null)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.install.Installer#install(org.crosswire.jsword.book.Book)
     */
    @Throws(InstallException::class, kotlin.coroutines.cancellation.CancellationException::class)
    override suspend fun install(book: Book, jobId: String?) {
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
            jobId = Progress.INSTALL_BOOK.replace("%s", book.initials)
        }
        val bridge = CancelBridge()
        val job = JobManager.createJob(jobId, jobName, bridge.canceller)

        var temp: KUri? = null
        var fileName: String? = null
        try {
            // Don't bother setting a size, we'll do it later.
            job.beginJob(jobName)

            // TRANSLATOR: Progress label indicating the Initialization of installing of a book.
            job.sectionName = JSMsg.gettext("Initializing")

            temp = NetUtil.getTemporaryURI("swd", ZIP_SUFFIX)

            fileName = packageDirectory + "/" + sbmd.initials + ZIP_SUFFIX

            bridge.download(toRemoteURI(book)) { download(job, packageDirectory, sbmd.initials + ZIP_SUFFIX, temp) }

            // Once the unzipping is started, we need to continue
            job.isCancelable = false
            if (!job.isFinished) {
                //copy into mods.d and modules under SWORD_HOME
                val dldir = SwordBookPath.getSwordDownloadLocation()
                // The lexical File spelling of a file URI is its path, as File(uri.path) had it.
                val tempSpelling = fileSpelling(temp)
                SwordArchive.unpackZipSpelling(tempSpelling, dldir.spelling, true, SwordConstants.DIR_CONF, SwordConstants.DIR_DATA)

                //copy everything else into JSWORD_HOME
                val jswordHome = fileSpelling(CWProject.instance().writableProjectDir!!)
                SwordArchive.unpackZipSpelling(tempSpelling, jswordHome, false, SwordConstants.DIR_CONF, SwordConstants.DIR_DATA)
                // TRANSLATOR: Progress label for installing the conf file for a book.
                job.sectionName = JSMsg.gettext("Copying config file")
                sbmd.library = pathUri(dldir.path)
                SwordBookDriver.registerNewBook(sbmd)
            }

        } catch (e: IOException) {
            log.error("Error in downloading " + fileName)
            bridge.disarm()
            job.cancel()
            throw InstallException("Error in downloading " + fileName, e)
        } catch (e: InstallException) {
            bridge.disarm()
            job.cancel()
            throw e
        } catch (e: BookException) {
            bridge.disarm()
            job.cancel()
            throw InstallException("Something went wrong with the book ", e)
        } catch (e: CancellationException) {
            // The calling coroutine was cancelled: cancel the job too, so its listeners see it end.
            bridge.disarm()
            job.cancel()
            throw e
        } finally {
            bridge.disarm()
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
    @Throws(InstallException::class, kotlin.coroutines.cancellation.CancellationException::class)
    override suspend fun reloadBookList() {
        // TRANSLATOR: Progress label for downloading one or more files.
        val jobName = JSMsg.gettext("Downloading files")
        val job = JobManager.createJob(Progress.RELOAD_BOOK_LIST, jobName, null)

        job.isNotifyUser = false
        job.beginJob(jobName)

        var errors: List<okio.Path>? = null
        try {
            val cacheDir = getCachedIndexDir()
            val confDir = NetUtil.lengthenURI(cacheDir!!, "mods.d.zip")
            val cache = getCachedIndexFile()
            download(job, catalogDirectory, FILE_LIST_GZ, cache)
            // It cannot be cancelled from this point forward
            job.isCancelable = false
            if (NetUtil.isFile(confDir!!)) {
                val confDirPath = confDir.getPath()!!
                val confDirPathOld = confDirPath + ".old"

                val dirConf = StorageLocation(confDirPath)
                val dirConfOld = StorageLocation(confDirPathOld)
                // Get rid of the old. It shouldn't be there, but just in case
                if (storageExists(dirConfOld)) {
                    KFiles.deleteRecursively(dirConfOld.path)
                }
                if (!storageMove(dirConf, dirConfOld)) {
                    errors = KFiles.deleteRecursively(dirConf.path)
                }
                // TODO(DM): list all that failed
                if (errors != null && !errors.isEmpty()) {
                    // TRANSLATOR: Common error condition: The file could not be deleted. There can be many reasons.
                    // {0} is a placeholder for the file.
                    throw InstallException(JSMsg.gettext("Unable to delete: {0}", errors[0]))
                }

                unpack(cacheDir, cache)
                // Best effort to delete
                KFiles.deleteRecursively(dirConfOld.path)
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
    @Throws(InstallException::class, kotlin.coroutines.cancellation.CancellationException::class)
    override suspend fun downloadSearchIndex(book: Book, tempDest: KUri) {
        // TRANSLATOR: Progress label for downloading one or more files.
        val jobName = JSMsg.gettext("Downloading files")
        val bridge = CancelBridge()
        val job = JobManager.createJob(Progress.DOWNLOAD_SEARCH_INDEX.replace("%s", book.initials), jobName, bridge.canceller)
        job.beginJob(jobName)

        try {
            bridge.download(toRemoteURI(book)) { download(job, packageDirectory + '/' + SEARCH_DIR, book.initials + ZIP_SUFFIX, tempDest) }
        } catch (ex: InstallException) {
            bridge.disarm()
            job.cancel()
            throw ex
        } catch (ex: CancellationException) {
            bridge.disarm()
            job.cancel()
            throw ex
        } finally {
            bridge.disarm()
            job.done()
        }
    }

    /**
     * Connects a JSword job to the coroutines of the download phase of a suspend install.
     *
     * - The job's canceller (the Progress API, for example the UI's cancel button) cancels only the install-owned
     *   scope that [download] runs in, never the caller's coroutine. [download] then ends with
     *   [DownloadCancelledException] for the given uri while the caller stays active, exactly what the thread
     *   interrupt produced before K5b.
     * - A cancelled caller coroutine passes through as its own CancellationException; the catch blocks of the suspend
     *   functions then cancel the job (the coroutine-to-job direction).
     * - The bridge is disarmed when the download phase ends (the job is not cancelable from then on, and a later
     *   Progress cancel must not touch anything) and before the installer cancels its own job after a failure, so an
     *   internal failure never cancels anything.
     */
    private class CancelBridge {
        private val armed = atomic(true)
        private val userCancelled = atomic(false)
        private val scope = atomic<Job?>(null)

        /** Handed to the JSword job. */
        val canceller: () -> Unit = {
            if (armed.value) {
                userCancelled.value = true
                scope.value?.cancel()
            }
        }

        fun disarm() {
            armed.value = false
        }

        /** Runs [block] in an install-owned scope that [canceller] can cancel; a user cancel becomes [DownloadCancelledException]. */
        suspend fun <T> download(uri: KUri?, block: suspend () -> T): T {
            // A failure of the block is carried out of the scope as is: the coroutines stack-trace recovery would
            // otherwise hand the caller a copy of the exception instead of the instance the download threw.
            var failure: Throwable? = null
            var value: T? = null
            try {
                coroutineScope {
                    scope.value = coroutineContext[Job]
                    if (userCancelled.value) cancel() // pressed before the scope existed
                    try {
                        value = block()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        failure = e
                    } finally {
                        armed.value = false // the download phase is over: nothing is cancelable any more
                        scope.value = null
                    }
                }
            } catch (e: CancellationException) {
                // Only the inner scope was cancelled and the caller is still active: that is the user's cancel.
                if (userCancelled.value && currentCoroutineContext().isActive) throw DownloadCancelledException(uri)
                throw e
            }
            failure?.let { throw it }
            @Suppress("UNCHECKED_CAST")
            return value as T
        }
    }

    /**
     * Unpack the cached index file to disk
     */
    @Throws(IOException::class)
    protected open fun unpack(cacheDir: KUri?, cache: KUri?) {
        SwordArchive.tarGzConfsToZipSpelling(
            fileSpelling(cache!!), storageChild(StorageLocation(cacheDir!!.getPath()!!), "mods.d.zip").spelling
        )
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
        // A missing cache is not downloaded here (getBooks never touches the network); the caller reloads the
        // book list explicitly. The list stays empty and unloaded, so a later getBooks sees a cache that appeared.
        if (!NetUtil.isFile(cache!!)) {
            if (!missingCacheLogged.getAndSet(true)) log.info("No cached index for {}; call reloadBookList()", host)
            return
        }

        // If it is not missing and mods.d doesn't exist
        // expand it
        // For backward compatibility.
        // It used to be that downloading the file didn't unpack it.
        // Now it does.
        if (!NetUtil.isFile(confDir!!)) {
            unpack(cacheDir, cache)
        }

        SwordArchive.visitConfZipSpelling(fileSpelling(confDir)) { path, buffer ->
                // Set the path to something that gives the path to the zip and the entry in the zip
                val sbmd = SwordBookMetaData(buffer, confDir.getPath() + '!' + path)
                sbmd.setDriver(fake)

                // skip any book that is not supported.
                if (!sbmd.isSupported) {
                    return@visitConfZipSpelling
                }

                val book: Book = SwordBook(sbmd, nullBackend)
                entries[book.initials + book.name] = book
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
    protected open fun getCachedIndexDir(): KUri? {
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
    protected open fun getCachedIndexFile(): KUri? {
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
     * Guards the book list reads (rule 19: was the monitor of the installer instance).
     */
    private val lock = SynchronizedObject()

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

    /** The missing-cache info is logged once per installer instance, not on every getBooks(). */
    private val missingCacheLogged = atomic(false)

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
         * The lexical File spelling of a file URI (what `File(uri.path).path` gave): its decoded path, normalised
         * the way the storage hooks spell file names.
         */
        private fun fileSpelling(uri: KUri): String = StorageLocation(uri.getPath()!!).spelling

        /**
         * What are we using as a temp filename?
         */
        private fun getTempFileExtension(host: String?, catalogDir: String?): String {
            return DOWNLOAD_PREFIX + host + catalogDir!!.replace('/', '_')
        }
    }
}
