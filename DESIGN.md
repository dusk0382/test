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

**Una sola pieza se repite** en catálogo, favoritos, resultados y carrito. No se
inventa una tarjeta distinta por pantalla:

```
┌──────────────────┐
│  imagen (1:1)    │  ♡        ← contenedor tonal, sin borde de color
│                  │
├──────────────────┤
│ Nombre a 2 líneas           ← bodyMedium, sentence case si viene en MAYÚSCULAS
│ Bs 1.365,50      (+2,1 %)   ← título con cifras tabulares, acento naranja
│                        [ − 2 + ]  o  [ + ]   ← stepper en la misma card
└──────────────────┘
```

Reglas del primitivo:

- Ancho fijo 2 columnas (`LazyVerticalGrid`, no staggered: ritmo parejo y medición
  más barata).
- Caja de imagen **1:1 siempre**, `ContentScale.Fit` sobre `surfaceContainerHighest`
  (las fotos vienen recortadas sobre blanco: el contenedor tonal evita el bloque blanco).
- Nombre: máximo 2 líneas con elipsis, altura estable.
- Precio: cifras tabulares, siempre con separador de miles es-VE.
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
3. **`FormatoNombreTest`** — nombres en MAYÚSCULAS se muestran en sentence case;
   los que ya vienen en mixta no se tocan (no se destrozan marcas).
4. **`tools/ui_lint.sh`** — grep determinista sobre el código Compose: colores
   hex fuera del tema, `.dp` fuera de los tokens de espaciado, emojis en strings,
   literales en MAYÚSCULAS, iconos sin `contentDescription`.
5. **Conteo de tells** — al cerrar un rediseño se revisa la lista de la §3 y se
   anota cuántos quedan. Cuatro o más = no se entrega.

## 7. Estructura de pantallas

- **Catálogo**: buscador (con limpiar) + icono de filtros con badge + FAB de
  escáner. Sin filas de chips de categoría. Los filtros activos se muestran como
  chips descartables **solo mientras existan**.
- **Filtros** (patrón de la investigación: Baymard documenta que poner el conteo por
  opción es la mejora de mayor impacto de una UI de filtros, y que forzar selección
  única genera abandono):
  - Un solo botón de filtros con badge de cuántos filtros hay activos, en una hoja.
  - **Conteo de resultados junto a cada opción** ("Despensa (109)").
  - **Multi-selección de rubros** (OR dentro de rubros, AND con el resto): poder ver
    "Limpieza y aseo" + "Despensa" a la vez es una necesidad real, no un extra.
  - Orden dentro de la misma hoja, y "Limpiar todo" visible.
  - Los filtros aplicados se muestran como chips descartables **pegados arriba** de la
    lista mientras existan, y el estado sobrevive al volver del detalle.
- **Rubros**: los ~100 tags de la API no se navegan. Se usa la clasificación
  derivada y **medida** de `domain/Rubros.kt` (nombre primero, tag específico como
  respaldo), dentro del selector de filtros con buscador, conteo por rubro y
  secciones alfabéticas. `Otros` es una opción visible, no un cajón escondido.
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

## 8. Auditoría por skills (2026-09-25, code a reescribir)

Pasada una por una de las 6 skills instaladas sobre el código actual. Lo que
sigue es la lista de fix pendiente por cada lente — el rediseño de pantallas la
cierra:

1. **compose-component-design**: `ProductoCard` no acepta `Modifier` (el caller
   no puede controlar ubicación). `Dato()` duplicado en Detail y Settings;
   `QtyButton` (Detail) y `Stepper` (Cart) son el mismo concepto con dos
   implementaciones → van a `ui/common` como un único componente.
2. **compose-performance**: `query` se colecta al tope de `CatalogScreen`, cada
   tecleo recompone chips + orden + grid scope. El campo de búsqueda debe ser
   dueño de su estado y empujar al VM. `fechaRepo` en el VM usa el patrón frágil
   `MutableStateFlow(null).also { launch {} }`. `ProductEntity` es estable y las
   grillas usan `key` — eso ya está bien.
3. **compose-animations**: la app no tiene una sola animación. Plan mínimo
   funcional (nada decorativo): `animateContentSize()` en steppers, `Crossfade`
   para vacío↔resultados, `fadeIn/out` en el NavHost, `AnimatedVisibility` para
   FAB y badge del carrito. Motion en fase draw/layout, jamás recomponiendo por
   frame (Mali-G52).
4. **styles** (Google): la API `Styles` que promueve requiere Compose
   1.12-alpha (bloqueada por AGP 9.1) — no aplicable. Su paso de auditoría
   encontró: color limpio (cero hex fuera de theme/ ✓) pero **7 radios de
   esquina escritos a mano** (8/12/14/16/20/28 dp) en vez de los tokens de
   `Forma.kt` → todos a `MaterialTheme.shapes.*`.
5. **edge-to-edge**: falta `enableEdgeToEdge()` en MainActivity (con targetSdk
   35 el sistema lo fuerza igual, pero sin la llamada los iconos de las barras
   dependen del default). Falta `isAppearanceLightNavigationBars` (solo se toca
   el de status) y `isNavigationBarContrastEnforced = false`. Son 3 líneas.
6. **anti-ai-slop-ui**: su lint web no escanea Kotlin (esperado). Grep propio:
   quedan 2 `border` sueltos (ProductoCard y CartRow — regla §3.2) y un emoji
   🛒 en el texto compartido del carrito (cambia por texto plano). El resto de
   tells de la §3 ya estaban cerrados.
