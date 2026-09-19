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
}