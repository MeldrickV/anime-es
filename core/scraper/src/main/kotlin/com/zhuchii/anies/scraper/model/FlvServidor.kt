package com.zhuchii.anies.scraper.model

/** Un servidor de video de la lista `/flv` de AnimeFLV: nombre visible y el
 *  hex del embed que hay que decodificar (port del `serv_list` del CLI). */
data class FlvServidor(
    val nombre: String,
    val hex: String,
)