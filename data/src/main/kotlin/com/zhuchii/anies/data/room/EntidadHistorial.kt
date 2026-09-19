package com.zhuchii.anies.data.room

import androidx.room.Entity

/** Anime visitado/visualizado. `ultimaVisualizacionAt` marca el orden del historial. */
@Entity(tableName = "historial", primaryKeys = ["source", "slug"])
data class EntidadHistorial(
    val source: String,
    val slug: String,
    val titulo: String,
    val coverUrl: String?,
    val ultimaVisualizacionAt: Long,
)