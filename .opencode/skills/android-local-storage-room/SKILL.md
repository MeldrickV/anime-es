---
description: >
  Room + KSP para :data del port: esquema (animes, favoritos, historial,
  progreso por episodio), migration del history.json del CLI, DAO reactivos
  con Flow y KSP. Aplica en las Fases 3 y 6.
---

# Local storage / Room (port ani-es)

Fuentes: noloman `android-local-storage` + Google android skills y el
formato del `history.json` del CLI original. Room 2.8.2 + KSP
(version ya en el catalogo; se activa junto con el modulo `:data` en F3).

## Esquema

- `animes` (pk slug + source): slug, fuente, titulo, cover, descripcion, tags.
- `favoritos` (pk animeId, timestamp): biblioteca del usuario.
- `historial` (pk id, animeId, episode; ultima vez): ultima reproduccion.
- `progreso` (pk animeId+episode): posicion en segundos y visto.

Relaciones: DAO que expone `Flow<List<Anime>>`, `upsert`, y consultas de
"no vistos" (join progreso/episodios) para los badges de F6.

## KSP y config

- Activar el plugin `com.google.devtools.ksp` version `2.2.21-2.0.4` en el
  build del modulo `:data` y el alias en `build.gradle.kts` raiz (hoy apply
  false para no resolver el plugin en el CI F0).
- Room 2.8: `typeConverters` para enum `Source` y datos de lista (tags).
- `fallbackToDestructiveMigration` NO: el historial es dato del usuario.
  Migraciones explicitas + test de migraciones cuando haya cambios de esquema.

## Migracion desde history.json del CLI

- `history.json` es un mapa `slug -> [indices de episodio vistos]` (logica de
  `/home/meldrickv/ani-es/ani-es`: `history.json` debe tener el formato vacio
  `{}` al empezar). En F6 la primera migracion lo convierte a `progreso` +
  `historial` marcando los episodios vistos como `visto=true` (los indices del
  archivo son los offsets de la lista de episodios de esa fuente).

## Acceso

- Datos solo via Repository (aguas arriba del ViewModel); las pantallas NUNCA
  tocan DAO directo.
- `dispatchers` no se usan a pelo: Room ya es un dispatcher propio; exponer
  solo `Flow`s observables y `suspend` para escritura.
- Filosofia del did: como en todo el port, la minima API posible
  (CRUD reducido) — el historial/pelican pequeño no necesita paginacion Paging 3.