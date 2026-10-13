package org.crosswire.common.progress

/** JVM thread boundary; common callers supply a host cancellation callback instead. */
object JobManagerJvm {
    @JvmStatic
    fun createJob(jobID: String, jobName: String, workerThread: Thread?): Progress =
        JobManager.createJob(jobID, jobName, workerThread?.let { thread -> { thread.interrupt() } })
}
