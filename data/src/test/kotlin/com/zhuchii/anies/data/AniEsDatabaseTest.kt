package com.zhuchii.anies.data

import androidx.room.Room
import com.zhuchii.anies.data.room.AniEsDatabase
import com.zhuchii.anies.data.room.EntidadFavorito
import com.zhuchii.anies.data.room.EntidadHistorial
import com.zhuchii.anies.data.room.EntidadProgreso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AniEsDatabaseTest {

    private lateinit var db: AniEsDatabase

    @Before
    fun montar() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AniEsDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun cerrar() {
        db.close()
    }

    @Test
    fun favoritos_insertar_contar_observar_y_eliminar() = runTest {
        val dao = db.favoritoDao()
        dao.insertar(EntidadFavorito("ANIME_FLV", "micro-magis", "Micro Magis", null, 1L))
        dao.insertar(EntidadFavorito("J_KANIME", "one-piece", "One Piece", "http://c/om/a.jpg", 2L))

        assertEquals(1, dao.contar("ANIME_FLV", "micro-magis"))
        assertTrue(dao.contar("J_KANIME", "one-piece") > 0)

        assertEquals(listOf("one-piece", "micro-magis"), dao.observarTodos().first().map { it.slug })
        assertTrue(dao.observarEsFavorito("ANIME_FLV", "micro-magis").first())
        assertFalse(dao.observarEsFavorito("ANIME_FLV", "one-piece").first())

        dao.eliminar("ANIME_FLV", "micro-magis")
        assertEquals(0, dao.contar("ANIME_FLV", "micro-magis"))

        dao.limpiar()
        assertTrue(dao.observarTodos().first().isEmpty())
    }

    @Test
    fun historial_upsert_actualiza_sin_duplicar() = runTest {
        val dao = db.historialDao()
        dao.insertar(EntidadHistorial("ANIME_FLV", "toki-wo-kakeru", "Toki wo Kakeru", null, 10L))
        dao.insertar(EntidadHistorial("ANIME_FLV", "shingeki-no-kyojin", "Shingeki", null, 30L))
        // Re-visita: refresca el mismo registro sin duplicar y lo acerca al tope.
        dao.insertar(EntidadHistorial("ANIME_FLV", "toki-wo-kakeru", "Toki wo Kakeru", null, 20L))

        val lista = dao.observarTodos().first()
        assertEquals(2, lista.size)
        assertEquals(listOf("shingeki-no-kyojin", "toki-wo-kakeru"), lista.map { it.slug })

        dao.eliminar("ANIME_FLV", "shingeki-no-kyojin")
        assertEquals(listOf("toki-wo-kakeru"), dao.observarTodos().first().map { it.slug })

        dao.limpiar()
        assertTrue(dao.observarTodos().first().isEmpty())
    }

    @Test
    fun progreso_guardar_obtener_y_eliminar() = runTest {
        val dao = db.progresoDao()
        assertNull(dao.obtener("ANIME_FLV", "micro-magis", 1))

        dao.guardar(EntidadProgreso("ANIME_FLV", "micro-magis", 1, 1_000L, 1_200_000L, 1L))
        dao.guardar(EntidadProgreso("ANIME_FLV", "micro-magis", 2, 500L, 1_200_000L, 2L))

        val capitulo1 = checkNotNull(dao.obtener("ANIME_FLV", "micro-magis", 1))
        assertEquals(1_000L, capitulo1.posicionMs)
        assertEquals(1_200_000L, capitulo1.duracionMs)

        assertEquals(listOf(1, 2), dao.observarAnime("ANIME_FLV", "micro-magis").first().map { it.episodio })

        dao.eliminar("ANIME_FLV", "micro-magis", 1)
        assertNull(dao.obtener("ANIME_FLV", "micro-magis", 1))
        assertEquals(listOf(2), dao.observarAnime("ANIME_FLV", "micro-magis").first().map { it.episodio })
    }
}