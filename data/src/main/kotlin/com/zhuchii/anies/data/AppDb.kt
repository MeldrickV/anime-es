package com.zhuchii.anies.data

import android.content.Context
import androidx.room.Room
import com.zhuchii.anies.data.room.AniEsDatabase

/**
 * Unico punto de acceso a la base Room sin inyeccion de dependencias.
 * Se inicializa una sola vez desde [AppDb.init] (AnieEsApp.onCreate); los
 * repositorios caen por defecto a [AppDb.obtener] y los tests inyectan su
 * propia base en memoria.
 */
object AppDb {
    @Volatile
    private var instancia: AniEsDatabase? = null

    fun init(context: Context) {
        if (instancia == null) {
            synchronized(this) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                        context.applicationContext,
                        AniEsDatabase::class.java,
                        "anies.db",
                    ).build()
                }
            }
        }
    }

    fun obtener(): AniEsDatabase = checkNotNull(instancia) {
        "AppDb no inicializado: llama a AppDb.init(context) en Application.onCreate"
    }
}