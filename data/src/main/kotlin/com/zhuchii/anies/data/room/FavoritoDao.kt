package com.zhuchii.anies.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritoDao {

    @Query("SELECT * FROM favoritos ORDER BY createdAt DESC")
    fun observarTodos(): Flow<List<EntidadFavorito>>

    @Query("SELECT EXISTS(SELECT 1 FROM favoritos WHERE source = :source AND slug = :slug)")
    fun observarEsFavorito(source: String, slug: String): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM favoritos WHERE source = :source AND slug = :slug")
    suspend fun contar(source: String, slug: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(favorito: EntidadFavorito)

    @Query("DELETE FROM favoritos WHERE source = :source AND slug = :slug")
    suspend fun eliminar(source: String, slug: String)

    @Query("DELETE FROM favoritos")
    suspend fun limpiar()
}