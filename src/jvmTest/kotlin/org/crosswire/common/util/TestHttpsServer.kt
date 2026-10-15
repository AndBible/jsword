package org.crosswire.common.util

import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Files
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/** Local-only TLS fixture; no production trust settings are changed permanently. */
internal class TestHttpsServer : AutoCloseable {
    private val workers = Executors.newCachedThreadPool()
    private val server = HttpsServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val stalled = CountDownLatch(1)
    private val release = CountDownLatch(1)
    val heads = AtomicInteger()

    init {
        server.httpsConfigurator = HttpsConfigurator(serverContext)
        server.executor = workers
        server.createContext("/") { exchange ->
            try {
                val head = exchange.requestMethod == "HEAD"
                if (head) heads.incrementAndGet()
                when (exchange.requestURI.path) {
                    "/drop" -> Unit
                    "/missing" -> exchange.sendResponseHeaders(404, -1)
                    "/redirect" -> {
                        exchange.responseHeaders.set("Location", "/ok")
                        exchange.sendResponseHeaders(302, -1)
                    }
                    "/lm" -> {
                        exchange.responseHeaders.set("Last-Modified", "Tue, 15 Nov 1994 08:12:31 GMT")
                        exchange.sendResponseHeaders(200, -1)
                    }
                    "/stall" -> {
                        exchange.sendResponseHeaders(200, 7)
                        exchange.responseBody.write("k4-".toByteArray())
                        exchange.responseBody.flush()
                        stalled.countDown()
                        release.await(5, TimeUnit.SECONDS)
                    }
                    else -> {
                        if (head || exchange.requestURI.path != "/chunked") {
                            exchange.responseHeaders.set("Content-Length", "7")
                        }
                        exchange.sendResponseHeaders(200, if (head) -1 else if (exchange.requestURI.path == "/chunked") 0 else 7)
                        if (!head) exchange.responseBody.write("k4-body".toByteArray())
                    }
                }
            } finally {
                exchange.close()
            }
        }
        server.start()
    }

    fun uri(path: String): URI = URI("https://127.0.0.1:${server.address.port}$path")

    override fun close() {
        release.countDown()
        server.stop(0)
        workers.shutdownNow()
        check(workers.awaitTermination(10, TimeUnit.SECONDS))
    }

    companion object {
        private val serverContext: SSLContext by lazy {
            val directory = Files.createTempDirectory("k4-tls")
            try {
                val store = directory.resolve("k4.p12")
                val keytool = java.nio.file.Path.of(System.getProperty("java.home"), "bin", "keytool")
                val process = ProcessBuilder(keytool.toString(), "-genkeypair", "-alias", "k4", "-keyalg", "RSA", "-keysize", "2048", "-dname", "CN=127.0.0.1", "-ext", "SAN=ip:127.0.0.1", "-validity", "2", "-storetype", "PKCS12", "-keystore", store.toString(), "-storepass", "k4pass", "-keypass", "k4pass").redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                check(process.waitFor() == 0) { output }
                val keys = KeyStore.getInstance("PKCS12")
                Files.newInputStream(store).use { keys.load(it, "k4pass".toCharArray()) }
                val managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                managers.init(keys, "k4pass".toCharArray())
                SSLContext.getInstance("TLS").apply { init(managers.keyManagers, null, SecureRandom()) }
            } finally {
                Files.deleteIfExists(directory.resolve("k4.p12"))
                Files.delete(directory)
            }
        }

        inline fun <T> withTrustAll(block: () -> T): T {
            val factory = HttpsURLConnection.getDefaultSSLSocketFactory()
            val verifier = HttpsURLConnection.getDefaultHostnameVerifier()
            val trust = object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            }
            val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trust), SecureRandom()) }
            try {
                HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory)
                HttpsURLConnection.setDefaultHostnameVerifier(HostnameVerifier { host, _ -> host == "127.0.0.1" })
                return block()
            } finally {
                HttpsURLConnection.setDefaultSSLSocketFactory(factory)
                HttpsURLConnection.setDefaultHostnameVerifier(verifier)
            }
        }
    }
}
