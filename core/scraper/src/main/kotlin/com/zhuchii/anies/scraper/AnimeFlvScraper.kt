package com.zhuchii.anies.scraper

import com.zhuchii.anies.scraper.model.AnimeDetalle
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.scraper.model.Episodio
import com.zhuchii.anies.scraper.model.HomeAnimes
import com.zhuchii.anies.scraper.model.VideoFuente
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
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

    /**
     * Resolucion del video de un episodio (F5). Port exacto de
     * `ver_episodio_af`: GET /ver/<slug>-<cap> -> `data-encrypt` (fallback hex
     * de "$data_id-$capi"), POST /flv y decodifica el hex de cada embed.
     *
     * La extraccion de la URL de video de CADA embed admite dos caminos:
     *  1) [resolverEmbed] inyectado (App): ejecuta el embed JS-SPA en un WebView
     *     y captura la peticion .m3u8/.mp4 (los hosts actuales no traen la URL
     *     en el HTML, que era lo que extraia `extraerUrlVideo` del Bash).
     *  2) fallback JVM: regex `extraerUrlVideo` sobre el HTML del embed (mismo
     *     comportamiento que el CLI; los fixtures de los tests lo usan).
     * Valida contra Range 0-0 con el Referer correcto (SRC_REFERER = origin
     * del embed) en ambos casos.
     */
    suspend fun video(
        slug: String,
        cap: String,
        resolverEmbed: suspend (embedUrl: String, referer: String) -> String? = { _, _ -> null },
    ): VideoFuente = withContext(Dispatchers.IO) {
        check(slug.isNotBlank() && cap.isNotBlank())
        val capurl = "$baseUrl/ver/$slug-$cap"

        val verHtml = get(capurl)
        var enc = AnimeFlvParser.parseEncrypt(verHtml)
        if (enc == null) {
            val dataId = AnimeFlvParser.parseDataId(get("$baseUrl/anime/$slug"))
            enc = dataId?.let { id -> "$id-$cap".toHex() }
        }
        checkNotNull(enc) { "AnimeFLV: no se encontro el data-encrypt del reproductor" }

        val servidores = AnimeFlvParser.parseFlvServidores(postFlv(enc, capurl))
        check(servidores.isNotEmpty()) { "AnimeFLV: sin servidores para $slug cap $cap" }

        for (servidor in servidores) {
            val embedUrl = AnimeFlvParser.decodificarHex(servidor.hex) ?: continue
            val videoUrl = resolverEmbed(embedUrl, capurl) ?: runCatching {
                AnimeFlvParser.extraerUrlVideo(get(embedUrl, capurl))
            }.getOrNull() ?: continue
            val referer = originOf(embedUrl) ?: continue
            if (esPlayable(videoUrl, referer)) {
                return@withContext VideoFuente(videoUrl, referer, videoUrl.contains(".m3u8"))
            }
        }
        error("AnimeFLV: ningun servidor ofrece video directo")
    }

    private fun get(url: String, referer: String? = null): String {
        val builder = Request.Builder().url(url).header("User-Agent", USER_AGENT)
        referer?.let { builder.header("Referer", it) }
        return client.newCall(builder.build()).execute().use { response ->
            check(response.isSuccessful) { "AnimeFLV HTTP ${response.code}" }
            response.body?.string().orEmpty()
        }
    }

    private fun postFlv(enc: String, capurl: String): String {
        val request = Request.Builder()
            .url("$baseUrl/flv")
            .header("User-Agent", USER_AGENT)
            .header("Referer", capurl)
            .header("X-Requested-With", "XMLHttpRequest")
            .post(FormBody.Builder().add("acc", "opt").add("i", enc).build())
            .build()
        return client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "AnimeFLV HTTP ${response.code}" }
            response.body?.string().orEmpty()
        }
    }

    /** Comprueba que el video responde (mismo Range 0-0 del `es_playable`). */
    private fun esPlayable(url: String, referer: String): Boolean = try {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", referer)
            .header("Range", "bytes=0-0")
            .build()
        client.newCall(request).execute().use { response ->
            response.code == 200 || response.code == 206
        }
    } catch (_: Exception) {
        false
    }

    private fun originOf(url: String): String? = try {
        val parsed = url.toHttpUrl()
        "${parsed.scheme}://${parsed.host}"
    } catch (_: Exception) {
        null
    }

    private fun String.toHex(): String = map { byte -> "%02x".format(byte.code) }.joinToString("")

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