package com.zhuchii.anies.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Overrides remotos de slugs (port del `excepciones.json` del CLI).
 *
 * El script Bash corrige slugs truncados por su saneamiento (`tr -cd`) con un
 * mapa remoto `slug-candidata -> URL real` que permite hotfix sin release.
 * Esta clase replica esa idea: descarga el mismo JSON una vez (cache en
 * memoria), resuelve la URL real y devuelve null ante cualquier fallo de red
 * o parseo para no bloquear nunca al llamador.
 */
class ExcepcionesRemotas(
    private val url: String = "https://zhuchii.github.io/ani-es/excepciones.json",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    @Volatile
    private var cache: Map<String, String>? = null

    /** URL real para una candidata (`.../slug` con o sin `/`), null si no hay. */
    suspend fun resolver(candidata: String): String? = withContext(Dispatchers.IO) {
        val mapa = cache ?: descargar()?.also { cache = it } ?: return@withContext null
        val sinBarra = candidata.trimEnd('/')
        mapa[candidata] ?: mapa[sinBarra] ?: mapa["$sinBarra/"]
    }

    private fun descargar(): Map<String, String>? = try {
        val respuesta = client.newCall(Request.Builder().url(url).build()).execute()
        respuesta.use {
            if (!it.isSuccessful) return null
            val cuerpo = it.body?.string() ?: return null
            Json.parseToJsonElement(cuerpo).jsonObject
                .mapNotNull { (clave, valor) ->
                    (valor as? JsonPrimitive)?.takeIf { p -> p.isString }?.content?.let { clave to it }
                }
                .toMap()
                .ifEmpty { null }
        }
    } catch (_: Exception) {
        null
    }
}
