---
name: ceco-tablero-precios
description: Reglas de la dirección visual "tablero de precios" de Cecosesola Precios. Úsala al crear o revisar cualquier pantalla Compose de esta app: catálogo, detalle, favoritos, carrito, escáner, ajustes, tarjetas, o el tema. Cubre el acento único, la regla de relleno-vs-texto, el renglón como primitivo único, y los tokens que existen para que no se escriba nada a mano.
metadata:
  keywords:
  - cecosesola
  - diseño
  - compose
  - tokens
  - accesibilidad
---

# Tablero de precios — dirección visual de Cecosesola Precios

## Principio

La referencia no es una app de tienda: es un **cartel de precios de feria**.
Números grandes, tinta oscura sobre papel claro, un solo color chillón para el
precio, cero decoración.

El usuario está parado en la feria, con una mano ocupada, al sol, en un Helio
G25 de gama baja. **Contraste y legibilidad ganan; adornos pierden.** Cada
decoración tiene que ganarse su lugar contra eso.

El error por defecto de un LLM en una app de precios es making it look like
Shopify: cards con borde, gradientes, badges de "nuevo", iconos en cada
renglón, sombras. Todo eso está prohibido acá. No es una preferencia: está
escrito como prohibición en `DESIGN.md` §3, y existe porque ya se hizo.

## Las cinco reglas que más se rompen

### 1. El naranja es relleno, nunca texto

`primary` = `#FD4902`. Es un color de **relleno**: botones, FAB. Si lo usás
como color de texto, fallás.

| combinación | ratio | veredicto |
|---|---|---|
| tinta `#2A1200` sobre `#FD4902` | **5.17:1** | ✅ es el texto de los botones primarios (`onPrimary`) |
| blanco sobre `#FD4902` | **3.43:1** | ❌ falla AA para texto normal — fue el bug del botón "Agregar al carrito" |
| `#FD4902` sobre superficie `#FFF8F6` | **3.26:1** | ❌ no lo uses como texto |
| `#FD4902` sobre `surfaceContainerHighest` `#F0E4DF` | **2.75:1** | ❌ no lo uses ni como icono de 20 dp |

**Para el precio como texto** existe un token propio, y es el correcto:

| token | claro | oscuro |
|---|---|---|
| `LocalColoresPrecio.current.acento` | `#B93300` **5.66:1** | `#FFB59B` **10.07:1** |

`primary` como relleno y como texto tienen requisitos de contraste distintos;
por eso hay dos tokens. El precio lo pinta `PriceText`
(`ui/common/Format.kt`), y la tarjeta lo usa bien vía `RenglonProducto`.

### 2. La jerarquía se hace con tamaño y peso, no con bordes

- **Prohibido** `border` / `BorderStroke` como estructura de una tarjeta. La
  jerarquía es tipografía, y separadores de tono (`surfaceContainer*`).
- **Prohibido** gradiente, glow, sombra de color, glassmorphism.
- Las tarjetas van con `elevation = 0` y el color de superficie.
- Máximo **3 tamaños de tipo por pantalla**.

### 3. El renglón es el único primitivo

`RenglonProducto` se repite en catálogo, favoritos, resultados. **Una sola
pieza, siempre igual** — no se inventa una tarjeta distinta por pantalla. Por
eso vive en `ui/common`, no en `ui/catalog`.

Lo único que cambia por pantalla es si el producto ya está en el carrito
(.stepper en vez del `+`), y nada más dentro de la caja:

```
┌──────────────────┐
│  imagen          │  ♡        ← plato claro, sin borde
├──────────────────┤
│ Nombre a 2 líneas           ← bodyMedium, sentence case
│ Bs 1.365,50                 ← cifras tabulares, acento de precio
│              [ − 2 + ]  o  [ + ]   ← control debajo, a la derecha
└──────────────────┘
```

- **Imagen 1:1** (`Modifier.aspectRatio(1f)`), `ContentScale.Fit` sobre
  `PlatoImagen`. El plato es claro en **los dos temas**: las fotos del origen
  vienen sobre blanco y así su fondo se funde en vez de quedar un bloque blanco
  sobre la tarjeta oscura. El corazón va con `TintaSobrePlato` (oscura en ambos).
- Nombre: **máximo 2 líneas** con elipsis y altura estable.
- Precio: **cifras tabulares** (`tnum`), separador de miles es-VE.
- **Nada más**: ni categoría, ni marca, ni descripción.

En el carrito el nombre usa el ancho completo; el importe va en la segunda fila a
la derecha (`PrecioTarjeta`) y el stepper a la izquierda. El stepper no le roba
ancho al nombre ni compite con la plata.

Mostrar un atributo solo en algunos ítems hace que el usuario descarte los
demás. El `%` de variación y el precio solidario van en el **detalle**, no en
la tarjeta.

### 4. No muestres dato inútil

La UI **no inventa** lo que los datos no dicen. `presentacion = "item"`,
`marca == categoria`, string vacío → **se oculta el campo**, no se pinta una
fila en blanco. El dato sucio es la norma en esta base: 518 productos con
nombres en MAYÚSCULAS mezclados con minúsculas, marcas sin tipo de producto,
tags con basura operativa.

### 5. Nombres en MAYÚSCULAS → sentence case

`formatearNombreProducto()` lo resuelve y **se llama en las tres pantallas que
muestran un producto** (`RenglonProducto`, `DetailScreen`, `CartScreen`). Una
pantalla nueva que pinte un nombre también tiene que llamarlo. Las marcas que ya
vienen en mixta no se tocan (no se destrozan): un nombre mixto queda literal.

## Los tokens ya existen — usalos

Nada de números sueltos. Cada token tiene un archivo y una razón.

| qué | token | archivo |
|---|---|---|
| espaciado | `Espacio.minimo/xs/s/m/l/xl/xxl`, `toqueMinimo` | `theme/Espacio.kt` |
| formas | `MaterialTheme.shapes.*` (8/12/16/20/28) | `theme/Forma.kt` |
| precio | `PrecioDetalle`, `PrecioTarjeta`, `PrecioApoyo` | `theme/Type.kt:73-83` |
| color de precio | `LocalColoresPrecio.current` | `theme/Theme.kt:118` |
| plato de imagen | `PlatoImagen`, `TintaSobrePlato` | `theme/Theme.kt` |
| moneda | `LocalUsdPrecio` | `ui/common/PrecioVisible.kt` |

Base 4 dp. Un `.dp` suelto en una pantalla es un defecto, no una excepción
(`Espacio.kt:6-7`).

Full-round **solo** para lo interactivo: buscador, chips, botones. Contenedores
de imagen y superficies usan radios medios.

## Prohibiciones anti-slop (las de `DESIGN.md` §3)

1. Sin emojis como iconos — ni en categorías, ajustes ni estados vacíos.
2. Sin bordes de color en tarjetas.
3. Sin etiquetas en MAYÚSCULAS generadas por la UI.
4. Sin filas idénticas de icono+título+subtítulo repetidas sin jerarquía.
5. Sin más de 3 tamaños de tipo por pantalla, ni más de un acento.
6. Sin gradientes, glows, sombras de color, glassmorphism.
7. Sin "todo redondeado".
8. Sin animaciones de entrada de listas ni crossfade de imágenes — 518 ítems en
   un A53.
9. Sin mostrar dato inútil.
10. Sin roles de color sin definir (caen al lila de Material3: de ahí salieron
    el buscador rosado y los bordes violeta).
11. Sin blanco sobre el naranja de marca.
12. Sin etiquetas ni descripciones secundarias innecesarias: que el diseño
    explique, no un subtítulo.

**Contar tells es la puerta de calidad**: al cerrar un rediseño se revisa esta
lista. Cuatro o más = no se entrega.

## Movimiento

150 ms para cambios de estado. `spring` **solo** en el stepper del carrito y el
toggle de favorito. El indicador de la barra de navegación puede animar.

Nunca en fase de composición ni recomponiendo por frame: el Mali-G52 no lo
paga. Cero animaciones de entrada de listas.

## Buscador: los cinco bugs que ya vivió ahí

Anotados porque son el tipo de defecto que vuelve cuando nadie sabe por qué el
código está como está:

1. **La grilla se queda a media lista al cambiar la búsqueda.** Sin `state`
   explícito, `LazyVerticalGrid` conserva un `rememberLazyGridState()` interno que
   sobrevive a los cambios de contenido: al filtrar a pocos resultados el índice
   del ancla se mantiene, y al limpiar el campo la lista se re-expande con el
   usuario donde estaba antes de filtrar. Se ve como "se scrolleó solo".
   Hoy la grilla recibe un `gridState` explícito y la política vive en
   `decidirScrollBusqueda` (`ui/catalog/CatalogScroll.kt`); el destino se aplica
   recién cuando `resultados.clave` es la del estado actual. **No** vuelvas al
   `key(query, ...)` ni a un `scrollToItem(0)` a ciegas: el primero descarta el
   estado y el segundo puede correr sin grilla montada.
2. **El placeholder desalineado.** Un `Text` apilado en un `Box` junto al
   `BasicTextField` no se alinea con el texto si el campo tiene `padding` y el
   `Text` no, y al teclear la primera letra todo salta.

   Se arregla con `decorationBox`, no con el parámetro `placeholder` de
   `BasicTextField`: **`placeholder` no existe en la sobrecarga que recibe un
   `String`**, solo en la de `TextFieldValue`, que es `ExperimentalFoundationApi`.
   (Verificado contra el fuente de androidx: la sobrecarga `value: String` expone
   `singleLine`, `keyboardOptions`, `keyboardActions` y `decorationBox`, y ningún
   `placeholder`.) En `decorationBox` el placeholder y el campo se componen
   dentro del mismo layout y del mismo padding, así que no se pueden desalinear.
   La lambda es `@Composable (innerTextField: @Composable () -> Unit) -> Unit` y
   hay que llamar `innerTextField()` exactamente una vez.
3. **La acción de teclado que no hace nada.** `ImeAction.Search` sin su
   `KeyboardActions` deja una tecla visible que no ocurre para nada. Peor que no
   tenerla.
4. **Borrar la búsqueda no volvía al punto de partida.** La posición previa
   estaba en un `remember` con un mapa por consulta: al abrir una ficha la
   pantalla se descompone y el mapa se recreaba vacío, así que limpiar la
   búsqueda mandaba al principio. Y al borrar letra por letra se restauraba la
   posición de cada prefijo de la pasada de escritura, con saltos que no
   significaban nada.
   Ahora `posicionAntesDeBuscar` es `rememberSaveable`; empezar una búsqueda
   guarda el punto y va al principio, limpiar vuelve ahí, y cambiar de una
   búsqueda a otra (teclear o borrar una letra) no mueve la grilla.
   `ScrollBusquedaTest` fija las reglas. **No** vuelvas a un mapa de posiciones
   por consulta.
5. **Una búsqueda sin resultados decía "Cargando precios…".** El estado vacío
   usaba `yaRefrescado`, que solo pasaba a `true` tras un pull-to-refresh manual:
   en una sesión normal, buscar algo inexistente se quedaba en "Cargando…" para
   siempre. La señal correcta es `total`: si hay datos, una lista vacía es un
   resultado. Mientras el debounce no asentó, dice "Buscando…"; el mensaje usa la
   consulta **asentada**, no la que se está tecleando.

Y tres cosas más que no son bugs pero conviene no romper:

- `escaparLike` en la consulta: sin eso, escribir `%` en el buscador trae los 518
  productos. Un `%` es un carácter normal en un nombre ("QUESO 100%").
- La búsqueda también acepta **código de barras** completo (8 o 13 dígitos,
  exacto, no `LIKE`): la app escanea códigos y los guarda, así que buscar el
  código de lo que acabás de escanear tiene que encontrarlo. La lógica pura vive
  en `data/repository/Busqueda.kt` y está en `BusquedaTest`.
- Los **conteos por rubro** usan la misma condición que la grilla, incluido el
  barcode exacto. Si no, buscar un código mostraba el producto pero el selector
  de filtros no lo contaba.

## Los cuatro bugs que mostraban los screenshots

Los encontró una revisión visual del APK instalado. Se anotan porque son la clase
de defecto que no se ve leyendo el código: los cuatro **compilan, pasan los tests y
el lint**.

1. **El "+" del carrito quedaba cortado contra el borde de la tarjeta.** En una
   tarjeta de dos columnas (~158dp) el precio (~95dp) y el stepper (2 botones +
   cifra) medían 215dp. La causa era un `Modifier.size(48.dp)` que yo mismo había
   puesto en los botones: `IconButton` ya aplica `minimumInteractiveComponentSize()`
   y después su propio `size(40.dp)`, así que el objetivo táctil **ya es de 48dp
   sin tocar nada**. Fijar 48dp desde el caller no cambia el dibujo (el `size` de M3
   se aplica al final de la cadena) pero sí reserva 48dp de fila para dibujar 40.
   El resultado en pantalla era una astilla vertical, y el producto era casi
   inagregable. **No pongas `size()` en un botón de icono: dejá que M3 lo haga.**
   `tools/ui_checks.py` lo verifica.
2. **"Ferias" se dibujaba apilada, una letra por renglón.** En un `Row`, los hijos
   sin peso se miden primero con el ancho completo y el peso se reparte con lo que
   sobra. `FilaDato` tenía `weight(1f)` en la etiqueta y **ninguna restricción en el
   valor**: un valor largo ("Feria Del Centro, Feria de Ruiz Pineda, …") se
   llevaba la fila y a la etiqueta le quedaba el ancho de un carácter. Si algo
   flexible convive con algo sin límite, el sin límite gana.
3. **Decimales desiguales en una columna de precios.** El patrón era `#,##0.##`:
   "2,1" al lado de "6,42" y "5,05". En un cartel de precios la comparación es de un
   vistazo y eso rompe la alineación. El dinero va con **dos decimales siempre**.
4. **La etiqueta del precio solidario.** El toggle de Ajustes y las tarjetas dicen
   **USD**, no CEC. La API llama `CEC` a su unidad base, pero su propio rate la
   define como 1 CEC = 1 USD (`forSales: [{USD,1},{VED,…}]`), así que CEC es un
   código interno y no una moneda que el usuario lea. Lo que sí hay que cuidar es
   no mezclar monedas: el precio solidario se compara contra su `oldPrice` (los
   dos en USD), nunca contra el precio del mirror en Bs. Detalle en `DESIGN.md`
   §5 y en la skill `ceco-precios-datos`.

Y dos cosas que son de jerarquía, no de función:

- El precio y el stepper competían en la misma línea, y **ganaba el stepper**: dos
  círculos de 48dp son visualmente más fuertes que el número, en una pantalla cuyo
  propósito es leer precios. El precio quedó en su propia línea y el control debajo,
  alineado a la derecha y con altura constante.
- El corazón de favorito usaba el token de acento de precio. Ese token está
  reservado al precio (§5): con seis favoritos la fila de corazones brillaba más
  que los precios. Un favorito tampoco es un `error`. Va `TintaSobrePlato`, oscura
  en los dos temas porque pisa el plato de imagen, no la superficie.
- Los badges de conteo (filtros y carrito) son relleno `primary`, no el `error`
  por defecto de M3: un conteo no es un error y el rojo metía un segundo acento.

## Insets: la trampa del Scaffold anidado

`MainActivity` tiene un `Scaffold` con `contentWindowInsets` por defecto
(`systemBars`) y su contenido hace `padding(insets)`. **`Modifier.padding(PaddingValues)`
mide y coloca, pero NO consume los insets.** Así que cualquier `Scaffold` dentro de
una pantalla vuelve a aplicar las barras del sistema: el status bar se suma dos
veces y, si no hay `bottomBar` interno, la barra de navegación también.

La firma es: los `Scaffold` internos van con `contentWindowInsets = WindowInsets(0, 0, 0, 0)`
y sus `TopAppBar` con `windowInsets = WindowInsets(0, 0, 0, 0)`.

Cómo se nota que es un defecto y no una decisión: las pantallas **sin** Scaffold
interno (catálogo, favoritos) están bien, así que navegar entre una y otra hace
saltar el layout una altura de status bar. Esa inconsistencia es la prueba.

## Accesibilidad (es parte del contrato, no un extra)

- Todo icono con `contentDescription`; decorativo lleva `null` explícito.
- `Stepper`: los botones `−`/`+` son `Text`, no iconos — necesitan etiqueta
  ("Quitar uno" / "Agregar uno") y `stateDescription` con la cantidad.
- El buscador necesita etiqueta accesible: un `BasicTextField` sin `label` ni
  placeholder le dice a TalkBack "campo de texto" y nada más. Se etiqueta con
  `contentDescription` en el campo, y el `placeholder` lleva
  `clearAndSetSemantics {}` para que no se anuncie dos veces lo mismo.
- Tamaño táctil mínimo 48 dp (`Espacio.toqueMinimo`). Un `Box(40.dp)` con un
  ícono de 20 dp no cumple.
- Texto blanco sobre la cámara necesita scrim: sobre una foto de producto
  blanca es invisible.

## Verificar

```bash
bash tools/ui_lint.sh    # tells deterministas: emoji, border, MAYÚSCULAS, hex fuera de theme, radios
./gradlew :app:testDebugUnitTest   # incluye TemaContrasteTest (WCAG medido)
```

`TemaContrasteTest` corre los ratios de contraste en la JVM sin Compose
(`Paleta.kt` es hex puro a propósito). Si tocás la paleta, corré ese test: el
WCAG se **mide**, no se estima.

### La trampa del test de contraste: cubre lo que se pintaba cuando se escribió

`TemaContrasteTest` lista los pares "que la app realmente pinta" — pero esa
lista se froze cuando el test se escribió. **Un par nuevo que nadie testea no
está verificado, por mucho que el test esté verde.**

Los dos pares que históricamente fallaban (`primary` sobre `surface`, 3.26:1, y
sobre `surfaceContainerHighest`, 2.75:1) ya están **arreglados y cubiertos** por
`TemaContrasteTest` (`157e42d`): el precio usa `LocalColoresPrecio.acento` y el
corazón, `onSurfaceVariant`. La regla sigue viva: **un par nuevo va al test en el
mismo commit**. Un fix de contraste sin su caso de test se puede revertir sin que
nada avise.
