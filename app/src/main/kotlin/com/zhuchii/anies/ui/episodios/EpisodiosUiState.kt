package com.zhuchii.anies.ui.episodios

import com.zhuchii.anies.scraper.model.Episodio

/** Estado de la lista de episodios de un anime (F4). */
sealed interface EpisodiosUiState {
    data object Cargando : EpisodiosUiState
    data class Error(val mensaje: String) : EpisodiosUiState
    data class Listo(val episodios: List<Episodio>) : EpisodiosUiState
}