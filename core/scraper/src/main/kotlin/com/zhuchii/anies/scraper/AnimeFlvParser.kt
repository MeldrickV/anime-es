package com.zhuchii.anies.scraper

/**
 * Parser puro (sin red) de las busquedas de AnimeFLV.
 *
 * Port del pipeline Bash original (/home/meldrickv/ani-es/ani-es):
 *   html=$(curl ...) -> se extraen los <a href="./anime/<slug>"> con un regex.
 * Aqui se hace lo mismo sobre una cadena HTML ya obtenida, manteniendo una
 * funcion determinista que se puede testear en la JVM con fixtures en CI.
 *
 * El busqueda real (HTTP) vive en [AnimeFlvScraper]; esta clase solo da
 * estructura al HTML para que el parser sea testeable de forma aislada.
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
}