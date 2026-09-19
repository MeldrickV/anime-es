package com.zhuchii.anies.ui.plataforma

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de plataformas: la fuente seleccionada (AnimeFLV o
 * J-Kanime) con sus populares/recientes (home) y una busqueda restringida a
 * esa fuente. Al cambiar de fuente se recarga la portada y se limpia la
 * busqueda anterior.
 */
class PlataformaViewModel(
    private val repository: BusquedaRepository = BusquedaRepository(
        AnimeFlvScraper(),
        JKanimeScraper(),
    ),
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlataformaUiState())
    val uiState: StateFlow<PlataformaUiState> = _uiState.asStateFlow()

    init {
        cargarHome()
    }

    fun onSeleccionarFuente(fuente: Source) {
        if (_uiState.value.fuente == fuente) return
        _uiState.update {
            it.copy(
                fuente = fuente,
                populares = emptyList(),
                recientes = emptyList(),
                errorHome = null,
                pestana = PestanaPlataforma.POPULARES,
                query = "",
                resultados = emptyList(),
                errorBuscar = null,
            )
        }
        cargarHome()
    }

    fun cargarHome() {
        val fuente = _uiState.value.fuente
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoHome = true, errorHome = null) }
            val resultado = capturar { repository.home(fuente) }
            _uiState.update {
                it.copy(
                    cargandoHome = false,
                    populares = resultado.getOrNull()?.populares.orEmpty(),
                    recientes = resultado.getOrNull()?.recientes.orEmpty(),
                    errorHome = resultado.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun onCambiarPestana(pestana: PestanaPlataforma) {
        _uiState.update { it.copy(pestana = pestana) }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun buscar() {
        val fuente = _uiState.value.fuente
        val q = _uiState.value.query.trim()
        if (q.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(buscando = true, errorBuscar = null) }
            val resultado = capturar { repository.buscarEn(fuente, q) }
            _uiState.update {
                it.copy(
                    buscando = false,
                    resultados = resultado.getOrNull().orEmpty(),
                    errorBuscar = resultado.exceptionOrNull()?.message,
                )
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