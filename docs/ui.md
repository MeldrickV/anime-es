# Mini design system (UI) — Ani-es Android

Convenciones de la capa grafica del port (Jetpack Compose, Material 3).
Leelas antes de tocar cualquier pantalla o componente de `:app`. Reglas
generales de Compose en la skill `.opencode/skills/compose-best-practices`.

## Piezas reutilizables (`app/.../ui/components`)

| Pieza | Rol |
|---|---|
| `CoverImage` | Miniatura/portada. Siempre pinta el cuadro que da el modifier sobre `surfaceVariant`; si el modelo no es una URL http dibuja un placeholder (nunca una fila rota). |
| `FilaAnime` | Fila de lista (busqueda, home, biblioteca, historial): CoverImage + titulo con ellipsis + fuente + click al detalle. |
| `Cargando` / `ErrorReintento` / `PantallaVacia` | Estados de carga, error con reintento y vacio. La UI los usa como componentes, no repite `when` con Box + posiciones. |

## Reglas

1. **Stateless**: las piezas reciben datos y callbacks; no leen ViewModels dentro.
2. **Estados como componentes**: carga/error/vacio -> `Cargando`, `ErrorReintento`, `PantallaVacia`. El caller pasa la posicion via modifier (`Modifier.align(Alignment.Center)` dentro de un Box).
3. **Portadas SIEMPRE por `CoverImage`** (placeholder incluido). En grillas/grades usar `Modifier.aspectRatio(2/3f)` sobre el tamaño.
4. **Listas**: `LazyColumn(Modifier.weight(1f))` con `items(items, key = { "${it.source.name}|${it.slug}" })` (key estable).
5. **Tema**: colores y tipografia solo desde `MaterialTheme` (el dark mode alterna por sistema); nada de colores ad-hoc por pantalla. Iconos SOLO del set de `material-icons-core` (salvo que se anada `material-icons-extended` con justificacion).
6. **Idioma**: textos en espanol como constantes inline o en el componente; no hay framework de i18n (decision F7).
7. **Previews**: componentes nuevos, `@Preview` claro/oscuro.

## Convenciones de navegacion y estado

- Cada pantalla = `XScreen` stateful (ViewModel con `collectAsStateWithLifecycle`) que delega en `PantallaX` stateless con los callbacks. Sin inyeccion de dependencias.
- Rutas: `detalle/{source}/{slug}?titulo=` (el detalle monta sus episodios; no hay pantalla de episodios suelta).
- El player no depende de la rotacion de la actividad: toda transicion de pantalla completa pasa por `solicitarPantallaCompleta` (guarda anti-bucle) y al salir se restaura `SCREEN_ORIENTATION_PORTRAIT`.