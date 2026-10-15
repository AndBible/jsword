package org.crosswire.common.progress

/**
 * Cancellation callback for a job that runs on the calling thread. TEMPORARY seam (K5b Task 13), removed by
 * K5b Task 17, where the suspend install makes coroutine cancellation cancel the job.
 *
 * JVM: interrupts the thread that called this function (what `JobManagerJvm.createJob(..., Thread.currentThread())`
 * did). iOS: null, so the job is not cancelable; nothing calls installers on iOS before M4.
 */
internal expect fun currentThreadCanceller(): (() -> Unit)?
