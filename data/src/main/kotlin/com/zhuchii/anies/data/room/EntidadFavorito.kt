package com.zhuchii.anies.data.room

import androidx.room.Entity

/**
 * Anime guardado como favorito. `source` se guarda como el nombre del enum
 * (`Source.name`) y se mapea a `Source` en la capa de repositorio.
 */
@Entity(tableName = "favoritos", primaryKeys = ["source", "slug"])
data class EntidadFavorito(
    val source: String,
    val slug: String,
    val titulo: String,
    val coverUrl: String?,
    val createdAt: Long,
)