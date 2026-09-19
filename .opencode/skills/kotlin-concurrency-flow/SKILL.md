---
description: >
  Reglas de coroutines y Flow para el port: dispatchers, cancelacion,
  concurrency de scrapers, StateFlow en ViewModels y manejo de errores.
  Aplica a ViewModels y a la logica async de :app y :core:scraper.
---

# Kotlin concurrency & flow (port ani-es)

Fuentes: chrisbanes/skills (coroutines y flow). Kotlin
2.2.21 + coroutines 1.10.2.

## Reglas base

- `suspend` para todo IO; **no** usar callbacks. OkHttp se usa via
  `suspendCancellableCoroutine` o lanzando dentro de `withContext(IO)`.
- En `:core:scraper` (JVM puro) los `scrapers` reciben el `OkHttpClient`
  inyectado por constructor (testeable); lanzan con `withContext(Dispatchers.IO)`.
- En ViewModels: `viewModelScope.launch` + `stateIn(viewModelScope, ...)`.
  Preferir `MutableStateFlow` privado + expuesto como `StateFlow` inmutable.
- **Concurrency**: cada llamada a una fuente es independiente. Para la
  busqueda en ambas fuentes (Inicio) usar `coroutineScope` + `async`/`awaitAll`
  con supervisor (`supervisorScope`) para que el fallo de J-Kanime no tumbe a
  AnimeFLV. Resultado parcial + estado de error granular.
- `Dispatchers.Main.immediate` para eventos rápidos; `Default` para parseo
  pesado (regex/JSoup), `IO` para red.

## Cancelacion y lifecycles

- Operaciones largas (descarga de episodio, parseo grande) deben ser
  cancelables: checkear `isActive` en loops y preferir `CoroutineScope` del
  UI (viewModelScope) — nunca `GlobalScope`.
- Respeta el lifecycle en la UI: recolectar flows con `collectAsStateWithLifecycle`
  (lifecycle-runtime-compose 2.9.4 ya en el proyecto).
- `Job` de reproducción: si el usuario cierra el player, se cancela el scope
  del reproductor -> Media3 hace release. Nunca leaks del ExoPlayer.

## Errores

- `Result<T>` o UiState con `error`/`message` para la UI; excepciones de red
  controladas (HttpException, timeouts) con reintento explicito en UI.
- En `:core:scraper`: excepcion propia `ScraperException` (fuente + razon)
  para no filtrar detalles internos a la UI.

## Tests (JVM)

- Tests de la logica async con `runTest` de kotlinx-coroutines-test
  (añadir a `:core:scraper` cuando hagan falta) — fakeOkHttpClient con
  `MockWebServer` (okhttp-mockwebserver 4.12.0) para los fixtures de F1.
- Estado "correcto": no usar `Thread.sleep` ni `delay` real en tests.