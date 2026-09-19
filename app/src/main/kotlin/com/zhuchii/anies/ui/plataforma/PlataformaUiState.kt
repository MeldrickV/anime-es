package com.zhuchii.anies.ui.plataforma

import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.scraper.model.Source

/** Pestañas de una plataforma: portadas (populares/recientes) y buscar en esa fuente. */
enum class PestanaPlataforma(val etiqueta: String) {
    POPULARES("Populares"),
    RECIENTES("Recientes"),
    BUSCAR("Buscar"),
}

/** Estado de la pantalla de plataforma (selector de fuente + pestañas). */
data class PlataformaUiState(
    val fuente: Source = Source.ANIME_FLV,
    val cargandoHome: Boolean = false,
    val populares: List<AnimeSummary> = emptyList(),
    val recientes: List<AnimeSummary> = emptyList(),
    val errorHome: String? = null,
    val pestana: PestanaPlataforma = PestanaPlataforma.POPULARES,
    val query: String = "",
    val buscando: Boolean = false,
    val resultados: List<AnimeSummary> = emptyList(),
    val errorBuscar: String? = null,
)