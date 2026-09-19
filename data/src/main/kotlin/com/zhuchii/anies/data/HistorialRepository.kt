package com.zhuchii.anies.data

import com.zhuchii.anies.data.model.AnimeHistorial
import com.zhuchii.anies.data.model.aDominio
import com.zhuchii.anies.data.room.AniEsDatabase
import com.zhuchii.anies.data.room.EntidadHistorial
import com.zhuchii.anies.scraper.model.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Historial de reproduccion: lo mas reciente primero. */
class HistorialRepository(
    private val db: AniEsDatabase = AppDb.obtener(),
) {
    private val dao = db.historialDao()

    fun observarHistorial(): Flow<List<AnimeHistorial>> =
        dao.observarTodos().map { lista -> lista.map(EntidadHistorial::aDominio) }

    /** Registra una visita; re-visitar el mismo anime mueve el registro al tope. */
    suspend fun registrar(source: Source, slug: String, titulo: String, coverUrl: String?) {
        dao.insertar(
            EntidadHistorial(
                source = source.name,
                slug = slug,
                titulo = titulo,
                coverUrl = coverUrl,
                ultimaVisualizacionAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun eliminar(source: Source, slug: String) = dao.eliminar(source.name, slug)

    suspend fun limpiar() = dao.limpiar()
}