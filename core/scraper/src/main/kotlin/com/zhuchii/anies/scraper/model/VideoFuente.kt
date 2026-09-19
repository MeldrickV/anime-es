package com.zhuchii.anies.scraper.model

import kotlinx.serialization.Serializable

/** URL de video resuelta para un episodio. `referer` es el SRC_REFERER del CLI
 *  (host del embed que sirve el video, p.ej. mp4upload.com) y `hls` indica que
 *  la URL es un playlist m3u8 (se toca con HlsMediaSource en el player, F5). */
@Serializable
data class VideoFuente(
    val url: String,
    val referer: String? = null,
    val hls: Boolean = false,
)