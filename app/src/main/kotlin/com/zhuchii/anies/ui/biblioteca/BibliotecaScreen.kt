package com.zhuchii.anies.ui.biblioteca

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.ui.components.Cargando
import com.zhuchii.anies.ui.components.FilaAnime
import com.zhuchii.anies.ui.components.PantallaVacia

/** Biblioteca: favoritos guardados en Room. Click abre detalle; la X lo quita. */
@Composable
fun BibliotecaScreen(
    onAnimeClick: (AnimeSummary) -> Unit,
    viewModel: BibliotecaViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Biblioteca",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(12.dp))

        when {
            uiState.cargando -> Box(Modifier.fillMaxWidth().weight(1f)) {
                Cargando(Modifier.align(Alignment.Center))
            }

            uiState.animes.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f)) {
                PantallaVacia(
                    mensaje = "Sin favoritos todavia.\nAbre un anime y toca el corazon para guardarlo.",
                    icono = Icons.Filled.Favorite,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> LazyColumn(Modifier.weight(1f)) {
                items(
                    items = uiState.animes,
                    key = { "${it.source.name}|${it.slug}" },
                ) { anime ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilaAnime(
                            anime = anime,
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.quitar(anime) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Quitar de la biblioteca",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}