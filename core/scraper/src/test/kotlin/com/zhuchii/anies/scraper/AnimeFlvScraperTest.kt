package com.zhuchii.anies.scraper

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnimeFlvScraperTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `buscar hace GET al endpoint del script con UA y parsea la respuesta`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody(Fixtures.cargar("animeflv/busqueda.html"))
                .setHeader("Content-Type", "text/html; charset=utf-8")
        )
        server.start()

        val scraper = AnimeFlvScraper(baseUrl = server.url("/").toString())
        val resultados = scraper.buscar("shingeki")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/animes?buscar=shingeki&pag=1", request.path)
        assertEquals(AnimeFlvScraper.USER_AGENT, request.getHeader("User-Agent"))
        assertTrue(resultados.isNotEmpty())
    }

    @Test
    fun `buscar codifica la query con espacios como en el script`() = runTest {
        server.enqueue(MockResponse().setBody(Fixtures.cargar("animeflv/busqueda.html")))
        server.start()

        AnimeFlvScraper(baseUrl = server.url("/").toString()).buscar("shingeki no kyojin")

        val request = server.takeRequest()
        assertEquals("/animes?buscar=shingeki%20no%20kyojin&pag=1", request.path)
    }

    @Test
    fun `buscar admite pagina y propaga errores HTTP`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        server.start()

        val scraper = AnimeFlvScraper(baseUrl = server.url("/").toString())
        val ex = runCatching { scraper.buscar("shingeki", pagina = 2) }.exceptionOrNull()

        assertEquals("/animes?buscar=shingeki&pag=2", server.takeRequest().path)
        assertTrue(ex is IllegalStateException)
    }

    @Test
    fun `detalle hace GET a anime con slug y parsea el fixture`() = runTest {
        server.enqueue(MockResponse().setBody(Fixtures.cargar("animeflv/detalle.html")))
        server.start()

        val detalle = AnimeFlvScraper(baseUrl = server.url("/").toString()).detalle("mao-2026")

        assertEquals("/anime/mao-2026", server.takeRequest().path)
        assertTrue(detalle.description!!.isNotBlank())
        assertEquals("mao-2026", detalle.slug)
    }

    @Test
    fun `home hace GET a la raiz y devuelve populares y recientes`() = runTest {
        server.enqueue(MockResponse().setBody(Fixtures.cargar("animeflv/home.html")))
        server.start()

        val home = AnimeFlvScraper(baseUrl = server.url("/").toString()).home()

        assertEquals("/", server.takeRequest().path)
        assertTrue(home.populares.isNotEmpty())
        assertTrue(home.recientes.isNotEmpty())
    }

    @Test
    fun `episodios hace GET al detalle y devuelve los caps del var eps`() = runTest {
        server.enqueue(MockResponse().setBody(Fixtures.cargar("animeflv/detalle.html")))
        server.start()

        val episodios = AnimeFlvScraper(baseUrl = server.url("/").toString()).episodios("mao-2026")

        assertEquals("/anime/mao-2026", server.takeRequest().path)
        assertEquals((1..24).map { it.toString() }, episodios.map { it.numero })
    }
}