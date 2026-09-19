package com.zhuchii.anies.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuchii.anies.data.BusquedaRepository
import com.zhuchii.anies.data.FavoritoRepository
import com.zhuchii.anies.data.HistorialRepository
import com.zhuchii.anies.scraper.AnimeFlvScraper
import com.zhuchii.anies.scraper.JKanimeScraper
import com.zhuchii.anies.scraper.model.AnimeDetalle
import com.zhuchii.anies.scraper.model.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Detalle (F2) + estado local del anime en F3: alternar favorito (corazon) y
 * registrar la visita en el historial cuando se carga el detalle.
 */
class DetalleViewModel(
    private val source: Source,
    private val slug: String,
    private val titulo: String = "",
    private val repository: BusquedaRepository = BusquedaRepository(
        AnimeFlvScraper(),
        JKanimeScraper(),
    ),
    private val favoritos: FavoritoRepository = FavoritoRepository(),
    private val historial: HistorialRepository = HistorialRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    private val _esFavorito = MutableStateFlow(false)
    val esFavorito: StateFlow<Boolean> = _esFavorito.asStateFlow()

    init {
        cargar()
        viewModelScope.launch {
            favoritos.observarEsFavorito(source, slug).collect { _esFavorito.value = it }
        }
    }

    fun cargar() {
        _uiState.value = DetalleUiState.Cargando
        viewModelScope.launch {
            val resultado = capturar { repository.detalle(source, slug) }
            _uiState.value = resultado.fold(
                onSuccess = { DetalleUiState.Listo(it) },
                onFailure = { DetalleUiState.Error(it.message ?: it::class.simpleName ?: "Error") },
            )
            resultado.getOrNull()?.let { registrarVisita(it) }
        }
    }

    fun onToggleFavorito() {
        val detalle = (_uiState.value as? DetalleUiState.Listo)?.detalle ?: return
        viewModelScope.launch {
            val tituloReal = titulo.ifBlank { detalle.title }
            if (_esFavorito.value) {
                favoritos.quitar(source, slug)
            } else {
                favoritos.agregar(source, slug, tituloReal, detalle.coverUrl)
            }
        }
    }

    private fun registrarVisita(detalle: AnimeDetalle) {
        viewModelScope.launch {
            try {
                historial.registrar(source, slug, titulo.ifBlank { detalle.title }, detalle.coverUrl)
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                // Fallar el historial no debe romper la visualizacion del detalle.
            }
        }
    }

    private suspend fun <T> capturar(bloque: suspend () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        Result.failure(t)
    }
}