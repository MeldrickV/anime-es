package com.zhuchii.anies.data.model

import com.zhuchii.anies.scraper.model.Source

/** Entrada del historial: anime visitado/visualizado. */
data class AnimeHistorial(
    val source: Source,
    val slug: String,
    val titulo: String,
    val coverUrl: String?,
    val ultimaVisualizacionAt: Long,
)