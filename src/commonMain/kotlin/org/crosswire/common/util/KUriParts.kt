package org.crosswire.common.util

/** URI syntax and quoting shared by the native seam and its JVM differential tests. */
internal object KUriParts {
    data class Parts(val scheme: String?, val rawSsp: String, val rawAuthority: String?, val rawUserInfo: String?, val host: String?, val port: Int, val rawPath: String?, val rawQuery: String?, val rawFragment: String?)

    private val syntax = Regex("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?$")
    private val schemeSyntax = Regex("[A-Za-z][A-Za-z0-9+.-]*")
    private val hex = "0123456789ABCDEF"

    fun parse(s: String): Parts? {
        if (!validCharacters(s)) return null
        val m = syntax.matchEntire(s) ?: return null
        val scheme = m.groups[2]?.value
        if (scheme != null && !schemeSyntax.matches(scheme)) return null
        val fragment = m.groups[9]?.value
        if (fragment?.contains('#') == true) return null
        val ssp = s.substring(if (scheme == null) 0 else scheme.length + 1).substringBefore('#')
        if (scheme != null && ssp.isEmpty()) return null
        if (scheme != null && !ssp.startsWith('/')) {
            return Parts(scheme, ssp, null, null, null, -1, null, null, fragment)
        }
        val authority = m.groups[4]?.value?.takeIf { it.isNotEmpty() }
        val path = m.groupValues[5]
        if (path.contains('[') || path.contains(']')) return null
        if (scheme == null && m.groups[3] == null && path.substringBefore('/').contains(':')) return null
        if (m.groups[3] != null && authority == null && path.isEmpty() && m.groups[6] == null && fragment == null) return null
        val server = authority?.let { parseServer(it) }
        if (authority != null && (authority.contains('[') || authority.contains(']')) && server == null) return null
        return Parts(scheme, ssp, authority, server?.first, server?.second, server?.third ?: -1, path, m.groups[7]?.value, fragment)
    }

    private fun validCharacters(s: String): Boolean {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c.code <= 32 || c.code == 127 || c in "\"<>\\^`{|}" || c.isWhitespace() || c.code in 128..159) return false
            if (c == '%') {
                if (i + 2 >= s.length || s[i + 1].digitToIntOrNull(16) == null || s[i + 2].digitToIntOrNull(16) == null) return false
                i += 2
            }
            i++
        }
        return true
    }

    private fun parseServer(a: String): Triple<String?, String, Int>? {
        val at = a.lastIndexOf('@')
        val user = if (at < 0) null else a.substring(0, at)
        if (user?.contains('@') == true) return null
        val hp = a.substring(at + 1)
        val host: String
        val portText: String?
        if (hp.startsWith('[')) {
            val end = hp.indexOf(']')
            if (end < 0) return null
            host = hp.substring(0, end + 1)
            val address = host.substring(1, host.length - 1).substringBefore('%')
            if (!validIpv6(address)) return null
            val tail = hp.substring(end + 1)
            if (tail.isNotEmpty() && !tail.startsWith(':')) return null
            portText = if (tail.isEmpty()) null else tail.substring(1)
        } else {
            host = hp.substringBefore(':')
            portText = if (hp.contains(':')) hp.substringAfter(':') else null
            if (host.isEmpty() || !Regex("[A-Za-z0-9.-]+").matches(host)) return null
            val labels = host.removeSuffix(".").split('.')
            if (labels.any { it.isEmpty() || it.startsWith('-') || it.endsWith('-') }) return null
            if (labels.size > 1 && labels.last().first().isDigit()) {
                if (labels.size != 4 || labels.any { it.toIntOrNull() !in 0..255 }) return null
            }
        }
        val port = if (portText.isNullOrEmpty()) -1 else {
            if (!portText.all { it in '0'..'9' }) return null
            portText.toIntOrNull() ?: return null
        }
        return Triple(user, host, port)
    }

    private fun validIpv6(s: String): Boolean {
        if (!s.contains(':') || s.count { it == ':' } > 7 && !s.contains("::")) return false
        if (s.indexOf("::") != s.lastIndexOf("::")) return false
        val groups = s.split(':').filter { it.isNotEmpty() }
        var units = 0
        for (g in groups) {
            if (g.contains('.')) {
                val bytes = g.split('.')
                if (g != groups.last() || bytes.size != 4 || bytes.any { it.toIntOrNull() !in 0..255 }) return false
                units += 2
            } else {
                if (g.length > 4 || g.any { it.digitToIntOrNull(16) == null }) return false
                units++
            }
        }
        return if (s.contains("::")) units < 8 else units == 8 && !s.startsWith(':') && !s.endsWith(':')
    }

    fun quotePath(raw: String): String = quote(raw, ":@/")

    internal fun quote(raw: String, extra: String): String = buildString {
        for (c in raw) {
            val allowed = c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c in "-._~!$&'()*+,;=" || c in extra
            if (allowed || c.code > 127 && !c.isWhitespace() && c.code !in 128..159) append(c)
            else for (b in c.toString().encodeToByteArray()) {
                val n = b.toInt() and 255
                append('%'); append(hex[n shr 4]); append(hex[n and 15])
            }
        }
    }

    fun decode(s: String): String = buildString {
        var i = 0
        while (i < s.length) {
            if (s[i] != '%') { append(s[i++]); continue }
            val bytes = mutableListOf<Byte>()
            while (i + 2 < s.length && s[i] == '%') {
                val a = s[i + 1].digitToIntOrNull(16) ?: break
                val b = s[i + 2].digitToIntOrNull(16) ?: break
                bytes.add(((a shl 4) or b).toByte()); i += 3
            }
            if (bytes.isEmpty()) append(s[i++]) else append(bytes.toByteArray().decodeToString())
        }
    }
}

/** Native URI value; raw escaped components govern equality, not decoded strings. */
internal class KUriModel private constructor(private val text: String, private val parts: KUriParts.Parts) {
    fun getScheme(): String? = parts.scheme
    fun getUserInfo(): String? = parts.rawUserInfo?.let(KUriParts::decode)
    fun getHost(): String? = parts.host
    fun getPort(): Int = parts.port
    fun getPath(): String? = parts.rawPath?.let(KUriParts::decode)
    fun getRawPath(): String? = parts.rawPath
    fun getQuery(): String? = parts.rawQuery?.let(KUriParts::decode)
    fun getFragment(): String? = parts.rawFragment?.let(KUriParts::decode)
    fun isAbsolute(): Boolean = parts.scheme != null
    override fun toString(): String = text

    private fun canonical(s: String?): String? = s?.replace(Regex("%[0-9a-fA-F]{2}")) { it.value.uppercase() }
    private fun identity(): List<Any?> = buildList {
        add(parts.scheme?.lowercase()); add(canonical(parts.rawFragment))
        val opaque = parts.rawPath == null
        add(opaque)
        if (opaque) add(canonical(parts.rawSsp)) else {
            if (parts.host != null) {
                add(canonical(parts.rawUserInfo)); add(parts.host.lowercase()); add(parts.port)
            } else { add(canonical(parts.rawAuthority)); add(null); add(-1) }
            add(canonical(parts.rawPath)); add(canonical(parts.rawQuery))
        }
    }
    override fun equals(other: Any?): Boolean = other is KUriModel && identity() == other.identity()
    override fun hashCode(): Int = identity().hashCode()

    companion object {
        fun parse(s: String): KUriModel = KUriModel(s, KUriParts.parse(s) ?: throw IllegalArgumentException("Illegal character in URI: $s"))
        fun build(scheme: String?, host: String?, path: String?, fragment: String?): KUriModel = build(scheme, null, host, -1, path, null, fragment)
        fun build(scheme: String?, userInfo: String?, host: String?, port: Int, path: String?, query: String?, fragment: String?): KUriModel {
            require(scheme == null || path.isNullOrEmpty() || path.startsWith('/')) { "Relative path in absolute URI" }
            val text = buildString {
                if (scheme != null) { append(scheme); append(':') }
                if (host != null) {
                    append("//")
                    if (userInfo != null) { append(KUriParts.quote(userInfo, ":")); append('@') }
                    append(if (host.contains(':') && !host.startsWith('[')) "[$host]" else host)
                    if (port != -1) { append(':'); append(port) }
                }
                if (path != null) append(KUriParts.quotePath(path))
                if (query != null) { append('?'); append(KUriParts.quote(query, ":@/?[]")) }
                if (fragment != null) { append('#'); append(KUriParts.quote(fragment, ":@/?[]")) }
            }
            val result = parse(text)
            require(host == null || result.parts.host != null) { "Invalid server authority: $host" }
            return result
        }
    }
}
