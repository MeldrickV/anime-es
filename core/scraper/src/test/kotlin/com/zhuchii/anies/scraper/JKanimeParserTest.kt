package com.zhuchii.anies.scraper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JKanimeParserTest {

    @Test
    fun `parseBusqueda extrae slug del href y titulo desde fixture real`() {
        val html = Fixtures.cargar("jkanime/busqueda.html")

        val resultados = JKanimeParser.parseBusqueda(html)

        assertTrue("El fixture real debe devolver resultados", resultados.isNotEmpty())
        val primero = resultados.first()
        assertEquals("shingeki-no-kyojin", primero.slug)
        assertEquals("Shingeki no Kyojin", primero.title)
        assertEquals(Source.J_KANIME, primero.source)
    }

    @Test
    fun `parseBusqueda deriva el slug del titulo si falta el href`() {
        val html = """
            <h5><a href="enlace-malformado">Shingeki no Kyojin La Pelicula!</a></h5>
        """.trimIndent()
        val resultado = JKanimeParser.parseBusqueda(html).single()
        assertEquals("shingeki-no-kyojin-la-pelicula", resultado.slug)
    }

    @Test
    fun `parseBusqueda deduplica titulos vacios o repetidos`() {
        val html = """
            <h5><a href="https://jkanime.net/naruto/">Naruto</a></h5>
            <h5><a href="https://jkanime.net/naruto/">Naruto Shippuden</a></h5>
            <h5><a href="https://jkanime.net/"></a></h5>
        """.trimIndent()
        val resultados = JKanimeParser.parseBusqueda(html)
        assertEquals(2, resultados.size)
    }

    @Test
    fun `parseBusqueda devuelve vacio sin resultados`() {
        assertEquals(0, JKanimeParser.parseBusqueda("<html><body>sin resultados</body></html>").size)
    }
}