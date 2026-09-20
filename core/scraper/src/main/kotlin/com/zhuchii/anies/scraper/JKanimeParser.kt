package com.zhuchii.anies.scraper

import com.zhuchii.anies.scraper.model.AnimeDetalle
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.scraper.model.Episodio
import com.zhuchii.anies.scraper.model.HomeAnimes
import com.zhuchii.anies.scraper.model.JkServidor
import com.zhuchii.anies.scraper.model.Source
import java.net.URI

/**
 * Parser puro (sin red) de las busquedas, la portada (home) y el detalle de
 * J-Kanime.
 *
 * Port del pipeline Bash original (/home/meldrickv/ani-es/ani-es):
 *   - busqueda = grep '<h5><a href="...">Titulo</a></h5>'.
 *   - detalle = pagina https://jkanime.net/<slug>/ (misma que baja el script
 *     para episodios); aqui se portan cover (.movpic), sinopsis
 *     (.anime_info > p.scroll), generos y estado (.anime_data) como decision F2.
 *   - home: "Top animes" -> populares y "Animes recientes" -> recientes
 *     (decision F2, misma base del script).
 *
 * El HTTP vive en [JKanimeScraper]; esta clase es determinista y se testea en
 * la JVM con fixtures reales capturados en CI.
 */
object JKanimeParser {

    private val BUSQUEDA_REGEX = Regex("""<h5><a\s+href="([^"]+)"[^>]*>([^<]*)</a></h5>""")

    /** Cover de cada resultado: el .anime__item trae la portada como imagen de
     *  fondo (`data-setbg`) justo antes de su <h5><a href=".../slug/">Titulo. */
    private val BUSQUEDA_COVER = Regex(
        """data-setbg="(https://[^"]+)"[\s\S]*?<h5><a\s+href="([^"]+)"[^>]*>[^<]*</a></h5>"""
    )

    /** Devuelve la lista de animes encontrados en una pagina de resultados J-Kanime. */
    fun parseBusqueda(html: String): List<AnimeSummary> {
        val portadas = BUSQUEDA_COVER.findAll(html)
            .associate { match ->
                val (cover, href) = match.destructured
                slugDeHref(href) to cover
            }
        return BUSQUEDA_REGEX.findAll(html)
            .map { match ->
                val (href, titulo) = match.destructured
                AnimeSummary(
                    source = Source.J_KANIME,
                    slug = slugDeHref(href) ?: slugDelTitulo(titulo),
                    title = titulo.trim(),
                    coverUrl = slugDeHref(href)?.let { portadas[it]?.takeIf { c -> c.startsWith("http") } },
                )
            }
            .distinctBy { it.slug }
            .toList()
    }

    private fun slugDeHref(href: String): String? = try {
        val path = URI(href).path?.trim('/') ?: return null
        path.split('/').lastOrNull()?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    /** Mismo saneamiento del script original para derivar el slug del titulo. */
    private fun slugDelTitulo(titulo: String): String =
        titulo
            .map { c -> if (c.isLetterOrDigit() || c == ' ') c else ' ' }
            .joinToString("")
            .trim()
            .replace(Regex("\\s+"), "-")
            .lowercase()

    // ---- Detalle (F2) ----

    private val DETALLE_INFO = Regex("""class="anime_info".*?(?=<p class="scroll">)""", RegexOption.DOT_MATCHES_ALL)
    private val DETALLE_COVER = Regex("""class="[^"]*\bmovpic\b[^"]*"[^>]*>\s*<img[^>]*src="([^"]+)"""")
    private val DETALLE_TITULO = Regex("""<h3>\s*([^<]+)\s*</h3>""")
    private val DETALLE_DESCRIPCION = Regex("""<p class="scroll">(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
    private val DETALLE_GENEROS = Regex("""<li><span>Generos:</span>(.*?)</li>""", RegexOption.DOT_MATCHES_ALL)
    private val DETALLE_GENERO_LINK = Regex("""href="[^"]*/genero/[^"]+"[^>]*>([^<]+)</a>""")
    private val DETALLE_EPISODIOS = Regex("""Episodios:</span>\s*(\d+)""")
    private val DETALLE_ESTADO = Regex("""Estado:</span>\s*<div[^>]*>([^<]+)""")

    /** Detalle de un anime: cover, sinopsis, generos, episodios y estado. */
    fun parseDetalle(html: String): AnimeDetalle {
        val info = DETALLE_INFO.find(html)?.value ?: html
        val generos = DETALLE_GENEROS.find(html)
            ?.groupValues?.get(1).orEmpty()
            .let { bloque ->
                DETALLE_GENERO_LINK.findAll(bloque).map { it.groupValues[1].trim() }.distinct().toList()
            }
        return AnimeDetalle(
            source = Source.J_KANIME,
            slug = "",
            title = DETALLE_TITULO.find(info)?.groupValues?.get(1)?.trim() ?: "",
            coverUrl = DETALLE_COVER.find(info)?.groupValues?.get(1)?.takeIf { it.startsWith("http") },
            description = DETALLE_DESCRIPCION.find(html)?.groupValues?.get(1)?.let(AnimeFlvParser::limpiarTitulo)
                ?.takeIf { it.isNotEmpty() },
            tags = generos,
            episodeCount = DETALLE_EPISODIOS.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            estado = DETALLE_ESTADO.find(html)?.groupValues?.get(1)?.trim(),
        )
    }

    // ---- Portada (home, F2) ----

    private val HOME_ANCLA = Regex(
        """<a[^>]*href="https://jkanime\.net/([^"/]+)/"[^>]*>(.*?)</a>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val HOME_IMG = Regex("""<img[^>]*(?:src|data-setbg)="([^"]+)"[^>]*alt="([^"]*)"""")

    /** Portada: populares = "Top animes", recientes = "Animes recientes". */
    fun parseHome(html: String): HomeAnimes {
        val top = seccion(html, "Top animes")
        val recientes = seccion(html, "Animes recientes")
        return HomeAnimes(
            populares = listarHome(top),
            recientes = listarHome(recientes),
        )
    }

    private fun listarHome(bloque: String): List<AnimeSummary> =
        HOME_ANCLA.findAll(bloque).mapNotNull { match ->
            val (slug, inner) = match.destructured
            val (cover, alt) = HOME_IMG.find(inner)?.destructured ?: return@mapNotNull null
            AnimeSummary(
                source = Source.J_KANIME,
                slug = slug,
                title = alt.trim(),
                coverUrl = cover.takeIf { it.startsWith("http") },
            )
        }.distinctBy { it.slug }.toList()

    /** Devuelve el bloque HTML de una seccion (entre su <h3>/<h4> y el siguiente). */
    private fun seccion(html: String, titulo: String): String {
        val pos = html.indexOf(titulo)
        if (pos < 0) return ""
        val inicio = maxOf(html.lastIndexOf("<h2", pos), html.lastIndexOf("<h3", pos), html.lastIndexOf("<h4", pos))
        if (inicio < 0) return ""
        val fin = listOf("<h2", "<h3", "<h4")
            .mapNotNull { html.indexOf(it, inicio + 3).takeIf { index -> index > 0 } }
            .minOrNull()
            ?: html.length
        return html.substring(inicio, fin)
    }

    // ---- Episodios (F4) ----

    private val CSRF = Regex("""meta name="csrf-token" content="([^"]+)"""")
    private val API_EPISODIOS = Regex("""url:\s*'https://jkanime\.net(/ajax/episodes/\d+/?)'""")
    private val TIPO = Regex("""Tipo:</span>\s*([^<]+)""")
    private val TOTAL = Regex(""""total"\s*:\s*(\d+)""")

    /** Token CSRF de la pagina de detalle (tests y POST del ajax de episodios). */
    fun parseCsrf(html: String): String? =
        CSRF.find(html)?.groupValues?.get(1)

    /** Ruta del endpoint ajax de episodios, relativa a la base del scraper.
     *  En la web aparece absoluta ("url: 'https://jkanime.net/ajax/episodes/201/'"),
     *  pero el metodo devuelve la ruta para que el scraper la resuelva contra su
     *  propia base (espejo del script, que hace POST al host de la fuente). */
    fun parseEpisodiosApi(html: String): String? =
        API_EPISODIOS.find(html)?.groupValues?.get(1)

    /** Tipo del anime (Serie/Pelicula); el CLI navega a /<slug>/pelicula si es Pelicula. */
    fun parseTipo(html: String): String? =
        TIPO.find(html)?.groupValues?.get(1)?.trim()

    /** Total de episodios del ajax; Port 1:1 del JSON del script (pagina 1). */
    fun parseEpisodiosTotal(json: String): Int =
        TOTAL.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    /** Lista de episodios 1..total, igual que el `seq 1..num_episodios` del CLI. */
    fun parseEpisodios(json: String): List<Episodio> {
        val total = parseEpisodiosTotal(json)
        return (1..total).map { Episodio(it.toString()) }
    }

    // ---- Video (F5) ----

    /** src del iframe del reproductor `jkplayer/um` o `umv` de la pagina del
     *  episodio (port del glob `jkanime.net/jkplayer/um*` del script). La URL
     *  puede ser absoluta (jkanime.net) o relativa; el scraper la resuelve. */
    private val JKPLAYER_IFRAME = Regex("""src="([^"]*/jkplayer/um[^"]*)"""")
    /** sufijo de la iframe de `/jk.php` (reproductor antiguo del script). */
    private val JKPHP_IFRAME = Regex("""<iframe[^>]+src="/jk\.php([^"]*)"""")
    /** Url directa del bloque `video: { url: '...' }` (mismo sed del CLI). */
    private val VIDEO_JS = Regex("""url:\s*'([^']+)'""")
    /** `var servers = [...]` de la pagina del episodio (fallback Mediafire). */
    private val SERVERS_VAR = Regex("""var servers = (\[.*?\]);""", RegexOption.DOT_MATCHES_ALL)
    private val SERVERS_ITEM = Regex("""\{"remote":"([^"]+)"[^}]*"server":"([^"]+)"""")
    private val MEDIAFIRE_DOWNLOAD = Regex("""href="([^"]*https://download[^"]*)"""")
    private val BASE64 = Regex("""^[A-Za-z0-9+/=\s]+$""")

    /** URL del iframe `jkplayer/um`/`umv` (null si la pagina no lo trae). */
    fun parseJkplayerUrl(html: String): String? =
        JKPLAYER_IFRAME.find(html)?.groupValues?.get(1)

    /** Sufijo tras `/jk.php` del iframe antiguo (incluye el `?` si existe). */
    fun parseJkPhpSuffix(html: String): String? =
        JKPHP_IFRAME.find(html)?.groupValues?.get(1)

    /** Primera `url: '...'` de la pagina del reproductor (jkplayer/jk.php). */
    fun parseVideoUrl(playerHtml: String): String? =
        VIDEO_JS.find(playerHtml)?.groupValues?.get(1)

    /** Servidores de `var servers` (port del JSON que parsea el script). */
    fun parseServidores(html: String): List<JkServidor>? {
        val bloques = SERVERS_VAR.find(html)?.groupValues?.get(1) ?: return null
        return SERVERS_ITEM.findAll(bloques).map {
            val (remote, server) = it.destructured
            JkServidor(server, remote)
        }.toList()
    }

    /** Decodifica base64 (port de `base64.b64decode(...).decode()`). */
    fun decodificarBase64(b64: String): String? {
        val limpio = b64.trim().replace(Regex("\\s+"), "")
        if (limpio.length % 4 != 0 || !BASE64.containsMatchIn(limpio)) return null
        return try {
            String(java.util.Base64.getDecoder().decode(limpio)).ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    /** Enlace `https://download...` de la pagina de Mediafire (port del script). */
    fun parseMediafireUrl(html: String): String? =
        MEDIAFIRE_DOWNLOAD.find(html)?.groupValues?.get(1)
}