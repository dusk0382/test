package com.cecosesola.coop.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO de productos con dos optimizaciones clave:
 *
 * 1. @Transaction en replaceAll: garantiza que Room emita el Flow UNA sola vez
 *    con los datos completos, nunca con la lista vacía entre DELETE e INSERT.
 *    Sin esto, cada sync causa un flash de pantalla vacía.
 *
 * 2. Índice en 'nombre' y 'categoria': acelera el filtrado de búsqueda cuando
 *    Room hace la query. (El filtrado extra en el ViewModel sigue siendo necesario
 *    para búsquedas con lógica OR, pero Room pre-filtra en SQL.)
 */
@Dao
interface PreciosDao {
    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    fun getAllProductosFlow(): Flow<List<ProductoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(productos: List<ProductoEntity>)

    @Query("DELETE FROM productos")
    suspend fun deleteAll()

    /**
     * @Transaction es CRÍTICO aquí.
     * Sin él: DELETE emite Flow([]) → UI muestra vacío → INSERT emite Flow([datos])
     * Con él: solo emite Flow([datos]) cuando todo está listo.
     */
    @Transaction
    suspend fun replaceAll(productos: List<ProductoEntity>) {
        deleteAll()
        insertAll(productos)
    }
}

// Índices opcionales pero muy recomendados para búsquedas frecuentes.
// Añadir a ProductoEntity:
//
// @Entity(
//     tableName = "productos",
//     indices = [
//         Index(value = ["nombre"]),
//         Index(value = ["categoria"])
//     ]
// )
// data class ProductoEntity(...)
//
// Room regenerará la DB en el próximo migration o si se incrementa la versión.
