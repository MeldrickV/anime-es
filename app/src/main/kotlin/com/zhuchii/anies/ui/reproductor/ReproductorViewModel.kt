package com.zhuchii.anies.ui.reproductor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuchii.anies.data.BusquedaRepository
import com.zhuchii.anies.data.ProgresoRepository
import com.zhuchii.anies.scraper.AnimeFlvScraper
import com.zhuchii.anies.scraper.JKanimeScraper
import com.zhuchii.anies.scraper.model.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Resuelve la URL directa del video (F5, port del CLI) y persiste el progreso
 * del episodio en Room ([ProgresoRepository]) mientras se reproduce.
 */
class ReproductorViewModel(
    private val source: Source,
    private val slug: String,
    private val cap: String,
    private val repository: BusquedaRepository = BusquedaRepository(
        AnimeFlvScraper(),
        JKanimeScraper(),
    ),
    private val progreso: ProgresoRepository = ProgresoRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReproductorUiState>(ReproductorUiState.Resolviendo)
    val uiState: StateFlow<ReproductorUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _uiState.value = ReproductorUiState.Resolviendo
        viewModelScope.launch {
            val resultado = capturar { repository.video(source, slug, cap) }
            _uiState.value = resultado.fold(
                onSuccess = { ReproductorUiState.Listo(it) },
                onFailure = { ReproductorUiState.Error(it.message ?: it::class.simpleName ?: "Error") },
            )
        }
    }

    /** Guarda la posicion del episodio ("pelicula" se registra como cap 1, igual que el CLI). */
    fun guardarProgreso(posicionMs: Long, duracionMs: Long) {
        viewModelScope.launch {
            try {
                progreso.guardar(source, slug, cap.toIntOrNull() ?: 1, posicionMs, duracionMs)
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                // El progreso no debe tumbar la reproduccion.
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