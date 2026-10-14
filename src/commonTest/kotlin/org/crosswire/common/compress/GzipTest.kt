package org.crosswire.common.compress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GzipTest {
    @Test fun testCompression() {
        val compressed = Gzip(compressionTestCorpus()).compress()
        val result = Gzip(compressed).uncompress().decodeToString()
        assertTrue(Regex("^          \r?\nThe First Book of Moses, called Genesis").containsMatchIn(result), "round trip ZIP uncompression")
    }
}
