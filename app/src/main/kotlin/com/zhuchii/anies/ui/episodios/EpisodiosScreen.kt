package com.zhuchii.anies.ui.episodios

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhuchii.anies.scraper.model.Episodio
import com.zhuchii.anies.scraper.model.Source

/** Lista de episodios de un anime en una fuente (F4). */
@Composable
fun EpisodiosScreen(
    source: Source,
    slug: String,
    titulo: String,
    onBack: () -> Unit,
    onEpisodioClick: (String) -> Unit,
    viewModel: EpisodiosViewModel = viewModel { EpisodiosViewModel(source, slug) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                text = titulo.ifBlank { "Episodios" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        when (val estado = uiState) {
            EpisodiosUiState.Cargando -> Box(Modifier.weight(1f).fillMaxWidth()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            is EpisodiosUiState.Error -> Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.align(Alignment.Center)) {
                    Text(
                        text = "No se pudieron cargar los episodios: ${estado.mensaje}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::cargar) { Text("Reintentar") }
                }
            }

            is EpisodiosUiState.Listo -> LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                items(estado.episodios, key = { it.numero }) { episodio ->
                    CeldaEpisodio(
                        episodio = episodio,
                        visto = episodio.numero in estado.vistos,
                        onClick = { onEpisodioClick(episodio.numero) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CeldaEpisodio(episodio: Episodio, visto: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = if (visto) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .aspectRatio(1.4f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = episodio.numero,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (visto) {
                Spacer(Modifier.size(4.dp))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Visto",
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}