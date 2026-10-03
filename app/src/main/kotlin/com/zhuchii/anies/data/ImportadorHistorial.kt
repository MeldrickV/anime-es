package com.zhuchii.anies.data

import com.zhuchii.anies.scraper.HistoriaCli
import com.zhuchii.anies.scraper.HistoryJsonParser
import com.zhuchii.anies.scraper.ExcepcionesRemotas
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.scraper.model.Source
import kotlinx.coroutines.CancellationException

/** Resumen de la sincronizacion con el history.json del CLI. */
data class ResultadoImportacion(
    val importados: Int,
    val sinCoincidencia: List<String>,
) {
    val mensaje: String
        get() = if (importados > 0) {
            "Importados $importados anime(s) desde history.json" +
                if (sinCoincidencia.isEmpty()) "." else " (${sinCoincidencia.size} sin coincidencia)."
        } else {
            "No se importo nada desde history.json." +
                if (sinCoincidencia.isEmpty()) "" else " Sin coincidencia: ${sinCoincidencia.joinToString(" · ")}."
        }
}

/**
 * Sincroniza el history.json del CLI (claves = titulos) contra Room. Como el
 * JSON guarda el TITULO y Room usa el slug, la unica via segura es resolver
 * cada titulo con la busqueda real de su fuente y tomar el primer resultado
 * cuyo titulo coincida normalizado. Lo importado queda como `historial`
 * (aparece en la lista) y como `progreso` (los badges de F6). Los titulos que
 * no coinciden no se eliminan: se reportan.
 */
class ImportadorHistorial(
    private val busqueda: BusquedaRepository,
    private val progreso: ProgresoRepository,
    private val historial: HistorialRepository,
    private val excepciones: ExcepcionesRemotas = ExcepcionesRemotas(),
) {
    suspend fun importar(entradas: Map<String, HistoriaCli>): ResultadoImportacion {
        var importados = 0
        val sinCoincidencia = mutableListOf<String>()

        for ((titulo, entrada) in entradas) {
            val source = fuente(entrada.source)
            val episodio = entrada.lastCap
            if (source == null || episodio <= 0) continue

            val coincidencia = try {
                resolverTitulo(source, titulo)
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                null
            }
            if (coincidencia == null) {
                sinCoincidencia += titulo
                continue
            }

            historial.registrar(
                source = coincidencia.source,
                slug = coincidencia.slug,
                titulo = coincidencia.title,
                coverUrl = coincidencia.coverUrl,
            )
            progreso.guardar(
                source = coincidencia.source,
                slug = coincidencia.slug,
                episodio = episodio,
                posicionMs = HistoryJsonParser.progresoToMs(entrada.progress),
                duracionMs = 0L,
            )
            importados++
        }

        return ResultadoImportacion(importados, sinCoincidencia)
    }

    private suspend fun resolverTitulo(source: Source, titulo: String): AnimeSummary? {
        // 1) Override remoto (slugs truncados que el saneado no resuelve).
        porExcepcion(source, titulo)?.let { return it }
        // 2) Busqueda real + match normalizado (via anterior).
        val buscado = normalizar(titulo)
        return busqueda.buscarEn(source, titulo)
            .firstOrNull { normalizar(it.title) == buscado }
    }

    /** Resuelve via `excepciones.json`: candidata -> URL real -> detalle. */
    private suspend fun porExcepcion(source: Source, titulo: String): AnimeSummary? = try {
        val base = when (source) {
            Source.J_KANIME -> "https://jkanime.net"
            Source.ANIME_FLV -> return null
        }
        val real = excepciones.resolver("$base/${slugDeTitulo(titulo)}/") ?: return null
        val slug = real.trimEnd('/').substringAfterLast('/')
        if (slug.isBlank()) return null
        val detalle = busqueda.detalle(source, slug)
        AnimeSummary(
            source = source,
            slug = slug,
            title = detalle.title.ifBlank { titulo },
            coverUrl = detalle.coverUrl,
        )
    } catch (ce: CancellationException) {
        throw ce
    } catch (_: Throwable) {
        null
    }

    /** Mismo saneamiento titulo->slug del CLI y del parser. */
    private fun slugDeTitulo(titulo: String): String =
        titulo.map { c -> if (c.isLetterOrDigit() || c == ' ') c else ' ' }
            .joinToString("")
            .trim()
            .replace(Regex("\\s+"), "-")
            .lowercase()

    private fun normalizar(s: String): String =
        s.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun fuente(source: String): Source? = when (source.trim().lowercase()) {
        "animeflv" -> Source.ANIME_FLV
        "jkanime" -> Source.J_KANIME
        else -> null
    }
}