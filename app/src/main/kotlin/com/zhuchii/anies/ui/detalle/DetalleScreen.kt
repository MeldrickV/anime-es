package com.zhuchii.anies.ui.detalle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.zhuchii.anies.scraper.model.AnimeDetalle
import com.zhuchii.anies.scraper.model.Source
import com.zhuchii.anies.ui.episodios.EpisodiosUiState
import com.zhuchii.anies.ui.episodios.EpisodiosViewModel
import com.zhuchii.anies.ui.episodios.FilaEpisodio

/** Detalle de un anime (F2) con los episodios integrados (F8, todo en una
 *  pantalla): cover, sinopsis, tags, favorito/historial (F3) y la lista de
 *  episodios con badges de visto (F6). El FAB salta al siguiente no visto. */
@Composable
fun DetalleScreen(
    source: Source,
    slug: String,
    titulo: String,
    onBack: () -> Unit,
    onEpisodioClick: (String) -> Unit,
    viewModel: DetalleViewModel = viewModel { DetalleViewModel(source, slug, titulo) },
    episodiosViewModel: EpisodiosViewModel =
        viewModel(key = "episodios-$source-$slug") { EpisodiosViewModel(source, slug) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val esFavorito by viewModel.esFavorito.collectAsStateWithLifecycle()
    val episodios by episodiosViewModel.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                    )
                }
                Text(
                    text = titulo.ifBlank { "Detalle" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            when (val estado = uiState) {
                DetalleUiState.Cargando -> Box(Modifier.weight(1f).fillMaxWidth()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is DetalleUiState.Error -> Box(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.align(Alignment.Center)) {
                        Text(
                            text = "No se pudo cargar el detalle: ${estado.mensaje}",
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = viewModel::cargar) { Text("Reintentar") }
                    }
                }

                is DetalleUiState.Listo -> ContenidoDetalle(
                    detalle = estado.detalle,
                    titulo = titulo.ifBlank { estado.detalle.title },
                    esFavorito = esFavorito,
                    onToggleFavorito = viewModel::onToggleFavorito,
                    episodios = episodios,
                    onReintentarEpisodios = episodiosViewModel::cargar,
                    onEpisodioClick = onEpisodioClick,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }

        val listo = episodios as? EpisodiosUiState.Listo
        val siguiente = listo?.let { lista ->
            lista.episodios.firstOrNull { it.numero !in lista.vistos }
                ?: lista.episodios.lastOrNull()
        }
        if (uiState is DetalleUiState.Listo && siguiente != null) {
            val hayaVistos = listo?.vistos?.isNotEmpty() == true
            ExtendedFloatingActionButton(
                onClick = { onEpisodioClick(siguiente.numero) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                text = { Text(if (hayaVistos) "Continuar" else "Empezar") },
            )
        }
    }
}

@Composable
private fun ContenidoDetalle(
    detalle: AnimeDetalle,
    titulo: String,
    esFavorito: Boolean,
    onToggleFavorito: () -> Unit,
    episodios: EpisodiosUiState,
    onReintentarEpisodios: () -> Unit,
    onEpisodioClick: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                if (detalle.coverUrl != null) {
                    AsyncImage(
                        model = detalle.coverUrl,
                        contentDescription = titulo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onToggleFavorito) {
                        Icon(
                            imageVector = if (esFavorito) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Filled.FavoriteBorder
                            },
                            contentDescription = if (esFavorito) {
                                "Quitar de favoritos"
                            } else {
                                "Añadir a favoritos"
                            },
                            tint = if (esFavorito) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                val etiquetas = listOfNotNull(
                    detalle.estado,
                    detalle.episodeCount.takeIf { it > 0 }?.let { "$it episodios" },
                )
                if (etiquetas.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = etiquetas.joinToString(" • "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (detalle.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = detalle.tags.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }

                val descripcion = detalle.description
                if (descripcion != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        item {
            Text(
                text = "Episodios",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        when (episodios) {
            EpisodiosUiState.Cargando -> item {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Cargando episodios…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            is EpisodiosUiState.Error -> item {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = "No se pudieron cargar los episodios: ${episodios.mensaje}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onReintentarEpisodios) { Text("Reintentar") }
                }
            }

            is EpisodiosUiState.Listo -> items(
                items = episodios.episodios,
                key = { it.numero },
            ) { episodio ->
                FilaEpisodio(
                    episodio = episodio,
                    visto = episodio.numero in episodios.vistos,
                    onClick = { onEpisodioClick(episodio.numero) },
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}