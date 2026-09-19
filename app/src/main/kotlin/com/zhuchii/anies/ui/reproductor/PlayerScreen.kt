package com.zhuchii.anies.ui.reproductor

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.zhuchii.anies.scraper.AnimeFlvScraper
import com.zhuchii.anies.scraper.model.Source
import com.zhuchii.anies.scraper.model.VideoFuente
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient

/** Reproductor de un episodio (F5): Media3/ExoPlayer con OkHttpDataSource y
 *  headers (Referer/User-Agent) que quitan los 403 de mp4upload/HLS. */
@Composable
fun PlayerScreen(
    source: Source,
    slug: String,
    cap: String,
    titulo: String,
    onBack: () -> Unit,
    viewModel: ReproductorViewModel = viewModel { ReproductorViewModel(source, slug, cap) },
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
                text = titulo.ifBlank { "Reproductor" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        when (val estado = uiState) {
            ReproductorUiState.Resolviendo -> Box(Modifier.weight(1f).fillMaxWidth()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            is ReproductorUiState.Error -> Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.align(Alignment.Center)) {
                    Text(
                        text = "No se pudo reproducir: ${estado.mensaje}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::cargar) { Text("Reintentar") }
                }
            }

            is ReproductorUiState.Listo -> Box(Modifier.weight(1f)) {
                ReproductorPlayback(
                    video = estado.video,
                    onGuardarProgreso = viewModel::guardarProgreso,
                )
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@OptIn(UnstableApi::class)
@Composable
private fun ReproductorPlayback(
    video: VideoFuente,
    onGuardarProgreso: (Long, Long) -> Unit,
) {
    val context = LocalContext.current
    val player = remember(video.url, video.referer) {
        ExoPlayer.Builder(context).build().apply {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
                .setUserAgent(AnimeFlvScraper.USER_AGENT)
                .setDefaultRequestProperties(
                    buildMap {
                        video.referer?.let { put("Referer", it) }
                    },
                )
            val mediaSource = if (video.hls) {
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(video.url))
            } else {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(video.url))
            }
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }
    var errorText by remember { mutableStateOf<String?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                errorText = "Error de reproduccion: ${error.errorCodeName}"
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        player.addListener(listener)
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            player.removeListener(listener)
            lifecycle.removeObserver(lifecycleObserver)
            onGuardarProgreso(
                player.currentPosition,
                player.duration.takeIf { it != C.TIME_UNSET } ?: 0L,
            )
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            delay(15_000)
            onGuardarProgreso(
                player.currentPosition,
                player.duration.takeIf { it != C.TIME_UNSET } ?: 0L,
            )
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }

    errorText?.let { mensaje ->
        Text(
            text = mensaje,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        )
    }
}