package com.zhuchii.anies.ui.plataforma

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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.scraper.model.Source
import com.zhuchii.anies.ui.components.FilaAnime

/** Contenedor stateful: fuente seleccionada y sus pestañas de contenido. */
@Composable
fun PlataformaScreen(
    onAnimeClick: (AnimeSummary) -> Unit,
    viewModel: PlataformaViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PantallaPlataforma(
        uiState = uiState,
        onFuente = viewModel::onSeleccionarFuente,
        onReintentarHome = viewModel::cargarHome,
        onPestana = viewModel::onCambiarPestana,
        onQueryChange = viewModel::onQueryChange,
        onBuscar = viewModel::buscar,
        onAnimeClick = onAnimeClick,
    )
}

@Composable
fun PantallaPlataforma(
    uiState: PlataformaUiState,
    onFuente: (Source) -> Unit,
    onReintentarHome: () -> Unit,
    onPestana: (PestanaPlataforma) -> Unit,
    onQueryChange: (String) -> Unit,
    onBuscar: () -> Unit,
    onAnimeClick: (AnimeSummary) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Plataformas",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Source.entries.forEachIndexed { index, fuente ->
                    SegmentedButton(
                        selected = uiState.fuente == fuente,
                        onClick = { onFuente(fuente) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = Source.entries.size,
                        ),
                    ) {
                        Text(fuente.label)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        TabRow(selectedTabIndex = uiState.pestana.ordinal) {
            PestanaPlataforma.entries.forEach { pestana ->
                Tab(
                    selected = uiState.pestana == pestana,
                    onClick = { onPestana(pestana) },
                    text = { Text(pestana.etiqueta) },
                )
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
            when (uiState.pestana) {
                PestanaPlataforma.POPULARES ->
                    ListaHome(
                        cargando = uiState.cargandoHome,
                        animes = uiState.populares,
                        error = uiState.errorHome,
                        onReintentar = onReintentarHome,
                        onAnimeClick = onAnimeClick,
                    )

                PestanaPlataforma.RECIENTES ->
                    ListaHome(
                        cargando = uiState.cargandoHome,
                        animes = uiState.recientes,
                        error = uiState.errorHome,
                        onReintentar = onReintentarHome,
                        onAnimeClick = onAnimeClick,
                    )

                PestanaPlataforma.BUSCAR -> PestanaBuscar(
                    uiState = uiState,
                    onQueryChange = onQueryChange,
                    onBuscar = onBuscar,
                    onAnimeClick = onAnimeClick,
                )
            }
        }
    }
}

@Composable
private fun ListaHome(
    cargando: Boolean,
    animes: List<AnimeSummary>,
    error: String?,
    onReintentar: () -> Unit,
    onAnimeClick: (AnimeSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        when {
            cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            error != null -> Column(Modifier.align(Alignment.Center)) {
                Text(
                    text = "No se pudo cargar la portada: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onReintentar) { Text("Reintentar") }
            }

            animes.isEmpty() -> Column(Modifier.align(Alignment.Center)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Sin animes en esta lista.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(
                    items = animes,
                    key = { "${it.source.name}|${it.slug}" },
                ) { anime ->
                    FilaAnime(anime, onClick = { onAnimeClick(anime) })
                }
            }
        }
    }
}

@Composable
private fun PestanaBuscar(
    uiState: PlataformaUiState,
    onQueryChange: (String) -> Unit,
    onBuscar: () -> Unit,
    onAnimeClick: (AnimeSummary) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            label = { Text("Buscar en ${uiState.fuente.label}") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onBuscar() }),
        )
        Button(
            onClick = onBuscar,
            enabled = uiState.query.isNotBlank() && !uiState.buscando,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Buscar")
        }

        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                uiState.buscando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                uiState.errorBuscar != null -> Text(
                    text = "No se pudo buscar: ${uiState.errorBuscar}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Center),
                )

                uiState.resultados.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                    items(
                        items = uiState.resultados,
                        key = { "${it.source.name}|${it.slug}" },
                    ) { anime ->
                        FilaAnime(anime, onClick = { onAnimeClick(anime) })
                    }
                }

                else -> Text(
                    text = "Sin resultados para ${uiState.fuente.label}.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}