package de.cloudstream.threesat

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jsoup.nodes.Document
import java.net.URI

internal data class Teaser(val title: String, val url: String, val image: String?, val isVideo: Boolean)
internal data class PlayerConfig(val content: String, val token: String, val live: Boolean)
internal data class MediaStream(
    val url: String,
    val format: String,
    val language: String,
    val audioClass: String,
    val quality: String,
    val height: Int?
)
internal data class Caption(val url: String, val language: String)

internal object ThreeSatParser {
    const val MAIN = "https://www.3sat.de"
    const val API = "https://api.3sat.de"
    const val LIVE = "$MAIN/programm?view=live"
    private const val REL = "http://zdf.de/rels/"
    private val mapper = ObjectMapper()

    fun json(text: String): JsonNode = mapper.readTree(text)

    fun websiteUrl(value: String): String? = runCatching {
        if (value.isBlank()) return@runCatching null
        val uri = URI(MAIN).resolve(value.trim())
        if (uri.scheme != "https" || uri.host != "www.3sat.de" || uri.userInfo != null) null
        else uri.toString().substringBefore('#')
    }.getOrNull()

    fun apiUrl(value: String): String? = runCatching {
        if (value.isBlank()) return@runCatching null
        val uri = URI(API).resolve(value.trim())
        if (uri.scheme != "https" || uri.host != "api.3sat.de" || uri.userInfo != null) null
        else uri.toString()
    }.getOrNull()

    fun teasers(document: Document): List<Teaser> = document
        .select("article.js-teaser, article.stage-teaser")
        .mapNotNull { article ->
            val link = article.selectFirst("a.teaser-title-link[href], a.is-clickarea-action[href], a.clickarea-link[href]")
                ?: return@mapNotNull null
            val url = websiteUrl(link.attr("href")) ?: return@mapNotNull null
            val title = link.attr("title").ifBlank {
                link.selectFirst(".js-teaser-title")?.text() ?: link.text()
            }.trim()
            if (title.isBlank()) return@mapNotNull null
            val img = article.selectFirst("img[data-src], img[src]")
            val image = img?.attr("data-src")?.ifBlank { img.attr("src") }
                ?.let(::websiteUrl)
            Teaser(title, url, image, article.selectFirst("[data-playlist-toggle-external-id]") != null)
        }.distinctBy { it.url }

    fun player(document: Document): PlayerConfig? {
        val players = document.select("[data-zdfplayer-jsb]")
            .mapNotNull { element ->
                runCatching { json(element.attr("data-zdfplayer-jsb")) }.getOrNull()
            }
        val config = players.firstOrNull { it.path("primary").asBoolean() }
            ?: players.singleOrNull()?.takeUnless { it.has("primary") } ?: return null
        val content = apiUrl(config.path("content").asText()) ?: return null
        val token = config.path("apiToken").asText().takeIf { it.isNotBlank() } ?: return null
        return PlayerConfig(content, token, config.path("isLivestream").asBoolean())
    }

    // Scope this lookup to the selected video's mainVideoContent. Recommended
    // videos contain the same relation and must never supply the playback URL.
    fun mainVideo(content: JsonNode): JsonNode = content.path("mainVideoContent").path(REL + "target")

    fun ptmdUrl(content: JsonNode): String? {
        val video = mainVideo(content)
        val template = video.path(REL + "streams/ptmd-template").asText().ifBlank {
            video.path("streams").path("default").path(REL + "streams/ptmd-template").asText()
        }.ifBlank { video.path(REL + "streams/ptmd").asText() }
        if (template.isBlank()) return null
        return apiUrl(template.replace("{playerId}", "ngplayer_2_4"))
    }

    private fun https(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme == "https" && uri.host != null && uri.userInfo == null
    }.getOrDefault(false)

    fun streams(ptmd: JsonNode): List<MediaStream> {
        val streams = mutableListOf<MediaStream>()
        for (priority in ptmd.path("priorityList")) {
            for (format in priority.path("formitaeten")) {
                val facets = format.path("facets").map { it.asText().lowercase() }
                if (format.path("drm").asBoolean() || facets.any { it.contains("drm") || it.contains("widevine") || it.contains("playready") }) continue
                for (quality in format.path("qualities")) {
                    for (track in quality.path("audio").path("tracks")) {
                        val url = track.path("uri").asText()
                        if (!https(url)) continue
                        val path = URI(url).path.lowercase()
                        val type = when {
                            path.endsWith(".m3u8") -> "hls"
                            path.endsWith(".mpd") -> "dash"
                            path.endsWith(".mp4") || path.endsWith(".webm") -> "video"
                            else -> continue
                        }
                        streams += MediaStream(
                            url, type,
                            track.path("language").asText("deu"),
                            track.path("class").asText("main"),
                            quality.path("quality").asText("auto"),
                            quality.path("highestVerticalResolution").asInt(0).takeIf { it > 0 }
                        )
                    }
                }
            }
        }
        return streams.distinctBy { it.url }
            .sortedBy { if (it.audioClass == "main") 0 else 1 }
    }

    fun captions(ptmd: JsonNode): List<Caption> = ptmd.path("captions").mapNotNull { caption ->
        val url = caption.path("uri").asText()
        if (!https(url)) null else Caption(url, caption.path("language").asText("deu"))
    }.distinctBy { it.url }
}
