package org.crosswire.common.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.util.Timer

class JobRaceTest {
    @Test
    fun aTickAfterDoneDoesNotUndoCompletion() {
        val predictions = Files.createTempFile("job-race", ".properties")
        val job = JobManager.createJob("k4-race") as Job
        try {
            // Keep a real timer tick from interleaving with the deliberately late tick/assertions.
            synchronized(job) {
                try {
                    job.beginJob("k4-race", predictions.toUri())
                    val timerField = Job::class.java.getDeclaredField("fakingTimer")
                    timerField.isAccessible = true
                    (timerField.get(job) as Timer).cancel()
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
