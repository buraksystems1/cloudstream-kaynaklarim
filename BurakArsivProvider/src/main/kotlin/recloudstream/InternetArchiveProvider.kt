package recloudstream

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

data class PlatformSample(
    val platformName: String,
    val imdbScore: Double,
    val hasTurkishDub: Boolean,
    val hasSubtitles: Boolean,
    val videoUrl: String
)

object LocalPlatformCatalog {
    val platforms = listOf(
        PlatformSample("BluTV", 8.2, true, true, "LINK_BURAYA"),
        PlatformSample("Netflix Türkiye", 8.3, true, true, "LINK_BURAYA"),
        PlatformSample("Amazon Prime Video", 8.1, true, true, "LINK_BURAYA"),
        PlatformSample("Gain", 7.9, true, true, "LINK_BURAYA"),
        PlatformSample("PuhuTV", 7.5, true, true, "LINK_BURAYA"),
        PlatformSample("Exxen", 6.8, true, true, "LINK_BURAYA"),
        PlatformSample("Disney+ Türkiye", 8.0, true, true, "LINK_BURAYA"),
        PlatformSample("Tabii (TRT)", 7.4, true, true, "LINK_BURAYA"),
        PlatformSample("TOD (BeIN)", 7.6, true, true, "LINK_BURAYA"),
        PlatformSample("MUBI", 8.4, false, true, "LINK_BURAYA")
    )
}

class BurakArsivProvider : MainAPI() {
    override var mainUrl = "https://example.com"
    override var name = "Burak Arşiv (Popüler Platformlar)"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "tr"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val catalogItems = LocalPlatformCatalog.platforms
        val movieSearchList = ArrayList<SearchResponse>()

        catalogItems.forEachIndexed { index, platformItem ->
            movieSearchList.add(
                MovieSearchResponse(
                    name = "${platformItem.platformName} (IMDb: ${platformItem.imdbScore})",
                    url = "$mainUrl/details/$index",
                    apiName = this.name,
                    type = TvType.Movie,
                    posterUrl = "https://example.com",
                    id = index
                )
            )
        }

        return HomePageResponse(
            listOf(HomePageList("Türkiye'de En Çok İzlenen Popüler Kanallar", movieSearchList, true)),
            hasNext = false
        )
    }

    override suspend fun load(url: String): LoadResponse? {
        val index = url.substringAfterLast("/").toIntOrNull() ?: 0
        val platformItem = LocalPlatformCatalog.platforms.getOrNull(index) ?: return null

        val optionsText = buildString {
            append("Mevcut Seçenekler: ")
            if (platformItem.hasTurkishDub) append("[Türkçe Dublaj] ")
            if (platformItem.hasSubtitles) append("[Altyazı] ")
        }

        return MovieLoadResponse(
            name = platformItem.platformName,
            url = url,
            apiName = this.name,
            type = TvType.Movie,
            dataUrl = url,
            posterUrl = "https://example.com",
            plot = "Bu kanal en popüler içerikleri barındırır. IMDb Puanı: ${platformItem.imdbScore}. $optionsText"
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val index = data.substringAfterLast("/").toIntOrNull() ?: 0
        val platformItem = LocalPlatformCatalog.platforms.getOrNull(index) ?: return false

        if (platformItem.hasSubtitles) {
            subtitleCallback.invoke(
                SubtitleFile(
                    lang = "Türkçe",
                    url = "https://example.com"
                )
            )
        }

        callback.invoke(
            newExtractorLink(
                source = this.name,
                name = if (platformItem.hasTurkishDub) "Türkçe Dublaj" else "Orijinal Ses",
                url = platformItem.videoUrl,
                referer = mainUrl,
                quality = Qualities.P1080.value
            )
        )

        return true
    }
}
