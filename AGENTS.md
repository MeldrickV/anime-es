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
| `:data` | FASE 3 | Room (biblioteca, historial, favs, progreso) — se POSTERGA a Fase 3 |

Flujo de datos:

```
UI (Compose) -> ViewModel -> Repository  -> :core:scraper (OkHttp) -> fuentes
                                  ^
                                  |-- :data / Room (Fase 3, estado persistente)
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
├── settings.gradle.kts          # :app + :core:scraper (:data en F3)
├── build.gradle.kts             # plugins raiz (solo los que se usan YA)
├── gradle.properties            # jvmargs, flags + anies.versionName/Code
├── gradle/libs.versions.toml    # catalog de versiones (matriz seccion 5)
├── gradle/wrapper/              # wrapper oficial gradle 8.13 (no regenerar)
├── core/scraper/                # Kotlin JVM puro, tests deterministas
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
  en `vww.animeflv.one` (el port usaba `/browse?q=` que NO existe en el
  script); J-Kanime se busca con GET `/buscar/<q>/` + regex `<h5><a href>`
  (csrf+POST es solo de episodios, F4). Fixtures reales capturados en
  commit F1 (CI-only) y workflow `capture-fixtures` documentado en
  `.opencode/skills/android-testing-fixtures`.
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
- [ ] F1: buscar real AnimeFLV+JKanime en `:core:scraper`, UI de busqueda.
- [ ] F2: detalle con cover/descripcion/tags + fixtures en CI.
- [ ] F3: `:data` Room (favs, historial, progreso) + activar KSP.
- [ ] F4: lista de episodios por anime.
- [ ] F5: player Media3 con headers (Referer/User-Agent) en el movil.
- [ ] F6: badges de no vistos + sincronizar historial desde history.json.
- [ ] F7: pulido (navegacion, dark mode, estados vacios, Lint 0 deps).
- [ ] Crear el remoto de GitHub (lo hace el usuario) y conectar.