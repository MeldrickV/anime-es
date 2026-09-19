package com.zhuchii.anies.data.model

import com.zhuchii.anies.scraper.model.Source

/** Anime favorito tal y como lo consume la UI. */
data class AnimeFavorito(
    val source: Source,
    val slug: String,
    val titulo: String,
    val coverUrl: String?,
    val createdAt: Long,
)