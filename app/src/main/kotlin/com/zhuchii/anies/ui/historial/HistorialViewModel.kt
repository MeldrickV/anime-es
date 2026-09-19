package com.zhuchii.anies.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuchii.anies.data.HistorialRepository
import com.zhuchii.anies.scraper.model.AnimeSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Observa el historial persistido en Room y permite borrarlo total o parcialmente. */
class HistorialViewModel(
    private val historial: HistorialRepository = HistorialRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historial.observarHistorial().collect { lista ->
                _uiState.value = HistorialUiState(
                    cargando = false,
                    animes = lista.map { entrada ->
                        AnimeSummary(entrada.source, entrada.slug, entrada.titulo, entrada.coverUrl)
                    },
                )
            }
        }
    }

    fun eliminar(anime: AnimeSummary) {
        viewModelScope.launch { historial.eliminar(anime.source, anime.slug) }
    }

    fun limpiar() {
        viewModelScope.launch { historial.limpiar() }
    }
}