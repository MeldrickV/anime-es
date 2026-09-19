package com.zhuchii.anies.ui.busqueda

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.scraper.model.Source
import com.zhuchii.anies.ui.theme.AniEsTheme

/** Contenedor stateful: conecta el ViewModel con la UI stateless. */
@Composable
fun InicioScreen(viewModel: BusquedaViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PantallaBusqueda(
        uiState = uiState,
        query = viewModel.query,
        onQueryChange = viewModel::onQueryChange,
        onBuscar = viewModel::buscar,
    )
}

@Composable
fun PantallaBusqueda(
    uiState: BusquedaUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onBuscar: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

            ResultadoBusqueda(uiState, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResultadoBusqueda(uiState: BusquedaUiState, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        when (uiState) {
            BusquedaUiState.Idle -> Text(
                text = "Escribe una busqueda para encontrar anime en AnimeFLV y J-Kanime.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            BusquedaUiState.Cargando -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )

            is BusquedaUiState.Resultado -> ContenidoResultado(
                uiState,
                Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ContenidoResultado(estado: BusquedaUiState.Resultado, modifier: Modifier = Modifier) {
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
                    AnimeFila(anime)
                }
            }
        }
    }
}

@Composable
private fun AnimeFila(anime: AnimeSummary) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = anime.title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = anime.source.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaBusquedaPreview() {
    AniEsTheme {
        PantallaBusqueda(
            uiState = BusquedaUiState.Resultado(
                animes = listOf(
                    AnimeSummary(Source.J_KANIME, "shingeki-no-kyojin", "Shingeki no Kyojin"),
                    AnimeSummary(Source.ANIME_FLV, "shingeki-no-kyojin", "Shingeki no Kyojin (AnimeFLV)"),
                ),
                errores = emptyList(),
            ),
            query = "shingeki",
            onQueryChange = {},
            onBuscar = {},
        )
    }
}