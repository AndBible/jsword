package org.crosswire.common.progress

import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** Legacy Thread characterization; stays JVM through Tasks 16-17. */
class JobCancelTest {
    private class CancellationFixture {
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
        var job: Progress? = null
        var listener: WorkListener? = null

        fun run(readiness: () -> Unit = { assertTrue(waiting.await(2, TimeUnit.SECONDS)) }, scenario: (Progress) -> Unit) {
            try {
                worker.start()
                readiness()
                val created = JobManagerJvm.createJob("cancel-test", "cancel-test", worker)
                job = created
                scenario(created)
            } finally { cleanup() }
        }

        fun listen(value: WorkListener) {
            listener = value
            JobManager.addWorkListener(value)
        }

        fun cleanup() {
            listener?.let { JobManager.removeWorkListener(it) }
            release.countDown()
            worker.interrupt()
            worker.join(2000)
            job?.done()
        }
    }

    @Test fun cancellationFinishesAndNotifiesBeforeInterruptEvenWhenDisabled() {
        val fixture = CancellationFixture()
        fixture.run { job ->
            val events = mutableListOf<String>()
            val listener = object : WorkListener {
                override fun workStateChanged(ev: WorkEvent) { events.add("state") }
                override fun workProgressed(ev: WorkEvent) {
                    if (ev.job === job) {
                        assertTrue(job.isFinished)
                        assertEquals(100, job.work)
                        assertEquals(0, fixture.interrupts)
                        assertSame(job, JobManager.findJob(job.jobID))
                        events.add("done")
                    }
                }
            }
            assertTrue(job.isCancelable)
            job.isCancelable = false
            fixture.listen(listener)
            job.cancel()
            assertTrue(fixture.stopped.await(2, TimeUnit.SECONDS))
            job.cancel()
            job.isCancelable = true
            assertFalse(job.isCancelable)
            assertEquals(1, fixture.interrupts)
            assertEquals(listOf("done"), events)
            assertNull(JobManager.findJob(job.jobID))
        }
    }

    @Test fun readinessFailureStillReleasesAndJoinsWorker() {
        val fixture = CancellationFixture()
        val error = IllegalStateException("readiness")
        try {
            assertSame(error, assertFailsWith<IllegalStateException> {
                fixture.run(readiness = {
                    assertTrue(fixture.waiting.await(2, TimeUnit.SECONDS))
                    throw error
                }) { fail("scenario must not run after readiness failure") }
            })
            assertFalse(fixture.worker.isDaemon)
            assertFalse(fixture.worker.isAlive, "shared fixture must clean readiness failure")
            assertNull(fixture.job)
            assertNull(fixture.listener)
        } finally {
            // Independent test rescue runs only AFTER observing a leak in the mutated subject.
            fixture.cleanup()
        }
    }

    @Test fun partialSetupFailureRemovesCreatedJobAndListener() {
        val fixture = CancellationFixture()
        val error = IllegalStateException("partial setup")
        var callbacks = 0
        try {
            assertSame(error, assertFailsWith<IllegalStateException> {
                fixture.run { _ ->
                    fixture.listen(object : WorkListener {
                        override fun workStateChanged(ev: WorkEvent) = Unit
                        override fun workProgressed(ev: WorkEvent) { callbacks++ }
                    })
                    throw error
                }
            })
            assertFalse(fixture.worker.isAlive)
            val job = fixture.job!!
            assertTrue(job.isFinished)
            assertNull(JobManager.findJob(job.jobID))
            assertEquals(0, callbacks, "listener must be removed before cleanup completion event")
            val probe = JobManager.createJob("cleanup-listener-probe")
            try { probe.done(); assertEquals(0, callbacks, "removed listener must not receive later events") }
            finally { probe.done() }
        } finally { fixture.cleanup() }
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
