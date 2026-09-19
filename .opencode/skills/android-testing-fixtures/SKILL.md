---
description: >
  Estrategia de testing JVM del port: tests deterministas sin red, fixtures
  de HTML capturadas por CI y el plan de captura/actualizacion. Aplica en
  toda fase que genere o toque tests o fixtures.
---

# Testing & fixtures (port ani-es)

Fuentes: noloman `android-testing-best-practices` y Jaewoong Eum (anterior
de Google, tests de Android). JUnit 4.13.2 (testDebugUnitTest en CI).

## Principio: nada raro que pase sin test

- Los parsers (funciones puras en `:core:scraper`) son el nucleo del port:
  cada uno con su test determinista sobre un fixture HTML **real**.
- La logica HTTP (headers, csrf, referer) se prueba con MockWebServer
  (okhttp-mockwebserver 4.12.0, ya en el catalogo auxiliar).
- Nada de `thread.sleep`, redes en tests, ni asserts flaky.

## Fixtures por CI (Fase 1)

Problem: los scraper dependen del HTML vivo de las dos fuentes y en CI el
indice puede cambiar (señales 403/estructura). Solucion: un workflow
`capture-fixtures` (se añade en F1) que:

1. Hace las peticiones reales (mismo flujo que :core:scraper) contra
   jKanime.net y animeflv.net con la fecha/version del capturador.
2. Vuelca los HTML como `core/scraper/src/test/resources/fixtures/*
   (sanitizados, sin cookies) y hace commit convencional
   `chore: update fixtures` (con `[skip ci]` para no re-bumpear).
3. Nunca se inventa HTML en el repo: si un fixture no llega, el commit no
   se hace y el CI de los scraper falla -> aviso de cambio de DOM en fuente.

## Que testear en cada fase

- F0: parsers puros (ya: `AnimeFlvParserTest`, casos edge de entidades/slug).
- F1: busqueda real (MockWebServer) + fixtures de busqueda de ambas fuentes.
- F2: detalle completo (cover/desc/tags/episodios) con su fixture.
- F3+: DAO/Repository de Room contra Room in-memory (androidx archcore
  testing) y migraciones de history.json.
- F5: no testeable en JVM (player); se valida en el movil (hm), documentar
  los pasos de prueba manual en el commit.

## Patron de test en `:core:scraper`

```kotlin
@Test
fun `parseBusqueda extrae slug y titulo limpio`() {
    val html = fixture("animeflv/busqueda.html")   // leido de resources
    assertEquals("shingeki-no-kyojin-the-final-season",
                 AnimeFlvParser.parseBusqueda(html).first().slug)
}
```

Cobertura minima por parser: 2-3 casos (feliz, vacio, entidades/tags raros) —
el objetivo es que los cambios de la fuente se detecten sin inspeccion visual.