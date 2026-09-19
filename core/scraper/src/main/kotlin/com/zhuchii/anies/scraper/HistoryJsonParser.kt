package com.zhuchii.anies.scraper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Entrada del history.json del CLI (fuente de verdad del historico previo). */
@Serializable
data class HistoriaCli(
    @SerialName("last_cap") val lastCap: Int = 0,
    val progress: String = "",
    val source: String = "",
)

/**
 * Parser del history.json del CLI: mapa `titulo -> { last_cap, progress, source }`,
 * el mismo formato que guarda `save_history` del script con jq. Lectura SEGURA:
 * campos desconocidos se ignoran y un JSON malformado devuelve una lista vacia
 * (la importacion nunca debe romper por un historico previo corrupto).
 */
object HistoryJsonParser {

    private val json = Json { ignoreUnknownKeys = true }

    /** Devuelve el mapa titulo -> entrada, descartando las que no declaran source. */
    fun parsear(contenido: String): Map<String, HistoriaCli> = try {
        json.decodeFromString<Map<String, HistoriaCli>>(contenido)
            .filterValues { it.source.isNotBlank() }
    } catch (_: Exception) {
        emptyMap()
    }

    /** `progress` del CLI ("HH:MM:SS") a milisegundos; cadena invalida -> 0. */
    fun progresoToMs(progress: String): Long {
        val partes = progress.trim().split(":")
        if (partes.isEmpty()) return 0L
        var total = 0L
        for (parte in partes) {
            total = total * 60 + (parte.toLongOrNull() ?: 0L)
        }
        return total * 1000
    }
}