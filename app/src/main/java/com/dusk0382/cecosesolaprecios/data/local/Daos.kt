package com.dusk0382.cecosesolaprecios.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    /**
     * Búsqueda + filtro por clases (multi-selección OR entre clases).
     *
     * `clasesCsv` es una lista separada por comas en vez de N slots fijos: la
     * versión anterior tenía 6 parámetros `clase0..clase5` y `searchFlow` pasaba
     * `getOrNull(0..5)`, así que al elegir 7 rubros (hay 10) la UI seguía
     * mostrando los 7 chips y el badge "7", pero la consulta solo aplicaba los 6
     * primeros en orden alfabético: el filtro mentía sobre el resultado.
     * Room no acepta `List<String>` como parámetro en un @Query, y `IN (:a, :b)`
     * con cardinalidad variable tampoco compila, así que la lista va cosida en un
     * string y se desarma con un `,` en la condición.
     */
    @Query(
        """
        SELECT * FROM products
        WHERE (:query = '' OR nombreNormalizado LIKE '%' || :query || '%' ESCAPE '\')
          AND (:clasesCsv = '' OR instr(',' || :clasesCsv || ',', ',' || clase || ',') > 0)
        ORDER BY
          CASE WHEN :orden = 'precio_asc' THEN precioBs END ASC,
          CASE WHEN :orden = 'precio_desc' THEN precioBs END DESC,
          CASE WHEN :orden = 'nombre' THEN nombreNormalizado END ASC,
          -- Sin desempate el orden no es estable entre recomposiciones: dos
          -- productos con el mismo precio se intercambian de fila en el LazyGrid.
          localId ASC
        """
    )
    fun searchFlow(
        query: String,
        clasesCsv: String,
        orden: String,
    ): Flow<List<ProductEntity>>

    /**
     * Conteo por clase **para la consulta actual**: es el número que va junto a
     * cada opción del filtro, y ese número tiene que ser el de lo que el usuario
     * está viendo. La versión anterior era un `GROUP BY clase` sobre la tabla
     * entera, así que escribiendo "leche" el selector ofrecía "Despensa (109)":
     * un número que no era el resultado de nada. Es la mejora de mayor impacto
     * según Baymard, y sirve de nada si el número miente.
     *
     * Se filtra por la búsqueda pero **no** por las clases ya seleccionadas: si
     * se filtrara, al elegir un rubro todos los demás contarían 0 y no habría
     * forma de cambiar de opinión.
     */
    @Query(
        """
        SELECT clase, COUNT(*) AS total FROM products
        WHERE (:query = '' OR nombreNormalizado LIKE '%' || :query || '%' ESCAPE '\')
        GROUP BY clase ORDER BY COUNT(*) DESC
        """
    )
    fun conteoPorClaseFlow(query: String): Flow<List<ClaseConteo>>

    @Query("SELECT * FROM products WHERE localId = :id")
    fun byIdFlow(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun byBarcode(barcode: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    fun countFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Query("SELECT * FROM products")
    suspend fun all(): List<ProductEntity>

    @Upsert
    suspend fun upsertAll(items: List<ProductEntity>)

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
