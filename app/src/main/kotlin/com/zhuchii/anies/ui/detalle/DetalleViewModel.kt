package com.zhuchii.anies.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuchii.anies.data.BusquedaRepository
import com.zhuchii.anies.scraper.AnimeFlvScraper
import com.zhuchii.anies.scraper.JKanimeScraper
import com.zhuchii.anies.scraper.model.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Carga el detalle (F2) de un anime concreto de una fuente. */
class DetalleViewModel(
    private val source: Source,
    private val slug: String,
    private val repository: BusquedaRepository = BusquedaRepository(
        AnimeFlvScraper(),
        JKanimeScraper(),
    ),
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _uiState.value = DetalleUiState.Cargando
        viewModelScope.launch {
            _uiState.value = try {
                DetalleUiState.Listo(repository.detalle(source, slug))
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                DetalleUiState.Error(t.message ?: t::class.simpleName ?: "Error")
            }
        }
    }
}