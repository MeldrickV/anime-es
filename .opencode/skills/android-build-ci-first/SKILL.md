---
description: >
  Build y CI del port: como tocar Gradle/catalog y los workflows de GitHub
  Actions SIN compilar en local (todo en CI), versionado automatico y reglas
  de cache/artifacts. Aplica a cambios de build, workflows o dependencias.
---

# Android build & CI-first (port ani-es)

Fuentes: noloman `android-build-infra` / `android-ci-cd` + Google Android
skills. La maquina local es un Ryzen 3 / 3.3 GiB — **nunca builds locales**.

## Reglas de oro de build

1. NO ejecutar gradle en esta maquina. Todo cambio de build/dependencia solo
   se valida con el CI; por eso cada commit de build debe ser conservador.
2. Las versiones viven en `gradle/libs.versions.toml` (catalog). No quemar
   versiones en `build.gradle.kts` salvo SDK/target.
3. Matriz de versiones fijada (AGENTS.md seccion 5). Si el primer commit F0
   del CI falla por resolucion/metadata, se acota el fix: primero bajar el
   BOM/Coil/Media3 a versiones de la era Kotlin 2.2 (NUNCA mezclar KGP 2.2
   con librerias compiladas en 2.3+ sin probar), y se documenta en la bitacora.
4. Wrapper oficial 8.13 ya commiteado; no regenerarlo (y no crearlo tampoco
   en local: el `gradle-wrapper.jar` se descargo de la fuente oficial).

## Workflows actuales

- `ci.yml`: on push/PR a main. Job `test` (temurin 17, `testDebugUnitTest`
  + `lintDebug`, artifact de reportes SIEMPRE) y job `build-apk` (solo push
  a main, `assembleDebug`, artifact `app-debug`, 30 dias).
  Flags de Gradle en CI (memoria): `--parallel --build-cache --no-daemon`;
  cache de `gradle/actions/setup-gradle@v4`.
- `version.yml`: bump automatico y limpio (amend sobre el commit reciente de
  main + tag), con sed sobre `anies.versionName/versionCode` de
  `gradle.properties` (por eso la version no vive en build.gradle.kts).

## Buenas practicas al tocar CI

- Concurrency group por ref + `cancel-in-progress: true` (ya configurado).
- Filtros de path para NO correr CI entero en cambios de docs: evaluar
  `paths-ignore` si el repo crece, pero mantener el job `test` el dia que
  un docs-only no deba gastar runner.
- Artefactos: `if-no-files-found: error` en el APK. Reportes de test/lint con
  `retention-days` cortos (7) para no agotar el storage del repo.
- Siempre `chmod +x gradlew` como primer paso (el bit de ejecutar se pierde
  en algunos clones).
- Cambios de Workflows YAML: validar localmente con `python3 -c
  "import yaml,sys; yaml.safe_load(open('...'))"` antes de commitear.

## Versionado semantico del port

- `feat:` -> MINOR++, `fix:` -> PATCH++, texto "BREAKING CHANGE" -> MAJOR++.
- `[skip ci]` en el mensaje (e.g. commits del bot de fixtures) para que
  version.yml ignore el bump. Un `chore:` de fixtures NO debe bumpear.