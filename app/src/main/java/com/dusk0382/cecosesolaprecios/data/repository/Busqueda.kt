package com.dusk0382.cecosesolaprecios.data.repository

import com.dusk0382.cecosesolaprecios.domain.esBarcodeValido
import com.dusk0382.cecosesolaprecios.domain.normalizarNombre

/**
 * Transformación de lo que el usuario escribe en el buscador a lo que entiende
 * la consulta SQL. Son funciones puras, sin Android, precisamente para poder
 * probarlas en la JVM: la búsqueda es la interacción más frecuente de la app y
 * no puede quedar como un `string` pelado armado en el repositorio.
 */

/**
 * La columna `nombreNormalizado` está normalizada, así que la consulta también:
 * escribir "Limón" tiene que encontrar "limon".
 */
internal fun consultaDeBusqueda(query: String): String = normalizarNombre(query)

/**
 * Escapa los comodines de `LIKE` antes de mandarlos a la consulta.
 *
 * Sin esto, escribir `%` en el buscador traía los 518 productos y `_`
 * emparejaba cualquier carácter. Un `%` es un carácter perfectamente normal en
 * un nombre de producto ("QUESO 100%") y el usuario lo escribe sin saber que
 * está disparando un comodín. El orden importa: primero la barra invertida,
 * después los comodines, o un `\` ya escapado se vuelve a escapar dos veces.
 */
internal fun escaparLike(texto: String): String = texto
    .replace("\\", "\\\\")
    .replace("%", "\\%")
    .replace("_", "\\_")

/**
 * Si lo que el usuario escribió es un código de barras, se devuelve tal cual para
 * compararlo exacto contra la columna. Cualquier otra entrada devuelve "" para
 * que la cláusula `barcode = ''` no empareje nada.
 *
 * Se aceptan de 8 a 14 dígitos, el mismo rango que el escáner: EAN-8, UPC-A (12),
 * EAN-13 y GTIN-14. Antes solo se reconocían 8 y 13, así que un UPC-A escaneado
 * funcionaba pero escrito a mano caía en la rama de texto y no encontraba nada.
 * Los códigos de puros ceros no cuentan: la API los usa como relleno.
 *
 * Por qué exacta y no un `LIKE`: un barcode es un identificador, no un texto. Una
 * coincidencia parcial sobre una columna sin índice recorre todos los renglones
 * y además puede traer productos distintos, que es peor que no traer.
 *
 * Por qué no cualquier cantidad de dígitos: "2026" son 4 dígitos y es un año; un
 * precio, un peso en gramos o una cantidad también son 4 o 6 dígitos. Sin este
 * filtro, escribir un precio en el buscador devolvería productos por
 * coincidencia de barcode. Tampoco se aceptan letras: el escáner descarta lo que
 * no sea dígito porque el código ya viene leído, pero acá no se corrige lo que
 * el usuario escribió.
 */
internal fun barcodeDeConsulta(query: String): String {
    val cruda = query.trim()
    return if (esBarcodeValido(cruda)) cruda else ""
}
