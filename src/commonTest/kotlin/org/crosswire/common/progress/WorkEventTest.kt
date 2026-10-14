package org.crosswire.common.progress

import kotlin.test.Test
import kotlin.test.assertEquals

class WorkEventTest {
    @Test fun testGetJob() {
        val job = JobManager.createJob("wibble")
        try {
            job.beginJob("wibble")
            val event = WorkEvent(job)
            assertEquals(job, event.job)
            assertEquals(job, event.getSource())
        } finally { job.done() }
    }
}
