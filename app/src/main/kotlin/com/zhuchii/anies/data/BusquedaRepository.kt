package com.zhuchii.anies.data

import com.zhuchii.anies.scraper.AnimeFlvScraper
import com.zhuchii.anies.scraper.JKanimeScraper
import com.zhuchii.anies.scraper.model.AnimeDetalle
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.scraper.model.Episodio
import com.zhuchii.anies.scraper.model.HomeAnimes
import com.zhuchii.anies.scraper.model.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Resultado agregado de la busqueda en ambas fuentes, con errores parciales. */
data class ResultadoBusqueda(
    val animes: List<AnimeSummary>,
    val errores: List<String>,
) {
    val conErrores: Boolean get() = errores.isNotEmpty()
}

/**
 * Fusiona la busqueda de AnimeFLV y J-Kanime en paralelo (supervisorScope no
 * hace falta: cada fuente se envuelve en try/catch propio) devolviendo el
 * resultado parcial de cada una. Que J-Kanime falle no tumba el resultado de
 * AnimeFLV y viceversa.
 */
class BusquedaRepository(
    private val animeFlv: AnimeFlvScraper,
    private val jkanime: JKanimeScraper,
) {
    suspend fun buscar(query: String): ResultadoBusqueda {
        val q = query.trim()
        if (q.isEmpty()) return ResultadoBusqueda(emptyList(), emptyList())

        return coroutineScope {
            val af = async { buscarParcial(q) { animeFlv.buscar(it) } }
            val jk = async { buscarParcial(q) { jkanime.buscar(it) } }
            val resultadoAf = af.await()
            val resultadoJk = jk.await()

            ResultadoBusqueda(
                animes = resultadoAf.animes + resultadoJk.animes,
                errores = listOfNotNull(
                    resultadoAf.error?.let { "AnimeFLV: $it" },
                    resultadoJk.error?.let { "J-Kanime: $it" },
                ),
            )
        }
    }

    suspend fun buscarEn(source: Source, query: String): List<AnimeSummary> = when (source) {
        Source.ANIME_FLV -> animeFlv.buscar(query)
        Source.J_KANIME -> jkanime.buscar(query)
    }

    /** Portada (populares/recientes) de una sola fuente (F2). */
    suspend fun home(source: Source): HomeAnimes = when (source) {
        Source.ANIME_FLV -> animeFlv.home()
        Source.J_KANIME -> jkanime.home()
    }

    /** Detalle (cover/sinopsis/tags) de un anime en una fuente (F2). */
    suspend fun detalle(source: Source, slug: String): AnimeDetalle = when (source) {
        Source.ANIME_FLV -> animeFlv.detalle(slug)
        Source.J_KANIME -> jkanime.detalle(slug)
    }

    /** Lista de episodios de un anime (F4). */
    suspend fun episodios(source: Source, slug: String): List<Episodio> = when (source) {
        Source.ANIME_FLV -> animeFlv.episodios(slug)
        Source.J_KANIME -> jkanime.episodios(slug)
    }

    private suspend fun buscarParcial(
        query: String,
        bloque: suspend (String) -> List<AnimeSummary>,
    ): Parcial = try {
        Parcial(bloque(query))
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        Parcial(emptyList(), t.message ?: t::class.simpleName)
    }

    private data class Parcial(val animes: List<AnimeSummary>, val error: String? = null)
}