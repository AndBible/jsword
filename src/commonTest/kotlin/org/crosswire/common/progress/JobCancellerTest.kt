package org.crosswire.common.progress

import kotlin.test.*

/** Moved from jvmTest with Job; original assertions and call counts unchanged. */
class JobCancellerTest {
    @Test fun hostCancellerRunsAfterCompletionAndOnlyOnceSequentially() {
        var calls = 0
        lateinit var job: Progress
        job = JobManager.createJob("host-cancel", "host-cancel") {
            assertTrue(job.isFinished)
            assertEquals(100, job.work)
            assertNull(JobManager.findJob(job.jobID))
            calls++
            Unit
        }
        try {
            assertTrue(job.isCancelable)
            job.isCancelable = false
            job.cancel(); job.cancel()
            assertEquals(1, calls)
        } finally { job.done() }
    }
}
