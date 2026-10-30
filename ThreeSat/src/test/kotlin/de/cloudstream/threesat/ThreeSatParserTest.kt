package de.cloudstream.threesat

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class ThreeSatParserTest {
    @Test fun `primary player wins over embedded clips`() {
        val doc = Jsoup.parse("""
            <div data-zdfplayer-jsb='{"content":"/clip.json","apiToken":"clip","primary":false}'></div>
            <div data-zdfplayer-jsb='{"content":"/main.json","apiToken":"current","primary":true}'></div>
        """)
        assertEquals(PlayerConfig("https://api.3sat.de/main.json", "current", false), ThreeSatParser.player(doc))
    }

    @Test fun `category with embedded clips is not a video page`() {
        val doc = Jsoup.parse("""<div data-zdfplayer-jsb='{"content":"/clip.json","apiToken":"clip","primary":false}'></div>""")
        assertNull(ThreeSatParser.player(doc))
    }

    @Test fun `recommendations never supply the main stream`() {
        val content = ThreeSatParser.json("""{
          "recommendations":{"http://zdf.de/rels/streams/ptmd-template":"/wrong/{playerId}"},
          "mainVideoContent":{"http://zdf.de/rels/target":{
            "http://zdf.de/rels/streams/ptmd-template":"/tmd/2/{playerId}/vod/ptmd/3sat/main/1"
          }}
        }""")
        assertEquals("https://api.3sat.de/tmd/2/ngplayer_2_4/vod/ptmd/3sat/main/1", ThreeSatParser.ptmdUrl(content))
        assertNull(ThreeSatParser.ptmdUrl(ThreeSatParser.json("""{"recommendations":{"http://zdf.de/rels/streams/ptmd-template":"/wrong"}}""")))
    }

    @Test fun `relative urls are resolved and token destinations restricted`() {
        assertEquals("https://www.3sat.de/wissen/test.html", ThreeSatParser.websiteUrl("/wissen/test.html#video"))
        assertEquals("https://api.3sat.de/tmd/test", ThreeSatParser.apiUrl("/tmd/test"))
        for (url in listOf("https://evil.example/x", "https://api.3sat.de.evil.example/x", "http://api.3sat.de/x", "https://user@api.3sat.de/x")) {
            assertNull(ThreeSatParser.apiUrl(url))
        }
    }

    @Test fun `teasers preserve umlauts lazy images and collection type`() {
        val doc = Jsoup.parse("""
            <article class="js-teaser"><img src="data:image/gif;base64,x" data-src="/assets/forest~768x432">
              <a class="teaser-title-link" href="/wissen/wald.html" title="Wälder &amp; Flüsse"></a>
              <button data-playlist-toggle-external-id="SCMS_123"></button></article>
            <article class="js-teaser"><a class="teaser-title-link" href="/wissen/wald.html" title="duplicate"></a></article>
            <article class="js-teaser"><a class="teaser-title-link" href="/wissen" title="Wissen"></a></article>
            <article class="js-teaser"><a class="teaser-title-link" href="https://elsewhere.example/" title="external"></a></article>
        """)
        val teasers = ThreeSatParser.teasers(doc)
        assertEquals(2, teasers.size)
        assertEquals("Wälder & Flüsse", teasers[0].title)
        assertEquals("https://www.3sat.de/assets/forest~768x432", teasers[0].image)
        assertTrue(teasers[0].isVideo)
        assertFalse(teasers[1].isVideo)
    }

    @Test fun `ptmd exposes clear streams audio variants and captions without duplicates`() {
        val ptmd = ThreeSatParser.json(javaClass.getResource("/ptmd.json")!!.readText())
        val streams = ThreeSatParser.streams(ptmd)
        assertEquals(3, streams.size)
        assertEquals(setOf("hls", "dash", "video"), streams.map { it.format }.toSet())
        assertEquals("ad", streams.last().audioClass)
        assertEquals(720, streams.first { it.format == "video" }.height)
        assertTrue(streams.none { "protected" in it.url })
        assertEquals(listOf(Caption("https://cdn.example/subs.vtt", "deu")), ThreeSatParser.captions(ptmd))
    }
}
