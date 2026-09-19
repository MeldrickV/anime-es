package com.zhuchii.anies.ui.busqueda

import com.zhuchii.anies.scraper.model.AnimeSummary

/** Estado de la pantalla de busqueda (un solo source of truth en el ViewModel). */
sealed interface BusquedaUiState {
    data object Idle : BusquedaUiState

    data object Cargando : BusquedaUiState

    data class Resultado(
        val animes: List<AnimeSummary>,
        val errores: List<String>,
    ) : BusquedaUiState
}