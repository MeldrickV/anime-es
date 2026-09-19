package com.zhuchii.anies.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        EntidadFavorito::class,
        EntidadHistorial::class,
        EntidadProgreso::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AniEsDatabase : RoomDatabase() {
    abstract fun favoritoDao(): FavoritoDao

    abstract fun historialDao(): HistorialDao

    abstract fun progresoDao(): ProgresoDao
}