package com.zhuchii.anies.ui.biblioteca

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuchii.anies.data.FavoritoRepository
import com.zhuchii.anies.scraper.model.AnimeSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Observa los favoritos persistidos en Room y permite quitarlos desde la lista. */
class BibliotecaViewModel(
    private val favoritos: FavoritoRepository = FavoritoRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(BibliotecaUiState())
    val uiState: StateFlow<BibliotecaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            favoritos.observarFavoritos().collect { lista ->
                _uiState.value = BibliotecaUiState(
                    cargando = false,
                    animes = lista.map { favorito ->
                        AnimeSummary(favorito.source, favorito.slug, favorito.titulo, favorito.coverUrl)
                    },
                )
            }
        }
    }

    fun quitar(anime: AnimeSummary) {
        viewModelScope.launch { favoritos.quitar(anime.source, anime.slug) }
    }
}