package com.zhuchii.anies.ui.episodios

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

/** Carga la lista de episodios de un anime en una fuente (F4). */
class EpisodiosViewModel(
    private val source: Source,
    private val slug: String,
    private val repository: BusquedaRepository = BusquedaRepository(
        AnimeFlvScraper(),
        JKanimeScraper(),
    ),
) : ViewModel() {

    private val _uiState = MutableStateFlow<EpisodiosUiState>(EpisodiosUiState.Cargando)
    val uiState: StateFlow<EpisodiosUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _uiState.value = EpisodiosUiState.Cargando
        viewModelScope.launch {
            val resultado = capturar { repository.episodios(source, slug) }
            _uiState.value = resultado.fold(
                onSuccess = { EpisodiosUiState.Listo(it) },
                onFailure = { EpisodiosUiState.Error(it.message ?: it::class.simpleName ?: "Error") },
            )
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