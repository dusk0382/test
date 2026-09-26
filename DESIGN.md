# DESIGN.md — Contrato de diseño de "Cecosesola Precios"

> Este archivo no es decoración: es la decisión tomada una vez y leída en cada
> sesión. Si algo contradice este documento, gana este documento. Si hay que
> cambiarlo, se cambia aquí primero y después en el código.

## 1. Contexto que manda

- **Trabajo del usuario**: parado en la feria, con una mano ocupada, quiere saber
  **cuánto cuesta algo**. Todo lo demás es secundario.
- **Hardware**: MediaTek Helio G25, 2–3 GB de RAM, panel LCD de gama baja, sol
  fuerte encima. Contraste y legibilidad ganan; adornos pierden.
- **Datos sucios**: 518–527 productos de dos fuentes. Nombres en MAYÚSCULAS
  mezclados con minúsculas, marcas sin tipo de producto ("Rikesa", "Mega miel"),
  tags con basura operativa. La UI **no inventa** lo que los datos no dicen:
  oculta el campo vacío o inútil en vez de mostrarlo.

## 2. Dirección visual: "tablero de precios"

La referencia mental es un **cartel de precios de feria**: números grandes, tinta
oscura sobre papel claro, un solo color chillón para el precio, cero decoración.
No es una tienda online: es una consulta de precio.

Consecuencias concretas:

- El **precio** es el elemento más grande de cada superficie donde aparece.
- **Un solo acento** (naranja de marca `#FD4902`) reservado para precio y acción
  primaria. Ni un color de acento más por pantalla.
- Superficies **cálidas y neutras**, sin gradientes, sin glows, sin blur.
- La jerarquía se hace con **tamaño y peso de tipografía**, no con bordes de
  color, sombras ni cajas anidadas.

## 3. Reglas prohibidas (slop detectado en esta app y en el resto del mundo)

Estas son las que ya cometimos. Ninguna vuelve sin justificación escrita aquí:

1. **Sin emojis como iconos.** Ni en categorías, ni en ajustes, ni en estados vacíos.
2. **Sin bordes de color** en tarjetas ni `border` como única estructura de una card.
3. **Sin etiquetas en MAYÚSCULAS** generadas por la UI (los toppings del OS no cuentan).
4. **Sin filas idénticas de icono+título+subtítulo** repetidas sin jerarquía.
5. **Sin más de 3 tamaños de tipo por pantalla** ni más de un acento de color.
6. **Sin gradientes, glows, sombras de color, glassmorphism** ni decoración sin función.
7. **Sin "todo redondeado"**: lo full-round se reserva a lo interactivo (buscador,
   chips, botones); contenedores de imagen y superficies usan radios medios.
8. **Sin animaciones de entrada de listas** ni crossfade de imágenes (518 ítems en un A53).
9. **Sin mostrar dato inútil**: `presentacion = "item"`, `marca == categoria`, campos
   vacíos no se pintan.
10. **Sin roles de color sin definir**. El esquema anterior declaraba 9 roles: todo
    lo demás (`secondary`, `tertiary`, `secondaryContainer`, `surfaceContainerHighest`,
    `outline`, `outlineVariant`) caía al **baseline lila** de Material3. Ése fue el
    mecanismo real del buscador rosado y de los bordes violeta. Ahora cada rol que
    puede pintar la app está definido y medido (`TemaContrasteTest`).
11. **Sin blanco sobre el naranja de marca**: blanco sobre `#FD4902` da **3.43:1** y
    falla AA para texto normal (era el botón "Agregar al carrito"). Sobre el naranja va
    tinta oscura `#2A1200` (5.17:1). El naranja es **relleno**, no color de texto.

## 4. El primitivo único: "renglón de precio"

**Una sola pieza se repite** en catálogo, favoritos y resultados. No se inventa
una tarjeta distinta por pantalla. Vive en `ui/common/RenglonProducto.kt`:

```
┌──────────────────┐
│  imagen (1:1)    │  ♡        ← contenedor tonal, sin borde de color
│                  │
├──────────────────┤
│ Nombre a 2 líneas           ← bodyMedium, sentence case si viene en MAYÚSCULAS
│ Bs 1.365,50            [ − 2 + ]  o  [ + ]   ← stepper en la misma card
└──────────────────┘
```

> El `(+2,1 %)` que aparece en un borrador anterior de este diagrama **no va en la
> tarjeta**: §7 lo prohíbe explícitamente. El diagrama manda, y su primera versión
> se contradecía a sí misma. La variación de precio vive en el detalle, donde está
> el resto de los datos.

**El carrito es la excepción, y por una razón funcional.** Una línea de carrito
necesita el importe de la línea (unitario × cantidad), que no cabe en una tarjeta
de dos columnas; forzarla sería peor diseño, no más consistencia. Lo que sí se le
exige es el **mismo lenguaje visual**: mismo `shapes.medium`, misma superficie sin
borde, mismo `bodyMedium` para el nombre, mismo token de acento y mismas cifras
tabulares para el precio. La regla es "una sola pieza", no "una sola geometría".

Reglas del primitivo:

- Ancho fijo 2 columnas (`LazyVerticalGrid`, no staggered: ritmo parejo y medición
  más barata).
- Caja de imagen **1:1 siempre**, `ContentScale.Fit` sobre `surfaceContainerHighest`
  (las fotos vienen recortadas sobre blanco: el contenedor tonal evita el bloque blanco).
  *Pendiente*: hoy la caja tiene alto fijo (`AltoImagenTarjeta = 132.dp`), que en
  una pantalla de 360 dp da 1.23:1 y cambia con el ancho. El 1:1 va con
  `Modifier.aspectRatio(1f)`; está anotado como deuda, no como decisión.
- Nombre: máximo 2 líneas con elipsis, altura estable, y **siempre** pasado por
  `formatearNombreProducto`.
- Precio: cifras tabulares, siempre con separador de miles es-VE, y pintado por
  `PriceText` — el único componente que escribe un precio en la app. El color por
  defecto es `LocalColoresPrecio.acento`, **nunca** `colorScheme.primary`.
- El stepper reemplaza al botón `+` **en el mismo lugar** cuando el producto ya está
  en el carrito: agregar deja de requerir abrir el detalle.
- Nada más en la tarjeta. Ni categoría, ni marca, ni descripción.

## 5. Tokens

- **Tipografía**: rampa M3 reducida y explícita (`Type.kt`). La jerarquía se hace con
  **tamaño + peso**, nunca con MAYÚSCULAS ni colores. `bodyMedium` para nombres,
  `labelLarge` para apoyo, y los estilo de precio con cifras tabulares.
  *Limitación medida*: los estilos `*Emphasized` de M3 Expressive son `internal` en
  material3 1.4.0 (como `MotionScheme`): se ven con javap pero el compilador los
  rechaza. Hasta migrar de AGP, la expresividad tipográfica se construye con escala y
  peso propios, no con la API de M3E.
- **Espaciado**: base 4 dp. Gutters de pantalla 16, entre tarjetas 12, dentro de
  tarjeta 8, entre secciones 24. Un `object Spacing` es el único lugar donde se
  escriben números de espaciado.
- **Formas**: `extraSmall 8`, `small 12`, `medium 16`, `large 20`, `extraLarge 28`.
  Full-round solo para interactivos (buscador, chips, FAB).
- **Movimiento** (funcional, no decorativo): 150 ms para cambios de estado;
  spring solo en el stepper del carrito y el toggle de favorito. Sin animaciones
  de entrada de listas. El indicador de la barra de navegación sí puede animar.
- **Color** (medido, ver `Paleta.kt` y `TemaContrasteTest`):
  - `primary` = naranja de marca `#FD4902`, **sólo relleno** (botones, FAB).
  - `onPrimary` = tinta oscura `#2A1200` (5.17:1), nunca blanco (3.43:1 ✗).
  - Acento de **precio** = `#B93300` en claro (5.66:1) y `#FFB59B` en oscuro
    (10.07:1). No se usa `primary` como texto: como relleno y como texto tienen
    requisitos de contraste distintos.
  - Superficies neutras cálidas con tres niveles (`surface`, `surfaceContainerHigh`,
    `surfaceContainerHighest`) para hacer jerarquía por **tono**, no por bordes.
  - `secondary`/`tertiary` son neutros cálidos de la familia de la marca: existen
    para que ningún componente caiga al lila por defecto.
  - Variación de precio, siempre **CEC contra CEC**: sube = rojo apagado (6.16:1),
    baja = verde apagado (6.17:1), en badges chicos, nunca tiñendo la tarjeta.

## 6. Puertas deterministas (no se juzga el diseño a ojo)

El diseño se verifica con cosas que fallan en CI, no con opiniones:

1. **`RubrosTest`** — la clasificación por rubros cubre ≥ 95 % del catálogo real,
   cada producto cae en un rubro y sólo uno, y hay casos puntuales congelados.
2. **`TemaContrasteTest`** — cada par texto/fondo que usa la app cumple WCAG AA
   (4.5:1 cuerpo, 3:1 texto grande e interfaz) en **tema claro y oscuro**. Este test
   existe porque "dark theme que apenas pasa el contraste" es un tell real y además
   un problema de accesibilidad.
   **Y su trampa**: el test lista los pares "que la app realmente pinta", pero esa
   lista se congeló cuando se escribió. Un par nuevo que nadie agrega no está
   verificado aunque el test esté verde — y así quedó el precio del detalle y del
   carrito en el naranja de marca (3.26:1) durante meses. Cuando se agrega o se
   cambia un par de color, **el caso de test va en el mismo commit**.
3. **`FormatoNombreTest`** — nombres en MAYÚSCULAS se muestran en sentence case;
   los que ya vienen en mixta no se tocan (no se destrozan marcas). El formateador
   se aplica en **las tres** pantallas que muestran un producto: si no, el mismo
   producto aparece en minúsculas en la grilla y en mayúsculas en su ficha.
4. **`tools/ui_lint.sh`** — tells deterministas sobre el código Compose, **corriendo
   en la CI antes de los tests**: colores hex fuera del tema, `.dp` de espaciado
   fuera de los tokens, emojis, literales en MAYÚSCULAS, `BorderStroke`, radios
   sueltos, áreas táctiles bajo 48 dp, `Icon` sin etiqueta y precio pintado con un
   `Text` propio. Este script llama a dos auxiliares:
   - **`tools/ui_checks.py`** — lo que no se puede hacer con grep: seguir una
     llamada a `Icon(` abierta y buscar el `contentDescription` de las líneas
     siguientes.
   - **`tools/imports.py`** — paquetes mal escritos (`coil` vs `coil3`, un símbolo
     del subpaquete equivocado), símbolos usados sin importar e imports muertos.
     Existe porque **aquí no se compila** (el SDK de Android es x86-64 y el host
     es aarch64), así que cada error de import cuesta un ciclo entero de CI.
5. **Conteo de tells** — al cerrar un rediseño se revisa la lista de la §3 y se
   anota cuántos quedan. Cuatro o más = no se entrega.

Las skills de `.claude/skills/` (`ceco-tablero-precios`, `ceco-precios-datos`,
`ceco-puertas`) llevan estas mismas reglas en forma ejecutable, con el `file:line`
de cada una. Este documento dice **por qué**; las skills dicen **dónde**.

## 7. Estructura de pantallas

- **Catálogo**: buscador (con limpiar) + icono de filtros con badge + FAB de
  escáner. Sin filas de chips de categoría. Los filtros activos se muestran como
  chips descartables **solo mientras existan**.
- **Filtros** (patrón de la investigación: Baymard documenta que poner el conteo por
  opción es la mejora de mayor impacto de una UI de filtros, y que forzar selección
  única genera abandono):
  - Un solo botón de filtros con badge de cuántos filtros hay activos, en una hoja.
  - **Conteo de resultados junto a cada opción** ("Despensa (109)"). El conteo
    respeta la **búsqueda activa** — es el número de lo que el usuario está
    viendo, no el del catálogo entero. Y **no** depende de las clases ya
    elegidas: si dependiera, al elegir un rubro todos los demás contarían 0 y no
    habría forma de cambiar de opinión.
  - **Multi-selección de rubros** (OR dentro de rubros, AND con el resto): poder ver
    "Limpieza y aseo" + "Despensa" a la vez es una necesidad real, no un extra.
    Sin tope: los 10 rubros se pueden elegir a la vez y el filtro los aplica a
    todos.
  - Orden dentro de la misma hoja, y "Limpiar todo" visible.
  - Los filtros aplicados se muestran como chips descartables **pegados arriba** de la
    lista mientras existan, y el estado sobrevive al volver del detalle.
- **Rubros**: los ~100 tags de la API no se navegan. Se usa la clasificación
  derivada y **medida** de `domain/Rubros.kt` (nombre primero, tag específico como
  respaldo), dentro del selector de filtros con conteo por rubro. `Otros` es una
  opción visible, no un cajón escondido.
  *Pendiente*: este documento pedía también buscador propio y secciones
  alfabéticas dentro de la hoja de filtros. Con 10 opciones no aporta nada y suma
  un campo más, así que la hoja es una `FlowRow` ordenada por cantidad. Si los
  rubros crecen, se revisa.
- **Consistencia en la tarjeta** (Baymard: 64% de los sitios falla en esto; mostrar un
  atributo sólo en algunos ítems hace que el usuario descarte los demás): la tarjeta
  muestra **siempre** imagen, nombre, precio y el control de carrito. El `%` de
  variación y el precio solidario CEC **no** van en la tarjeta aunque existan para
  algunos productos: van en el detalle, donde están todos los datos.
- **Detalle**: nombre, precio grande, precio solidario CEC si existe, y **solo los
  datos reales** (se ocultan presentación vacía, marca redundante). Acción fija
  abajo: stepper si ya está en el carrito, botón si no. Sin imagen gigante vacía.
- **Barra inferior**: `ShortNavigationBar` en Catálogo, Favoritos y Carrito.
  **Oculta** en Detalle y Escáner (tareas de pantalla completa).

## 8. Auditoría por skills (cerrada el 2026-09-26)

Pasada una por una las 6 skills instaladas más 3 escritas para este proyecto. Los
6 hallazgos originales están **resueltos**; quedan abajo como registro de por qué
se hicieron, porque volver a leerlos evita reintroducirlos:

1. **compose-component-design** ✅ — `RenglonProducto` acepta `Modifier`;
   `Dato()` unificado como `FilaDato`; `QtyButton` y `Stepper` fusionados en un
   solo `Stepper` de `ui/common`; el primitivo se mudó de `ui/catalog` a
   `ui/common` (lo usan catálogo y favoritos). *Pendiente*: `PriceText` ya
   acepta `Modifier` ✓, pero `EtapaVacio` y `HojaFiltros` siguen siendo privados
   de su pantalla.
2. **compose-performance** ✅ — el campo de búsqueda es dueño de su estado y empuja
   al VM con debounce; el patrón `MutableStateFlow.also { launch {} }` ya no está;
   `LocalUsdPrecio` pasó de `staticCompositionLocalOf` a `compositionLocalOf`
   (con el `static`, cambiar la moneda recomponía **toda** la app); `Regex`
   compiladas a nivel de archivo en vez de por llamada y por tarjeta; el
   formateo del precio va en `remember`; `sincronizando`/`yaRefrescado` se
   exponen como `StateFlow` de solo lectura.
3. **compose-animations** ✅ — `animateContentSize` en el stepper, `Crossfade`
   para vacío↔resultados, `AnimatedVisibility` en el FAB y en la barra inferior.
   *Pendiente*: transiciones de `fadeIn/out` en el `NavHost` y `AnimatedVisibility`
   en el badge del carrito.
4. **styles** (Google) ✅ en lo aplicable — la API `Styles` requiere Compose
   1.12-alpha, bloqueada por AGP 9.1: no aplica. Los 7 radios sueltos quedaron en
   `MaterialTheme.shapes.*`, y ahora `ui_lint.sh` falla si vuelve a aparecer uno.
5. **edge-to-edge** ✅ — `enableEdgeToEdge()` en `MainActivity` y las tres llamadas
   de apariencia de barras en `Theme.kt`. *Pendiente*: `themes.xml` todavía pone
   `android:statusBarColor` en el naranja de marca y fondo de ventana blanco, lo
   que se ignora en API 35+ pero produce un destello blanco antes del primer
   frame en versiones anteriores.
6. **anti-ai-slop-ui** ✅ — cero `border` sueltos, cero emojis, cero hex fuera del
   tema, cero MAYÚSCULAS de UI. Su lint web no escanea Kotlin, así que el grep
   propio (`tools/ui_lint.sh` + `ui_checks.py`) es el que manda, y ahora corre en
   la CI.

## 9. Deuda conocida (anotada, no escondida)

- **La caja de imagen de la tarjeta no es 1:1** (§4): tiene alto fijo de 132 dp.
- **La migración de Room v1→v2 no está verificada por nada**: `exportSchema = false`,
  no hay schemas exportados para diffear, no hay `MigrationTestHelper` y no hay
  tests instrumentados. Lo único que la protege es el comentario.
- **Sin tests instrumentados**: nada de UI, nada de Room real, nada de escáner.
  El smoke test de CI *abre* la app; no la usa.
- **Las imágenes de la API oficial llegan por `http://`** al mismo host que sirve
  el GraphQL, con la excepción de cleartext en `network_security_config.xml`. Un
  atacante en el camino puede inyectar imágenes y el payload de precios.
- **`ProductEntity.updatedAt` se recorta con `substringBefore('T')`**: funciona
  para el ISO-8601 del mirror, no verificado para el de la API oficial.
- **No hay reconciliación**: un producto que desaparece de la API oficial sigue
  ocupando su fila. Es el precio de no borrar, y se acepta a propósito.
