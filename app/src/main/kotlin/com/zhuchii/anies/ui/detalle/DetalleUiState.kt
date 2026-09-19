package com.zhuchii.anies.ui.detalle

import com.zhuchii.anies.scraper.model.AnimeDetalle

/** Estado del detalle de un anime (cover/sinopsis/tags, F2). */
sealed interface DetalleUiState {
    data object Cargando : DetalleUiState

    data class Listo(val detalle: AnimeDetalle) : DetalleUiState

    data class Error(val mensaje: String) : DetalleUiState
}