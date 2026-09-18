package com.dusk0382.cecosesolaprecios.domain

/**
 * Clasificación de rubros **derivada y medida**, porque la fuente no trae una
 * taxonomía usable:
 *
 * - `departments` del GraphQL son centros de costo / áreas físicas (VIVERES 491,
 *   "Deposito Ruiz Pineda" 65, "Feria Grande" 25, "MINI FERIA" 19) y 81 productos
 *   viven en más de uno: es logística, no navegación.
 * - `tags` cubre 366/527 y mezcla tipos de producto con **banderas operativas**
 *   (`secundario` 47, `prioridad` 30, `upc` 22, `sin_existencia` 8), duplicados
 *   (`cereales`/`cereal`, `verdura`/`verduras`, `enlatado`/`enlatados`) y erratas
 *   (`seundario`).
 * - Los tags amplios (`viveres` 63, `hogar`, `aseo`) son canastas, no tipos: al
 *   usarlos como respaldo caían productos de limpieza y bebidas en "Despensa"
 *   (el acuerdo bajaba de 90 % a 67 %). Por eso el **nombre manda** y el tag sólo
 *   ayuda cuando es específico.
 *
 * Medido contra los fixtures reales (`precios.json` 518 + `downloadfile_graphql.json`
 * 527): cobertura > 97 % del catálogo con estos rubros, quedando fuera sólo
 * combos, remates y reciclaje (que ni son productos de consulta).
 *
 * Sin emojis ni iconos: la etiqueta es el dato (ver DESIGN.md §3).
 */
enum class Rubro(val etiqueta: String) {
    MASCOTAS("Mascotas"),
    FERRETERIA("Ferretería"),
    FRUTAS_Y_VERDURAS("Frutas y verduras"),
    BEBIDAS_Y_LACTEOS("Bebidas y lácteos"),
    DULCES_Y_SNACKS("Dulces y snacks"),
    PANADERIA("Panadería"),
    SALSAS_Y_ADEREZOS("Salsas y aderezos"),
    DESPENSA("Despensa"),
    LIMPIEZA_Y_ASEO("Limpieza y aseo"),
    OTROS("Otros"),
}

/**
 * Un patrón por rubro. Los límites de palabra (`\b...\b`) y el plural opcional no son
 * cosmética: sin ellos "crema" agarraba "Leche **Descrema**da", "pasta" agarraba
 * "**Pasta** Dental" (despensa en vez de aseo) y "tornillo" clasificaba la pasta
 * corta como ferretería. El plural cubre "es" y "s" ("tallarin**es**", "protector**es**")
 * sin listar cada variante a mano.
 */
private fun reg(vararg alternativas: String): Regex =
    Regex("\\b(?:" + alternativas.joinToString("|") + ")(?:es|s)?\\b")

/**
 * Orden = prioridad de match, y es parte de la regla: lo ambiguo primero.
 *
 * 1. Limpieza/aseo va **antes** de bebidas y despensa porque "crema dental",
 *    "pasta dental", "lavalozas crema" y "cepillo" caerían en el rubro equivocado.
 * 2. Bebidas va antes que frutas: en un nombre de bebida la fruta suele ser
 *    ingrediente ("refresco de uva"), no el producto.
 * 3. "Podría ser más preciso con más contexto, pero esto es lo que la fuente
 *    permite": la clasificación perfecta desde el nombre no existe (marcas sueltas
 *    como "Rikesa" o "Haragan" no dicen qué son). Se mide el resultado en
 *    `RubrosTest` y `Otros` queda visible, no escondido.
 */
private val REGLAS: List<Pair<Rubro, Regex>> = listOf(
    // Deliberadamente estrecho: "Pan de Perros Caliente" (pan de hot dog) NO es
    // comida de mascota, y "gato" suelto también agarra marcas de café.
    Rubro.MASCOTAS to reg("perrarina", "mascota", "alimento para perro", "alimento para gato"),
    // Sin "tornillo" (es forma de pasta) ni "cable" (genérico): sólo lo eléctrico real.
    Rubro.FERRETERIA to reg("bombillo", "fosforo", "led"),
    Rubro.LIMPIEZA_Y_ASEO to reg(
        "detergente", "jabon", "limpiador", "limpieza", "desinfectante", "cloro", "lavaplatos",
        "lavalozas", "lava lava", "suavizante", "suavisante", "blanqueador", "esponja", "escoba",
        "trapero", "bolsa", "saco", "papel", "servilleta", "aluminio", "plastico", "desechable",
        "multiuso", "cera", "vela", "velon", "tobo", "toallin", "toalla", "shampoo", "shampu",
        "champu", "dental", "cepillo", "desodorante", "prestobarba", "panal", "higiene", "mentol",
        "avivir", "nutribela", "protector", "oki",
    ),
    Rubro.BEBIDAS_Y_LACTEOS to reg(
        "leche", "lacteo", "yogur", "queso", "mantequilla", "matequilla", "jugo", "malta", "refresco",
        "bebida", "agua", "cerveza", "vino", "nescafe", "cafe", "chocolate", "malteada", "suero",
        "natilla", "cerelac", "chicha", "manzanilla", "yogurt",
    ),
    Rubro.FRUTAS_Y_VERDURAS to reg(
        "verdura", "fruta", "tomate", "cebolla", "papa", "zanahoria", "lechuga", "repollo", "aji",
        "cilantro", "perejil", "platano", "cambur", "topocho", "naranja", "limon", "aguacate", "yuca",
        "auyama", "remolacha", "espinaca", "rabano", "pepino", "pimenton", "guayaba", "mango", "pina",
        "melon", "patilla", "matero", "abono", "brote", "tamarindo", "quinchoncho", "jamaica", "manzana",
        "ajo", "guisante", "uvas pasas",
    ),
    Rubro.DULCES_Y_SNACKS to reg(
        "dulce", "chucheria", "chupeta", "caramelo", "chicle", "golosina", "gelatina", "flip", "pudin",
        "merengada", "snack", "papita", "mani", "galleton", "galleta", "galeta", "helado", "sirope",
        "mermelada", "arequipe", "cocada", "pepito", "pepitona", "maizorito", "miel", "compota",
        "crema de arroz", "nestun",
    ),
    Rubro.PANADERIA to reg(
        "pan", "panad", "baguette", "arepa", "tortilla", "pastel", "torta", "semita", "francela",
        "bizcocho", "ponque", "cachito", "catalina", "panetton",
    ),
    Rubro.SALSAS_Y_ADEREZOS to reg(
        "salsa", "mayonesa", "mostaza", "ketchup", "aderezo", "vinagre", "aceite", "manteca",
        "margarina", "alino", "adobo", "cubito", "condimento", "azafran", "oregano", "comino",
        "curcuma", "aceituna", "alcaparra", "sal",
    ),
    Rubro.DESPENSA to reg(
        "arroz", "pasta", "tallarin", "espagueti", "spaghetti", "macarron", "fideo", "harina", "maizina",
        "maiz", "avena", "granos", "caraota", "frijol", "garbanzo", "lenteja", "azucar", "panela",
        "levadura", "cereal", "fororo", "cornflake", "hojuela", "atun", "sardina", "enlatado",
        "encurtido", "girasol", "trigo", "granola", "sopa", "soya", "afrecho", "huevo", "mortadela",
        "diablito", "pizza", "pasticho", "vainilla", "nutrisoy", "frutos secos",
    ),
)

/**
 * Tags que **sí** aportan tipo de producto. Los amplios u operativos quedan fuera a
 * propósito: usarlos como respaldo empeoraba la clasificación.
 */
private val TAG_ARUBRO: Map<String, Rubro> = mapOf(
    "verdura" to Rubro.FRUTAS_Y_VERDURAS,
    "verduras" to Rubro.FRUTAS_Y_VERDURAS,
    "fruta" to Rubro.FRUTAS_Y_VERDURAS,
    "frutas" to Rubro.FRUTAS_Y_VERDURAS,
    "hortaliza" to Rubro.FRUTAS_Y_VERDURAS,
    "lacteos" to Rubro.BEBIDAS_Y_LACTEOS,
    "cafe" to Rubro.BEBIDAS_Y_LACTEOS,
    "bebida" to Rubro.BEBIDAS_Y_LACTEOS,
    "bebidas" to Rubro.BEBIDAS_Y_LACTEOS,
    "jugos" to Rubro.BEBIDAS_Y_LACTEOS,
    "panes" to Rubro.PANADERIA,
    "pan" to Rubro.PANADERIA,
    "panaderia" to Rubro.PANADERIA,
    "salsas" to Rubro.SALSAS_Y_ADEREZOS,
    "mayonesa" to Rubro.SALSAS_Y_ADEREZOS,
    "aderezo" to Rubro.SALSAS_Y_ADEREZOS,
    "aceite" to Rubro.SALSAS_Y_ADEREZOS,
    "cereales" to Rubro.DESPENSA,
    "cereal" to Rubro.DESPENSA,
    "pasta" to Rubro.DESPENSA,
    "granos" to Rubro.DESPENSA,
    "harina" to Rubro.DESPENSA,
    "enlatado" to Rubro.DESPENSA,
    "enlatados" to Rubro.DESPENSA,
    "sardina" to Rubro.DESPENSA,
    "caraotas" to Rubro.DESPENSA,
    "embutidos" to Rubro.DESPENSA,
    "chucheria" to Rubro.DULCES_Y_SNACKS,
    "chucherias" to Rubro.DULCES_Y_SNACKS,
    "confiteria" to Rubro.DULCES_Y_SNACKS,
    "galletas" to Rubro.DULCES_Y_SNACKS,
    "gelatina" to Rubro.DULCES_Y_SNACKS,
    "jabon" to Rubro.LIMPIEZA_Y_ASEO,
    "limpieza" to Rubro.LIMPIEZA_Y_ASEO,
    "detergente" to Rubro.LIMPIEZA_Y_ASEO,
)

/**
 * Rubro de un producto. El nombre manda; el tag específico resuelve el resto
 * (28 productos del catálogo real, típicamente por nombre de marca: "Rikesa",
 * "Mega miel"). Lo que no se puede clasificar **no se esconde**: es [Rubro.OTROS].
 */
fun rubroDe(nombre: String, tags: List<String> = emptyList()): Rubro {
    val n = normalizarNombre(nombre)
    REGLAS.forEach { (rubro, patron) -> if (patron.containsMatchIn(n)) return rubro }
    tags.forEach { tag ->
        TAG_ARUBRO[normalizarNombre(tag)]?.let { return it }
    }
    return Rubro.OTROS
}

/**
 * Los nombres del mirror vienen mezclados: "ABONO LIQUIDO" junto a "aceite de oliva
 * extra virgen capri 250 cm3". Un nombre **enteramente** en mayúsculas se muestra en
 * sentence case; si viene en mixta no se toca, para no destrozar marcas ni siglas
 * ("PREMIUM" en medio de un nombre mixto queda como esté).
 */
fun formatearNombreProducto(nombre: String): String {
    val limpio = nombre.replace(Regex("\\s+"), " ").trim()
    val letras = limpio.filter { it.isLetter() }
    if (letras.isEmpty() || letras != letras.uppercase()) return limpio
    return limpio.lowercase().replaceFirstChar { it.titlecase() }
}
