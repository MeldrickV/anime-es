package com.zhuchii.anies.scraper

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ExcepcionesRemotasTest {

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
    fun `resolver devuelve la URL real con y sin barra final`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"https://jkanime.net/clase-de-2-banme/":"https://jkanime.net/clase-de-2-banme-ni-youkoso/"}""")
                .setHeader("Content-Type", "application/json"),
        )
        server.start()

        val excepciones = ExcepcionesRemotas(url = server.url("/excepciones.json").toString())

        assertEquals(
            "https://jkanime.net/clase-de-2-banme-ni-youkoso/",
            excepciones.resolver("https://jkanime.net/clase-de-2-banme/"),
        )
        assertEquals(
            "https://jkanime.net/clase-de-2-banme-ni-youkoso/",
            excepciones.resolver("https://jkanime.net/clase-de-2-banme"),
        )
        assertNull(excepciones.resolver("https://jkanime.net/pokemon/"))
    }

    @Test
    fun `fallo de red o JSON invalido devuelve null sin romper`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        server.start()

        val excepciones = ExcepcionesRemotas(url = server.url("/excepciones.json").toString())

        assertNull(excepciones.resolver("https://jkanime.net/pokemon/"))
    }
}
