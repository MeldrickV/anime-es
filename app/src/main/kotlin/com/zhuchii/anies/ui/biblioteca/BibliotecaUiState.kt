package com.zhuchii.anies.ui.biblioteca

import com.zhuchii.anies.scraper.model.AnimeSummary

/** Estado de la biblioteca: la lista de favoritos del usuario. */
data class BibliotecaUiState(
    val cargando: Boolean = true,
    val animes: List<AnimeSummary> = emptyList(),
)