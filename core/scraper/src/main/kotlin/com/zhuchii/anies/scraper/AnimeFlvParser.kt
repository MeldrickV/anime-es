package com.zhuchii.anies.scraper

import com.zhuchii.anies.scraper.model.AnimeDetalle
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.scraper.model.Episodio
import com.zhuchii.anies.scraper.model.HomeAnimes
import com.zhuchii.anies.scraper.model.Source

/**
 * Parser puro (sin red) de las busquedas, la portada (home) y el detalle de
 * AnimeFLV.
 *
 * Port del pipeline Bash original (/home/meldrickv/ani-es/ani-es):
 *   - busqueda: html=$(curl ...) -> se extraen los <a href="./anime/<slug>">.
 *   - detalle: GET $AF_BASE/anime/<slug> (ya descargada en el script); aqui
 *     se portan ademas cover (og:image / .info-l), sinopsis (.tx>p),
 *     generos (.gn) y numero de episodios (data-ep) como decision de F2.
 *   - home: la portada de la fuente no la usa el CLI; se portan las secciones
 *     "Animes en Emision" -> populares y "Ultimos episodios agregados" ->
 *     recientes (decision F2, misma base del script).
 *
 * Todo es determinista y se testea en la JVM con fixtures reales capturados
 * en CI. El HTTP vive en [AnimeFlvScraper].
 */
object AnimeFlvParser {

    // Espejo del regex del script: <h3 class="h"><a href="./anime/<slug>"...><titulo></a></h3>
    private val BUSQUEDA_REGEX = Regex(
        """<h3 class="h"><a href="\./anime/([^\"]+)"[^>]*>(.*?)</a></h3>"""
    )

    private val TAG_REGEX = Regex("<[^>]*>")
    private val ENTITIES = mapOf(
        "&amp;" to "&", "&lt;" to "<", "&gt;" to ">",
        "&quot;" to "\"", "&#039;" to "'", "&nbsp;" to " "
    )

    /** Limpia el titulo: quita tags residuales y decodifica entidades HTML. */
    fun limpiarTitulo(raw: String): String {
        var texto = TAG_REGEX.replace(raw, "")
        ENTITIES.forEach { (ent, ch) -> texto = texto.replace(ent, ch) }
        return texto.trim()
    }

    /** Devuelve la lista de animes encontrados en una pagina de resultados AnimeFLV. */
    fun parseBusqueda(html: String): List<AnimeSummary> =
        BUSQUEDA_REGEX.findAll(html)
            .map { match ->
                val (slug, rawTitulo) = match.destructured
                AnimeSummary(
                    source = Source.ANIME_FLV,
                    slug = slug,
                    title = limpiarTitulo(rawTitulo),
                )
            }
            .distinctBy { it.slug }
            .toList()

    private val DETALLE_COVER = Regex("""class="info-l".*?data-src="([^"]+)"""", RegexOption.DOT_MATCHES_ALL)
    private val DETALLE_COVER_OG = Regex("""<meta property="og:image" content="([^"]+)"""")
    private val DETALLE_SLUG = Regex("""data-sl="([^"]+)"""")
    private val DETALLE_EPISODIOS = Regex("""data-ep="(\d+)"""")
    private val DETALLE_DESCRIPCION = Regex("""<div class="tx"><p>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
    private val DETALLE_GENEROS = Regex("""href="\./animes\?genero=[^"]+"[^>]*>([^<]+)</a>""")
    private val DETALLE_ESTADO = Regex("""class="st c-e"><span>([^<]+)</span>""")

    /** Detalle de un anime: cover, sinopsis, generos y total de episodios. */
    fun parseDetalle(html: String): AnimeDetalle {
        val cover = DETALLE_COVER.find(html)?.groupValues?.get(1)
            ?: DETALLE_COVER_OG.find(html)?.groupValues?.get(1)
        return AnimeDetalle(
            source = Source.ANIME_FLV,
            slug = DETALLE_SLUG.find(html)?.groupValues?.get(1) ?: "",
            coverUrl = cover?.takeIf { it.startsWith("http") },
            description = DETALLE_DESCRIPCION.find(html)?.groupValues?.get(1)?.let(::limpiarTitulo)
                ?.takeIf { it.isNotEmpty() },
            tags = DETALLE_GENEROS.findAll(html).map { it.groupValues[1].trim() }.distinct().toList(),
            episodeCount = DETALLE_EPISODIOS.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            estado = DETALLE_ESTADO.find(html)?.groupValues?.get(1)?.trim(),
        )
    }

    // ---- Portada (home) ----

    private val HOME_EMISION = Regex(
        """href="[^"]*?/anime/([^"/]+)"[^>]*>(.*?)</a>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val HOME_EPISODIOS = Regex(
        """href="[^"]*?/ver/([^"/]+?)-\d+"[^>]*title="Ver ([^"]+?) episodio \d+""",
        RegexOption.DOT_MATCHES_ALL,
    )

    /** Portada: populares = seccion "Animes en Emision", recientes = "Ultimos
     *  episodios agregados" (episodios nuevos, mapeados a su anime). */
    fun parseHome(html: String): HomeAnimes {
        val emision = seccion(html, "Animes en Emisión")
        val recientes = seccion(html, "Últimos episodios agregados")
        return HomeAnimes(
            populares = HOME_EMISION.findAll(emision).map { match ->
                val (slug, rawTitulo) = match.destructured
                AnimeSummary(Source.ANIME_FLV, slug, limpiarTitulo(rawTitulo))
            }.distinctBy { it.slug }.toList(),
            recientes = HOME_EPISODIOS.findAll(recientes).map { match ->
                val (slug, titulo) = match.destructured
                AnimeSummary(Source.ANIME_FLV, slug, limpiarTitulo(titulo))
            }.distinctBy { it.slug }.toList(),
        )
    }

    /** Devuelve el bloque HTML de una seccion (entre su <h2> y el siguiente). */
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

    private val EPS_VAR = Regex("""var eps = (\[.*?\]);""", RegexOption.DOT_MATCHES_ALL)
    private val EPS_ITEM = Regex("""\["(\d+)","(\d+)",""\]""")

    /** Lista de episodios desde `var eps` del detalle: caps sueltos y rangos
     *  (p.ej. ["24","0",""] = un cap; ["13","24"] = rango 13..24), ascendente,
     *  sin duplicados y sin el cap "0". Port del parse del script. */
    fun parseEpisodios(html: String): List<Episodio> {
        val bloques = EPS_VAR.find(html)?.groupValues?.get(1) ?: return emptyList()
        return EPS_ITEM.findAll(bloques)
            .map { it.destructured }
            .flatMap { (a, b) ->
                val menor = minOf(a.toIntOrNull() ?: 0, b.toIntOrNull() ?: 0)
                val mayor = maxOf(a.toIntOrNull() ?: 0, b.toIntOrNull() ?: 0)
                if (mayor == 0) emptyList() else (menor..mayor).map { Episodio(it.toString()) }
            }
            .filter { it.numero != "0" }
            .distinctBy { it.numero }
            .sortedBy { it.numero.toIntOrNull() ?: 0 }
            .toList()
    }
}