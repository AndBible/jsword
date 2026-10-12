package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals

class KUriDifferentialTest {
    private val parsed = listOf(
        "file:/sdcard/My%20Bibles/%C3%A4/mods.d", "file:///home/u/.sword/", "file:/C:/Users/x/AppData/Roaming/JSword",
        "https://www.crosswire.org/ftpmirror/pub/sword/raw/mods.d.tar.gz", "https://user@h.org:8443/p?q=1#f",
        "http://h.org/a%2Bb+c", "mods.d/kjv.conf", "jar:file:/a.jar!/x", "file:/a/b/../c", "https://h.org",
        "jar:file:/a.jar!/x?version=1#entry", "mailto:user@example.org", "urn:example:animal:ferret:nose",
        "", "#fragment", "?query", "//h.org/path", "file:/", "http://h.org/?#",
        "http://user%20name@h.org:80/a?q=a%2Bb+c#f%20g", "http://[::1]:8080/x",
        "http://registry_name/x", "http://h.org:abc/x", "http://h.org:/x",
        "file:/%C3%28", "file:/%FF", "file:/ä", "http://h.org/a%2fb",
    )

    @Test fun parsedMembersMatchJava() {
        for (s in parsed) assertMembers(java.net.URI(s), KUriModel.parse(s), s)
    }

    @Test fun builtFromComponentsMatchesJava() {
        val paths = listOf("/sdcard/My Bibles/ä/mods.d", "/a#b", "/a?b", "/100%", "/x+y", "/tab\there")
        for (p in paths) {
            assertMembers(java.net.URI("file", null, p, null), KUriModel.build("file", null, p, null), p)
            assertMembers(java.net.URI("https", null, "h.org", -1, p, null, null),
                KUriModel.build("https", null, "h.org", -1, p, null, null), p)
        }
    }

    @Test fun componentQuotingMatchesJava() {
        val cases = listOf(
            Components("https", "user name%:@", "h.org", 8443, "/a b%?#[]", "q=a b%#[]?/", "f a%#[]?/"),
            Components("http", null, "::1", 80, "/x", "", ""),
            Components(null, null, null, -1, "mods.d/a b.conf", null, null),
            Components("file", null, null, -1, "/ä", null, "ä"),
            Components("http", "name", "h.org", -1, null, "q", "f"),
        )
        for (c in cases) with(c) {
            assertMembers(java.net.URI(scheme, userInfo, host, port, path, query, fragment),
                KUriModel.build(scheme, userInfo, host, port, path, query, fragment), c.toString())
        }
    }

    @Test fun rejectsWhatJavaRejects() {
        for (s in listOf("file:/a b", "http://h.org/%zz", "http://h.org/a|b", "http://h.org/{x}",
            "file:/%", "file:/%0", "1bad:/x", ":x", "http:", "http://", "http://[broken]/x",
            "file:/a\\b", "file:/a\u0000b", "file:/a\u007Fb", "file:/a[b", "http://h.org/#a#b")) {
            val javaRejects = runCatching { java.net.URI(s) }.isFailure
            val kRejects = runCatching { KUriModel.parse(s) }.isFailure
            assertEquals(javaRejects, kRejects, s)
        }
    }

    @Test fun componentConstructionRejectionsMatchJava() {
        for (c in listOf(
            Components("file", null, null, -1, "relative", null, null),
            Components("1bad", null, "h.org", -1, "/x", null, null),
            Components("http", null, "bad host", -1, "/x", null, null),
            Components("http", null, "h.org", -2, "/x", null, null),
        )) with(c) {
            val javaRejects = runCatching { java.net.URI(scheme, userInfo, host, port, path, query, fragment) }.isFailure
            val kRejects = runCatching { KUriModel.build(scheme, userInfo, host, port, path, query, fragment) }.isFailure
            assertEquals(javaRejects, kRejects, c.toString())
        }
    }

    @Test fun asciiSyntaxAndQuotingMatchJava() {
        for (code in 0..127) {
            val c = code.toChar()
            for (s in listOf("file:/a${c}b", "http://h.org/x?q=a${c}b", "http://h.org/x#a${c}b", "urn:a${c}b")) {
                val j = runCatching { java.net.URI(s) }
                val k = runCatching { KUriModel.parse(s) }
                assertEquals(j.isSuccess, k.isSuccess, "character $code in $s")
                if (j.isSuccess) assertMembers(j.getOrThrow(), k.getOrThrow(), "character $code")
            }
            val p = "/a${c}b"
            assertMembers(java.net.URI("file", null, p, null), KUriModel.build("file", null, p, null), "quoted character $code")
        }
    }

    @Test fun unicodeAndUtf8DecodingMatchJava() {
        for (p in listOf("/a b", "/a b", "/a b", "/ä", "/😀")) {
            assertMembers(java.net.URI("file", null, p, null), KUriModel.build("file", null, p, null), p)
            assertEquals(runCatching { java.net.URI("file:$p") }.isSuccess,
                runCatching { KUriModel.parse("file:$p") }.isSuccess, p)
        }
        for (p in listOf("/%E0%80%80", "/%ED%A0%80", "/%F0%80%80%80", "/%E2%82", "/%F0%9F%98%80", "/%C3ä%A4")) {
            assertMembers(java.net.URI("file:$p"), KUriModel.parse("file:$p"), p)
        }
    }

    @Test fun serverAuthorityMatchesJava() {
        for (authority in listOf("localhost", "123", "1host", "a.123", "127.0.0.1", "999.0.0.1", "h.org.",
            "-h.org", "h-.org", "h..org", "user@@h.org", "user:pass@h.org", "h.org:2147483648", "[::]",
            "[::1]", "[2001:db8::1]", "[1:2:3:4:5:6:7:8]", "[:::1]", "[::ffff:192.0.2.1]")) {
            val s = "http://$authority/x"
            val j = runCatching { java.net.URI(s) }
            val k = runCatching { KUriModel.parse(s) }
            assertEquals(j.isSuccess, k.isSuccess, s)
            if (j.isSuccess) assertMembers(j.getOrThrow(), k.getOrThrow(), s)
        }
    }

    @Test fun equalityLikeJava() {
        val pairs = listOf(
            "HTTPS://H.ORG/a" to "https://h.org/a", "https://h.org/a" to "https://h.org/A", "file:/a%20b" to "file:/a%20b",
            "file:/a%2fb" to "file:/a%2Fb", "file:/a%41" to "file:/aA", "file:/x" to "file:///x",
            "jar:file:/a.jar!/x" to "JAR:file:/a.jar!/x", "jar:file:/a.jar!/x%2f" to "jar:file:/a.jar!/x%2F",
            "http://user%2f@h.org/p?q=%2f#%2f" to "HTTP://user%2F@H.ORG/p?q=%2F#%2F",
            "http://registry_name/x" to "http://REGISTRY_NAME/x", "http://h.org" to "http://h.org/",
            "http://h.org:80/x" to "http://h.org/x", "http://h.org/x?" to "http://h.org/x",
            "http://h.org/x#" to "http://h.org/x", "file:/a/../b" to "file:/b",
        )
        for ((a, b) in pairs) {
            val j1 = java.net.URI(a)
            val j2 = java.net.URI(b)
            val k1 = KUriModel.parse(a)
            val k2 = KUriModel.parse(b)
            assertEquals(j1 == j2, k1 == k2, "$a vs $b")
            if (j1 == j2) assertEquals(k1.hashCode(), k2.hashCode(), "$a vs $b hash contract")
        }
    }

    private fun assertMembers(j: java.net.URI, k: KUriModel, message: String) {
        assertEquals(j.scheme, k.getScheme(), "$message scheme")
        assertEquals(j.userInfo, k.getUserInfo(), "$message userInfo")
        assertEquals(j.host, k.getHost(), "$message host")
        assertEquals(j.port, k.getPort(), "$message port")
        assertEquals(j.path, k.getPath(), "$message path")
        assertEquals(j.rawPath, k.getRawPath(), "$message rawPath")
        assertEquals(j.query, k.getQuery(), "$message query")
        assertEquals(j.fragment, k.getFragment(), "$message fragment")
        assertEquals(j.isAbsolute, k.isAbsolute(), "$message absolute")
        assertEquals(j.toString(), k.toString(), "$message text")
    }

    private data class Components(
        val scheme: String?, val userInfo: String?, val host: String?, val port: Int,
        val path: String?, val query: String?, val fragment: String?,
    )
}
