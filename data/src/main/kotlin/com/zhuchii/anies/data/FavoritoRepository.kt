package com.zhuchii.anies.data

import com.zhuchii.anies.data.model.AnimeFavorito
import com.zhuchii.anies.data.model.aDominio
import com.zhuchii.anies.data.room.AniEsDatabase
import com.zhuchii.anies.data.room.EntidadFavorito
import com.zhuchii.anies.scraper.model.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Favoritos del usuario: observar, comprobar y alternar. */
class FavoritoRepository(
    private val db: AniEsDatabase = AppDb.obtener(),
) {
    private val dao = db.favoritoDao()

    fun observarFavoritos(): Flow<List<AnimeFavorito>> =
        dao.observarTodos().map { lista -> lista.map(EntidadFavorito::aDominio) }

    fun observarEsFavorito(source: Source, slug: String): Flow<Boolean> =
        dao.observarEsFavorito(source.name, slug)

    suspend fun esFavorito(source: Source, slug: String): Boolean =
        dao.contar(source.name, slug) > 0

    suspend fun agregar(source: Source, slug: String, titulo: String, coverUrl: String?) {
        dao.insertar(
            EntidadFavorito(
                source = source.name,
                slug = slug,
                titulo = titulo,
                coverUrl = coverUrl,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun quitar(source: Source, slug: String) = dao.eliminar(source.name, slug)

    suspend fun limpiar() = dao.limpiar()
}