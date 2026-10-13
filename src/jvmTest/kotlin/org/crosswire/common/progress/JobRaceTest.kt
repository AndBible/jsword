package org.crosswire.common.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import org.crosswire.common.util.Cancellable

class JobRaceTest {
    @Test
    fun aTickAfterDoneDoesNotUndoCompletion() {
        val predictions = Files.createTempFile("job-race", ".properties")
        val job = JobManager.createJob("k4-race") as Job
        try {
            // Keep the real scheduler from interleaving with the deliberately late tick/assertions.
            val lockField = Job::class.java.getDeclaredField("lock")
            lockField.isAccessible = true
            synchronized(lockField.get(job)) {
                try {
                    job.beginJob("k4-race", predictions.toUri())
                    val timerField = Job::class.java.getDeclaredField("fakingTimer")
                    timerField.isAccessible = true
                    (timerField.get(job) as Cancellable).cancel()
                    val startField = Job::class.java.getDeclaredField("startTime")
                    startField.isAccessible = true
                    val tickTime = startField.getLong(job) + job.totalWork / 2

                    job.done()
                    job.predictTick(tickTime)

                    assertEquals(100, job.work)
                    assertTrue(job.isFinished)
                    assertEquals(job.totalWork, job.workDone)
                } finally {
                    job.done()
                }
            }
        } finally {
            Files.deleteIfExists(predictions)
        }
    }
}
