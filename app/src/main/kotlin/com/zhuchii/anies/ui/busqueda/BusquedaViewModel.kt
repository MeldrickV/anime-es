package com.zhuchii.anies.ui.busqueda

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuchii.anies.data.BusquedaRepository
import com.zhuchii.anies.scraper.AnimeFlvScraper
import com.zhuchii.anies.scraper.JKanimeScraper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BusquedaViewModel(
    private val repository: BusquedaRepository = BusquedaRepository(
        AnimeFlvScraper(),
        JKanimeScraper(),
    ),
) : ViewModel() {

    private val _uiState = MutableStateFlow<BusquedaUiState>(BusquedaUiState.Idle)
    val uiState: StateFlow<BusquedaUiState> = _uiState.asStateFlow()

    var query by mutableStateOf("")
        private set

    fun onQueryChange(nueva: String) {
        query = nueva
    }

    fun buscar() {
        val q = query.trim()
        if (q.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = BusquedaUiState.Cargando
            val resultado = repository.buscar(q)
            _uiState.value = BusquedaUiState.Resultado(resultado.animes, resultado.errores)
        }
    }
}