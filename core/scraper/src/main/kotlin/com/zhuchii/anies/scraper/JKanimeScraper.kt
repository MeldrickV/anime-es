package com.zhuchii.anies.scraper

import com.zhuchii.anies.scraper.model.AnimeSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Extraccion real (red) de J-Kanime.
 *
 * Port del pipeline Bash de /home/meldrickv/ani-es/ani-es:
 *   - busqueda = GET simple https://jkanime.net/buscar/<query>/ (page 1),
 *     query con espacios como guiones bajos; parse con [JKanimeParser].
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