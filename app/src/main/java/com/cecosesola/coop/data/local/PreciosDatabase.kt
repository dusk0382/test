package com.cecosesola.coop.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProductoEntity::class, MetadataEntity::class, FavoritoEntity::class, BusquedaEntity::class],
    version = 3,
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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PreciosDatabase::class.java,
                    "precios_cecosesola.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
