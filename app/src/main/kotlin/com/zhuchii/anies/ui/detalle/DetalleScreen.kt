package com.zhuchii.anies.ui.detalle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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

/** Detalle de un anime: cover, sinopsis y tags (F2) + favorito e historial (F3). */
@Composable
fun DetalleScreen(
    source: Source,
    slug: String,
    titulo: String,
    onBack: () -> Unit,
    viewModel: DetalleViewModel = viewModel { DetalleViewModel(source, slug, titulo) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val esFavorito by viewModel.esFavorito.collectAsStateWithLifecycle()

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
                modifier = Modifier.weight(1f).fillMaxWidth(),
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
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
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
                    imageVector = if (esFavorito) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
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

        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ver episodios (F4)")
        }

        Spacer(Modifier.height(24.dp))
    }
}