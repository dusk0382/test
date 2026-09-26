package com.dusk0382.cecosesolaprecios.data.repository

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
 * Si lo que el usuario escribió es un código de barras (EAN-8 de 8 dígitos o
 * EAN-13 de 13, que son los que trae la API oficial), se devuelve tal cual para
 * compararlo exacto contra la columna. Cualquier otra entrada devuelve "" para
 * que la cláusula `barcode = ''` no empareje nada.
 *
 * Por qué exacta y no un `LIKE`: un barcode es un identificador, no un texto. Una
 * coincidencia parcial sobre una columna sin índice recorre todos los renglones
 * y además puede traer productos distintos, que es peor que no traer.
 *
 * Por qué se limita a 8 y 13 dígitos: "2026" también son 4 dígitos y es un año;
 * un número de precio o un peso en gramos también son dígitos. Sin este filtro,
 * escribir un precio en el buscador devolvería productos por coincidencia de
 * barcode.
 */
internal fun barcodeDeConsulta(query: String): String {
    val cruda = query.trim()
    val esBarcode = (cruda.length == 8 || cruda.length == 13) && cruda.all { it.isDigit() }
    return if (esBarcode) cruda else ""
}
