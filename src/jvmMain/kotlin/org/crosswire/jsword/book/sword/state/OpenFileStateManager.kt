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
 * © CrossWire Bible Society, 2013 - 2016
 *
 */
/**
 * File management of SWORD Books.
 */
package org.crosswire.jsword.book.sword.state

import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.BlockType
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import java.util.HashMap
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * Manages the creation and re-distribution of open file states. This increases
 * performance as more often than not, the same file state may be used. For
 * example we may be carrying out a contains() operation followed by a read to
 * disk for a particular key
 * 
 * Each [BookMetaData] has a corresponding a file state which is
 * different to another. Furthermore, concurrent accesses cannot share this file
 * state as the [OpenFileState] records where in the file it is, for
 * reading several verses together for example. As a result, we want to key a
 * lookup by [BookMetaData], which then gives us a pool of available
 * file states... We create some more if none are available.
 * 
 * In order to prevent memory leaks (OpenFileStates might be quite heavy as they do some internal caching of file data..
 * In order to avoid many file references piling up in memory, we implement a background cleaning thread which will clean
 * up redundant keys every so often.
 *
 *
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * 
 * @author DM Smith
 * @author Chris Burrell
 */
class OpenFileStateManager
/**
 * prevent instantiation
 */
private constructor(cleanupIntervalSeconds: Int, maxExpiry: Int) {
    private val metaToStates: MutableMap<BookMetaData, Queue<OpenFileState>> = HashMap()

    @Volatile
    private var shuttingDown = false

    private val monitoringThread: ScheduledFuture<*> = Executors.newScheduledThreadPool(1, object : ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            val t = Thread(r)
            t.isDaemon = true
            return t
        }
    }).scheduleWithFixedDelay(object : Runnable {
        override fun run() {
            // check the state of the maps and queues... The queues may have too much in them and that will in turn max out
            // the heap.
            val currentTime = System.currentTimeMillis()

            for (e in this@OpenFileStateManager.metaToStates.values) {
                val iterator = e.iterator()
                while (iterator.hasNext()) {
                    val state = iterator.next()
                    if (state.getLastAccess() + maxExpiry * 1000 < currentTime) {
                        //release resources
                        state.releaseResources()

                        //remove from the queues
                        iterator.remove()
                    }
                }
            }
        }
    }, 0, cleanupIntervalSeconds.toLong(), TimeUnit.SECONDS)

    @Throws(BookException::class)
    fun getRawBackendState(metadata: BookMetaData): RawBackendState {
        ensureNotShuttingDown()

        val state: RawBackendState? = getInstance(metadata)
        if (state == null) {
            LOGGER.trace("Initializing: {}", metadata.initials)
            return RawBackendState(metadata)
        }

        LOGGER.trace("Reusing: {}", metadata.initials)
        return state
    }

    @Throws(BookException::class)
    fun getRawFileBackendState(metadata: BookMetaData): RawFileBackendState {
        ensureNotShuttingDown()

        val state: RawFileBackendState? = getInstance(metadata)
        if (state == null) {
            LOGGER.trace("Initializing: {}", metadata.initials)
            return RawFileBackendState(metadata)
        }

        LOGGER.trace("Reusing: {}", metadata.initials)
        return state
    }

    @Throws(BookException::class)
    fun getGenBookBackendState(metadata: BookMetaData): GenBookBackendState {
        ensureNotShuttingDown()

        val state: GenBookBackendState? = getInstance(metadata)
        if (state == null) {
            LOGGER.trace("Initializing: {}", metadata.initials)
            return GenBookBackendState(metadata)
        }

        LOGGER.trace("Reusing: {}", metadata.initials)
        return state
    }

    @Throws(BookException::class)
    fun getRawLDBackendState(metadata: BookMetaData): RawLDBackendState {
        ensureNotShuttingDown()

        val state: RawLDBackendState? = getInstance(metadata)
        if (state == null) {
            LOGGER.trace("Initializing: {}", metadata.initials)
            return RawLDBackendState(metadata)
        }

        LOGGER.trace("Reusing: {}", metadata.initials)
        return state
    }

    @Throws(BookException::class)
    fun getZLDBackendState(metadata: BookMetaData): ZLDBackendState {
        ensureNotShuttingDown()

        val state: ZLDBackendState? = getInstance(metadata)
        if (state == null) {
            LOGGER.trace("Initializing: {}", metadata.initials)
            return ZLDBackendState(metadata)
        }

        LOGGER.trace("Reusing: {}", metadata.initials)
        return state
    }

    @Throws(BookException::class)
    fun getZVerseBackendState(metadata: BookMetaData, blockType: BlockType): ZVerseBackendState {
        ensureNotShuttingDown()

        val state: ZVerseBackendState? = getInstance(metadata)
        if (state == null) {
            LOGGER.trace("Initializing: {}", metadata.initials)
            return ZVerseBackendState(metadata, blockType)
        }

        LOGGER.trace("Reusing: {}", metadata.initials)
        return state
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : OpenFileState> getInstance(metadata: BookMetaData): T? {
        val availableStates = getQueueForMeta(metadata)
        val state = availableStates.poll() as T?

        //while not strictly necessary, the documentation suggests that iterating through the collection
        //gives you a snapshot at some point in time, though not necessarily consistent, so just in case this remains
        //in access of the iterator() functionality, we update the last access date to avoid it being destroyed while we
        //use it
        if (state != null) {
            state.setLastAccess(System.currentTimeMillis())
        }
        return state
    }

    private fun getQueueForMeta(metadata: BookMetaData): Queue<OpenFileState> {
        var availableStates = metaToStates[metadata]
        if (availableStates == null) {
            synchronized(OpenFileState::class.java) {
                availableStates = ConcurrentLinkedQueue()
                metaToStates[metadata] = availableStates!!
            }
        }
        return availableStates!!
    }

    fun release(fileState: OpenFileState?) {
        if (fileState == null) {
            // can't release anything. JSword has failed to open a file state,
            // and a finally block is trying to close this
            return
        }

        fileState.setLastAccess(System.currentTimeMillis())

        // instead of releasing, we add to our queue
        val bmd = fileState.getBookMetaData()
        val queueForMeta = getQueueForMeta(bmd)
        LOGGER.trace("Offering to releasing: {}", bmd.initials)
        val offered = queueForMeta.offer(fileState)

        // ignore if we couldn't offer to the queue
        if (!offered) {
            LOGGER.trace("Released: {}", bmd.initials)
            fileState.releaseResources()
        }
    }

    /**
     * Shuts down all open files
     */
    fun shutDown() {
        shuttingDown = true
        this.monitoringThread.cancel(true)
        for (e in metaToStates.values) {
            var state: OpenFileState? = null
            while (e.poll().also { state = it } != null) {
                state!!.releaseResources()
            }
        }
    }

    @Throws(BookException::class)
    private fun ensureNotShuttingDown() {
        if (shuttingDown) {
            throw BookException("Unable to read book, application is shutting down.")
        }
    }

    companion object {
        @Volatile
        private var manager: OpenFileStateManager? = null
        private val LOGGER: Logger = LoggerFactory.getLogger(OpenFileStateManager::class)

        /**
         * Allow the caller to initialize with their own settings. Should the OpenFileStateManager already be initialized
         * a no-op will occur. No need for double-checked locking here
         * 
         * @param cleanupIntervalSeconds seconds before cleanup
         * @param maxExpiry 
         */
        @JvmStatic
        fun init(cleanupIntervalSeconds: Int, maxExpiry: Int) {
            synchronized(OpenFileStateManager::class.java) {
                if (manager == null) {
                    manager = OpenFileStateManager(cleanupIntervalSeconds, maxExpiry)
                } else {
                    // already initialized
                    LOGGER.warn("The OpenFileStateManager has already been initialised, potentially with its default settings. The following values were ignored: cleanUpInterval [{}], maxExpiry=[{}]", cleanupIntervalSeconds.toString(), maxExpiry.toString())
                }
            }
        }

        /**
         * Singleton instance method to return the one and only Open File State Manager
         * @return the singleton
         */
        @JvmStatic
        fun instance(): OpenFileStateManager {
            if (manager == null) {
                synchronized(OpenFileStateManager::class.java) {
                    init(60, 60)
                }
            }
            return manager!!
        }
    }
}
