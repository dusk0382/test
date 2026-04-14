package com.cecosesola.coop.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductoEntity::class,
        MetadataEntity::class,
        FavoritoEntity::class,
        BusquedaEntity::class
    ],
    // Versión 4: añadidos índices en ProductoEntity (nombre, categoria)
    // e índice UNIQUE en BusquedaEntity (query).
    // fallbackToDestructiveMigration() se encarga de la migración automáticamente.
    version = 4,
    exportSchema = false
)
abstract class PreciosDatabase : RoomDatabase() {

    abstract fun preciosDao(): PreciosDao
    abstract fun metadataDao(): MetadataDao
    abstract fun favoritosDao(): FavoritosDao
    abstract fun busquedasDao(): BusquedasDao

    companion object {
        @Volatile
        private var INSTANCE: PreciosDatabase? = null

        fun getInstance(context: Context): PreciosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): PreciosDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                PreciosDatabase::class.java,
                "precios_cecosesola.db"
            )
                // WAL (Write-Ahead Logging): permite leer y escribir simultáneamente
                // en hilos distintos sin bloqueo. Crítico para que el Flow de productos
                // no se bloquee mientras replaceAll escribe en IO.
                // En SQLite por defecto, una escritura bloquea TODAS las lecturas.
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
