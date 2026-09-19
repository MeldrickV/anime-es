package com.zhuchii.anies.data.model

import com.zhuchii.anies.data.room.EntidadFavorito
import com.zhuchii.anies.data.room.EntidadHistorial
import com.zhuchii.anies.data.room.EntidadProgreso
import com.zhuchii.anies.scraper.model.Source

/** Mapeo entidad<->dominio publico del modulo :data. */
internal fun EntidadFavorito.aDominio(): AnimeFavorito = AnimeFavorito(
    source = Source.valueOf(source),
    slug = slug,
    titulo = titulo,
    coverUrl = coverUrl,
    createdAt = createdAt,
)

internal fun EntidadHistorial.aDominio(): AnimeHistorial = AnimeHistorial(
    source = Source.valueOf(source),
    slug = slug,
    titulo = titulo,
    coverUrl = coverUrl,
    ultimaVisualizacionAt = ultimaVisualizacionAt,
)

internal fun EntidadProgreso.aDominio(): AnimeProgreso = AnimeProgreso(
    source = Source.valueOf(source),
    slug = slug,
    episodio = episodio,
    posicionMs = posicionMs,
    duracionMs = duracionMs,
    actualizadoAt = actualizadoAt,
)