package recloudstream

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.newHomePageResponse

class BurakArsivProvider : MainAPI() {
    override var mainUrl = "https://example.com"
    override var name = "Burak Arşiv (Popüler Platformlar)"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "tr"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val catalogItems = PlatformCatalog.listAll()
        val movieSearchList = ArrayList<SearchResponse>()

        catalogItems.forEachIndexed { index, platformItem ->
            val searchResponse = newMovieSearchResponse(
                name = "${platformItem.platformName} (IMDb: ${platformItem.imdbScore})",
                url = "$mainUrl/details/$index",
                type = TvType.Movie,
                fix = false
            ) {
                this.posterUrl = "https://example.com"
            }
            movieSearchList.add(searchResponse)
        }

        return newHomePageResponse(
            listOf(HomePageList("Türkiye'de En Çok İzlenen Popüler Kanallar", movieSearchList, true)),
            hasNext = false
        )
    }

    override suspend fun load(url: String): LoadResponse? {
        val index = url.substringAfterLast("/").toIntOrNull() ?: 0
        val platformItem = PlatformCatalog.listAll().getOrNull(index) ?: return null

        val optionsText = buildString {
            append("Mevcut Seçenekler: ")
            if (platformItem.hasTurkishDub) append("[Türkçe Dublaj] ")
            if (platformItem.hasSubtitles) append("[Altyazı] ")
        }

        return newMovieLoadResponse(
            name = platformItem.platformName,
            url = url,
            type = TvType.Movie,
            dataUrl = url
        ) {
            this.posterUrl = "https://example.com"
            this.plot = "Bu kanal en popüler içerikleri barındırır. IMDb Puanı: ${platformItem.imdbScore}. $optionsText"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val index = data.substringAfterLast("/").toIntOrNull() ?: 0
        val platformItem = PlatformCatalog.listAll().getOrNull(index) ?: return false

        if (platformItem.hasSubtitles) {
            subtitleCallback.invoke(
                SubtitleFile(
                    lang = "Türkçe",
                    url = "https://example.com"
                )
            )
        }

        callback.invoke(
            ExtractorLink(
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
