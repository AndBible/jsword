package org.crosswire.ksword.k5b

import org.junit.Assert.assertArrayEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Writes (only with `-Dk5b.fixture.write=true`) and otherwise verifies the committed catalog fixture. */
class K5bCatalogFixtureWriter {
    @Test fun writeFixture() {
        assumeTrue("Set -Dk5b.fixture.write=true", System.getProperty("k5b.fixture.write") == "true")
        val target = File(checkNotNull(System.getProperty("k5b.project.dir")), "src/jvmTest/resources/k5b/mods.d.tar.gz")
        target.parentFile.mkdirs()
        target.writeBytes(K5bCatalogFixture.bytes(K5bCatalogFixture.confDir()))
    }

    @Test fun committedFixtureMatchesGenerator() {
        assumeTrue("Fixture is being written", System.getProperty("k5b.fixture.write") != "true")
        assertArrayEquals(K5bCatalogFixture.bytes(K5bCatalogFixture.confDir()), K5bCatalogFixture.committedBytes())
    }
}
