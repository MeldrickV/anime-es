---
description: >
  Reglas de red y scraping (OkHttp/JSoup) para :core:scraper: replicar los
  flujos del script Bash original (AnimeFLV y J-Kanime) con headers,
  referer, csrf y parsing determinista. Aplica cada vez que se toque scraping.
---

# Networking & scraper (port ani-es)

Fuentes: noloman `android-networking` + el guion original del CLI
(`/home/meldrickv/ani-es/ani-es`). OkHttp 4.12.0 + JSoup 1.18.3.

## Fuente de verdad: el script Bash

Todo lo que salga de aqui DEBE reflejar el script original, no inventos:

- **AnimeFLV**: `GET https://www3.animeflv.net/browse?q=<query>` -> lista con
  `<li><h3 class="h"><a href="./anime/<slug>"><titulo></a></h3></li>`.
  Detalle por slug: `/anime/<slug>` -> cover, `sinopsis`, `<div>` de
  generos/tags y lista de episodios. Episodio: POST `/flv` con el
  parámetro `data-encrypt` (ofuscado con el algoritmo de 3 letras del setup
  del script) para obtener los enlaces con calidad; `SRC_REFERER` = host del
  embed y validación de _playable_ con requests de byte-range (responde 200/206).
- **J-Kanime**: csrf `_token` del HTML base + `POST /buscar/` con el query;
  episodios via `GET /ajax/episodes/<id>/`; decode del player
  (jkplayer/jk.php o `data-src` del iframe) para enlaces directos.

## Patron Kotlin

- Todo el scraping vive en `:core:scraper` como **Kotlin JVM puro** con
  `OkHttpClient` inyectado (creado con alcance de aplicacion en `:app`).
- Endpoints/URLs/headers como constantes en cada scraper (ver
  `AnimeFlvScraper.kt`/`JKanimeScraper.kt`: mismas cadenas y headers que el
  script: UA de navegador, `Accept-Inmodificable`... espejo exacto del curl).
- El parsing en funciones puras (`AnimeFlvParser`) que solo reciben el HTML:
  testeables sin red con fixtures (F1).
- Los enlaces finales de video se devuelven con su `referer` requerido:
  `VideoLink(url, referer, quality)` para que el player sepa que header poner.
  La calidad preferida por ficha viene de `excepciones.json` (descarga en CI
  o cache en F3).

## Errores y fiabilidad

- Timeouts cortos (connect 15s, call 30s), reintento simple en buses sin 2xx.
- Deteccion de cambio de layout: si una fecha de parsing clave devuelve 0
  resultados, loggear el HTML crudo (truncado) en el error -> señalar que la
  fuente cambio su DOM (esto difiere los 403 del CI).
- Nunca parsear con regex frágil en donde JSoup pueda hacer selectores
  estables; el uso de regex queda acotado a lo que el script ya hace.
- Respeta el ritmo: agrega una pausa mínima entre peticiones a cada fuente
  para no abusar (mismo criterio del CLI).

## Tests

- Fixtures de HTML capturadas por el workflow `capture-fixtures` (F1)
  alojadas en `core/scraper/src/test/resources/fixtures/` y consumidas por
  los parsers; nunca inventar HTML "parecido" que luego difiera del real.
- MockWebServer (okhttp-mockwebserver 4.12.0) para probar los flujos HTTP
  completos del scraper sin tocar Internet en el job `test`.