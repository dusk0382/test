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
    // v2: `clase` en products (rubro derivado, columna filtrable).
    // Sin fallback destructivo a propósito: `favorites` y `cart_items` son datos
    // del usuario, no cache. Toda versión nueva exige una Migration en MIGRATIONS;
    // si falta, Room falla al abrir en vez de borrar el carrito en silencio.
    // `products` sí es cache, pero eso no justifica arriesgar las otras tablas.
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
