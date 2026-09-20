# AGENTS.md — Ani-es Android (contexto de trabajo para agentes)

> Guia maestra del port. LEER COMPLETA antes de tocar cualquier cosa.
> El port TOMA decisiones ya cerradas con el usuario (seccion 4): no reabrir
> decisiones salvo que haya evidencia nueva de que rompe el primer CI.

## 1. Que es esto

`ani-es` es un CLI en Bash que baja y reproduce anime desde DOS fuentes
gratuitas. Este repo es el **port nativo a Android** con UI grafica (Jetpack
Compose): la misma logica de extraccion, pero con thumbnails, descripciones,
tags, biblioteca/favoritos, historial y badges de episodios vistos.

- Paquete/aplicacion: `com.zhuchii.anies` (app `Ani-es`).
- Repo base (fuente de verdad del scraping): `/home/meldrickv/ani-es/`.
  El archivo `ani-es` (VERSION 1.4.1) es el script Bash original.
- Repo NUEVO: el `ani-es` original no se modifica para este port.

## 2. Arquitectura del port

Modulos (Gradle multiplataforma):

| Modulo | Tipo | Rol |
|---|---|---|
| `:app` | Android app / Compose | UI (Inicio, Explorar, Biblioteca), navegacion, Media3 (player) |
| `:core:scraper` | Kotlin JVM puro | Logica de extraccion AnimeFLV + J-Kanime (OkHttp/JSoup), sin Android |
| `:data` | Android lib / Room | F3: biblioteca/favoritos, historial, progreso (Room 2.8.2 + KSP) |

Flujo de datos:

```
UI (Compose) -> ViewModel -> Repository  -> :core:scraper (OkHttp) -> fuentes
                                  ^
                                  |-- :data / Room (estado persistente)
                            -> Media3 ExoPlayer (streaming con headers)
```

## 3. Reglas de oro

1. **NUNCA compilar/testear en la maquina local.** El PC es un AMD Ryzen 3
   2200U con 3.3 GiB de RAM y Ubuntu 22.04.5 LTS; un build de Gradle lo
   congelaria. Todo build/test/lint corre SOLO en GitHub Actions (CI).
   En local solo se puede: leer, editar, `git`, y validar YAML con python3.
2. **Todo cambio se valida con el CI**, y el CI es el revisor numero uno:
   si el primer commit del scaffolding no pasa
   `testDebugUnitTest + lintDebug + assembleDebug`, ajustar la matriz de
   versiones (AGP/Gradle/Kotlin/BOM) antes de avanzar.
3. **La fuente de verdad del scraping es el script Bash** de
   `/home/meldrickv/ani-es/ani-es`. Cualquier extraccion se porta 1:1 de ahi
   (pipelines, regex, endpoints, referer, user-agent). No inventar flujos.
4. **Historico de usuario SEGURO**: el history.json del CLI pasa a Room de
   forma migratable. No hay peligro de perdida; es simple lectura.
5. **Ligereza**: dependencias minimas y justificadas. Nada de inyeccion de
   dependencias, nada de frameworks de UI alternativos a Compose.
   Convencional commits SIEMPRE: `feat:`, `fix:`, `docs:`, `refactor:`, etc.
6. **Solo Depura el CI cuando falla**: cada fix del CI se documenta aqui.

## 4. Decisiones aprobadas por el usuario (NO reabrir)

- Kotlin + Jetpack Compose (Material 3). Nada de XML/Views ni Flutter.
- Player: **Media3/ExoPlayer interno** con OkHttpDataSource + headers
  (Referer/User-Agent) para pelo de los 403 de mp4upload. La reproduccion
  ocurre en el movil del usuario.
- Fixtures de las fuentes: **capturadas en CI** (GitHub Actions) y luego
  incluidas en el repo para tests offline deterministas (Fase 1).
- Repo local sin remoto aun; el remoto de GitHub lo crea el usuario despues.
- Skills/agente de IA: **DENTRO del repo** (carpeta `.opencode/`), no globales.
- El primer CI valida la matriz de versiones; mientras tanto NO construir en local.
- Fases del roadmap (no saltarse): F0 scaffolding -> F1 busqueda real ->
  F2 detalles (cover+desc+tags) -> F3 Room (favs/historial/progreso) ->
  F4 lista de episodios -> F5 Player Media3 -> F6 badges no vistos ->
  F7 estabilizar/pulir. Alpha = F1-F4, Beta = F5-F6, Estable = F7 con `app:debug`.

## 5. Matriz de versiones (VALIDADOR = primer CI verde)

Matriz conservadora de la era estable (2025) elegida por compatibilidad de
metadata: Kotlin 2.2.x NO puede consumir librerias compiladas con Kotlin 2.3+,
asi que el BOM de Compose se fija en la era 1.9.x compilada con Kotlin 2.2.

| Componente | Version | Nota |
|---|---|---|
| Gradle (wrapper) | 8.13 | wrapper OFICIAL ya en el repo |
| AGP | 8.13.0 | |
| Kotlin (+plugin Compose, Serialization) | 2.2.21 | metadata liberada |
| KSP | 2.2.21-2.0.4 | se activa con `:data` (F3) |
| JDK | 17 (temurin) | en CI y toolchains |
| compileSdk / targetSdk | 36 | |
| minSdk | 26 | Android 8.0+ (mismo rango usuario) |
| Compose BOM | 2025.10.00 | linea 1.9.x, Kotlin 2.2 |
| core-ktx | 1.16.0 | |
| activity-compose | 1.10.1 | |
| lifecycle (runtime/viewmodel-compose) | 2.9.4 | |
| navigation-compose | 2.9.0 | |
| Media3 | 1.8.0 | exoplayer + exoplayer-hls + datasource-okhttp |
| OkHttp | 4.12.0 | |
| JSoup | 1.18.3 | |
| Coil 3 | 3.2.0 | `coil-compose` + `coil-network-okhttp` |
| kotlinx-coroutines | 1.10.2 | |
| kotlinx-serialization | 1.8.1 | |
| Room | 2.8.2 | se activa con `:data` (F3) |
| JUnit | 4.13.2 | |

Ruta de upgrade documentada (NO ejecutar de golpe):
AGP 9.x + Gradle 9.x + compileSdk 37 + BOM 2026.08.00 + Kotlin embebido
(compose compiler). Al activarla revisar compatibilidad de metadata otra vez.

## 6. CI / Workflows

- `.github/workflows/ci.yml`: en cada push/PR a main. Jobs:
  `test` (JDK 17, `testDebugUnitTest` + `lintDebug`) y `build-apk`
  (push a main solamente, `assembleDebug` -> artifact `app-debug`).
  Con `gradle/actions/setup-gradle@v4` (cache), `--parallel --build-cache
  --no-daemon`, y reportes subidos como artifact (incluso si falla).
  Los FIXES del CI se documentan en la seccion 9.
- `.github/workflows/version.yml`: bump de version automatico y limpio.
  `feat:` -> MINOR++, `fix:` -> PATCH++, `BREAKING` -> MAJOR++ en
  `anies.versionName` de `gradle.properties`. Hace amend sobre el ultimo
  commit de main (no crea commits de mas), tag `vX.Y.Z` y force push.
  Depender de como se integre el trabajo: squash/merge.

## 7. Estructura de archivos

```
ani-es-android/
├── settings.gradle.kts          # :app + :core:scraper + :data
├── build.gradle.kts             # plugins raiz (solo los que se usan YA)
├── gradle.properties            # jvmargs, flags + anies.versionName/Code
├── gradle/libs.versions.toml    # catalog de versiones (matriz seccion 5)
├── gradle/wrapper/              # wrapper oficial gradle 8.13 (no regenerar)
├── core/scraper/                # Kotlin JVM puro, tests deterministas
├── data/                        # F3: Room (favoritos, historial, progreso)
└── app/                         # Compose: MainActivity, theme, pantallas
```

## 8. Fuentes y scraping (port del Bash)

Doble fuente, misma logica que el script original:

- **AnimeFLV** (`AnimeFlvScraper`): GET `/animes?buscar=<query>&pag=N` en
  `https://vww.animeflv.one` (espejo exacto del script; la busqueda NO es
  `/browse?q=`) + regex de busqueda, luego endpoint `/flv` con `data-encrypt`
  (`data-id-episodio` en hex como fallback) + `SRC_REFERER` = host del embed,
  validacion de _playable_ contra respuestas HTTP 200/206 de partes de video.
- **J-Kanime** (`JKanimeScraper`): **busqueda = GET simple**
  `https://jkanime.net/buscar/<query_con_guiones>/` + regex
  `<h5><a href="...">Titulo</a></h5>` (slug del href, ultimo segmento).
  El flujo csrf (_token) + POST aplica SOLO a episodios via
  `/ajax/episodes/<id>/`; decode del player (jkplayer/jk.php) para enlaces
  directos.

Derivados F2 (NO estan en el Bash; usan las MISMA base/endpoints del script,
decision cerrada con el usuario):

- **Detalle** en ambas fuentes (cover + sinopsis + tags): AnimeFLV usa
  `GET $AF_BASE/anime/<slug>` (la misma pagina que baja el script para
  `data-id`/`data-sl`/`eps`); selectores: cover `og:image` o `data-src` de
  `.info-l`, sinopsis `div.tx > p`, generos `ul.gn`, episodios `data-ep`.
  J-Kanime usa `GET https://jkanime.net/<slug>/`: cover `class="movpic"`,
  sinopsis `p.scroll` de `.anime_info`, generos/estado/episodios en
  `ul` de `.anime_data`.
- **Portada (home)** en ambas fuentes: AnimeFLV `GET $AF_BASE/` secciones
  "Animes en Emision" -> populares y "Ultimos episodios agregados" ->
  recientes (episodio nuevo mapeado a su anime, sin numero). J-Kanime
  `GET https://jkanime.net/` secciones "Top animes" -> populares y "Animes
  recientes" -> recientes. Cada item lleva su cover.

Constantes clave que respetar (mismas que el Bash): User-Agent de navegador,
timeouts cortos, `SRC_REFERER` fijado al host del embed al descargar el video.
Ver `core/scraper/src/main/kotlin/com/zhuchii/anies/scraper/*` para la forma
exacta de cada pipeline (Fase 1 = mover aqui toda la logica).

Nota: `excepciones.json` de fichas con calidad preferida se descarga de
`https://zhuchii.github.io/ani-es/excepciones.json` (mismo sitio que ya usa el
CLI). Se incorporara como cache local en F3/Room.

## 9. Bitacora de decisiones y fixes del CI (actualizar en cada cambio)

- `2026-09-19` F0: matriz de versiones fijada en la era conservadora
  (BOM 2025.10.00 con Kotlin 2.2.21 para garantizar compatibilidad de
  metadata; Media3 1.8.0, Coil 3.2.0 - mismas razones).
- `2026-09-19` F0: usesCleartextTraffic=true en el manifest por los hosts de
  streaming que aun sirven por http; revisar por host cuando exista el player.
- `2026-09-19` F0: version centralizada en gradle.properties para que
  version.yml pueda hacer sed sin tocar build.gradle.kts.
- `2026-09-19` F1: corregida la busqueda de AnimeFLV a `/animes?buscar=`
  en `vww.animeflv.one` (el /browse?q= del primer port no existe en el
  script); J-Kanime se busca con GET `/buscar/<q>/` + regex `<h5><a href>`
  (csrf+POST es solo de episodios, F4). Fixtures capturados en CI y
  workflow `capture-fixtures` (ver skill `android-testing-fixtures`).
- `2026-09-19` F1 CI-fix: primer CI real fallo en `:core:scraper:compileKotlin`
  con `Unresolved reference 'model'`: `AnimeSummary.kt` vivia en `model/`
  pero declaraba el package padre `com.zhuchii.anies.scraper`. Se declara el
  package correcto `com.zhuchii.anies.scraper.model` y se anaden imports de
  `Source`/`AnimeSummary` en parsers y tests que hasta ahora dependian del
  package compartido.
- `2026-09-19` F1 CI-fix version.yml: `new_version` se emitia SIEMPRE (fuera
  del if/else de bump), asi que un `chore:`/`docs:` amend+taggeaba el commit
  igualmente (force-push que rompio el fast-forward de la rama). Ahora solo se
  emite cuando hay bump (feat/fix/BREAKING); los commits del bot con `[skip ci]`
  y los chore/docs quedan intactos. Nota: los force-push del bot (GITHUB_TOKEN)
  no re-disparan workflows, asi que no hay bucle de versionado.
- `2026-09-19` F1 CI-fix version.yml (2): el paso "Read version actual" hacia
  `echo "msg=$(git log --pretty=%B)" >> $GITHUB_OUTPUT`, que FALLA con
  commits multi-linea (`Invalid format`). El mensaje ya no pasa por
  GITHUB_OUTPUT: se lee con `git log` dentro del paso "Calcular nueva
  version".
- `2026-09-19` F2: home (populares/recientes) y detalle (cover/sinopsis/tags)
  de AMBAS fuentes: `AnimeFlvParser`/`JKanimeParser` reciben `parseHome` y
  `parseDetalle`; los scrapers exponen `home()` y `detalle(slug)`. Decisiones
  de mapeo: AnimeFLV "populares" = seccion "Animes en Emision" (el espejo no
  tiene seccion "Populares" propia); "recientes" = "Ultimos episodios
  agregados" (episodios nuevos mapeados a su anime quitando el numero).
  J-Kanime "populares" = "Top animes", "recientes" = "Animes recientes".
  Fixtures nuevos: `animeflv/{home,detalle}.html`, `jkanime/{home,detalle}.html`.
- `2026-09-19` F2 app: barra de navegacion inferior con 4 destinos
  (Busqueda, Plataformas, Biblioteca, Historial) en `PrincipalScreen`
  (NavHost + NavigationBar); Plataformas elige fuente con SegmentedButton y
  muestra pestañas Populares/Recientes/Buscar (ViewModel por pantalla, sin
  DI); resultados de busqueda y listas del home son clickables y abren
  `DetalleScreen` (ruta `detalle/{source}/{slug}?titulo=`). Biblioteca e
  Historial son placeholder hasta F3. Coil 3 se configura como singleton en
  `AnieEsApp` implementando `SingletonImageLoader.Factory` con
  `OkHttpNetworkFetcherFactory` + User-Agent de navegador (covers de
  animeflv/jkdesa). Se agrego `material-icons-core` EXPLICITO: material3
  1.4.0 (BOM 2025.10) ya no lo trae transitivo y `Icons.Filled.*` no
  compilaria sin el.
- `2026-09-19` F2 CI-fix capture-fixtures: el run fallaba con 0 jobs
  (workflow ni siquiera arrancaba). Causa: `- name: Capturar detalle ... (F2:
  cover/sinopsis/tags)` con `:` + espacio dentro del scalar sin comillas =
  YAML invalido ("mapping values are not allowed here"). Los name con
  `:` se entrecomillan.
- `2026-09-19` F2 CI-fix: `DetalleScreen.kt` no compilaba con "Smart cast to
  'String' is impossible, because 'description' is a public API property
  declared in different module": smart cast solo aplica dentro del mismo
  modulo, y `AnimeDetalle` vive en `:core:scraper`. La sinopsis se captura
  en un `val` local antes del `if`.
- `2026-09-19` F3: modulo `:data` (Android, Room 2.8.2 + KSP
  2.2.21-2.0.4). Entidades `favoritos`/`historial`/`progreso` (source como
  nombre del enum, mapeada a `Source` en repositorios) + DAOs + `AppDb`
  singleton inicializado en `AnieEsApp.onCreate` (sin DI). `:data` depende
  de `:core:scraper` SOLO por el enum `Source` (no duplicarlo).
  Repositorios `Favorito`/`Historial`/`Progreso`. Tests de DAO con
  Robolectric 4.14.1 (`@Config(sdk=[35])`, base en memoria).
- `2026-09-19` F3 app: Biblioteca e Historial reales (Room): listas
  observables, click -> detalle, X para quitar, "Borrar todo" en historial.
  Detalle registra la visita en historial al cargar y alterna favorito con
  el corazon (Favorite/FavoriteBorder de material-icons-core). El progreso
  por episodio queda en tablas/repos listos; lo escribira el player (F5).
- `2026-09-19` F3 CI-fix: reversion de icons-core — `ListAlt` NO esta en el
  set core (48 iconos); solo `Favorite`, `FavoriteBorder`, `List` (automirrored),
  `Close`, etc. Usar solo iconos del core o anadir material-icons-extended.
- `2026-09-19` F5: resolucion de video port 1:1 del CLI. `VideoFuente(url,
  referer, hls)` + `FlvServidor`/`JkServidor`. AnimeFLV `video(slug, cap)`:
  GET `/ver/<slug>-<cap>` -> `data-encrypt` (fallback hex de `"$dataId-$cap"`
  con el `data-id` del detalle), `POST /flv` (FormBody `acc=opt&i=<enc>` +
  `X-Requested-With: XMLHttpRequest`, Referer=capurl), `parseFlvServidores`
  (12 servidores, dedupe y mp4upload primero), decodificarHex del embed, GET
  del embed con Referer=capurl, `extraerUrlVideo` (primera `\.mp4|\.m3u8`),
  `esPlayable` Range `bytes=0-0` (200/206) con Referer = origin del embed.
  J-Kanime `video(slug, cap)`: iframe `jkplayer/um|umv` (resuelto contra
  capurl si es relativo) -> su pagina trae `video: { url: '...' }` (HLS
  directo, m3u8 firma con st/e que expiran), fallback `/jk.php`, fallback
  `var servers = [...]` -> servidor "Mediafire" -> `remote` base64 -> pagina
  `https://download...`. Fixtures live capturados en CI: `animeflv/{ver,flv}.html`,
  `jkanime/{episodio,player}.html` (4 pasos nuevos en capture-fixtures).
  Tests de parsers + scrapers (MockWebServer: flujo real AnimeFLV con referer
  localhost, fallback J-Kanime Mediafire). App: `PlayerScreen` con ExoPlayer +
  `OkHttpDataSource.Factory` (User-Agent navegador + Referer cuando
  `video.referer != null`), `HlsMediaSource`/`ProgressiveMediaSource` segun
  `video.hls`, `PlayerView` (media3-ui, `resizeMode` fit); `ReproductorViewModel`
  con estado Resolviendo/Error/Listo y `guardarProgreso` (cap "pelicula" ->
  episodio 1, igual que CLI) cada 15 s y al salir; pausa en `ON_STOP`.
- `2026-09-19` F5 CI-fixes (4): 1) `String.toHex` usaba `CharSequence.joinToString`
  que NO existe (String no es Iterable) -> `map { }.joinToString("")`;
  2) `PlayerView.setShowController` no existe en la API de media3-ui 1.8
  (solo showController()/hideController()) -> quitar, default useController
  ya es true; 3) `Modifier.weight` se usaba dentro de `ReproductorPlayback`
  fuera del scope del Column (unresolved) -> envolver el reproductor en un
  `Box(Modifier.weight(1f))` en la pantalla; 4) lint `UnsafeOptInUsageError`
  de Media3: `@OptIn(UnstableApi::class)` NO lo silencia (es un check de lint
  error-level, no del compilador) -> `@SuppressLint("UnsafeOptInUsageError")`
  en `ReproductorPlayback`.
- `2026-09-19` F5: el ciclo de fix contra version.yml queda asi: version.yml
  amendea SIEMPRE el ultimo commit con `[skip ci]` + force-push nada mas
  pusheas, asi que cada fix exige `git fetch origin && git rebase origin/main`
  antes de volver a pushear (el parche previo sale como "ya en upstream").
- `2026-09-19` F6: badges de episodios vistos: `EpisodiosViewModel` observa
  `ProgresoRepository` y `EpisodiosUiState.Listo` gana `vistos: Set<String>`.
  Numero "pelicula" se guarda como episodio 1 (igual que CLI) pero se mapea
  de vuelta a "pelicula" cuando la lista contiene ese numero. La celda pintada
  usa surfaceVariant + check (icons-core).
- `2026-09-19` F6: sincronizacion del history.json del CLI via SAF
  (`OpenDocument`). `HistoryJsonParser` (en `:core:scraper`, kotlinx-
  serialization): mapa `titulo -> {last_cap, progress, source}`, lectura
  tolerante (ignoreUnknownKeys, JSON invalido -> vacio, sin source -> fuera)
  + `progresoToMs("HH:MM:SS")`. `ImportadorHistorial` (app): como el JSON
  guarda TITULO y Room usa slug, resuelve cada titulo con la busqueda real de
  su fuente y solo toma el primer resultado cuyo titulo coincida normalizado;
  escribe `historial` + `progreso` (los badges de F6). Los titulos sin
  coincidencia se reportan, no se borran. Historias vacias o fuente desconocida
  se saltan. Boton "Importar history.json" en HistorialScreen (outlined + Add).
- `2026-09-19` F7: pulido = modo oscuro + lint limpio + estados vacios.
  Compose ya alternaba el colorScheme por sistema; falta el tema base:
  `values-night/themes.xml` (parent `android:Theme.Material.NoActionBar`, fondo
  `background_dark`, system bars transparentes) para que el arranque no
  destelle en claro y las system bars sean oscuras. `app/lint.xml` ignora SOLO
  los avisos decision-driven — `NewerVersionAvailable`,
  `AndroidGradlePluginVersion`, `GradleDependency`, `OldTargetApi` (matriz de
  versiones congelada, sec. 5) y `HardcodedText` (espanol por diseno, sin
  i18n); el resto del lint mantiene la exigencia y no se anade ningun dep.
  Estado vacio de populares/recientes en Plataforma centrado con icono (mismo
  patron que Biblioteca/Historial). F7 cerrado con su primer CI verde; la
  fase Estable se cumple: `assembleDebug` produce `app-debug.apk` (v0.9.0).
- `2026-09-19` F8: pantalla completa horizontal en el player sin depender del
  bloqueo de rotacion: `configChanges="orientation|screenSize|keyboardHidden"`
  en MainActivity (evita recrear/ reiniciar el video), boton nativo de Media3
  (`setFullscreenButtonClickListener` estable + `setFullscreenButtonState`
  UnstableApi bajo el `@OptIn`/`@SuppressLint` ya presentes) que fuerza
  `SCREEN_ORIENTATION_LANDSCAPE`, oculta cabecera + system bars (inmersivo
  `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`) y al salir vuelve al
  `SCREEN_ORIENTATION_PORTRAIT` (decidido con el usuario). El click del boton
  no captura `enPantallaCompleta` del factory (quedaria stale): se lee con
  `rememberUpdatedState`.
- `2026-09-19` F8: episodios en modo lista (LazyColumn, fila = numero en
  bulto + badge visto) EMBEBIDOS en el detalle: se elimina `EpisodiosScreen`
  y la ruta `episodios/{source}/{slug}`; `DetalleScreen` monta su propio
  `EpisodiosViewModel` (key `episodios-$source-$slug`) y un solo LazyColumn
  pinta cabecera + episodios; FAB `Empezar`/`Continuar` salta al primer
  episodio no visto (vistos de F6). Click de episodio -> player directo.
- `2026-09-19` F8: AnimeFLV dejo de reproducirse porque los hosts de los
  embeds (bysesukior, dooodster, ...) ahora sirven una SPA de JS que resuelve
  el .m3u8/.mp4 por XHR/fetch: el HTML NO trae URL directa, asique
  `extraerUrlVideo` (y el CLI, misma regex) devuelve null. Fix SOLO en la
  app: `ResolverEmbedWebView` (injectado en `AnimeFlvScraper.video` como
  `resolverEmbed`, default null = ruta CLI para los tests JVM) ejecuta el
  embed en un WebView oculto reutilizado (resumeTimers, cookies,
  User-Agent navegador), captura en `shouldInterceptRequest` la primera
  peticion .m3u8/.mp4, con fallback de `video.currentSrc`, Referer=capurl al
  cargar y timeout 12s (Runnable reutilizable). Es una excepcion documentada
  a la regla "1:1 del Bash": la fuente cambio y el port se adapta.
- `2026-09-19` F8 CI-fix: 1) `companion.appInstance = this` compila? NO
  (`Unresolved reference 'companion'`) -> asignar el campo de la companion
  directamente; 2) `Handler.postDelayed(...)` devuelve `Boolean`, no el
  Runnable: guardar el `Runnable` para poder `removeCallbacks` en la
  cancelacion.
- `2026-09-19` F8: el remoto ya existe (MeldrickV/anime-es) y CI corre con
  DPT/git + API de GitHub desde esta maquina (PAT del usuario). El item F7
  "Pendiente: crear el remoto" queda obsoleto; el flujo es push -> CI ->
  version.yml amendea con [skip ci] + force-push (re-fetch/rebase antes de
  cada push local, reset --hard al final).
- `2026-09-20` fix v0.9.3 (diagnosticado desde logcat del movil, sin test
  interactivo): 1) crash de AnimeFLV al cargar un capitulo = `reportar()` en
  `ResolverEmbedWebView` se invocaba desde `shouldInterceptRequest`, que WebView
  llama en su thread de fondo (`ThreadPoolForeg`); ahi tocaba
  `view.stopLoading()` y `cont.resume()` fuera del main looper ->
  `RuntimeException: A WebView method was called on thread...` (dialogo de
  crash; el primer caso murio sin dialogo). Fix: todo el reporte ("encontrada" +
  stopLoading + resume) pasa por `Handler(Looper.getMainLooper()).post`, con la
  guarda de doble-resume dentro del post. 2) fullscreen reiniciaba el video y
  no agrandaba nada: en `PlayerScreen` el `ReproductorPlayback` se montaba en
  ramas if/else distintas (Box vs Column) y el `remember(player)` se desechaba
  al alternar -> ExoPlayer nuevo desde cero. Fix: posicion de composicion UNICA
  para el reproductor dentro de un Box, y la cabecera se quita con
  `padding(top = 0/56dp)` segun `enPantallaCompleta` (sin desmontar el player).
  Hallazgos de doblaje (revisados): J-Kanime SI tiene seccion latino
  (`jkanime.net/categoria/latino/`, ~30 series, e.g. Hunter x Hunter 2011
  "Latino" fan-dub, Beyblade, Inazuma Eleven) y el player de episodio ofrece
  audio "Espanol latino" en esas series. AnimeFLV (vww.animeflv.one) NO separa
  doblaje en el catalogo: todo figura "Sub español latino"; el audio latino,
  cuando existe, va por servidor dentro del mismo episodio (por eso el CLI
  prefiere mp4upload). Sin cambios de codigo por ahora, solo nota.

## 10. Skills del proyecto (carpeta .opencode/skills)

Skills curadas para este port (NO instalar colecciones masivas genericas;
ver blog oficial de Android de ago 2026). Cargarlas por nombre cuando el
trabajo toque su area:

- `compose-best-practices` (UI Compose, state/effects, M3)
- `kotlin-concurrency-flow` (coroutines/Flow)
- `android-media-exoplayer` (Media3 + OkHttpDataSource headers)
- `android-networking-scraper` (OkHttp/JSoup + referer/UA de las fuentes)
- `android-local-storage-room` (Room, KSP)
- `android-testing-fixtures` (tests JVM + fixtures capturadas en CI)
- `android-build-ci-first` (Gradle, GH Actions, CI-only)

Sources autorizadas usadas para construir estas skills: blog pit de Android,
chrisbanes/skills, noloman/Android-AI-skills, Google Android skills.

## 11. Trabajo pendiente (roadmap)

- [x] F0 scaffolding (este repo) + AGENTS con todo el contexto.
- [x] F1: buscar real AnimeFLV+JKanime en `:core:scraper`, UI de busqueda.
- [x] F2: detalle con cover/descripcion/tags + fixtures en CI; navegacion
      (Plataformas con populares/recientes/buscar, Biblioteca, Historial).
- [x] F3: `:data` Room (favs, historial, progreso) + activar KSP.
- [x] F4: lista de episodios por anime.
- [x] F5: player Media3 con headers (Referer/User-Agent) en el movil.
- [x] F6: badges de no vistos + sincronizar historial desde history.json.
- [x] F7: pulido (navegacion, dark mode, estados vacios, Lint 0 deps).
- [x] F8: fullscreen horizontal del player (sin depender del bloqueo de
      rotacion, vuelve a vertical), episodios en detalle en modo lista con
      FAB Empezar/Continuar, y resolucion de embeds JS-SPA de AnimeFLV via
      WebView oculto (adaptacion al cambio de los hosts de video).
- [x] Crear el remoto de GitHub (hecho: MeldrickV/anime-es) y conectar;
      CI por push + version.yml autobump.