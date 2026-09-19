package com.zhuchii.anies.data

import com.zhuchii.anies.data.model.AnimeProgreso
import com.zhuchii.anies.data.model.aDominio
import com.zhuchii.anies.data.room.AniEsDatabase
import com.zhuchii.anies.data.room.EntidadProgreso
import com.zhuchii.anies.scraper.model.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Progreso de reproduccion por episodio. La UI todavia no lo escribe (eso llega
 * con el player de F5); la tabla, DAOs y este repositorio quedan listos desde F3.
 */
class ProgresoRepository(
    private val db: AniEsDatabase = AppDb.obtener(),
) {
    private val dao = db.progresoDao()

    fun observarProgreso(source: Source, slug: String): Flow<List<AnimeProgreso>> =
        dao.observarAnime(source.name, slug).map { lista -> lista.map(EntidadProgreso::aDominio) }

    suspend fun obtener(source: Source, slug: String, episodio: Int): AnimeProgreso? =
        dao.obtener(source.name, slug, episodio)?.aDominio()

    suspend fun guardar(source: Source, slug: String, episodio: Int, posicionMs: Long, duracionMs: Long) {
        dao.guardar(
            EntidadProgreso(
                source = source.name,
                slug = slug,
                episodio = episodio,
                posicionMs = posicionMs,
                duracionMs = duracionMs,
                actualizadoAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun eliminar(source: Source, slug: String, episodio: Int) =
        dao.eliminar(source.name, slug, episodio)
}