package com.zhuchii.anies.scraper

import com.zhuchii.anies.scraper.model.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun `parseDetalle extrae cover, sinopsis, generos, episodios y slug`() {
        val detalle = AnimeFlvParser.parseDetalle(Fixtures.cargar("animeflv/detalle.html"))

        assertEquals(Source.ANIME_FLV, detalle.source)
        assertEquals("mao-2026", detalle.slug)
        assertTrue("cover debe ser una URL http", detalle.coverUrl!!.startsWith("http"))
        assertTrue(detalle.coverUrl!!.contains("mao-2026.webp"))
        assertTrue("sinopsis no vacia", detalle.description!!.contains("Nanoka"))
        assertTrue(
            "generos de la pagina",
            detalle.tags.containsAll(listOf("Historico", "Misterio", "Sobrenatural")),
        )
        assertEquals(24, detalle.episodeCount)
        assertEquals("En emision", detalle.estado)
    }

    @Test
    fun `parseHome separa populares (emision) y recientes (episodios) desde home real`() {
        val home = AnimeFlvParser.parseHome(Fixtures.cargar("animeflv/home.html"))

        assertTrue("populares no vacio", home.populares.isNotEmpty())
        assertTrue("recientes no vacio", home.recientes.isNotEmpty())
        assertTrue(home.populares.all { it.source == Source.ANIME_FLV && it.slug.isNotBlank() && it.title.isNotBlank() })
        assertTrue(home.recientes.all { it.source == Source.ANIME_FLV && it.slug.isNotBlank() && it.title.isNotBlank() })
        home.recientes.forEach { anime ->
            assertTrue("el slug no debe arrastrar el numero de episodio: ${anime.slug}",
                !Regex("-\\d+$").containsMatchIn(anime.slug))
        }
    }

    @Test
    fun `parseEpisodios genera 1..24 desde el detalle real`() {
        val episodios = AnimeFlvParser.parseEpisodios(Fixtures.cargar("animeflv/detalle.html"))

        assertEquals(24, episodios.size)
        assertEquals("1", episodios.first().numero)
        assertEquals("24", episodios.last().numero)
        assertEquals((1..24).map { it.toString() }, episodios.map { it.numero })
    }

    @Test
    fun `parseEpisodios expande rangos y quita el cap 0`() {
        val html = """<script>var eps = [["24","0",""],["13","24"],["5","3",""]];</script>"""
        val numeros = AnimeFlvParser.parseEpisodios(html).map { it.numero }

        assertEquals((1..24).map { it.toString() }, numeros)
    }

    @Test
    fun `parseEpisodios devuelve vacio sin var eps`() {
        assertEquals(0, AnimeFlvParser.parseEpisodios("<html>sin eps</html>").size)
    }

    @Test
    fun `parseEncrypt y parseDataId desde fixtures reales (F5)`() {
        val ver = Fixtures.cargar("animeflv/ver.html")

        assertNotNull("data-encrypt del reproductor", AnimeFlvParser.parseEncrypt(ver))
        assertEquals(7242, AnimeFlvParser.parseDataId(Fixtures.cargar("animeflv/detalle.html")))
    }

    @Test
    fun `parseFlvServidores extrae, dedup y ordena mp4upload primero desde flv real`() {
        val servidores = AnimeFlvParser.parseFlvServidores(Fixtures.cargar("animeflv/flv.html"))

        assertTrue("varios servidores del fixture real", servidores.size > 3)
        assertTrue(
            "mp4upload al frente: ${servidores.first().nombre}",
            servidores.first().nombre.contains("mp4upload", ignoreCase = true),
        )
        assertEquals(servidores.size, servidores.distinctBy { it.hex }.size)
        assertTrue(AnimeFlvParser.decodificarHex(servidores.first().hex)!!.startsWith("http"))
    }

    @Test
    fun `extraerUrlVideo captura mp4 y m3u8 sin falsos positivos`() {
        val mp4 = """<img src="https://cdn.example.com/a.jpg"> <video src="https://cdn.example.com/v.mp4?t=1"></video>"""
        assertEquals("https://cdn.example.com/v.mp4?t=1", AnimeFlvParser.extraerUrlVideo(mp4))

        val m3u8 = """<script>var s='https://x.net/play.m3u8?u=9';</script>"""
        assertEquals("https://x.net/play.m3u8?u=9", AnimeFlvParser.extraerUrlVideo(m3u8))

        assertNull(AnimeFlvParser.extraerUrlVideo("""<video src="https://cdn.example.com/solo-jpg.jpg"></video>"""))
    }

    @Test
    fun `decodificarHex port del bytes-fromhex del CLI`() {
        assertEquals("https://mp4upload.com/embed-abc.html", AnimeFlvParser.decodificarHex("68747470733a2f2f6d703475706c6f61642e636f6d2f656d6265642d6162632e68746d6c"))
        assertNull("hex invalido", AnimeFlvParser.decodificarHex("zz"))
    }
}