package com.zhuchii.anies.scraper

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JKanimeScraperTest {

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
    fun `buscar hace GET simple a buscar con guiones bajos y UA del script`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody(Fixtures.cargar("jkanime/busqueda.html"))
                .setHeader("Content-Type", "text/html; charset=utf-8")
        )
        server.start()

        val scraper = JKanimeScraper(baseUrl = server.url("/").toString())
        val resultados = scraper.buscar("shingeki no kyojin")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/buscar/shingeki_no_kyojin/", request.path)
        assertEquals(JKanimeScraper.USER_AGENT, request.getHeader("User-Agent"))
        assertTrue(resultados.isNotEmpty())
        assertEquals("shingeki-no-kyojin", resultados.first().slug)
    }

    @Test
    fun `buscar propaga errores HTTP`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        server.start()

        val scraper = JKanimeScraper(baseUrl = server.url("/").toString())
        val ex = runCatching { scraper.buscar("shingeki") }.exceptionOrNull()

        assertEquals("/buscar/shingeki/", server.takeRequest().path)
        assertTrue(ex is IllegalStateException)
    }
}