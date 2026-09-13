package com.dusk0382.cecosesolaprecios.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        FavoriteEntity::class,
        CartItemEntity::class,
        MetaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun cartDao(): CartDao
    abstract fun metaDao(): MetaDao

    companion object {
        const val NAME = "cecosesola.db"
    }
}
