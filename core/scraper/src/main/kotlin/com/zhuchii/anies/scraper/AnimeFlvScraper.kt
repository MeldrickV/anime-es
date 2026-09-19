package com.zhuchii.anies.scraper

import com.zhuchii.anies.scraper.model.AnimeDetalle
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.scraper.model.Episodio
import com.zhuchii.anies.scraper.model.HomeAnimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Extraccion real (red) de AnimeFLV.
 *
 * Port del pipeline Bash de /home/meldrickv/ani-es/ani-es:
 *   - GET $AF_BASE/animes?buscar=<query>&pag=<N> (AF_BASE = https://vww.animeflv.one)
 *   - GET $AF_BASE/anime/<slug> para el detalle (data-id, data-sl, eps)
 *   - parse con [AnimeFlvParser]
 *
 * Espejo EXACTO del script: endpoint, base y User-Agent son los mismos. La
 * paginacion usa el parametro `pag`; la busqueda sin pag es la pagina 1. El
 * detalle y el home son decisiones F2 sobre las mismas URLs del script.
 */
class AnimeFlvScraper(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://vww.animeflv.one",
) {
    suspend fun buscar(query: String, pagina: Int = 1): List<AnimeSummary> =
        withContext(Dispatchers.IO) {
            check(query.isNotBlank())
            val request = Request.Builder()
                .url("$baseUrl/animes?buscar=${query.urlEncoded()}&pag=$pagina")
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "AnimeFLV HTTP ${response.code}" }
                AnimeFlvParser.parseBusqueda(response.body?.string().orEmpty())
            }
        }

    /** Detalle (F2): GET $AF_BASE/anime/<slug>, mismo endpoint del script. */
    suspend fun detalle(slug: String): AnimeDetalle = withContext(Dispatchers.IO) {
        check(slug.isNotBlank())
        val request = Request.Builder()
            .url("$baseUrl/anime/$slug")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "AnimeFLV HTTP ${response.code}" }
            AnimeFlvParser.parseDetalle(response.body?.string().orEmpty())
        }
    }

    /** Portada (F2): GET $AF_BASE/ (populares/recientes del home real). */
    suspend fun home(): HomeAnimes = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "AnimeFLV HTTP ${response.code}" }
            AnimeFlvParser.parseHome(response.body?.string().orEmpty())
        }
    }

    /**
     * Episodios (F4): GET $AF_BASE/anime/<slug> (la misma pagina que ya usa el
     * script para data-id/data-sl/eps) y parse del `var eps` del detalle.
     */
    suspend fun episodios(slug: String): List<Episodio> = withContext(Dispatchers.IO) {
        check(slug.isNotBlank())
        val request = Request.Builder()
            .url("$baseUrl/anime/$slug")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "AnimeFLV HTTP ${response.code}" }
            AnimeFlvParser.parseEpisodios(response.body?.string().orEmpty())
        }
    }

    private fun String.urlEncoded(): String =
        URLEncoder.encode(this, "UTF-8").replace("+", "%20")

    companion object {
        /** Mismo UA que el curl del script original (Chrome 91 de Windows). */
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}