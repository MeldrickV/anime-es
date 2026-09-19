package com.zhuchii.anies.scraper

import com.zhuchii.anies.scraper.model.Source
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

    @Test
    fun `parseHome separa top animes (populares) y animes recientes con cover`() {
        val home = JKanimeParser.parseHome(Fixtures.cargar("jkanime/home.html"))

        assertTrue("populares no vacio", home.populares.isNotEmpty())
        assertTrue("recientes no vacio", home.recientes.isNotEmpty())

        val top = home.populares.first()
        assertEquals("one-piece", top.slug)
        assertEquals("One Piece", top.title)
        assertEquals(Source.J_KANIME, top.source)
        assertTrue("cover del top", top.coverUrl!!.contains("cdn.jkdesa.com"))

        assertTrue("recientes empieza por cluster-edge", home.recientes.first().slug == "cluster-edge")
        home.recientes.forEach { anime ->
            assertTrue(anime.coverUrl!!.startsWith("http"))
        }
    }

    @Test
    fun `parseDetalle extrae cover, sinopsis, generos y estado`() {
        val detalle = JKanimeParser.parseDetalle(Fixtures.cargar("jkanime/detalle.html"))

        assertEquals(Source.J_KANIME, detalle.source)
        assertEquals("One Piece", detalle.title)
        assertEquals("one-piece.jpg", detalle.coverUrl!!.substringAfterLast('/'))
        assertTrue("sinopsis no vacia", detalle.description!!.contains("Luffy"))
        assertTrue(
            "generos de la pagina",
            detalle.tags.containsAll(listOf("Accion", "Aventura", "Comedia", "Shounen")),
        )
        assertEquals("En emision", detalle.estado)
    }
}