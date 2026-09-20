package com.zhuchii.anies.ui.busqueda

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.ui.components.Cargando
import com.zhuchii.anies.ui.components.FilaAnime
import com.zhuchii.anies.ui.components.PantallaVacia

/** Contenedor stateful: conecta el ViewModel con la UI stateless. */
@Composable
fun BusquedaScreen(
    onAnimeClick: (AnimeSummary) -> Unit,
    viewModel: BusquedaViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PantallaBusqueda(
        uiState = uiState,
        query = viewModel.query,
        onQueryChange = viewModel::onQueryChange,
        onBuscar = viewModel::buscar,
        onAnimeClick = onAnimeClick,
    )
}

@Composable
fun PantallaBusqueda(
    uiState: BusquedaUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onBuscar: () -> Unit,
    onAnimeClick: (AnimeSummary) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Ani-es",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Buscar anime") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onBuscar() }),
        )

        Button(
            onClick = onBuscar,
            enabled = query.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Buscar")
        }

        Spacer(Modifier.height(16.dp))

        ResultadoBusqueda(uiState, onAnimeClick, Modifier.weight(1f))
    }
}

@Composable
private fun ResultadoBusqueda(
    uiState: BusquedaUiState,
    onAnimeClick: (AnimeSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        when (uiState) {
            BusquedaUiState.Idle -> PantallaVacia(
                mensaje = "Escribe una busqueda para encontrar anime en AnimeFLV y J-Kanime.",
                icono = Icons.Filled.Search,
                modifier = Modifier.align(Alignment.Center),
            )

            BusquedaUiState.Cargando -> Cargando(Modifier.align(Alignment.Center))

            is BusquedaUiState.Resultado -> ContenidoResultado(
                uiState,
                onAnimeClick,
                Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ContenidoResultado(
    estado: BusquedaUiState.Resultado,
    onAnimeClick: (AnimeSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        if (estado.errores.isNotEmpty()) {
            estado.errores.forEach { mensaje ->
                Text(
                    text = "No se pudo consultar $mensaje",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        val resultadosTextos = when {
            estado.animes.isEmpty() && estado.errores.isEmpty() ->
                "Sin resultados para la busqueda."

            estado.animes.isEmpty() ->
                "Todas las fuentes fallaron. Intenta otra vez."

            else -> null
        }

        if (resultadosTextos != null) {
            Text(
                text = resultadosTextos,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    items = estado.animes,
                    key = { "${it.source.name}|${it.slug}" },
                ) { anime ->
                    FilaAnime(
                        anime = anime,
                        onClick = { onAnimeClick(anime) },
                    )
                }
            }
        }
    }
}