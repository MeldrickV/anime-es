package com.zhuchii.anies.ui.reproductor

import com.zhuchii.anies.scraper.model.VideoFuente

/** Estado de la resolucion del video de un episodio (F5). */
sealed interface ReproductorUiState {
    data object Resolviendo : ReproductorUiState
    data class Error(val mensaje: String) : ReproductorUiState
    data class Listo(val video: VideoFuente) : ReproductorUiState
}