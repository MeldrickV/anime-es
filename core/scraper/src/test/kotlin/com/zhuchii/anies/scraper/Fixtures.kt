package com.zhuchii.anies.scraper

/** Carga un fixture de HTML real capturado (CI o curl). Nunca HTML inventado. */
object Fixtures {

    fun cargar(ruta: String): String {
        val stream = requireNotNull(
            Fixtures::class.java.classLoader?.getResourceAsStream("fixtures/$ruta")
        ) { "Fixture no encontrada: $ruta" }
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}