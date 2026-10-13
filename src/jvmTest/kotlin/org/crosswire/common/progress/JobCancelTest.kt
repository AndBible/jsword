package org.crosswire.common.progress

import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** Legacy Thread characterization; stays JVM through Tasks 16-17. */
class JobCancelTest {
    @Test fun cancellationFinishesAndNotifiesBeforeInterruptEvenWhenDisabled() {
        val waiting = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        val release = CountDownLatch(1)
        var interrupts = 0
        val worker = object : Thread() {
            override fun run() {
                waiting.countDown()
                try { release.await() } catch (_: InterruptedException) { stopped.countDown() }
            }
            override fun interrupt() { interrupts++; super.interrupt() }
        }
        var createdJob: Progress? = null
        var addedListener: WorkListener? = null
        try {
            worker.start()
            assertTrue(waiting.await(2, TimeUnit.SECONDS))
            val job = JobManagerJvm.createJob("cancel-test", "cancel-test", worker)
            createdJob = job
            val events = mutableListOf<String>()
            val listener = object : WorkListener {
                override fun workStateChanged(ev: WorkEvent) { events.add("state") }
                override fun workProgressed(ev: WorkEvent) {
                    if (ev.job === job) {
                        assertTrue(job.isFinished)
                        assertEquals(100, job.work)
                        assertEquals(0, interrupts)
                        assertSame(job, JobManager.findJob(job.jobID))
                        events.add("done")
                    }
                }
            }
            assertTrue(job.isCancelable)
            job.isCancelable = false
            addedListener = listener
            JobManager.addWorkListener(listener)
            job.cancel()
            assertTrue(stopped.await(2, TimeUnit.SECONDS))
            job.cancel()
            job.isCancelable = true
            assertFalse(job.isCancelable)
            assertEquals(1, interrupts)
            assertEquals(listOf("done"), events)
            assertNull(JobManager.findJob(job.jobID))
        } finally {
            addedListener?.let { JobManager.removeWorkListener(it) }
            release.countDown()
            worker.interrupt(); worker.join(2000)
            createdJob?.done()
        }
    }

    @Test fun readinessFailureStillReleasesAndJoinsWorker() {
        val release = CountDownLatch(1)
        val worker = Thread { try { release.await() } catch (_: InterruptedException) { } }
        val error = IllegalStateException("readiness")
        assertSame(error, assertFailsWith<IllegalStateException> {
            try {
                worker.start()
                throw error
            } finally {
                release.countDown()
                worker.interrupt()
                worker.join(2000)
            }
        })
        assertFalse(worker.isAlive)
    }

    @Test fun listenerFailureLeavesFinishedJobRegisteredAndPreventsInterrupt() {
        var interrupts = 0
        val worker = object : Thread() { override fun interrupt() { interrupts++ } }
        val job = org.crosswire.common.progress.JobManagerJvm.createJob("throw-test", "throw-test", worker)
        val error = IllegalStateException("listener")
        val listener = object : WorkListener {
            override fun workStateChanged(ev: WorkEvent) = Unit
            override fun workProgressed(ev: WorkEvent) { if (ev.job === job) throw error }
        }
        try {
            JobManager.addWorkListener(listener)
            assertSame(error, assertFailsWith<IllegalStateException> { job.cancel() })
            assertTrue(job.isFinished)
            assertSame(job, JobManager.findJob(job.jobID))
            job.cancel()
            assertEquals(0, interrupts)
        } finally { JobManager.removeWorkListener(listener); job.done() }
    }

    @Test fun finishedAndWorkerlessJobsDoNotInterrupt() {
        var interrupts = 0
        val worker = object : Thread() { override fun interrupt() { interrupts++ } }
        val finished = org.crosswire.common.progress.JobManagerJvm.createJob("finished-test", "finished-test", worker)
        val workerless = JobManager.createJob("workerless-test")
        try {
            finished.done(); finished.cancel()
            assertEquals(0, interrupts)
            assertFalse(workerless.isCancelable)
            workerless.isCancelable = true
            assertFalse(workerless.isCancelable)
            workerless.cancel()
            assertTrue(workerless.isFinished)
        } finally { finished.done(); workerless.done() }
    }

    @Test fun managerIteratorIsSnapshotAndCannotRemove() {
        val first = JobManager.createJob("snapshot-first")
        val snapshot = JobManager.iterator()
        val second = JobManager.createJob("snapshot-second")
        try {
            val values = snapshot.asSequence().toList()
            assertTrue(values.contains(first))
            assertFalse(values.contains(second))
            val iterator = JobManager.iterator()
            iterator.next()
            assertFailsWith<UnsupportedOperationException> { iterator.remove() }
        } finally { first.done(); second.done() }
    }
}
