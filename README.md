# ani-es-android

Port Android de **ani-es**, el CLI de anime con doble fuente
(AnimeFLV + J-Kanime). Misma logica de extraccion que el script Bash
original (`/home/meldrickv/ani-es/ani-es`), pero con UI grafica:

- Busqueda, portadas, descripciones y tags.
- Barra de navegacion: Busqueda global, Plataformas (AnimeFLV/J-Kanime con
  mas populares/recientes y buscar por fuente), Biblioteca e Historial.
- Biblioteca/favoritos, historial y progreso por episodio.
- Reproductor interno (Media3/ExoPlayer) con headers Referer/User-Agent.

## Estado

| Fase | Contenido | Estado |
|---|---|---|
| F0 | Scaffolding: modulos `:app` + `:core:scraper`, CI, versionado auto | hecho |
| F1 | Busqueda real contra ambas fuentes | hecho |
| F2 | Navegacion (busqueda/plataformas/biblioteca/historial), portada home y detalle (cover, descripcion, tags) | hecho |
| F3 | `:data` Room | pendiente |
| F4 | Episodios | pendiente |
| F5 | Player | pendiente |
| F6 | Badges no-vistos | pendiente |
| F7 | Estable | pendiente |

## Construir

Este repo **no se compila en local** (maquina de desarrollo con 3.3 GiB de
RAM). Todo build/test/lint corre en GitHub Actions:

- `./gradlew testDebugUnitTest` -> tests JVM
- `./gradlew lintDebug` -> lint
- `./gradlew assembleDebug` -> APK (artifact `app-debug`)

Consulta `AGENTS.md` para el detalle completo: arquitectura, matriz de
versiones, reglas de scraping, y bitacora de decisiones.