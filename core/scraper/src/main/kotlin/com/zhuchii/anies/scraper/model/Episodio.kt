package com.zhuchii.anies.scraper.model

import kotlinx.serialization.Serializable

/** Un episodio de un anime en su fuente. `numero` es lo que se muestra y se
 *  usa para construir la URL del reproductor: en AnimeFLV `/ver/<slug>-<numero>`
 *  y en J-Kanime `/<slug>/<numero>` (con "pelicula" para las peliculas, igual
 *  que el CLI). Port 1:1 del seq del script: J-Kanime genera 1..total y
 *  AnimeFLV los caps de `var eps`. */
@Serializable
data class Episodio(
    val numero: String,
)