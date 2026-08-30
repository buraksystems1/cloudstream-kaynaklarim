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

class BurakArsivProvider : MainAPI() {
    override var mainUrl = "https://example.com"
    override var name = "Burak Arşiv (Popüler Platformlar)"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "tr" // Dil seçeneğini Türkçe yaptık
    override val hasMainPage = true

    // Cloudstream ana sayfasında 10 popüler taslak platformu listeleyen fonksiyon
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val catalogItems = PlatformCatalog.listAll()
        val movieSearchList = ArrayList<SearchResponse>()

        catalogItems.forEachIndexed { index, platform ->
            // Her platformu Cloudstream'in anlayacağı bir arama/kart sonucuna dönüştürüyoruz
            movieSearchList.add(
                newMovieSearchResponse(
                    name = "${platform.platformName} (IMDb: ${platform.imdbScore})",
                    url = "$mainUrl/details/$index", // Her biri için taslak bir detay linki oluşturduk
                    apiName = this.name,
                    type = TvType.Movie
                ) {
                    // Kartın üzerinde Dublaj ve Altyazı bilgisini not olarak gösteriyoruz
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

    // Kullanıcı ana sayfadan bir kanala tıkladığında detay sayfasını yükleyen fonksiyon
    override suspend fun load(url: String): LoadResponse? {
        // Tıklanan elemanın indeksini URL'den çekiyoruz
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
            this.rating = (platform.imdbScore * 10).toInt() // Cloudstream puan sistemine uyarladık
        }
    }

    // "Oynat" butonuna basıldığında video kaynağını ve altyazıyı bağlayan fonksiyon
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val index = data.substringAfterLast("/").toIntOrNull() ?: 0
        val platform = PlatformCatalog.listAll().getOrNull(index) ?: return false

        // Eğer platformda altyazı seçeneği true ise altyazı dosyasını yüklüyoruz
        if (platform.hasSubtitles) {
            subtitleCallback.invoke(
                SubtitleFile(
                    lang = "Türkçe",
                    url = "https://example.com" // Taslak altyazı adresi
                )
            )
        }

        // Video oynatıcıyı yasal ve güvenli test videosuyla tetikliyoruz
        callback.invoke(
            ExtractorLink(
                source = this.name,
                name = if (platform.hasTurkishDub) "Türkçe Dublaj (1080p)" else "Orijinal Ses (1080p)",
                url = platform.videoUrl, // PlatformCatalog.kt içindeki güvenli video URL'si
                referer = mainUrl,
                quality = Qualities.P1080.value
            )
        )

        return true
    }
}
