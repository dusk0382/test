package com.dusk0382.cecosesolaprecios.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query(
        """
        SELECT * FROM products
        WHERE (:query = '' OR nombreNormalizado LIKE '%' || :query || '%')
          AND (:clase0 IS NULL OR clase = :clase0 OR clase = :clase1 OR clase = :clase2
               OR clase = :clase3 OR clase = :clase4 OR clase = :clase5)
        ORDER BY
          CASE WHEN :orden = 'precio_asc' THEN precioBs END ASC,
          CASE WHEN :orden = 'precio_desc' THEN precioBs END DESC,
          CASE WHEN :orden = 'nombre' THEN nombreNormalizado END ASC
        """
    )
    fun searchFlow(
        query: String,
        clase0: String?,
        clase1: String?,
        clase2: String?,
        clase3: String?,
        clase4: String?,
        clase5: String?,
        orden: String,
    ): Flow<List<ProductEntity>>

    @Query("SELECT clase, COUNT(*) AS total FROM products GROUP BY clase ORDER BY COUNT(*) DESC")
    fun conteoPorClaseFlow(): Flow<List<ClaseConteo>>

    @Query("SELECT * FROM products WHERE localId = :id")
    fun byIdFlow(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun byBarcode(barcode: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    fun countFlow(): Flow<Int>

    @Query("SELECT repoId FROM products WHERE repoId IS NOT NULL")
    suspend fun allRepoIds(): List<String>

    @Query("SELECT * FROM products WHERE repoId = :repoId LIMIT 1")
    suspend fun byRepoId(repoId: String): ProductEntity?

    @Query("SELECT * FROM products WHERE apiId = :apiId LIMIT 1")
    suspend fun byApiId(apiId: String): ProductEntity?

    @Query("SELECT * FROM products WHERE nombreNormalizado = :normalizado LIMIT 1")
    suspend fun byNombreNormalizado(normalizado: String): ProductEntity?

    @Query("SELECT * FROM products")
    suspend fun all(): List<ProductEntity>

    @Upsert
    suspend fun upsertAll(items: List<ProductEntity>)

    @Upsert
    suspend fun upsert(item: ProductEntity): Long

    @Query("DELETE FROM products WHERE fuente = 'api'")
    suspend fun deleteApiOnly()
}

@Dao
interface FavoriteDao {
    @Query("SELECT productId FROM favorites")
    fun idsFlow(): Flow<List<Long>>

    @Query(
        """
        SELECT p.* FROM products p JOIN favorites f ON f.productId = p.localId
        ORDER BY f.addedAt DESC
        """
    )
    fun favoritosConProductosFlow(): Flow<List<ProductEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE productId = :id)")
    suspend fun isFavorite(id: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE productId = :id)")
    fun isFavoriteFlow(id: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE productId = :id")
    suspend fun remove(id: Long)
}

@Dao
interface CartDao {
    @Query(
        """
        SELECT c.productId AS productId, c.quantity AS quantity, p.nombre AS nombre,
               p.precioBs AS precioBs, p.precioCec AS precioCec
        FROM cart_items c JOIN products p ON p.localId = c.productId
        ORDER BY c.addedAt
        """
    )
    fun itemsWithProductFlow(): Flow<List<CartLine>>

    @Upsert
    suspend fun upsert(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :id")
    suspend fun remove(id: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clear()

    @Query("SELECT quantity FROM cart_items WHERE productId = :id")
    suspend fun quantityOf(id: Long): Int?

    @Query("SELECT quantity FROM cart_items WHERE productId = :id")
    fun quantityOfFlow(id: Long): Flow<Int?>

    @Query("SELECT COUNT(*) FROM cart_items")
    fun countFlow(): Flow<Int>

    @Query("SELECT productId, quantity FROM cart_items")
    fun quantitiesFlow(): Flow<List<CartCantidad>>
}

/** Fila de conteo por clase (para el selector de filtros con conteo por opción). */
data class ClaseConteo(
    val clase: String,
    val total: Int,
)

/** Proyección productId → cantidad (mapa para steppers de tarjeta). */
data class CartCantidad(
    val productId: Long,
    val quantity: Int,
)

data class CartLine(
    val productId: Long,
    val quantity: Int,
    val nombre: String,
    val precioBs: Double,
    /** Precio solidario (USD). Null mientras la API oficial no haya enriquecido. */
    val precioCec: Double?,
)

@Dao
interface MetaDao {
    @Upsert
    suspend fun put(item: MetaEntity)

    @Query("SELECT value FROM meta WHERE `key` = :key")
    suspend fun get(key: String): String?
}
