---
description: >
  Patrones robustos de Jetpack Compose (Material 3) para el port: state vs
  events, remember/derivedStateOf, hoisting, lazy lists, navegacion y tema.
  Aplica cuando se toque cualquier pantalla o componente de :app.
---

# Compose best practices (port ani-es)

Fuentes: blog oficial de Android (recomendacion Magnet/sept 2025) y
chrisbanes/skills. Reglas para que la UI del port sea estable y testeable.

## State y eventos (port de las Fases 1-7)

- **Un solo source of truth** por pantalla: el `ViewModel` expone
  `StateFlow<UiState>`; la UI nunca escribe en el modelo.
- Los `UiEvent`s van de la UI al ViewModel (funciones por evento).
  No "estado mutado desde la composicion".
- `state`, `events` y `stateful/stateless` (Leland Richardson):
  componentes "stateless" para reutilizar/probar; el contenedor "stateful"
  conecta con el ViewModel. En `:app` todo lo reutilizable sera stateless.
- Derivaciones baratas con `remember`/`remember(key)`/`derivedStateOf`.
  `produceState`/`LaunchedEffect` SOLO para arrancar trabajo async.
- Ojo con `remember` y configuracion de fuentes externas: si una pantalla
  depende de algo que cambia (e.g. fuente seleccionada de AnimeFLV o J-Kanime)
  la clave del remember debe incluirla, o forzar `key(...)`/`state m = rememberSaveable`.

## Lazy lists y contenido

- `LazyColumn`/`LazyVerticalGrid` con `items(items, key={})`;
  la `key` debe ser estable (slug del anime). No items sin key en listas con
  badges/historial que cambian en F6.
- Portadas con Coil 3 (`AsyncImage`) y placeholders para que el grid no
  parpadee; el tamaño de la portada se fija en `Modifier.aspectRatio(2/3)`.
- Para estados: `Loading`, `Error` (reintento) y `Empty` como estados del
  UiState, NO pintando condicionales a pelo sin envoltorio.

## Navegacion

- navigation-compose 2.9.0; rutas tipadas y un solo `NavHost`.
  Pantallas previstas: Inicio (busqueda), Explorar, Detalle (slug), Reproducir
  (slug + episodio), Biblioteca.
- Tipos seguros con `kotlinx-serialization` de la 2.9.0 (no strings a mano).

## Tema y M3

- `AniEsTheme` en `ui/theme` ya existe: seguir su convencion
  (dark/lightColorScheme). No crear temas ad-hoc por pantalla.
- edge-to-edge activado en MainActivity; respeta los insets en Scaffold.

## Rendimiento / tooling

- `debugImplementation` del tooling; usar previews (`@Preview`) en las
  pantallas nuevas para revisar sin dispositivo.
- Lint de Compose (estable) del AGP 8.13 ya activo en CI: objetivo
  `lintDebug` con cero issues en F7.

## Checklist antes de dar una pantalla por terminada

- [ ] ViewModel con StateFlow + UiEvent, contenedor stateful / hijo stateless
- [ ] keys estables en items
- [ ] Loading / Error / Empty cubiertos
- [ ] preview añadida
- [ ] respeta dark mode y edge-to-edge