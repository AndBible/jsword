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
 * A generic method of having jobs that execute in the background over some time.
 */
package org.crosswire.common.progress

import org.crosswire.common.util.NetUtil
import org.crosswire.common.util.PropertyMap
import org.crosswire.jsword.JSMsg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.ArrayList
import java.util.HashMap
import java.util.Timer
import java.util.TimerTask

/**
 * A Generic method of keeping track of Threads and monitoring their progress.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class Job
/**
 * Create a new Job. This will automatically fire a workProgressed event to
 * all WorkListeners, with the work property of this job set to 0.
 * 
 * @param jobID the job identifier
 * @param jobName
 *            Short description of this job
 * @param worker
 *            Optional thread to use in request to stop worker
 */
// was protected (package access for JobManager): Kotlin has no package visibility, internal compiles to public
internal constructor(
    /**
     * The Job ID associated with this job
     * @return the job ID
     */
    override val jobID: String,
    /**
     * A short descriptive phrase
     */
    jobName: String,
    /**
     * Optional thread to monitor progress
     */
    private val workerThread: Thread?
) : Progress {
    /**
     * A short descriptive phrase
     */
    @get:Synchronized
    override val jobName: String = jobName

    /**
     * People that want to know about "cancelable" changes
     */
    private var listeners: MutableList<WorkListener> = ArrayList()

    /**
     * Does this job allow interruptions?
     */
    private var cancelable: Boolean = workerThread != null

    /**
     * The type of job being performed. This is used to simplify code.
     */
    private var jobMode: ProgressMode = ProgressMode.PREDICTIVE

    /**
     * Total amount of work to do.
     */
    private var totalUnits = 0

    /**
     * Have we just finished?
     */
    private var finished = false

    override var isNotifyUser: Boolean = true

    /**
     * The amount of work done against the total.
     */
    private var workUnits = 0

    /**
     * The officially reported progress
     */
    private var percent = 0

    /**
     * Description of what we are doing
     */
    private var currentSectionName: String? = null

    /**
     * The URI to which we load and save timings
     */
    private var predictionMapURI: URI? = null

    /**
     * The timings loaded from where they were saved after the last run
     */
    private var currentPredictionMap: MutableMap<String, Int>? = null

    /**
     * The timings as measured this time
     */
    private var nextPredictionMap: MutableMap<String?, Int>? = null

    /**
     * When did this job start? Measured in milliseconds since beginning of epoch.
     */
    private var startTime: Long = 0

    /**
     * The timer that lets us post fake progress events.
     */
    private var fakingTimer: Timer? = null

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#beginJob(java.lang.String)
     */
    override fun beginJob(sectionName: String?) {
        beginJob(sectionName, 100)
    }

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#beginJob(java.lang.String, int)
     */
    override fun beginJob(sectionName: String?, totalWork: Int) {
        if (this.finished) {
            return
        }

        synchronized(this) {
            finished = false
            currentSectionName = sectionName
            totalUnits = totalWork
            jobMode = if (totalUnits == 100) ProgressMode.PERCENT else ProgressMode.UNITS
        }

        // Report that the Job has started.
        if (isNotifyUser) JobManager.fireWorkProgressed(this)
    }

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#beginJob(java.lang.String, java.net.URI)
     */
    override fun beginJob(sectionName: String?, predictURI: URI?) {
        if (finished) {
            return
        }

        synchronized(this) {
            currentSectionName = sectionName
            predictionMapURI = predictURI
            jobMode = ProgressMode.PREDICTIVE
            startTime = System.currentTimeMillis()

            fakingTimer = Timer()
            fakingTimer!!.schedule(PredictTask(), 0L, REPORTING_INTERVAL.toLong())

            // Load currentPredictionMap. It's not a disaster if it doesn't load
            totalUnits = loadPredictions()

            // There were no prior predictions so punt.
            if (totalUnits == Progress.UNKNOWN) {
                // if we have nothing to go on use our assumption
                totalUnits = EXTRA_TIME
                jobMode = ProgressMode.UNKNOWN
            }

            // And the predictions for next time
            nextPredictionMap = HashMap()
        }

        // Report that the Job has started.
        if (isNotifyUser) JobManager.fireWorkProgressed(this)
    }

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#getProgressMode()
     */
    override val progressMode: ProgressMode
        get() = jobMode

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#getTotalWork()
     * @see org.crosswire.common.progress.Progress#setTotalWork(int)
     */
    override var totalWork: Int
        @Synchronized get() = totalUnits
        set(totalWork) {
            this.totalUnits = totalWork
        }

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#getWork()
     * @see org.crosswire.common.progress.Progress#setWork(int)
     */
    override var work: Int
        get() = percent
        set(work) {
            workDone = work
        }

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#getWorkDone()
     * @see org.crosswire.common.progress.Progress#setWork(int)
     */
    override var workDone: Int
        get() = workUnits
        set(work) {
            if (finished) {
                return
            }

            synchronized(this) {
                if (workUnits == work) {
                    return
                }

                workUnits = work

                val oldPercent = percent
                percent = 100 * workUnits / totalUnits
                if (oldPercent == percent) {
                    return
                }
            }

            if (isNotifyUser) JobManager.fireWorkProgressed(this)
        }

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#incrementWorkDone(int)
     */
    override fun incrementWorkDone(step: Int) {
        if (finished) {
            return
        }

        synchronized(this) {
            workUnits += step

            val oldPercent = percent
            try {
                percent = (100L * workUnits / totalUnits).toInt()
            } catch (e: ArithmeticException) {
                log.error("Artihmetic exception", e)
            }
            if (oldPercent == percent) {
                return
            }
        }

        if (isNotifyUser) JobManager.fireWorkProgressed(this)
    }

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#getSectionName()
     * @see org.crosswire.common.progress.Progress#setSectionName(java.lang.String)
     */
    override var sectionName: String?
        get() = currentSectionName
        set(sectionName) {
            if (finished) {
                return
            }

            var doUpdate = false
            synchronized(this) {
                // If we are in some kind of predictive mode, then measure progress toward the expected end.
                if (jobMode == ProgressMode.PREDICTIVE || jobMode == ProgressMode.UNKNOWN) {
                    doUpdate = updateProgress(System.currentTimeMillis())

                    // We are done with the current section and are starting another
                    // So record the length of the last section
                    if (nextPredictionMap != null) {
                        nextPredictionMap!![currentSectionName] = workUnits
                    }
                }

                currentSectionName = sectionName
            }

            // Don't automatically tell listeners that the label changed.
            // Only do so if it is time to do an update.
            if (doUpdate && isNotifyUser) {
                JobManager.fireWorkProgressed(this)
            }
        }

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#done()
     */
    override fun done() {
        // TRANSLATOR: This shows up in a progress bar when progress is finished.
        val sectionName = JSMsg.gettext("Done")

        synchronized(this) {
            finished = true

            currentSectionName = sectionName

            // Turn off the timer
            if (fakingTimer != null) {
                fakingTimer!!.cancel()
                fakingTimer = null
            }

            workUnits = totalUnits
            percent = 100

            if (nextPredictionMap != null) {
                nextPredictionMap!![currentSectionName] = (System.currentTimeMillis() - startTime).toInt()
            }
        }

        // Report that the job is done.
        if (isNotifyUser) JobManager.fireWorkProgressed(this)

        synchronized(this) {
            if (predictionMapURI != null) {
                savePredictions()
            }
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#cancel()
     */
    override fun cancel() {
        if (!finished) {
            ignoreTimings()
            done()
            if (workerThread != null) {
                workerThread.interrupt()
            }
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#isFinished()
     */
    override val isFinished: Boolean
        get() = finished

    /* (non-Javadoc)
     * @see org.crosswire.common.progress.Progress#isCancelable()
     * @see org.crosswire.common.progress.Progress#setCancelable(boolean)
     */
    override var isCancelable: Boolean
        get() = cancelable
        set(newInterruptable) {
            if (workerThread == null || finished) {
                return
            }
            cancelable = newInterruptable
            fireStateChanged()
        }

    /**
     * Add a listener to the list
     * 
     * @param li the interested listener
     */
    @Synchronized
    fun addWorkListener(li: WorkListener) {
        val temp: MutableList<WorkListener> = ArrayList()
        temp.addAll(listeners)

        if (!temp.contains(li)) {
            temp.add(li)
            listeners = temp
        }
    }

    /**
     * Remote a listener from the list
     * 
     * @param li the disinterested listener
     */
    @Synchronized
    fun removeWorkListener(li: WorkListener) {
        if (listeners.contains(li)) {
            val temp: MutableList<WorkListener> = ArrayList()
            temp.addAll(listeners)
            temp.remove(li)
            listeners = temp
        }
    }

    /**
     * Inform the listeners that the cancelable state has changed
     */
    protected fun fireStateChanged() {
        val ev = WorkEvent(this)

        // we need to keep the synchronized section very small to avoid deadlock
        // certainly keep the event dispatch clear of the synchronized block or
        // there will be a deadlock
        val temp: MutableList<WorkListener> = ArrayList()
        synchronized(this) {
            temp.addAll(listeners)
        }

        // We ought only to tell listeners about jobs that are in our
        // list of jobs so we need to fire before delete.
        val count = temp.size
        for (i in 0 until count) {
            temp[i].workStateChanged(ev)
        }
    }

    /**
     * Get estimated the percent progress
     * 
     * @param now the current point in progress
     * @return true if there is an update to progress.
     */
    @Synchronized
    protected fun updateProgress(now: Long): Boolean {
        val oldPercent = percent
        workUnits = (now - startTime).toInt()

        // Are we taking more time than expected?
        // Then we are at 100%
        if (workUnits > totalUnits) {
            workUnits = totalUnits
            percent = 100
        } else {
            percent = 100 * workUnits / totalUnits
        }
        return oldPercent != percent
    }

    /**
     * Load the predictive timings if any
     * 
     * @return the length of progress
     */
    private fun loadPredictions(): Int {
        var maxAge = Progress.UNKNOWN
        try {
            currentPredictionMap = HashMap()
            val temp: PropertyMap = NetUtil.loadProperties(predictionMapURI!!)

            // Determine the predicted time from the current prediction map
            for (title in temp.keys) {
                val timestr = temp[title]

                try {
                    val time = Integer.valueOf(timestr)
                    currentPredictionMap!![title] = time

                    // if this time is later than the latest
                    val age = time.toInt()
                    if (maxAge < age) {
                        maxAge = age
                    }
                } catch (ex: NumberFormatException) {
                    log.error("Time format error", ex)
                }
            }
        } catch (ex: IOException) {
            log.debug("Failed to load prediction times - guessing")
        }

        return maxAge
    }

    /**
     * Save the known timings to a properties file.
     */
    private fun savePredictions() {
        // Now we know the start and the end we can convert all times to
        // percents
        val predictions = PropertyMap()
        for (sectionName in nextPredictionMap!!.keys) {
            val age = nextPredictionMap!![sectionName]
            predictions[sectionName] = age!!.toString()
        }

        // And save. It's not a disaster if this goes wrong
        try {
            NetUtil.storeProperties(predictions, predictionMapURI!!, "Predicted Startup Times")
        } catch (ex: IOException) {
            log.error("Failed to save prediction times", ex)
        }
    }

    /**
     * Typically called from in a catch block, this ensures that we don't save
     * the timing file because we have a messed up run.
     */
    @Synchronized
    private fun ignoreTimings() {
        predictionMapURI = null
    }

    /**
     * So we can fake progress for Jobs that don't tell us how they are doing
     */
    internal inner class PredictTask : TimerTask() {
        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        override fun run() {
            if (updateProgress(System.currentTimeMillis())) {
                JobManager.fireWorkProgressed(this@Job)
            }
        }
    }

    private companion object {
        private const val REPORTING_INTERVAL = 100

        /**
         * The amount of extra time if the predicted time was off and more time is needed.
         */
        private const val EXTRA_TIME = 2 * REPORTING_INTERVAL

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(Job::class.java)
    }
}
