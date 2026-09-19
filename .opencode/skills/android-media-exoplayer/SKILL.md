---
description: >
  Media3/ExoPlayer interno para el port: OkHttpDataSource con headers
  (Referer/User-Agent) que quita los 403 de mp4upload, HLS y ciclo de vida
  del ExoPlayer dentro del player de la app. Aplica cuando se construya el
  reproductor (Fase 5).
---

# Android media / ExoPlayer (port ani-es)

Fuentes: Google Android skills (media/ExoPlayer) y noloman
`android-media`. Media3 1.8.0 (exoplayer, exoplayer-hls, datasource-okhttp).

## El problema que resuelve

Los enlaces directos de las fuentes (mp4upload y otras) devuelven **403 sin
headers**: exigen `Referer` correcto y `User-Agent` de navegador. En el CLI
Bash esto se hacia con `curl -e` (referer) a `SRC_REFERER` (el host del
embed). En Android la duplica OkHttpDataSource **con cabeza de pasada
por peticion**.

## Patron correcto

- Construir un `OkHttpDataSource` con un `OkHttpClient` cuyo interceptor
  añade `Referer` al host correcto de la fuente (determinado en `:core:scraper`
  al decodificar el episodio) y el `User-Agent` del script original.
- No usar la `DefaultDataSource` para el alojamiento de video: el flujo es
  `OkHttpDataSource -> DefaultDataSource` (progressive + segmentos HLS).
- `DefaultHttpDataSource` con `setUserAgent`/`setDefaultRequestProperties`
  NO llega en Android: en Media3 se usa OkHttpDataSource justamente.

```kotlin
val src: DataSource.Factory = CacheDataSource.Factory()
    .setCache(simpleCache)
    .setUpstreamDataSourceFactory(
        OkHttpDataSource.Factory(client)
            .setDefaultRequestProperties(mapOf("Referer" to referer))
            .setUserAgent(USER_AGENT)
    )
```

- ExoPlayer con `MediaItem` por URI; `PlayerView`/`Compose` propio que expone
  `ExoPlayer` al Compose player: `ProgressiveMediaSource.Factory(src)` para
  mp4 (fuente principal) y `HlsMediaSource.Factory(src)` si la fuente da m3u8.

## Ciclo de vida (F5)

- El `ExoPlayer` se crea junto al al scope de la pantalla de reproduccion y
  `release()` cuando se sale (remember + DisposableEffect con
  `remember { ExoPlayer.Builder(context) ... }`).
- Pausa/reanuda con el lifecycle de la actividad (onPause -> pause,
  onResume -> play), salvo reproduccion en background no deseada.
- Persistir la posicion (`getCurrentPosition()`) en el ViewModel para
  restaurar al reentrar y guardarla a Room en F3/F6 como progreso del episodio.

## Errores tipicos de la fuente

- 403 por Referer: logs de `Player.Listener.onPlayerError` con identidad del
  error -> mapear a "fuente bloqueada, probar episodio alternativo".
- Netflix/DRM: no aplica (las fuentes son HTTP player/iframe), no meter
  Widevine. Evitar `media3-exoplayer-dash` innecesario: HLS + progressive.
- Dimension del cache: `SimpleCache` en `context.cacheDir` con
  `LeastRecentlyUsedCacheEvictor` para no llenar el telefono.

## Dependencias (ya en el catalog)

`media3-exoplayer`, `media3-exoplayer-hls`, `media3-datasource-okhttp`
(1.8.0). No se requiere `media3-ui` si el player es Compose propio; se puede
añadir si se decide usar `PlayerView` en un AndroidView.