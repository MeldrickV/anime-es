package com.zhuchii.anies.scraper.model

import kotlinx.serialization.Serializable

/** Fuente de donde sale un resultado. Refleja la doble fuente del script original. */
enum class Source(val label: String) {
    J_KANIME("J-Kanime"),
    ANIME_FLV("AnimeFLV"),
}

/** Resumen de un anime tal y como aparece en una lista/busqueda de la fuente. */
@Serializable
data class AnimeSummary(
    val source: Source,
    val slug: String,
    val title: String,
    val coverUrl: String? = null,
    val description: String? = null,
)