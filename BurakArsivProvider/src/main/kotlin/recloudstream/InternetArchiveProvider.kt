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

class BurakArsivProvider : MainAPI() { // Sınıf adını klasörünle birebir eşitledik
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
            listOf(HomePageList("Türkiye'de En Çok İzlenen Popüler Kanallar", movieSearchList, true)),
            hasNext = false
        )
    }

    override suspend fun load(url: String): LoadResponse? {
        val index = url.substringAfterLast("/").toIntOrNull() ?: 0
        val platform = PlatformCatalog.listAll().getOrNull(index) ?: return null

        return newMovieLoadResponse(platform.platformName, url, this.name, url) {
            this.posterUrl = "https://example.com"
            this.plot = "IMDb Puanı: ${platform.imdbScore}. Dublaj: ${platform.hasTurkishDub}, Altyazı: ${platform.hasSubtitles}"
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
            subtitleCallback.invoke(SubtitleFile("Türkçe", "https://example.com"))
        }

        callback.invoke(
            ExtractorLink(
                source = this.name,
                name = if (platform.hasTurkishDub) "Türkçe Dublaj" else "Orijinal Ses",
                url = platform.videoUrl,
                referer = mainUrl,
                quality = Qualities.P1080.value
            )
        )
        return true
    }
}
