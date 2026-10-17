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

import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JUnit Test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class JobTest {

    companion object {
        private const val WIBBLE = "wibble"
    }

    @Test
    fun testJob() {
        var job: Progress
        val tempfile = File.createTempFile("jobtest", "tmp")
        val uri = tempfile.toURI()

        job = JobManager.createJob(WIBBLE)
        job.beginJob(WIBBLE)

        assertEquals(WIBBLE, job.jobName)
        assertFalse(job.isFinished)
        assertFalse(job.isCancelable)
        assertEquals(WIBBLE, job.sectionName)
        assertEquals(0, job.work)
        job.done()
        assertTrue(job.isFinished)
        assertEquals(100, job.work)
        assertFalse(job.isCancelable)

        job = JobManagerJvm.createJob(UUID.randomUUID().toString(), WIBBLE, Thread.currentThread())
        job.beginJob(WIBBLE)
        assertEquals(WIBBLE, job.jobName)
        assertFalse(job.isFinished)
        assertTrue(job.isCancelable)
        assertEquals(WIBBLE, job.sectionName)
        assertEquals(0, job.work)
        job.done()
        assertTrue(job.isFinished)
        assertEquals(100, job.work)
        // assertEquals(job.isCancelable, false);

        job = JobManager.createJob(WIBBLE)
        job.beginJob(WIBBLE, uri)
        job.totalWork = 100
        assertEquals(WIBBLE, job.jobName)
        assertFalse(job.isFinished)
        assertFalse(job.isCancelable)
        assertEquals(WIBBLE, job.sectionName)
        job.cancel()
        job.done()
        assertTrue(job.isFinished)
        assertEquals(100, job.work)
        // assertFalse(job.isCancelable);

        job = JobManagerJvm.createJob(UUID.randomUUID().toString(), WIBBLE, Thread.currentThread())
        job.beginJob(WIBBLE, uri)
        assertEquals(WIBBLE, job.jobName)
        assertFalse(job.isFinished)
        assertTrue(job.isCancelable)
        assertEquals(WIBBLE, job.sectionName)
        job.done()
        assertTrue(job.isFinished)
        assertEquals(100, job.work)
        // assertFalse(job.isCancelable);
    }
}
