---
name: ceco-precios-datos
description: Reglas de la fusión de las dos fuentes de precios de Cecosesola (mirror GitHub + GraphQL oficial). Úsala al tocar MergeEngine, ProductRepository, los DTOs, los workers de sync, los DAOs o el modelo de datos. Cubre el contrato de monedas Bs vs CEC, la regla de no perder productos, y por qué el fixture real es la única verdad al parsear.
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
| Trae | `id`, `nombre`, `precio` (Bs), `imagen` | barcode, marca, presentación, tags, imagen grande, **precio CEC** y precio anterior |
| Cadencia | sync 6 h, clave `fecha_actualizacion` | sync 1 día, clave `priceList.model.version` |

`data.downloadFile` es un **string con JSON adentro**: hay dos niveles de parseo
(`Apis.kt:55-66`).

## Contrato de monedas — la regla que más se rompe

Tres números distintos. No son intercambiables:

- `precioBs` — lo que **se muestra**. Canónico del mirror. `ProductEntity.precioBs`.
- `precioCec` — precio solidario en unidades CEC. Solo de la API.
- `precioAnteriorCec` — el CEC anterior. **Solo de la API.**

**El delta de precio se calcula CEC↔CEC, jamás Bs↔CEC.** Mezclar monedas da un
porcentaje sin significado, y como los precios en Bs se actualizan a otra
frecuencia que lossolidarios, el número "correcto" en Bs y el número que el
usuario quiere ver (subió o bajó el precio solidario) no coinciden.
`DeltaBadge.kt:36-37` es el único lugar que calcula el delta: recibe dos CEC.
Ojo con su guarda de `DeltaBadge.kt:33` — si el precio anterior llega `null` no
pinta nada y **no hay ningún síntoma**: el badge simplemente no aparece. Por eso
un `precioAnteriorCec` siempre-nulo se puede esconder meses.

La tasa de `officialRate` es **Bs (VED) por unidad CEC**, no por USD:
`officialRate.model.base == "CEC"`. Ojo al etiquetarla en la UI — ya se
etiquetó mal una vez como "1 USD = Bs …" cuando el valor es VED-por-CEC.

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
6. **Hay claves de nivel superior que valen oro y se ignoran**:
   `productsPriceChanged` (la lista real de cambios de precio) y `cachedAt`
   (mejor clave de idempotencia que `version`). `PayloadOficial`
   (`OfficialDtos.kt:33-38`) no las declara.
7. **El servidor sirve un snapshot cacheado** (`cached` en el payload): la
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
  filas en DB, es una señal de que algo se perdió. Hoy se parsea y no se cruza.
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
