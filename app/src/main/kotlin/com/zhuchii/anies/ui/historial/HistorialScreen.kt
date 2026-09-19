package com.zhuchii.anies.ui.historial

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.ui.components.FilaAnime

/** Historial: lo ultimo visto en Room. Click abre detalle; X borra, "Borrar
 *  todo" limpia, e "Importar history.json" sincroniza el historial del CLI
 *  via SAF (F6). */
@Composable
fun HistorialScreen(
    onAnimeClick: (AnimeSummary) -> Unit,
    viewModel: HistorialViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val abrirArchivo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val contenido = runCatching {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            }.getOrNull()
            if (contenido != null) viewModel.importar(contenido)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Historial",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (!uiState.cargando && uiState.animes.isNotEmpty()) {
                TextButton(onClick = viewModel::limpiar) {
                    Text("Borrar todo")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { abrirArchivo.launch(arrayOf("application/json")) },
                enabled = !uiState.importando,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text("Importar history.json")
            }
            if (uiState.importando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        uiState.mensaje?.let { mensaje ->
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        when {
            uiState.cargando -> Box(Modifier.fillMaxWidth().weight(1f)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            uiState.animes.isEmpty() && uiState.mensaje == null -> Box(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.align(Alignment.Center)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Sin historial aun.\nVisita el detalle de un anime o importa tu history.json del CLI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> LazyColumn(Modifier.weight(1f)) {
                items(
                    items = uiState.animes,
                    key = { "${it.source.name}|${it.slug}" },
                ) { anime ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilaAnime(
                            anime = anime,
                            onClick = { onAnimeClick(anime) },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.eliminar(anime) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Borrar del historial",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}