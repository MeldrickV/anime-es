package com.zhuchii.anies.data.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgresoDao {

    @Query("SELECT * FROM progreso WHERE source = :source AND slug = :slug ORDER BY episodio ASC")
    fun observarAnime(source: String, slug: String): Flow<List<EntidadProgreso>>

    @Query("SELECT * FROM progreso WHERE source = :source AND slug = :slug AND episodio = :episodio")
    suspend fun obtener(source: String, slug: String, episodio: Int): EntidadProgreso?

    /** Inserta o actualiza el progreso de un episodio (misma source+slug+episodio). */
    @Upsert
    suspend fun guardar(progreso: EntidadProgreso)

    @Query("DELETE FROM progreso WHERE source = :source AND slug = :slug AND episodio = :episodio")
    suspend fun eliminar(source: String, slug: String, episodio: Int)
}