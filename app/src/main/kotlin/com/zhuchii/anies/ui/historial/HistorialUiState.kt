package com.zhuchii.anies.ui.historial

import com.zhuchii.anies.scraper.model.AnimeSummary

/** Estado del historial: entradas recientes primero + estado de la importacion
 *  del history.json del CLI (F6). */
data class HistorialUiState(
    val cargando: Boolean = true,
    val animes: List<AnimeSummary> = emptyList(),
    val importando: Boolean = false,
    val mensaje: String? = null,
)