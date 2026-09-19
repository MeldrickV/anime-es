package com.zhuchii.anies.scraper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryJsonParserTest {

    private val ejemplo = """
        {
          "Jujutsu Kaisen": {
            "last_cap": 10,
            "progress": "00:15:32",
            "source": "animeflv"
          },
          "One Piece": {
            "last_cap": 194,
            "progress": "22:10:05",
            "source": "jkanime"
          },
          "Ignorado sin source": {
            "last_cap": 3,
            "progress": "00:00:10"
          }
        }
    """.trimIndent()

    @Test
    fun `parsea el formato del history json del cli`() {
        val entradas = HistoryJsonParser.parsear(ejemplo)

        assertEquals(2, entradas.size)
        val jujutsu = entradas.getValue("Jujutsu Kaisen")
        assertEquals(10, jujutsu.lastCap)
        assertEquals("animeflv", jujutsu.source)
        assertEquals("00:15:32", jujutsu.progress)

        val onePiece = entradas.getValue("One Piece")
        assertEquals(194, onePiece.lastCap)
        assertEquals("jkanime", onePiece.source)
    }

    @Test
    fun `ignora entradas sin source`() {
        assertTrue(HistoryJsonParser.parsear(ejemplo).none { it.key.contains("Ignorado") })
    }

    @Test
    fun `json malformado devuelve mapa vacio`() {
        assertTrue(HistoryJsonParser.parsear("{esto no es json").isEmpty())
        assertTrue(HistoryJsonParser.parsear("").isEmpty())
    }

    @Test
    fun `campos desconocidos no rompen el parseo`() {
        val conExtra = """
            {"Anime X": {"last_cap": 5, "progress": "00:01:00", "source": "jkanime", "fecha": "2025-01-01"}}
        """.trimIndent()
        assertEquals(1, HistoryJsonParser.parsear(conExtra).size)
    }

    @Test
    fun `progreso hh mm ss se convierte a milisegundos`() {
        assertEquals(15L * 60 * 1000 + 32 * 1000, HistoryJsonParser.progresoToMs("00:15:32"))
        assertEquals(22L * 3600 * 1000 + 10L * 60 * 1000 + 5 * 1000, HistoryJsonParser.progresoToMs("22:10:05"))
        assertEquals(0L, HistoryJsonParser.progresoToMs(""))
        assertEquals(0L, HistoryJsonParser.progresoToMs("no-es-una-hora"))
    }
}