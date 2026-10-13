package org.crosswire.ksword.parity

import java.io.*
import org.crosswire.common.progress.JobManager
import org.crosswire.common.progress.WorkEvent
import org.crosswire.common.util.ReporterEvent
import org.junit.Test
import kotlin.test.*

class ProgressEventSerializationParityTest {
    private fun bytes(value: Any): ByteArray = ByteArrayOutputStream().also { out ->
        ObjectOutputStream(out).use { it.writeObject(value) }
    }.toByteArray()

    private fun read(data: ByteArray, loader: ClassLoader): Any =
        object : ObjectInputStream(ByteArrayInputStream(data)) {
            override fun resolveClass(desc: ObjectStreamClass): Class<*> = Class.forName(desc.name, false, loader)
        }.use { it.readObject() }

    private fun verify(old: Any, current: Any) {
        val oldBytes = bytes(old)
        val currentBytes = bytes(current)
        assertContentEquals(oldBytes, currentBytes)
        val oldClass = old.javaClass
        val newClass = current.javaClass
        assertEquals(oldClass.superclass.name, newClass.superclass.name)
        val oldDesc = ObjectStreamClass.lookup(oldClass)
        val newDesc = ObjectStreamClass.lookup(newClass)
        assertEquals(oldDesc.serialVersionUID, newDesc.serialVersionUID)
        assertEquals(oldDesc.fields.map { it.name to it.type.name }, newDesc.fields.map { it.name to it.type.name })
        for ((data, loader) in listOf(oldBytes to newClass.classLoader, currentBytes to Oracle.loader)) {
            val restored = read(data, loader)
            assertNull(restored.javaClass.getMethod("getSource").invoke(restored))
        }
    }

    @Test fun nonSerializableReporterSourceIsNotWrittenAndStreamsMatchBothWays() {
        val oldClass = Oracle.loader.loadClass("org.crosswire.common.util.ReporterEvent")
        val source = Any()
        val old = oldClass.getConstructor(Any::class.java, String::class.java).newInstance(source, "message")
        verify(old, ReporterEvent(source, "message"))
    }

    @Test fun realNonSerializableJobSourceIsNotWrittenAndStreamsMatchBothWays() {
        val oldManager = Oracle.loader.loadClass("org.crosswire.common.progress.JobManager")
        val oldJob = oldManager.getMethod("createJob", String::class.java).invoke(null, "serial-job")
        val job = JobManager.createJob("serial-job")
        try {
            val oldProgress = Oracle.loader.loadClass("org.crosswire.common.progress.Progress")
            val old = Oracle.loader.loadClass("org.crosswire.common.progress.WorkEvent")
                .getConstructor(oldProgress).newInstance(oldJob)
            verify(old, WorkEvent(job))
        } finally {
            job.done()
            oldJob.javaClass.getMethod("done").invoke(oldJob)
        }
    }
}
