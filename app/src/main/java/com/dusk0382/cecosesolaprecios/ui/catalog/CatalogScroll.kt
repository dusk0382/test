package com.dusk0382.cecosesolaprecios.ui.catalog

/**
 * Qué hacer con la grilla cuando cambia la búsqueda **asentada** (la que ya pasó
 * el debounce y produjo la lista que se está viendo).
 *
 * Las reglas, y por qué:
 *
 * - Al empezar a buscar, los resultados arrancan arriba y se guarda el punto de
 *   partida. Ese punto es lo que se restaura al borrar la búsqueda.
 * - Al borrar la búsqueda, se vuelve al punto de partida. Es el caso que el
 *   usuario pidió explícitamente: "al borrar la búsqueda debe respetar el scroll
 *   previo".
 * - Cambiar de una búsqueda a otra (teclear o borrar una letra) **no mueve** la
 *   grilla. Antes se restauraba la posición guardada de cada prefijo, que solo
 *   tenía sentido cuando ese prefijo se escribió: borrar una letra devolvía al
 *   usuario a la posición de la pasada de escritura, no a la que tenía.
 *
 * Es una función pura, sin Compose, para poder probarla en la JVM. La pantalla
 * se encarga de guardar el resultado y de esperar a que la lista mostrada sea
 * la del estado actual antes de aplicar [DecisionScroll.destino].
 */
internal data class DecisionScroll(
    /** Índice al que ir, o null si no hay que mover la grilla. */
    val destino: Int?,
    /** Dónde estaba el usuario antes de empezar a buscar. */
    val posicionPrevia: Int?,
)

internal fun decidirScrollBusqueda(
    consultaAnterior: String,
    consultaNueva: String,
    indiceActual: Int,
    posicionPrevia: Int?,
): DecisionScroll = when {
    consultaAnterior == consultaNueva -> DecisionScroll(null, posicionPrevia)

    // Empezó una búsqueda. Si no había posición previa, se guarda la actual y se
    // va al principio. Si ya había una (la búsqueda se restauró después de morir
    // el proceso), no se pisa con la posición de los resultados ni se mueve: el
    // usuario no escribió nada nuevo.
    consultaAnterior.isBlank() && consultaNueva.isNotBlank() -> DecisionScroll(
        destino = if (posicionPrevia == null) 0 else null,
        posicionPrevia = posicionPrevia ?: indiceActual,
    )

    // Se borró la búsqueda: volver al punto de partida.
    consultaAnterior.isNotBlank() && consultaNueva.isBlank() ->
        DecisionScroll(posicionPrevia ?: 0, null)

    // Cambió una búsqueda por otra: no mover.
    else -> DecisionScroll(null, posicionPrevia)
}
