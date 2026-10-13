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
package org.crosswire.common.progress

import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

import org.crosswire.common.util.randomJobId
import org.crosswire.common.util.CopyOnWriteList
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * JobManager is responsible for creating jobs and informing listeners about the
 * progress they make to completion.
 * 
 * <p>
 * Example code:
 * 
 * <pre>
 * final Thread worker = new Thread(&quot;DisplayPreLoader&quot;)
 * {
 *     public void run()
 *     {
 *         URL predictURI = Project.instance().getWritablePropertiesURI(&quot;save-name&quot;);
 *         Progress job = JobManager.createJob(&quot;Job Title&quot;, predictURI, this, true);
 *         try
 *         {
 *             job.setProgress(&quot;Step 1&quot;);
 *             ...
 *             job.setProgress(&quot;Step 2&quot;);
 *             ...
 *         }
 *         catch (Exception ex)
 *         {
 *             ...
 *             job.ignoreTimings();
 *         }
 *         finally
 *         {
 *             job.done();
 *         }
 *     }
 * };
 * worker.setPriority(Thread.MIN_PRIORITY);
 * worker.start();
 * </pre>
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class JobManager
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Create a new Job that cannot be canceled.
         * 
         * @param jobName the name of the Job
         * @return the job
         */
        @JvmStatic
        fun createJob(jobName: String): Progress {
            return createJob(randomJobId(), jobName, null)
        }

        /**
         * Create a new Job that can be canceled.
         * 
         * @param jobID a unique identifier for the job
         * @param jobName the name of the Job
         * @param workerThread the thread on which this job runs
         * @return the job
         */
        @JvmStatic
        fun createJob(jobID: String, jobName: String, canceller: (() -> Unit)?): Progress {
            val job: Progress = Job(jobID, jobName, canceller)
            synchronized(lock) { jobs.add(job) }

            log.debug("job starting: {}", job.jobName)

            return job
        }

        @JvmStatic
        fun findJob(jobID: String): Progress? {
            return synchronized(lock) { jobs.firstOrNull { it.jobID == jobID } }
        }

        /**
         * Add a listener to the list
         * 
         * @param li the interested listener
         */
        @JvmStatic
        fun addWorkListener(li: WorkListener?) {
            listeners.add(li)
        }

        /**
         * Remove a listener from the list
         * 
         * @param li the disinterested listener
         */
        @JvmStatic
        fun removeWorkListener(li: WorkListener?) {
            listeners.remove(li)
        }

        /**
         * Accessor for the currently known jobs
         * 
         * @return an iterator over the jobs
         */
        @JvmStatic
        fun iterator(): MutableIterator<Progress> {
            val snapshot = synchronized(lock) { jobs.toList() }.iterator()
            return object : MutableIterator<Progress> {
                override fun hasNext() = snapshot.hasNext()
                override fun next() = snapshot.next()
                override fun remove(): Unit = throw UnsupportedOperationException()
            }
        }

        /**
         * @return the number of current jobs
         */
        @JvmStatic
        fun getJobCount(): Int {
            return synchronized(lock) { jobs.size }
        }

        /**
         * Inform the listeners that a title has changed.
         * 
         * @param job the job that has made progress
         */
        // was protected static (package access for Job): Kotlin has no package visibility, so public
        @JvmStatic
        fun fireWorkProgressed(job: Progress) {
            val ev = WorkEvent(job)

            // We ought only to tell listeners about jobs that are in our
            // list of jobs so we need to fire before delete.
            for (worker in listeners) {
                worker!!.workProgressed(ev)
            }

            // Do we need to remove the job? Note that the section above will
            // probably execute after this so we will be firing events for jobs
            // that are no longer in our list of jobs. ho hum.
            if (job.isFinished) {
                log.debug("job finished: {}", job.jobName)
                synchronized(lock) { jobs.remove(job) }
            }
        }

        /**
         * List of listeners using thread safe list
         */
        private val listeners: MutableList<WorkListener?> = CopyOnWriteList()

        /**
         * List of current jobs
         */
        private val lock = SynchronizedObject()
        private val jobs: MutableSet<Progress> = linkedSetOf()

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(JobManager::class)
    }
}
