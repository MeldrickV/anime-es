package com.zhuchii.anies.data.room

import androidx.room.Entity

/**
 * Progreso de reproduccion de un episodio. Se escribe cuando exista el player
 * (F5); desde F3 queda lista la tabla, las DAOs y el repositorio.
 */
@Entity(tableName = "progreso", primaryKeys = ["source", "slug", "episodio"])
data class EntidadProgreso(
    val source: String,
    val slug: String,
    val episodio: Int,
    val posicionMs: Long,
    val duracionMs: Long,
    val actualizadoAt: Long,
)