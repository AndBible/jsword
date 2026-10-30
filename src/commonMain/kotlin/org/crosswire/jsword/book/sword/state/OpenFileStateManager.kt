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

import kotlin.jvm.JvmStatic

import org.crosswire.jsword.book.BookException
import org.crosswire.common.util.currentTimeMillis
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.sword.BlockType
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.crosswire.common.util.scheduleRepeating
import kotlin.concurrent.Volatile

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
    private val lock = SynchronizedObject()
    private val metaToStates = mutableMapOf<BookMetaData, ArrayDeque<OpenFileState>>()
    private val generations = mutableMapOf<String, Any>()

    @Volatile
    private var shuttingDown = false

    private val monitoringThread = scheduleRepeating(cleanupIntervalSeconds * 1000L) {
        synchronized(lock) {
            val currentTime = currentTimeMillis()
            for (states in metaToStates.values) {
                val iterator = states.iterator()
                while (iterator.hasNext()) {
                    val state = iterator.next()
                    if (state.getLastAccess() + maxExpiry * 1000 < currentTime) {
                        state.releaseResources()
                        iterator.remove()
                    }
                }
            }
        }
    }

    @Throws(BookException::class)
    fun getRawBackendState(metadata: BookMetaData): RawBackendState =
        checkout(metadata) { RawBackendState(metadata) }

    @Throws(BookException::class)
    fun getRawFileBackendState(metadata: BookMetaData): RawFileBackendState =
        checkout(metadata) { RawFileBackendState(metadata) }

    @Throws(BookException::class)
    fun getGenBookBackendState(metadata: BookMetaData): GenBookBackendState =
        checkout(metadata) { GenBookBackendState(metadata) }

    @Throws(BookException::class)
    fun getRawLDBackendState(metadata: BookMetaData): RawLDBackendState =
        checkout(metadata) { RawLDBackendState(metadata) }

    @Throws(BookException::class)
    fun getZLDBackendState(metadata: BookMetaData): ZLDBackendState =
        checkout(metadata) { ZLDBackendState(metadata) }

    @Throws(BookException::class)
    fun getZVerseBackendState(metadata: BookMetaData, blockType: BlockType): ZVerseBackendState =
        checkout(metadata) { ZVerseBackendState(metadata, blockType) }

    @Suppress("UNCHECKED_CAST")
    private fun <T : AbstractOpenFileState> checkout(metadata: BookMetaData, create: () -> T): T = synchronized(lock) {
        ensureNotShuttingDown()
        val state = metaToStates.getOrPut(metadata) { ArrayDeque() }.removeFirstOrNull() as T?
        LOGGER.trace(if (state == null) "Initializing: {}" else "Reusing: {}", metadata.initials)
        (state ?: create()).also {
            it.poolGeneration = generations.getOrPut(metadata.initials) { Any() }
            it.setLastAccess(currentTimeMillis())
        }
    }

    /** Close idle handles now; borrowed handles finish their read and are discarded on return. */
    internal fun evict(initials: String) = synchronized(lock) {
        generations[initials] = Any()
        val iterator = metaToStates.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key.initials != initials) continue
            entry.value.forEach { it.releaseResources() }
            iterator.remove()
        }
    }

    fun release(fileState: OpenFileState?) {
        if (fileState == null) return
        synchronized(lock) {
            val metadata = fileState.getBookMetaData()
            if (shuttingDown || (fileState is AbstractOpenFileState &&
                    fileState.poolGeneration !== generations[metadata.initials])) {
                fileState.releaseResources()
                return@synchronized
            }
            fileState.setLastAccess(currentTimeMillis())
            LOGGER.trace("Offering to releasing: {}", metadata.initials)
            metaToStates.getOrPut(metadata) { ArrayDeque() }.addLast(fileState)
        }
    }

    /** Shuts down all pooled open files. */
    fun shutDown() {
        shuttingDown = true
        monitoringThread.cancel()
        synchronized(lock) {
            for (states in metaToStates.values) {
                while (states.isNotEmpty()) states.removeFirst().releaseResources()
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
        private val managerLock = SynchronizedObject()
        internal fun createForTest(cleanupIntervalSeconds: Int, maxExpirySeconds: Int): OpenFileStateManager =
            OpenFileStateManager(cleanupIntervalSeconds, maxExpirySeconds)

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
            synchronized(managerLock) {
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
                synchronized(managerLock) {
                    init(60, 60)
                }
            }
            return manager!!
        }
    }
}
