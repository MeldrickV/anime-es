package com.zhuchii.anies.scraper.model

import kotlinx.serialization.Serializable

/**
 * Listas de la portada (home) de cada fuente. No existen en el script Bash
 * (el CLI solo busca); se portan del frontend real de cada plataforma como
 * decision de F2: AnimeFLV "populares" -> seccion "Animes en Emision" y
 * J-Kanime "populares" -> "Top animes"; "recientes" -> "Ultimos episodios
 * agregados" (AnimeFLV) y "Animes recientes" (J-Kanime).
 */
@Serializable
data class HomeAnimes(
    val populares: List<AnimeSummary> = emptyList(),
    val recientes: List<AnimeSummary> = emptyList(),
)

/** Detalle de un anime: cover, sinopsis y tags (F2). El titulo lo trae el
 *  AnimeSummary con el que se navega; aqui queda vacio salvo que la fuente
 *  lo provea en el propio pagina de detalle. */
@Serializable
data class AnimeDetalle(
    val source: Source,
    val slug: String,
    val title: String = "",
    val coverUrl: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val episodeCount: Int = 0,
    val estado: String? = null,
)