package de.cloudstream.threesat

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ThreeSatProvider : MainAPI() {
    override var mainUrl = ThreeSatParser.MAIN
    override var name = "3sat Mediathek"
    override var lang = "de"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Live)

    override val mainPage = mainPageOf(
        mainPage("$mainUrl/", "Empfehlungen", true),
        mainPage("$mainUrl/dokumentation", "Dokumentationen", true),
        mainPage("$mainUrl/wissen", "Wissen", true),
        mainPage("$mainUrl/kultur", "Kultur", true),
        mainPage("$mainUrl/kabarett", "Kabarett", true),
        mainPage("live", "Live", true)
    )

    private suspend fun document(url: String): Document {
        val safeUrl = ThreeSatParser.websiteUrl(url)
            ?: throw ErrorLoadingException("Ungültige 3sat-Adresse")
        return Jsoup.parse(app.get(safeUrl).text, safeUrl)
    }

    private fun Teaser.searchResponse(): SearchResponse = if (isVideo) {
        newMovieSearchResponse(title, url) { posterUrl = image }
    } else {
        newTvSeriesSearchResponse(title, url) { posterUrl = image }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        if (page > 1) return newHomePageResponse(request, emptyList(), false)
        val items = if (request.data == "live") {
            listOf(newLiveSearchResponse("3sat Livestream", ThreeSatParser.LIVE) {
                posterUrl = "$mainUrl/assets/3sat-logo-rot-100~768x432"
            })
        } else {
            ThreeSatParser.teasers(document(request.data)).map { it.searchResponse() }
        }
        return newHomePageResponse(request, items, false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return search(query, 1).items
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        if (query.isBlank() || page < 1) return newSearchResponseList(emptyList(), false)
        val url = "$mainUrl/suche?q=" + java.net.URLEncoder.encode(query, "UTF-8") + "&page=$page"
        val doc = document(url)
        val items = ThreeSatParser.teasers(doc).map { it.searchResponse() }
        return newSearchResponseList(items, items.isNotEmpty() && doc.selectFirst("a.loadmore-btn[href]") != null)
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = document(url)
        val player = ThreeSatParser.player(doc)
        val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("h1")?.text() ?: "3sat"
        val image = doc.selectFirst("meta[property=og:image]")?.attr("content")
        val description = doc.selectFirst("meta[name=description]")?.attr("content")

        if (player != null) {
            val content = ThreeSatParser.json(app.get(player.content, headers = auth(player)).text)
            val videoTitle = content.path("title").asText().ifBlank { title }
            return newMovieLoadResponse(
                videoTitle, url, if (player.live) TvType.Live else TvType.Movie, url
            ) {
                posterUrl = image
                plot = content.path("leadParagraph").asText().ifBlank { description.orEmpty() }
                duration = ThreeSatParser.mainVideo(content).path("duration").asInt(0)
                    .takeIf { it > 0 }?.let { (it + 59) / 60 }
            }
        }

        val episodes = ThreeSatParser.teasers(doc).filter { it.isVideo && it.url != url }.map { teaser ->
            newEpisode(teaser.url) {
                name = teaser.title
                posterUrl = teaser.image
            }
        }
        if (episodes.isEmpty()) throw ErrorLoadingException("Auf dieser Seite sind keine verfügbaren Videos vorhanden.")
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            posterUrl = image
            plot = description
        }
    }

    private fun auth(player: PlayerConfig) = mapOf(
        "Api-Auth" to "Bearer ${player.token}",
        "Referer" to "$mainUrl/"
    )

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // Store the page URL, not the token or expiring CDN links, in Cloudstream.
        val player = ThreeSatParser.player(document(data))
            ?: throw ErrorLoadingException("Für diesen Eintrag wurde kein 3sat-Player gefunden.")
        val content = ThreeSatParser.json(app.get(player.content, headers = auth(player)).text)
        val ptmdUrl = ThreeSatParser.ptmdUrl(content)
            ?: throw ErrorLoadingException("3sat stellt für dieses Video keine Stream-Metadaten bereit.")
        val ptmd = ThreeSatParser.json(app.get(ptmdUrl, headers = auth(player)).text)
        val streams = ThreeSatParser.streams(ptmd)
        if (streams.isEmpty()) {
            throw ErrorLoadingException("Kein frei abspielbarer Stream verfügbar. Bitte Verfügbarkeit, Region und Sendezeit prüfen.")
        }
        ThreeSatParser.captions(ptmd).forEach { caption ->
            subtitleCallback(newSubtitleFile(languageName(caption.language), caption.url))
        }
        for (stream in streams) {
            val audio = when (stream.audioClass) {
                "ad" -> "Audiodeskription"
                "ot" -> "Originalton"
                else -> languageName(stream.language)
            }
            val type = when (stream.format) {
                "hls" -> ExtractorLinkType.M3U8
                "dash" -> ExtractorLinkType.DASH
                else -> ExtractorLinkType.VIDEO
            }
            callback(newExtractorLink(
                name, "3sat · $audio · ${stream.format.uppercase()} · ${stream.quality}", stream.url, type
            ) {
                referer = "$mainUrl/"
                quality = stream.height ?: Qualities.Unknown.value
            })
        }
        return true
    }

    private fun languageName(code: String) = when (code.lowercase()) {
        "de", "deu", "ger" -> "Deutsch"
        "en", "eng" -> "Englisch"
        "fr", "fra", "fre" -> "Französisch"
        else -> code
    }
}
