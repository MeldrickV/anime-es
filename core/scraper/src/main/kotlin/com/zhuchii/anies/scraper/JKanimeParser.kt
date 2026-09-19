package com.zhuchii.anies.scraper

import com.zhuchii.anies.scraper.model.AnimeSummary
import java.net.URI

/**
 * Parser puro (sin red) de las busquedas de J-Kanime.
 *
 * Port del pipeline Bash original (/home/meldrickv/ani-es/ani-es):
 *   grep -oP '<h5><a href="...">\K.*?(?=</a></h5>)'
 *
 * A diferencia del script (que solo captura el titulo para reconstruir el
 * slug al navegar), aqui tambien se captura el href absoluto
 * `https://jkanime.net/<slug>/` para quedarnos con un slug fiable. Si el href
 * no estuviera (cambio de DOM), se deriva del titulo con el mismo
 * saneamiento que hace el script (`tr -cd '[:alnum:] -'` + `-` + lower).
 */
object JKanimeParser {

    private val BUSQUEDA_REGEX = Regex("""<h5><a\s+href="([^"]+)"[^>]*>([^<]*)</a></h5>""")

    /** Devuelve la lista de animes encontrados en una pagina de resultados J-Kanime. */
    fun parseBusqueda(html: String): List<AnimeSummary> =
        BUSQUEDA_REGEX.findAll(html)
            .map { match ->
                val (href, titulo) = match.destructured
                AnimeSummary(
                    source = Source.J_KANIME,
                    slug = slugDeHref(href) ?: slugDelTitulo(titulo),
                    title = titulo.trim(),
                )
            }
            .distinctBy { it.slug }
            .toList()

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
}