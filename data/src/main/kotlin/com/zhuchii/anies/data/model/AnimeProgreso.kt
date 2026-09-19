package com.zhuchii.anies.data.model

import com.zhuchii.anies.scraper.model.Source

/** Progreso de reproduccion de un episodio (se escribe desde el player, F5). */
data class AnimeProgreso(
    val source: Source,
    val slug: String,
    val episodio: Int,
    val posicionMs: Long,
    val duracionMs: Long,
    val actualizadoAt: Long,
)