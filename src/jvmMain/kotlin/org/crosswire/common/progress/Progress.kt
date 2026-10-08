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

import java.net.URI

/**
 * A Generic way of keeping track of Threads and monitoring their progress.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
interface Progress {
    /**
     * Start the task measured from 0 to 100. It is the caller's responsibility to compute percentages.
     * 
     * @param sectionName
     *            the initial name of the job.
     */
    fun beginJob(sectionName: String?)

    /**
     * Start the task reporting progress toward total work. Percentages will be
     * computed on behalf of the caller.
     * 
     * @param sectionName
     *            the initial name of the job.
     * @param totalWork
     *            the total amount that is to be worked.
     */
    fun beginJob(sectionName: String?, totalWork: Int)

    /**
     * Start the task using timings from a prior run as a guess for the current
     * run. If there are no predictions then progress is faked.
     * 
     * @param sectionName
     *            the initial name of the job.
     * @param predictURI
     *            the URI of a properties file from which past behavior is read
     *            and the results of the current run are stored.
     */
    fun beginJob(sectionName: String?, predictURI: URI?)

    /**
     * @return the job name
     */
    val jobName: String

    /**
     * @return the unique ID of the job
     */
    val jobID: String

    /**
     * Gets the current ProgressMode. Builders of progress bars should use
     * an indeterminant progress bar for ProgressMode.UNKNOWN and ProgressMode.PREDICTIVE.
     * @return the current progress mode.
     */
    val progressMode: ProgressMode

    /**
     * The total amount of work to be done, or UNKNOWN if it not known.
     *
     * Setting it changes the total amount of work to be done. This can be called any time. It
     * is the responsibility of the caller for it to be meaningful. It is
     * ignored when the task is started with a prediction properties file.
     * The value is in units that make sense to the caller.
     */
    var totalWork: Int

    /**
     * The computed percentage as an integer, typically from 0 to 100, i.e. the amount of work done as a percentage.
     *
     * Setting it indicates progress toward 100%. Note this is just a call to setWorkDone.
     */
    var work: Int

    /**
     * The amount of work done so far as reported by the caller.
     *
     * Setting it indicates progress toward the whole. It is up to the caller to give a
     * value that makes sense. When using 100 as a basis, it is typically a
     * number from 0 to 100. In every case, it is a number from 0 to totalWork.
     */
    var workDone: Int

    /**
     * Indicate progress toward the whole. It is up to the caller to give a
     * value that makes sense.
     * 
     * @param step
     *            the amount of work done since the last call.
     */
    fun incrementWorkDone(step: Int)

    /**
     * The section name is used in reporting progress.
     *
     * Setting it means we have moved onto another section so update the section title.
     * The section name is used in reporting progress.
     */
    var sectionName: String?

    /**
     * Called to indicate that we are finished doing work.
     */
    fun done()

    /**
     * Cancel the job (if possible). If isCancelable() is false, then the job
     * will be canceled if cancelable becomes true. There is no guarantee that
     * 
     */
    fun cancel()

    /**
     * Used to determine whether job is done or canceled or reached totalWork.
     * 
     * @return true if finished
     */
    val isFinished: Boolean

    /**
     * Might the job be cancelable? True if the job can be cancelled.
     *
     * Setting it indicates whether the job is cancelable or not.
     */
    var isCancelable: Boolean

    var isNotifyUser: Boolean

    companion object {
        /**
         * Indicate that the total amount of work is unknown.
         */
        const val UNKNOWN: Int = -1
        const val INSTALL_BOOK: String = "INSTALL_BOOK-%s"
        const val RELOAD_BOOK_LIST: String = "RELOAD_BOOK_LIST"
        const val DOWNLOAD_SEARCH_INDEX: String = "DOWNLOAD_SEARCH_INDEX-%s"
        const val CREATE_INDEX: String = "CREATE_INDEX-%s"
    }
}
