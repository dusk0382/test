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
por eso hay dos tokens. La tarjeta de catálogo lo usa bien
(`CatalogScreen.kt:416`).

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
│  imagen (1:1)    │  ♡        ← contenedor tonal, sin borde
├──────────────────┤
│ Nombre a 2 líneas           ← bodyMedium, sentence case
│ Bs 1.365,50      (+2,1 %)   ← cifras tabulares, acento de precio
│                        [ − 2 + ]  o  [ + ]
└──────────────────┘
```

- **Imagen 1:1 siempre**, `ContentScale.Fit` sobre `surfaceContainerHighest`
  (las fotos vienen recortadas sobre blanco: el tono evita el bloque blanco).
- Nombre: **máximo 2 líneas** con elipsis y altura estable.
- Precio: **cifras tabulares** (`tnum`), separador de miles es-VE.
- **Nada más**: ni categoría, ni marca, ni descripción.

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

`formatearNombreProducto()` lo resuelve, pero **hay que llamarlo en todos
lados**: hoy la tarjeta lo aplica y el detalle y el carrito no, así que el
mismo producto se ve distinto según dónde. Las marcas que ya vienen en mixta
no se tocan (no se destrozan).

## Los tokens ya existen — usalos

Nada de números sueltos. Cada token tiene un archivo y una razón.

| qué | token | archivo |
|---|---|---|
| espaciado | `Espacio.minimo/xs/s/m/l/xl/xxl`, `toqueMinimo` | `theme/Espacio.kt` |
| formas | `MaterialTheme.shapes.*` (8/12/16/20/28) | `theme/Forma.kt` |
| precio | `PrecioDetalle`, `PrecioTarjeta`, `PrecioApoyo` | `theme/Type.kt:73-83` |
| color de precio | `LocalColoresPrecio.current` | `theme/Theme.kt:118` |
| moneda | `LocalUsdPrecio` | `ui/common/PrecioVisible.kt:13` |

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

**Contar tells es la puerta de calidad**: al cerrar un rediseño se revisa esta
lista. Cuatro o más = no se entrega.

## Movimiento

150 ms para cambios de estado. `spring` **solo** en el stepper del carrito y el
toggle de favorito. El indicador de la barra de navegación puede animar.

Nunca en fase de composición ni recomponiendo por frame: el Mali-G52 no lo
paga. Cero animaciones de entrada de listas.

## Buscador: los tres bugs que ya vivió ahí

Anotados porque son el tipo de defecto que vuelve cuando nadie sabe por qué el
código está como está:

1. **La grilla se queda a media lista al cambiar la búsqueda.** Sin `state`
   explícito, `LazyVerticalGrid` conserva un `rememberLazyGridState()` interno que
   sobrevive a los cambios de contenido: al filtrar a pocos resultados el índice
   del ancla se mantiene, y al limpiar el campo la lista se re-expande con el
   usuario donde estaba antes de filtrar. Se ve como "se scrolleó solo".
   El arreglo es `key(query, filtros, orden) { rememberLazyGridState() }`:
   cambiar las claves descarta el `remember` y la grilla arranca en 0 por
   construcción. **No** con `LaunchedEffect { scrollToItem(0) }`, que llama
   `scrollToItem` sobre un estado que puede no estar montado (si la búsqueda no
   deja resultados, la grilla no está compuesta) y depende de que el scroll
   pendiente se aplique al re-adjuntarse.
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

Y dos cosas más que no son bugs pero conviene no romper:

- `escaparLike` en la consulta: sin eso, escribir `%` en el buscador trae los 518
  productos. Un `%` es un carácter normal en un nombre ("QUESO 100%").
- La búsqueda también acepta **código de barras** completo (8 o 13 dígitos,
  exacto, no `LIKE`): la app escanea códigos y los guarda, así que buscar el
  código de lo que acabás de escanear tiene que encontrarlo. La lógica pura vive
  en `data/repository/Busqueda.kt` y está en `BusquedaTest`.

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

Hoy hay dos pares que la app pinta y el test **no** cubre, y los dos fallan:

| par | ratio | dónde |
|---|---|---|
| `primary` sobre `surface` | 3.26:1 | precio en detalle y carrito |
| `primary` sobre `surfaceContainerHighest` | 2.75:1 | corazón de favorito activo |

Cuando corrijas uno, **agregá el par al `TemaContrasteTest` en el mismo commit**.
Un fix de contraste sin su caso de test es un fix que se puede revertir sin que
nada se avise.
