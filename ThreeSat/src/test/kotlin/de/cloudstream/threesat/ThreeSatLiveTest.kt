package de.cloudstream.threesat

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI

/** Optional network test: THREESAT_LIVE_TEST=1 ./gradlew :ThreeSat:testDebugUnitTest --rerun-tasks */
class ThreeSatLiveTest {
    private fun get(url: String, token: String? = null): String {
        val proxyUrl = System.getenv("HTTPS_PROXY") ?: System.getenv("https_proxy")
        val proxy = proxyUrl?.let { URI(it) }?.let { Proxy(Proxy.Type.HTTP, InetSocketAddress(it.host, it.port)) }
            ?: Proxy.NO_PROXY
        val connection = URI(url).toURL().openConnection(proxy) as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 20_000
        if (token != null) connection.setRequestProperty("Api-Auth", "Bearer $token")
        connection.setRequestProperty("Referer", ThreeSatParser.MAIN + "/")
        return try {
            assertEquals("HTTP status for $url", 200, connection.responseCode)
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally { connection.disconnect() }
    }

    @Test fun `current website search vod and live resolve through real api`() {
        assumeTrue(System.getenv("THREESAT_LIVE_TEST") == "1")
        val search = ThreeSatParser.teasers(Jsoup.parse(get(ThreeSatParser.MAIN + "/suche?q=natur")))
        assertTrue(search.any { it.isVideo })
        val videoUrl = search.first { it.isVideo }.url
        for (url in listOf(videoUrl, ThreeSatParser.LIVE)) {
            val player = ThreeSatParser.player(Jsoup.parse(get(url)))
            assertNotNull("Primary player on $url", player)
            player!!
            assertEquals(url == ThreeSatParser.LIVE, player.live)
            val content = ThreeSatParser.json(get(player.content, player.token))
            val ptmdUrl = ThreeSatParser.ptmdUrl(content)
            assertNotNull("Main video PTMD on $url", ptmdUrl)
            val streams = ThreeSatParser.streams(ThreeSatParser.json(get(ptmdUrl!!, player.token)))
            assertTrue("Playable streams on $url", streams.isNotEmpty())
            // A region block can prevent fetching the CDN from CI. Resolve and
            // parse the official API here; playback is verified in Cloudstream.
            println("3sat live check: ${if (player.live) "LIVE" else "VOD"}, ${streams.size} streams")
        }
    }
}
