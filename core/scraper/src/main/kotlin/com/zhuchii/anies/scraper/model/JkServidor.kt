package com.zhuchii.anies.scraper.model

/** Un servidor de `var servers` de la pagina de episodio de J-Kanime. El CLI
 *  elige el servidor "Mediafire" y usa el `remote` (base64) como pagina
 *  intermedia para sacar el enlace `https://download...` (fallback F5). */
data class JkServidor(
    val nombre: String,
    val remoteB64: String,
)