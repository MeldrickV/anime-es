package com.zhuchii.anies.ui.historial

import com.zhuchii.anies.scraper.model.AnimeSummary

/** Estado del historial: entradas de reproduccion mas recientes primero. */
data class HistorialUiState(
    val cargando: Boolean = true,
    val animes: List<AnimeSummary> = emptyList(),
)