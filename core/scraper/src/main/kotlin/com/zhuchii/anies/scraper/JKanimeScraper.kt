package com.zhuchii.anies.scraper

import com.zhuchii.anies.scraper.model.AnimeDetalle
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.scraper.model.Episodio
import com.zhuchii.anies.scraper.model.HomeAnimes
import com.zhuchii.anies.scraper.model.VideoFuente
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Extraccion real (red) de J-Kanime.
 *
 * Port del pipeline Bash de /home/meldrickv/ani-es/ani-es:
 *   - busqueda = GET simple https://jkanime.net/buscar/<query>/ (page 1),
 *     query con espacios como guiones bajos; parse con [JKanimeParser].
 *   - detalle (F2) = GET https://jkanime.net/<slug>/ (misma pagina que usa el
 *     script para episodios).
 *   - home (F2) = GET https://jkanime.net/ (populares/recientes).
 *   - el flujo csrf (_token) + POST SOLO aplica a episodios via
 *     /ajax/episodes/<id>/ (F4), NO a la busqueda.
 *   - decode del player (jkplayer/jk.php) para los enlaces directos (F4).
 */
class JKanimeScraper(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://jkanime.net",
) {
    suspend fun buscar(query: String): List<AnimeSummary> = withContext(Dispatchers.IO) {
        check(query.isNotBlank())
        val normalized = query.trim().replace(' ', '_')
        val request = Request.Builder()
            .url("$baseUrl/buscar/$normalized/")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "J-Kanime HTTP ${response.code}" }
            JKanimeParser.parseBusqueda(response.body?.string().orEmpty())
        }
    }

    /** Detalle (F2): GET https://jkanime.net/<slug>/ (pagina del script). */
    suspend fun detalle(slug: String): AnimeDetalle = withContext(Dispatchers.IO) {
        check(slug.isNotBlank())
        val request = Request.Builder()
            .url("$baseUrl/$slug/")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "J-Kanime HTTP ${response.code}" }
            JKanimeParser.parseDetalle(response.body?.string().orEmpty())
        }
    }

    /** Portada (F2): GET https://jkanime.net/ (Top animes y recientes). */
    suspend fun home(): HomeAnimes = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(baseUrl)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "J-Kanime HTTP ${response.code}" }
            JKanimeParser.parseHome(response.body?.string().orEmpty())
        }
    }

    /**
     * Episodios (F4). Port del flujo csrf+POST del script:
     *   - GET $nuevaurl (sin cookies) -> csrf-token, ruta de /ajax/episodes/<id>/
     *     y "Tipo:" (una Pelicula navega a /<slug>/pelicula).
     *   - POST $ruta-ajax con _token + X-Requested-With + X-CSRF-TOKEN + la cookie
     *     de sesion capturada en el GET (el script la guarda con `curl -c`).
     *   - Se lee el `total` del JSON y se genera 1..total (seq del CLI).
     */
    suspend fun episodios(slug: String): List<Episodio> = withContext(Dispatchers.IO) {
        check(slug.isNotBlank())
        val pagina = newCall(
            Request.Builder()
                .url("$baseUrl/$slug/")
                .header("User-Agent", USER_AGENT)
                .build(),
        )
        val tipo = JKanimeParser.parseTipo(pagina.html)
        if (tipo.equals("Pelicula", ignoreCase = true)) {
            return@withContext listOf(Episodio("pelicula"))
        }
        val csrf = JKanimeParser.parseCsrf(pagina.html)
        val apiPath = JKanimeParser.parseEpisodiosApi(pagina.html)
        check(csrf != null && apiPath != null) {
            "J-Kanime: no se encontro el endpoint de episodios en la pagina"
        }
        val body = FormBody.Builder().add("_token", csrf).build()
        val request = Request.Builder()
            .url("$baseUrl$apiPath")
            .header("User-Agent", USER_AGENT)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("X-CSRF-TOKEN", csrf)
            .header("Cookie", pagina.cookies)
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "J-Kanime HTTP ${response.code}" }
            JKanimeParser.parseEpisodios(response.body?.string().orEmpty())
        }
    }

    /** GET con captura de la cookie de sesion (equivalentes -c/-b de curl). */
    private suspend fun newCall(request: Request): Pagina = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "J-Kanime HTTP ${response.code}" }
            val html = response.body?.string().orEmpty()
            val cookies = response.headers("Set-Cookie").mapNotNull { header ->
                try {
                    val c = Cookie.parse(request.url, header) ?: return@mapNotNull null
                    "${c.name}=${c.value}"
                } catch (_: Exception) {
                    null
                }
            }.joinToString("; ")
            Pagina(html, cookies)
        }
    }

    /**
     * Resolucion del video de un episodio (F5). Port de `videocap`:
     *   1) iframe `jkplayer/um`/`umv` de la pagina del episodio -> su pagina
     *      de reproductor tiene `video: { url: '...' }` (HLS directo).
     *   2) iframe `/jk.php` (antiguo) -> mismo parse.
     *   3) fallback `var servers` -> "Mediafire" -> `remote` (base64) como
     *      pagina intermedia -> enlace `https://download...`.
     */
    suspend fun video(slug: String, cap: String): VideoFuente = withContext(Dispatchers.IO) {
        check(slug.isNotBlank() && cap.isNotBlank())
        val capurl = "$baseUrl/$slug/$cap"
        val pagina = newCall(
            Request.Builder().url(capurl).header("User-Agent", USER_AGENT).build(),
        )

        // 1) jkplayer/um (o umv): la URL es absoluta o relativa -> resolver.
        val jkplayerUrl = JKanimeParser.parseJkplayerUrl(pagina.html)
        val jkplayerAbs = jkplayerUrl?.let { url ->
            try { capurl.toHttpUrl().resolve(url)?.toString() } catch (_: Exception) { null }
        }
        if (jkplayerAbs != null) {
            val videoUrl = JKanimeParser.parseVideoUrl(get(jkplayerAbs, capurl))
            if (!videoUrl.isNullOrBlank()) {
                return@withContext VideoFuente(videoUrl, null, videoUrl.contains(".m3u8"))
            }
        }

        // 2) jk.php (antiguo)
        val jkphp = JKanimeParser.parseJkPhpSuffix(pagina.html)
        if (jkphp != null) {
            val videoUrl = JKanimeParser.parseVideoUrl(get("$baseUrl/jk.php$jkphp", capurl))
            if (!videoUrl.isNullOrBlank()) {
                return@withContext VideoFuente(videoUrl, null, videoUrl.contains(".m3u8"))
            }
        }

        // 3) fallback Mediafire (`var servers` del cap)
        val mediafireUrl = JKanimeParser.parseServidores(pagina.html)
            ?.firstOrNull { it.nombre.equals("Mediafire", ignoreCase = true) }?.remoteB64
            ?.let(JKanimeParser::decodificarBase64)
        if (mediafireUrl != null) {
            val download = JKanimeParser.parseMediafireUrl(get(mediafireUrl))
            if (!download.isNullOrBlank()) {
                return@withContext VideoFuente(download, null, false)
            }
        }

        error("J-Kanime: no se pudo resolver el video del capitulo $cap")
    }

    private fun get(url: String, referer: String? = null): String {
        val builder = Request.Builder().url(url).header("User-Agent", USER_AGENT)
        referer?.let { builder.header("Referer", it) }
        return client.newCall(builder.build()).execute().use { response ->
            check(response.isSuccessful) { "J-Kanime HTTP ${response.code}" }
            response.body?.string().orEmpty()
        }
    }

    private data class Pagina(val html: String, val cookies: String)

    companion object {
        /** Mismo UA que el wget del script original (Chrome 91 de Windows). */
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}