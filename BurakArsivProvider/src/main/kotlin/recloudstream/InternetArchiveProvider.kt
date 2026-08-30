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
import com.lagradost.cloudstream3.utils.SubtitleFile

class InternetArchiveProvider : MainAPI() {
    override var mainUrl = "https://example.com"
    override var name = "Burak Arşiv (Popüler Platformlar)"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "tr"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val catalogItems = PlatformCatalog.listAll()
        val movieSearchList = ArrayList<SearchResponse>()

        catalogItems.forEachIndexed { index, platform ->
            movieSearchList.add(
                newMovieSearchResponse(
                    name = "${platform.platformName} (IMDb: ${platform.imdbScore})",
                    url = "$mainUrl/details/$index",
                    apiName = this.name,
                    type = TvType.Movie
                ) {
                    this.posterUrl = "https://example.com"
                }
            )
        }

        return HomePageResponse(
            listOf(
                HomePageList(
                    name = "Türkiye'de En Çok İzlenen Popüler Kanallar",
                    list = movieSearchList,
                    isHorizontalLayout = true
                )
            ),
            hasNext = false
        )
    }

    override suspend fun load(url: String): LoadResponse? {
        val index = url.substringAfterLast("/").toIntOrNull() ?: 0
        val platform = PlatformCatalog.listAll().getOrNull(index) ?: return null

        val optionsText = buildString {
            append("Mevcut Seçenekler: ")
            if (platform.hasTurkishDub) append("[Türkçe Dublaj] ")
            if (platform.hasSubtitles) append("[Altyazı] ")
        }

        return newMovieLoadResponse(
            name = platform.platformName,
            url = url,
            apiName = this.name,
            dataUrl = url
        ) {
            this.posterUrl = "https://example.com"
            this.plot = "Bu kanal en popüler içerikleri barındırır. IMDb Puanı: ${platform.imdbScore}. $optionsText"
            this.rating = (platform.imdbScore * 10).toInt()
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val index = data.substringAfterLast("/").toIntOrNull() ?: 0
        val platform = PlatformCatalog.listAll().getOrNull(index) ?: return false

        if (platform.hasSubtitles) {
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
                name = if (platform.hasTurkishDub) "Türkçe Dublaj (1080p)" else "Orijinal Ses (1080p)",
                url = platform.videoUrl,
                referer = mainUrl,
                quality = Qualities.P1080.value
            )
        )

        return true
    }
}
