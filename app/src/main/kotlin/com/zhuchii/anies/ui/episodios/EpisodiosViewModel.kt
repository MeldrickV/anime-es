package com.zhuchii.anies.ui.episodios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuchii.anies.data.BusquedaRepository
import com.zhuchii.anies.data.ProgresoRepository
import com.zhuchii.anies.scraper.AnimeFlvScraper
import com.zhuchii.anies.scraper.JKanimeScraper
import com.zhuchii.anies.scraper.model.Episodio
import com.zhuchii.anies.scraper.model.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Carga la lista de episodios de un anime y observa el progreso para los
 *  badges de "visto" (F6). */
class EpisodiosViewModel(
    private val source: Source,
    private val slug: String,
    private val repository: BusquedaRepository = BusquedaRepository(
        AnimeFlvScraper(),
        JKanimeScraper(),
    ),
    private val progreso: ProgresoRepository = ProgresoRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<EpisodiosUiState>(EpisodiosUiState.Cargando)
    val uiState: StateFlow<EpisodiosUiState> = _uiState.asStateFlow()

    /** Episodios (Int) con progreso registrado del player o del import (F6). */
    private var progresoEpisodios: Set<Int> = emptySet()

    init {
        cargar()
        viewModelScope.launch {
            progreso.observarProgreso(source, slug).collect { lista ->
                progresoEpisodios = lista.map { it.episodio }.toSet()
                val actual = _uiState.value
                if (actual is EpisodiosUiState.Listo) {
                    _uiState.value = actual.copy(vistos = numerosVistos(actual.episodios))
                }
            }
        }
    }

    fun cargar() {
        _uiState.value = EpisodiosUiState.Cargando
        viewModelScope.launch {
            val resultado = capturar { repository.episodios(source, slug) }
            _uiState.value = resultado.fold(
                onSuccess = { EpisodiosUiState.Listo(it, numerosVistos(it)) },
                onFailure = { EpisodiosUiState.Error(it.message ?: it::class.simpleName ?: "Error") },
            )
        }
    }

    /** Convierte los episodios vistos (Int) a los numeros mostrados: las
     *  peliculas se guardan como episodio 1 (igual que el CLI) pero se marcan
     *  con su numero real `pelicula`. */
    private fun numerosVistos(episodios: List<Episodio>): Set<String> {
        val numeros = episodios.map { it.numero }.toSet()
        return buildSet {
            progresoEpisodios.forEach { episodio ->
                add(if (episodio == 1 && numeros.contains("pelicula")) "pelicula" else episodio.toString())
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