---
name: ceco-precios-datos
description: Reglas de la fusión de las dos fuentes de precios de Cecosesola (mirror GitHub + GraphQL oficial). Úsala al tocar MergeEngine, ProductRepository, los DTOs, los workers de sync, los DAOs o el modelo de datos. Cubre el contrato de las dos monedas (bolívares y el precio solidario en
  dólares, que la API llama CEC), la regla de no perder productos, y por qué el fixture real es la única verdad al parsear.
metadata:
  keywords:
  - cecosesola
  - merge
  - precios
  - datos
---

# Datos de Cecosesola Precios

## Principio

Dos fuentes, un precio. El mirror es la verdad del **precio**; la API oficial
es la verdad de todo lo **demás**. Y una fila que ya existe en la base del
usuario no se sobrescribe ni se borra: se enriquece.

## Las dos fuentes

| | Mirror (repo GitHub) | API oficial (GraphQL) |
|---|---|---|
| Endpoint | `raw.githubusercontent.com/dusk0382/cecosesola-data/main/precios.json` | `POST kana.imk.cecosesola.coop/graphql` → `{"query":"query { downloadFile }"}` |
| Latencia | ~100 KB, CDN, segundos | ~1 MB, **7-40 s**, sin gzip |
| Trae | `id`, `nombre`, `precio` (Bs), `imagen` | barcode, marca, presentación, tags, imagen grande, **precio solidario en USD** (el payload lo llama CEC) y su valor anterior |
| Cadencia | sync 6 h, clave `fecha_actualizacion` | sync 1 día, clave `priceList.model.version` |

`data.downloadFile` es un **string con JSON adentro**: hay dos niveles de parseo
(`Apis.kt:55-66`).

## Contrato de monedas — la regla que más se rompe

Tres números distintos, en **dos monedas**:

- `precioBs` — el precio en bolívares. Canónico del mirror. `ProductEntity.precioBs`.
- `precioCec` — el precio solidario **en dólares**. Solo de la API.
- `precioAnteriorCec` — el precio solidario anterior. **Solo de la API.**

### "CEC" es el código interno de la API, no una moneda

El payload llama `CEC` a lo que en la app es el precio solidario en dólares, y el
usuario **no conoce esa palabra: nunca aparece en la interfaz**. Todos los
`precioBs` se muestran como "Bs" y todos los `precioCec` como "USD".

Que el origen lo llame CEC no lo convierte en una tercera moneda. El propio
payload lo resuelve: `officialRate.model.base == "CEC"` con
`forSales: [{destination: "USD", value: 1}, {destination: "VED", value: 832.49}]`
— o sea **1 CEC = 1 USD**, y 832,49 son justamente los bolívares de un dólar. La
tasa que se muestra en Ajustes es, por lo tanto, "1 USD = Bs 832,49".

**Ya se Cometió el error dos veces, en direcciones opuestas.** Primero se
etiquetó la tasa como "1 CEC = Bs …", que es un código que el usuario no
reconoce. Y al "corregirlo" se agravó el error al asumir que CEC era una
unidad distinta del dólar y que 832,49 no podía ser la tasa del dólar. Cuando
cambies una etiqueta de moneda, leé el `officialRate` completo antes de decidir.

Los **nombres de campo** (`precioCec`, `precioAnteriorCec`) conservan el CEC a
propósito: describen lo que trae el origen, y renombrarlos rompería la
trazabilidad con la API sin ganar nada.

### El delta: misma moneda de los dos lados, nunca mezclada

`DeltaBadge.kt:36-37` es el único lugar que calcula el delta, y recibe
`precioCec` y `precioAnteriorCec` — **los dos de la misma moneda**, que es lo que
importa. Calcularlo contra el precio en Bs daría un porcentaje sin significado:
los precios en Bs se actualizan a otra frecuencia que los solidarios, así que el
"cambio" que se vería sería en parte el de la tasa, no el del producto.

Ojo con su guarda de `DeltaBadge.kt:33` — si el precio anterior llega `null` no
pinta nada y **no hay ningún síntoma**: el badge simplemente no aparece. Por eso
un `precioAnteriorCec` siempre-nulo se puede esconder meses.

## Trampas permanentes del payload de la API

Son propiedades de los datos, no bugs: siguen siendo verdad después de
cualquier fix. `OfficialDtos.kt:19-21` lo dice: es una API interna no
documentada y todo llega rare.

1. **`oldPrice` viene anidado un nivel más de lo que parece.** Es un objeto
   con la *misma forma* que `pricePublished`, no un `amount` pelado:
   `pricePublished.oldPrice.priceBase.amount.$numberDecimal`. Tiparlo como
   monto plano compila perfecto y devuelve `null` en silencio.
2. **`_id` a veces es int y a veces string** → `FlexIdSerializer`
   (`OfficialDtos.kt:100-117`).
3. **Montos siempre son `{"$numberDecimal": "..."}`**, y pueden traer coma
   decimal → `DecimalAmount.toDoubleOrNull()` (`OfficialDtos.kt:68-69`) es el
   único tolerante. Los tres lectores que hoy leen `.numberDecimal` a mano
   deben pasar por ahí.
4. **El nodo va envuelto**: `{"_ns": "pdt", "model": {...}}` → `Wrapper<T>`.
5. **Los nombres no son únicos.** El mirror real tiene **18 grupos de nombres
   duplicados cubriendo 42 productos**; hay productos con el mismo nombre y
   precio distinto (p.ej. `Jabón de baño Dalan 125gr` ×4 a 765 y 745). Por eso
   el match por nombre normalizado es un **fallback**, nunca la estrategia
   primary, y su colisión tiene que resolverse sin perder la fila.
6. **Las imágenes vienen en `http://`.** 453 de las 527 referencias del fixture,
   en ~55 hosts distintos, de los cuales solo dos son de Cecosesola
   (`kana.imk.cecosesola.coop` y `kana.develop.cecosesola.imolko.net`). Los otros
   ~50 son basura scrapeada, un URL cada uno. Los dos de Cecosesola responden por
   https (200) y los http solo redirigen (308), así que `domain/Urls.kt` sube el
   esquema **al guardar**, y el `network_security_config` quedó en
   `cleartextTrafficPermitted="false"` sin allowlist. Antes allowlistaba un host:
   146 imágenes (28%) no cargaban y nadie sabía por qué.
7. **Hay claves de nivel superior que valen oro y se ignoran**:
   `productsPriceChanged` (la lista real de cambios de precio) y `cachedAt`
   (mejor clave de idempotencia que `version`). `PayloadOficial`
   (`OfficialDtos.kt:33-38`) no las declara.
8. **El servidor sirve un snapshot cacheado** (`cached` en el payload): la
   `version` puede ir detrás de la realidad.

## Regla de no pérdida

- El merge es **upsert masivo**. Nunca `DELETE all` — el proyecto anterior
  perdió favoritos así y `MergeTest` guarda esa cicatriz.
- Una fila existente se **enriquece** (`copy()` arrancando de la vieja), nunca
  se reemplaza. Al enriching, `precioBs` se deja como está: la API no redefine
  el precio visible (`MergeEngine.kt:89`).
- El `precioBs` de una fila que solo existe en la API es un **derivado**
  (`precioCec * tasa`). Es una aproximación: si la tasa no está disponible, no
  se puede fingir un precio en Bs.
- Si el conteo del mirror (`totalProductos`, `RepoDtos.kt:11`) no cuadra con las
  filas en DB, es una señal de que algo se perdió. Ya se cruza y avisa por log.
- Todo el read-modify-write del merge va en **una transacción**. Sin ella, dos
  workers concurrentes se pisan: el read ocurre antes del write del otro y el
  `copy()` devuelve campos que nadie escribió.

## La regla que más caro sale: el fixture es la verdad

`app/src/test/resources/fixtures/` tiene capturas reales de ambas fuentes.

**Nunca assertar un campo del payload contra un DTO construido a mano.** Eso
testea la función de merge, no el parser, y deja el parser sin cubrir mientras
el test pasa en verde.

Así se rompió una vez: `MergeTest` construye `ApiEnriquecido` a mano con
`precioAnteriorCec = cec * 0.9` y asserta que sobrevive al merge
(`MergeTest.kt:12-17`, `:41`). Pasa. Pero el `precioAnteriorCec` real llega por
`MergeEngine.kt:57`, y como `oldPrice` estaba mal tipado **el precio anterior
era siempre `null` y la variación de precio nunca se mostró en la app**. Un
test verde y una feature muerta al mismo tiempo.

Al tocar un DTO, el assert va contra el fixture, con el valor real medido
del archivo. Si el valor del fixture y el del assert difieren, gana el fixture.

## Verificar

```bash
# tests JVM (incluye el anti-drift contra los fixtures)
./gradlew :app:testDebugUnitTest
```

Los cuatro tests: `ParserTest` (parseo anti-drift), `MergeTest` (fusión),
`RubrosTest` (clasificación ≥95 %), `TemaContrasteTest` (WCAG).

Al **añadir** un caso a `ParserTest`, sacá el valor real del fixture:

```bash
python3 -c "
import json
d=json.load(open('app/src/test/resources/fixtures/downloadfile_graphql.json'))
p=json.loads(d['data']['downloadFile'])['products'][0]['model']
print(json.dumps(p['pricePublished'], indent=1))"
```

## Antes de dar por terminada una modificación de sync

- [ ] ¿El idempotency key sigue cubriendo el caso? `syncBase` salta si
      `fecha_actualizacion` no cambió; `syncEnrich` si `priceList.version` no
      cambió.
- [ ] ¿Un fallo se distingue de un "no había cambios"? Hoy `false` significa
      las dos cosas y el worker reporta `Result.success()` en ambos.
- [ ] ¿El fallo es reintentable? Un 404 permanente con backoff exponencial y
      `KEEP` reintenta para siempre sin logs ni dead-letter.
- [ ] ¿Se conserva la otra fuente? Un write del mirror no puede pisar un
      `apiId` que el enrich ya había puesto.
