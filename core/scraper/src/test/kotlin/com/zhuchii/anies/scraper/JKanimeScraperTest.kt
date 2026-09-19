package com.zhuchii.anies.scraper

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `detalle hace GET al slug con slash y parsea el fixture`() = runTest {
        server.enqueue(MockResponse().setBody(Fixtures.cargar("jkanime/detalle.html")))
        server.start()

        val detalle = JKanimeScraper(baseUrl = server.url("/").toString()).detalle("one-piece")

        assertEquals("/one-piece/", server.takeRequest().path)
        assertTrue(detalle.title == "One Piece")
        assertTrue(detalle.description!!.isNotBlank())
    }

    @Test
    fun `home hace GET a la raiz y devuelve populares y recientes`() = runTest {
        server.enqueue(MockResponse().setBody(Fixtures.cargar("jkanime/home.html")))
        server.start()

        val home = JKanimeScraper(baseUrl = server.url("/").toString()).home()

        assertEquals("/", server.takeRequest().path)
        assertTrue(home.populares.isNotEmpty())
        assertTrue(home.recientes.isNotEmpty())
    }

    @Test
    fun `episodios hace GET de detalle y POST ajax con csrf y cookie de sesion`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody(Fixtures.cargar("jkanime/detalle.html"))
                .setHeader("Set-Cookie", "session_cookie=abc123; Path=/; HttpOnly")
                .setHeader("Content-Type", "text/html; charset=utf-8")
        )
        server.enqueue(
            MockResponse()
                .setBody(Fixtures.cargar("jkanime/episodios.json"))
                .setHeader("Content-Type", "application/json; charset=utf-8")
        )
        server.start()

        val scraper = JKanimeScraper(baseUrl = server.url("/").toString())
        val episodios = scraper.episodios("one-piece")

        assertEquals("/one-piece/", server.takeRequest().path)

        val post = server.takeRequest()
        assertEquals("POST", post.method)
        assertEquals("/ajax/episodes/201/", post.path)
        assertEquals("XMLHttpRequest", post.getHeader("X-Requested-With"))
        assertTrue("csrf token en header X-CSRF-TOKEN", post.getHeader("X-CSRF-TOKEN")!!.isNotBlank())
        assertEquals("session_cookie=abc123", post.getHeader("Cookie"))
        assertTrue("_token en el cuerpo", post.body?.readUtf8().orEmpty().contains("_token="))

        assertEquals(1178, episodios.size)
        assertEquals("1", episodios.first().numero)
        assertEquals("1178", episodios.last().numero)
    }

    @Test
    fun `episodios devuelve pelicula cuando el tipo es Pelicula`() = runTest {
        val html = """
            <html><head><meta name="csrf-token" content="abc"></head>
            <body><div class="anime_data"><span>Tipo:</span> Pelicula</div></body></html>
        """.trimIndent()
        server.enqueue(MockResponse().setBody(html))
        server.start()

        val episodios = JKanimeScraper(baseUrl = server.url("/").toString()).episodios("kimi-no-na-wa")

        assertEquals(listOf("pelicula"), episodios.map { it.numero })
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `video resuelve via jkplayer um y detecta HLS`() = runTest {
        server.start()
        val playerUrl = server.url("/jkplayer/um?e=abc&t=xyz").toString()
        val m3u8 = server.url("/stream/index.m3u8").toString()
        server.enqueue(
            MockResponse().setBody("""<html><iframe src="$playerUrl"></iframe></html>"""),
        )
        server.enqueue(
            MockResponse().setBody("""<html><script>video: { url: '$m3u8', type: 'customHls' }</script></html>"""),
        )

        val fuente = JKanimeScraper(baseUrl = server.url("/").toString()).video("one-piece", "1")

        assertEquals(m3u8, fuente.url)
        assertTrue(fuente.hls)
        assertNull(fuente.referer)

        assertEquals("/one-piece/1", server.takeRequest().path)
        val player = server.takeRequest()
        assertEquals("/jkplayer/um", player.path)
        assertEquals("e=abc&t=xyz", player.requestUrl?.query)
    }

    @Test
    fun `video usa fallback Mediafire cuando no hay jkplayer`() = runTest {
        server.start()
        val remoteB64 = java.util.Base64.getEncoder()
            .encodeToString(server.url("/mediafire-file").toString().toByteArray())
        val capPage = """
            <html><script>var servers = [
                {"remote":"$remoteB64","slug":"x","server":"Mediafire","lang":1,"size":"52 MB","append":0}
            ];</script></html>
        """.trimIndent()
        val download = "https://download123.mediafire.com/file/pelicula.mp4"
        server.enqueue(MockResponse().setBody(capPage))
        server.enqueue(
            MockResponse().setBody("""<html><a href="$download">Bajar</a></html>"""),
        )

        val fuente = JKanimeScraper(baseUrl = server.url("/").toString()).video("kimi-no-na-wa", "pelicula")

        assertEquals(download, fuente.url)
        assertFalse(fuente.hls)

        assertEquals("/kimi-no-na-wa/pelicula", server.takeRequest().path)
        assertEquals("/mediafire-file", server.takeRequest().path)
    }
}