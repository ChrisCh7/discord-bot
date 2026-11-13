package com.chrisch.discordbot.command

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import reactor.netty.http.server.HttpServer
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnshortenLinkTest {

    @Test
    fun `public addresses are allowed`() {
        listOf("8.8.8.8", "100.63.255.255", "100.128.0.1", "2001:4860:4860::8888", "fe00::1").forEach {
            assertFalse(UnshortenLink().isNonPublic(InetAddress.getByName(it)), it)
        }
    }

    @Test
    fun `non-public addresses are blocked`() {
        listOf(
            "127.0.0.1", "10.0.0.1", "192.168.1.1", "169.254.169.254", "0.0.0.0",
            "100.64.0.1", "100.127.255.255", "::1", "fe80::1", "fc00::1", "fdff::1"
        ).forEach {
            assertTrue(UnshortenLink().isNonPublic(InetAddress.getByName(it)), it)
        }
    }

    @Test
    fun `request to a loopback address is blocked before it is sent`() {
        val requestReceived = AtomicBoolean(false)
        val server = HttpServer.create().host("127.0.0.1").port(0)
            .handle { _, res -> requestReceived.set(true); res.send() }
            .bindNow()

        try {
            val history = runBlocking { UnshortenLink().followRedirects(URI("http://127.0.0.1:${server.port()}/")) }

            assertNull(history)
            assertFalse(requestReceived.get())
        } finally {
            server.disposeNow()
        }
    }
}
