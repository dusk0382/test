package com.dusk0382.cecosesolaprecios.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        FavoriteEntity::class,
        CartItemEntity::class,
        MetaEntity::class,
    ],
    // v2: `clase` en products (rubro derivado, columna filtrable). El fallback
    // destructivo vive en AppModule: la BD es un cache re-sincronizable, nunca
    // datos del usuario (favoritos/carrito sí sobreviven porque están en otras
    // tablas — solo products se trunca y se repone del mirror en el primer sync).
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun cartDao(): CartDao
    abstract fun metaDao(): MetaDao

    companion object {
        const val NAME = "cecosesola.db"

        /** v2 añade la columna `clase` (rubro derivado) sobre products. */
        val MIGRATIONS = arrayOf(
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE products ADD COLUMN clase TEXT NOT NULL DEFAULT 'Otros'")
                }
            },
        )
    }
}
