package com.zhuchii.anies.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhuchii.anies.data.BusquedaRepository
import com.zhuchii.anies.data.HistorialRepository
import com.zhuchii.anies.data.ImportadorHistorial
import com.zhuchii.anies.data.ProgresoRepository
import com.zhuchii.anies.scraper.AnimeFlvScraper
import com.zhuchii.anies.scraper.HistoryJsonParser
import com.zhuchii.anies.scraper.JKanimeScraper
import com.zhuchii.anies.scraper.model.AnimeSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Observa el historial persistido en Room, permite borrarlo y sincronizar el
 *  history.json del CLI (F6). */
class HistorialViewModel(
    private val historial: HistorialRepository = HistorialRepository(),
) : ViewModel() {

    private val importador = ImportadorHistorial(
        busqueda = BusquedaRepository(AnimeFlvScraper(), JKanimeScraper()),
        progreso = ProgresoRepository(),
        historial = historial,
    )

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historial.observarHistorial().collect { lista ->
                _uiState.update {
                    it.copy(
                        cargando = false,
                        animes = lista.map { entrada ->
                            AnimeSummary(
                                entrada.source,
                                entrada.slug,
                                entrada.titulo,
                                entrada.coverUrl,
                            )
                        },
                    )
                }
            }
        }
    }

    fun eliminar(anime: AnimeSummary) {
        viewModelScope.launch { historial.eliminar(anime.source, anime.slug) }
    }

    fun limpiar() {
        viewModelScope.launch { historial.limpiar() }
    }

    /** Parsea y sincroniza el contenido del history.json elegido (SAF). */
    fun importar(contenido: String) {
        val entradas = HistoryJsonParser.parsear(contenido)
        if (entradas.isEmpty()) {
            _uiState.update {
                it.copy(importando = false, mensaje = "El archivo no contiene un history.json valido.")
            }
            return
        }
        _uiState.update { it.copy(importando = true, mensaje = null) }
        viewModelScope.launch {
            val resultado = importador.importar(entradas)
            _uiState.update { it.copy(importando = false, mensaje = resultado.mensaje) }
        }
    }
}