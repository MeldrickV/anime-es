package com.zhuchii.anies.scraper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimeFlvParserTest {

    @Test
    fun `parseBusqueda extrae slug y titulo limpio desde fixture real`() {
        val html = Fixtures.cargar("animeflv/busqueda.html")

        val resultados = AnimeFlvParser.parseBusqueda(html)

        assertTrue("El fixture real debe devolver resultados", resultados.isNotEmpty())
        val primero = resultados.first()
        assertEquals("shingeki-no-kyojin-season-2-movie-kakusei-no-houkou-latino", primero.slug)
        assertEquals(Source.ANIME_FLV, primero.source)
        assertTrue(primero.title.isNotBlank())
    }

    @Test
    fun `limpiarTitulo quita tags y decodifica entidades`() {
        assertEquals("Naruto: Shippuden", AnimeFlvParser.limpiarTitulo("<b>Naruto:&nbsp;Shippuden</b>"))
        assertEquals("A & B", AnimeFlvParser.limpiarTitulo("A &amp; B"))
        assertEquals("Shingeki no Kyojin 'The Final Season'",
            AnimeFlvParser.limpiarTitulo("&nbsp;Shingeki no Kyojin &#039;The Final Season&#039;&nbsp;"))
    }

    @Test
    fun `parseBusqueda deduplica slugs repetidos`() {
        val html = """
            <li><h3 class="h"><a href="./anime/violet-evergarden">Violet Evergarden</a></h3></li>
            <li><h3 class="h"><a href="./anime/violet-evergarden">Violet Evergarden 2</a></h3></li>
        """.trimIndent()
        assertEquals(1, AnimeFlvParser.parseBusqueda(html).size)
    }

    @Test
    fun `parseBusqueda devuelve vacio sin resultados`() {
        assertEquals(0, AnimeFlvParser.parseBusqueda("<html><body>sin resultados</body></html>").size)
    }
}