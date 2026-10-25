# DESIGN.md — Contrato de diseño de "Cecosesola Precios"

> Este archivo no es decoración: es la decisión tomada una vez y leída en cada
> sesión. Si hay que cambiarlo, se cambia aquí primero y después en el código.
>
> **Vigencia (2026-10-23).** En las *decisiones* manda este documento; en los
> *hechos* manda el código. Si acá dice cómo se ve algo y la app instalada dice
> otra cosa, gana el código y este archivo se corrige en el mismo commit. Lo
> único que no envejece solo es lo que está codificado como test o checker (§6);
> el resto es prosa y se relee contra la pantalla antes de creerle.

## 1. Contexto que manda

- **Trabajo del usuario**: parado en la feria, con una mano ocupada, quiere saber
  **cuánto cuesta algo**. Todo lo demás es secundario.
- **Hardware**: MediaTek Helio G25, 2–3 GB de RAM, panel LCD de gama baja, sol
  fuerte encima. Contraste y legibilidad ganan; adornos pierden.
- **Datos sucios y crecientes**: el catálogo es la unión de dos fuentes (mirror +
  API oficial) y crece con ellas: al 2026-10-24 el mirror trae 542 productos y la
  API 557, y el merge simulado da ~590 filas porque también se muestran las filas
  solo-API. No hay un total fijo de 518/527. Nombres en MAYÚSCULAS mezclados con
  minúsculas, marcas sin tipo de producto ("Rikesa", "Mega miel"), tags con basura
  operativa. La UI **no inventa** lo que los datos no dicen: oculta el campo vacío
  o inútil en vez de mostrarlo.

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
8. **Sin animaciones de entrada de listas** ni crossfade de imágenes (cientos de
   ítems en un A53).
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
12. **Sin etiquetas ni descripciones secundarias innecesarias.** Deja que el
    diseño haga la explicación.

## 4. El primitivo único: "renglón de precio"

**Una sola pieza se repite** en catálogo, favoritos y resultados. No se inventa
una tarjeta distinta por pantalla. Vive en `ui/common/RenglonProducto.kt`:

```
┌──────────────────┐
│  imagen          │  ♡        ← plato claro, sin borde de color
│                  │
├──────────────────┤
│ Nombre a 2 líneas           ← bodyMedium, sentence case si viene en MAYÚSCULAS
│ Bs 1.365,50                 ← precio en su propia línea
│              [ − 2 + ]  o  [ + ]   ← control debajo, alineado a la derecha
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
En la fila del carrito el nombre ocupa el ancho completo y el importe va en la
segunda fila, a la derecha, con `PrecioTarjeta`; el stepper queda a la izquierda.
El control no le roba ancho al nombre ni compite con la plata.

Reglas del primitivo:

- Ancho fijo 2 columnas (`LazyVerticalGrid`, no staggered: ritmo parejo y medición
  más barata).
- Caja de imagen **1:1** (`Modifier.aspectRatio(1f)`), `ContentScale.Fit` sobre
  `PlatoImagen`. El plato es **claro en los dos temas** a propósito: las fotos
  del origen vienen recortadas sobre blanco, así que su fondo se funde con el
  plato en vez de quedar un rectángulo blanco flotando sobre la tarjeta oscura.
  La tinta del corazón es oscura en ambos temas porque pisa el plato, no la
  superficie.
- Nombre: máximo 2 líneas con elipsis, altura estable, y **siempre** pasado por
  `formatearNombreProducto`.
- Precio: cifras tabulares, siempre con separador de miles es-VE, y pintado por
  `PriceText` — el único componente que escribe un precio en la app. El color por
  defecto es `LocalColoresPrecio.acento`, **nunca** `colorScheme.primary`.
- El stepper reemplaza al botón `+` **en el mismo espacio reservado** (la línea de
  abajo, a la derecha) cuando el producto ya está en el carrito: agregar deja de
  requerir abrir el detalle y la tarjeta no cambia de alto. El precio no comparte
  línea con el control: dos círculos de 48 dp son más fuertes que el número, y en
  una pantalla de precios no puede ganar el stepper.
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
  tarjeta 8, entre secciones 24. Un `object Espacio` es el único lugar donde se
  escriben números de espaciado.
- **Formas**: `extraSmall 8`, `small 12`, `medium 16`, `large 20`, `extraLarge 28`.
  Full-round solo para interactivos (buscador, chips, botones).
- **Movimiento** (funcional, no decorativo): 150 ms para cambios de estado (barra
  inferior, badge del carrito y transiciones del `NavHost`); spring solo en el
  stepper del carrito, vía `animateContentSize`. El favorito no anima: solo cambia
  el icono. Sin animaciones de entrada de listas. El indicador de la barra de
  navegación sí puede animar.
- **Color** (medido, ver `Paleta.kt` y `TemaContrasteTest`):
  - `primary` = naranja de marca `#FD4902`, **sólo relleno** (botones, controles de
    acción).
  - `onPrimary` = tinta oscura `#2A1200` (5.17:1), nunca blanco (3.43:1 ✗).
  - Acento de **precio** = `#B93300` en claro (5.66:1) y `#FFB59B` en oscuro
    (10.07:1). No se usa `primary` como texto: como relleno y como texto tienen
    requisitos de contraste distintos.
  - Superficies neutras cálidas con tres niveles (`surface`, `surfaceContainerHigh`,
    `surfaceContainerHighest`) para hacer jerarquía por **tono**, no por bordes.
  - `PlatoImagen` = `#FFF8F6` y `TintaSobrePlato` = `#241A17` (16.20:1): fondo y
    tinta de la caja de imagen. No cambian con el tema (ver §4).
  - **Dos monedas en pantalla: bolívares y el precio solidario en dólares.** "CEC" es
    el código con el que la API nombra ese precio y **no aparece nunca en la
    interfaz**: `precioBs` se muestra "Bs" y `precioCec` se muestra "USD". El propio
    payload lo confirma —`officialRate.base == "CEC"` con
    `forSales: [{USD, 1}, {VED, 832.49}]`, o sea 1 CEC = 1 USD y 832,49 son los
    bolívares de un dólar—, así que la tasa de Ajustes es "1 USD = Bs 832,49". Los
    nombres de campo conservan el CEC porque describen lo que trae el origen.
    Detalle en `.claude/skills/ceco-precios-datos`.
  - `secondary`/`tertiary` son neutros cálidos de la familia de la marca: existen
    para que ningún componente caiga al lila por defecto.
  - Variación de precio, siempre **de la misma moneda a los dos lados** —el
    precio solidario contra su valor anterior, nunca contra el precio en Bs—:
    sube = rojo apagado (6.16:1),
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
   sueltos, elevación distinta de 0, `Icon` sin etiqueta, precio pintado con un
   `Text` propio y `size()` propio en botones de icono (rompe el objetivo táctil
   que ya pone M3). Este script llama a dos auxiliares:
   - **`tools/ui_checks.py`** — lo que no se puede hacer con grep: seguir una
     llamada a `Icon(` abierta y buscar el `contentDescription` de las líneas
     siguientes.
   - **`tools/imports.py`** — paquetes mal escritos (`coil` vs `coil3`, un símbolo
     del subpaquete equivocado), símbolos usados sin importar e imports muertos.
     Existe porque **aquí no se compila** (el SDK de Android es x86-64 y el host
     es aarch64), así que cada error de import cuesta un ciclo entero de CI.
5. **Conteo de tells** — al cerrar un rediseño se revisa la lista de la §3 y se
   anota cuántos quedan. Cuatro o más = no se entrega.
6. **`ScrollBusquedaTest`** — la política de scroll al empezar, borrar o cambiar
   la búsqueda: borrar una letra no mueve la grilla, limpiar vuelve a la posición
   previa, y una búsqueda restaurada no la pisa.

Las skills de `.claude/skills/` (`ceco-tablero-precios`, `ceco-precios-datos`,
`ceco-puertas`) llevan estas mismas reglas en forma ejecutable, con el `file:line`
de cada una. Este documento dice **por qué**; las skills dicen **dónde**.

## 7. Estructura de pantallas

- **Catálogo**: buscador (con limpiar) + icono de filtros con badge + icono de
  escáner en la cabecera (no un FAB: flotando tapaba el `+` de la tarjeta
  derecha). Sin filas de chips de categoría. Los filtros activos se muestran como
  chips descartables **solo mientras existan**.
  - **Scroll al buscar**: al empezar una búsqueda la grilla va al principio y
    guarda la posición previa; al borrar la búsqueda vuelve a esa posición.
    Borrar letra por letra no salta a la posición de cada prefijo. La
    restauración espera a que la lista mostrada sea la del estado actual
    (`resultados.clave`), no la anterior.
  - **Sin resultados**: el mensaje usa la consulta **asentada** (la que produjo
    la lista) y dice "Buscando…" mientras el debounce no llegó. Un catálogo con
    datos no muestra "Cargando precios…": una lista vacía es un resultado, no
    una carga.
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
  - El conteo usa la **misma condición que la grilla**, incluido el barcode
    exacto: si buscar un código muestra un producto, su rubro también cuenta.
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
  variación y el precio solidario **no** van en la tarjeta aunque existan para
  algunos productos: van en el detalle, donde están todos los datos.
- **Detalle**: nombre, precio grande, precio solidario en USD si existe, y **solo los
  datos reales** (se ocultan presentación vacía, marca redundante). Acción fija
  abajo: stepper si ya está en el carrito, botón si no. Sin imagen gigante vacía.
- **Ajustes**: frescura de las dos fuentes, tasa, ferias y total de productos.
  Avisa si el precio solidario tiene más de 12 h y ofrece reconstruir el
  enriquecimiento (borra el CEC y lo vuelve a bajar; favoritos y carrito se
  conservan). El toggle se llama “Mostrar precio solidario (USD)”, no “precios en
  USD”: no es una conversión.
- **Barra inferior**: `NavigationBar` en Catálogo, Favoritos y Carrito.
  **Oculta** en Detalle, Escáner y Ajustes (tareas de pantalla completa): la
  barra solo existe en las tres rutas de `destinos` (`MainActivity.kt`).

## 8. Auditoría por skills (cerrada el 2026-09-26)

Pasada una por una las 6 skills instaladas más 3 escritas para este proyecto. Los
6 hallazgos originales están **resueltos**; quedan abajo como registro de por qué
se hicieron, porque volver a leerlos evita reintroducirlos:

1. **compose-component-design** ✅ — `RenglonProducto` acepta `Modifier`;
   `Dato()` unificado como `FilaDato`; `QtyButton` y `Stepper` fusionados en un
   solo `Stepper` de `ui/common`; el primitivo se mudó de `ui/catalog` a
   `ui/common` (lo usan catálogo y favoritos). *Pendiente*: `PriceText` ya
   acepta `Modifier` ✓, pero `EstadoVacio` y `HojaFiltros` siguen siendo privados
   de su pantalla.
2. **compose-performance** ✅ — el texto de búsqueda vive en la pantalla y el VM
   recibe cada tecla; un solo debounce en el VM alimenta grilla y conteos; el
   patrón `MutableStateFlow.also { launch {} }` ya no está;
   `LocalUsdPrecio` pasó de `staticCompositionLocalOf` a `compositionLocalOf`
   (con el `static`, cambiar la moneda recomponía **toda** la app); `Regex`
   compiladas a nivel de archivo en vez de por llamada y por tarjeta; el
   formateo del precio va en `remember`; `sincronizando` se expone como
   `StateFlow` de solo lectura.
3. **compose-animations** ✅ — `animateContentSize` en el stepper, `Crossfade`
   para vacío↔resultados y botón↔stepper, `AnimatedVisibility` en el icono de
   limpiar búsqueda, en la barra inferior y en el badge del carrito; `NavHost` con
   `fadeIn/fadeOut` de 150 ms en las cuatro transiciones (`MainActivity.kt`).
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

- **La migración de Room v1→v2 no está verificada por nada**: `exportSchema = false`,
  no hay schemas exportados para diffear, no hay `MigrationTestHelper` y no hay
  tests instrumentados. Lo único que la protege es el comentario.
- **Sin tests instrumentados**: nada de UI, nada de Room real, nada de escáner.
  El smoke test de CI *abre* la app; no la usa.
- **Las imágenes se piden por https**: `domain/Urls.kt` sube el esquema a https
  antes de guardar la URL y `network_security_config.xml` prohíbe cleartext
  (`cleartextTrafficPermitted="false"`). Los hosts de Cecosesola responden por
  https (los `http://` solo redirigen), así que el cambio no rompe nada. Si un
  host externo no soporta https, su imagen no carga: es preferible a permitir
  cleartext para todos.
- **`ProductEntity.updatedAt` se recorta con `substringBefore('T')`**: el
  ISO-8601 de la API está verificado en el fixture
  (`2026-09-12T21:24:27.906Z`), así que el corte funciona; pero el detalle muestra
  `YYYY-MM-DD` mientras Ajustes usa `dd/MM/yyyy a las HH:mm`. Consistencia
  pendiente, no dato roto.
- **El orden por precio y la moneda visible**: la consulta ordena por `precioBs`.
  Cuando el CEC está fresco, el mirror y la API son proporcionales (el scraper
  deriva los Bs del CEC con la tasa), así que el orden coincide con las cifras
  USD. Si el enriquecimiento está viejo, las cifras y el orden pueden no
  coincidir; el problema real es la frescura, no la consulta.
- **No hay reconciliación**: un producto que desaparece de la API oficial sigue
  ocupando su fila. Es el precio de no borrar, y se acepta a propósito.
