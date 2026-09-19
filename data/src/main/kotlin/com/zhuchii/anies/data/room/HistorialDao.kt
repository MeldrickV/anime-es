package com.zhuchii.anies.data.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HistorialDao {

    @Query("SELECT * FROM historial ORDER BY ultimaVisualizacionAt DESC")
    fun observarTodos(): Flow<List<EntidadHistorial>>

    /** Inserta o refresca (misma source+slug): mueve el anime al tope del historial. */
    @Upsert
    suspend fun insertar(historial: EntidadHistorial)

    @Query("DELETE FROM historial WHERE source = :source AND slug = :slug")
    suspend fun eliminar(source: String, slug: String)

    @Query("DELETE FROM historial")
    suspend fun limpiar()
}