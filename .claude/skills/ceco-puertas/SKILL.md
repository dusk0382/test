---
name: ceco-puertas
description: Cómo se verifica un cambio en Cecosesola Precios y por qué no hay build local. Úsala antes de dar cualquier cambio por terminado, cuando tyres de escribir un test, o cuando dudes de si un cambio está verificado. Explica el loop de CI, la disciplina de fixtures, y los falsos-verdes que yaelvaron a este repo.
metadata:
  keywords:
  - testing
  - ci
  - verificacion
  - workflow
---

# Puertas de verificación — Cecosesola Precios

## Lo primero, siempre

**En esta máquina no se compila Android.** Los binarios de `/opt/android-sdk`
(aapt2, zipalign, adb) son x86-64 y el host es aarch64 sin binfmt ni box64.
Gradle del sistema es demasiado viejo y AGP necesita aapt2 de todas formas.

Java, `d8.jar` y `apksigner.jar` sí corren. Gradle tampoco.

Consecuencia: **no existe "corrí los tests y pasaron" localmente.** Todo lo que
se afirma sobre un cambio sin haberlo pasado por CI es una suposición. Si vas a
reportar un cambio como terminado y no hubo un run verde que lo cubra, decilo.

## El loop

```
commit en rama  →  push  →  PR a main  →  job build (1.4 min)  →  iterar
                                              ↓
                                    merge a main  →  job smoke (3.1 min)
                                              ↓
                                    arranque real de ambos APKs en emulador
```

- El job `build` corre `testDebugUnitTest` + `assembleDebug` + `assembleRelease`.
  **1.4 min.** Es el ciclo de iteración.
- El job `smoke` instala y abre debug y release en emulador. **3.1 min.** Solo
  corre en push a `main`; en `pull_request` está salteado.
- `concurrency: cancel-in-progress: true` — **un push mata el run en vuelo**.
  Si empujás dos veces seguidas, no obtenés señal de ninguna de las dos. Esperá
  el resultado antes de empujar de nuevo.
- Medido: el job `build` tarda **1.4-4.8 min** según el tamaño de Gradle (frío vs
  incremental) y el emulador **3.1 min**. No esperes 90 s y des por hecho que se
  colgó.

Por eso se trabaja en rama y se mergea una vez: `main` es de donde el usuario
instala, y una racha de pushes con el mismo bug dejó cinco commits consecutivos
en rojo (runs 53-57) hasta que se cazó el crash de arranque.

## Leer los resultados

No hay `gh` CLI en esta máquina, pero la API funciona con un token con scope de
Actions. Ojo: `run_number` **no** es el `id` del run — equivocarse da un 404
confuso que parece falta de permisos.

```bash
T=github_pat_...   # nunca en un archivo del repo
# listar runs (id y run_number juntos)
curl -sS -H "Authorization: Bearer $T" \
  "https://api.github.com/repos/dusk0382/test/actions/runs?per_page=5" \
  | python3 -c "
import json,sys
for r in json.load(sys.stdin)['workflow_runs']:
    print(r['id'], r['run_number'], r['status'], r['conclusion'], r['head_sha'][:8])"

# jobs y logs de un run (usar el ID, no el run_number)
curl -sS -L -H "Authorization: Bearer $T" \
  "https://api.github.com/repos/dusk0382/test/actions/jobs/<job_id>/logs"
```

Los artefactos del run incluyen `logcat-arranque` (el logcat del arranque en
emulador), `r8-mapping` (para retrazar un stack de release) y los dos APKs.

## Los tests

Cuatro archivos, JVM puro, sin instrumentación. Fixtures reales en
`app/src/test/resources/fixtures/`.

| test | qué protege |
|---|---|
| `ParserTest` | el parseo no se desvía del payload real (anti-drift) |
| `MergeTest` | la fusión de las dos fuentes |
| `FormatoNombreTest` | sentence case en MAYÚSCULAS, y que no destroce lo que ya viene en mixta |
| `RubrosTest` | la clasificación cubre ≥95 % del catálogo real, ningún rubro se lo come todo |
| `TemaContrasteTest` | WCAG AA en claro y oscuro, sin roles sin definir |

Correr: `./gradlew :app:testDebugUnitTest` (necesita red la primera vez; en CI
corre siempre).

## Los otros dos checks, antes de los tests

Ambos están en la CI y los dos nacieron de un error real:

- **`bash tools/ui_lint.sh`** — tells de diseño (§3 de `DESIGN.md`): emojis,
  `BorderStroke`, MAYÚSCULAS de UI, hex fuera del tema, `.dp` de espaciado fuera
  de los tokens, elevación, `Icon` sin etiqueta, precio pintado con un `Text`
  propio, áreas táctiles bajo 48 dp. Llama a `tools/ui_checks.py`, que hace lo
  que grep no puede: seguir una llamada a `Icon(` abierta y buscar el
  `contentDescription` de las líneas siguientes.
- **`python3 tools/imports.py`** — paquetes mal escritos, símbolos del subpaquete
  equivocado, símbolos usados sin importar, imports muertos. Existe por la razón
  de este documento: **aquí no se compila**, así que un error de import cuesta un
  ciclo entero. Los que más salieron: `androidx.compose.ui.text.TextAlign` (es
  `.text.style`), `coil.compose` (es `coil3.compose`), y un import que se queda
  atrás al mover código de archivo.

### Verificá un checker antes de confiar en su verde

Un check que no falla nunca también pasa. Antes de dar por bueno un checker
nuevo, rompé a propósito los casos que dice cubrir y confirmá que los detecta; y
después confirmá que el código real está limpio. Si un checker marca algo que
mirando el código está bien, el bug está en el checker.

Dos bugs reales encontrados así: el filtro de `Icon` se comía todas las llamadas
que tenían `Icons.` dentro (casi todas, porque el ícono se pasa como argumento), y
el de símbolos sin importar contaba como "usado" un símbolo nombrado en un
comentario.


## La disciplina que más caro salió

**Un test que construye el DTO a mano no testea el parser.** Testea la función
de merge y deja el parseo sin cubrir mientras el CI está verde.

Así se murió la variación de precio: `MergeTest` armaba `ApiEnriquecido` a mano
con `precioAnteriorCec = cec * 0.9` y assertaba que sobrevivía al merge. El
assert pasaba. Pero el precio anterior real llega por el parser, y como
`oldPrice` estaba tipado con una forma que el payload no tiene, **el campo era
siempre `null` y la feature nunca se mostró**. Test verde, feature muerta,
durante semanas.

Corolarios:

- Si tocás un DTO, el assert va **contra el fixture**, con el valor real medido
  del archivo. Si el valor del fixture y el del assert difieren, **gana el
  fixture**.
- Un test que pasa porque la entrada la armaste vos no prueba que la entrada
  real se parsee. Es peor que no tener test: da confianza falsa.
- Cuando un bug se encuentra leyendo datos crudos, agregar el test que lo
  habría cazado **en el mismo commit del fix**.

## Qué NO cubre la CI

- No hay tests instrumentados: nada de Room real, nada de migraciones, nada de
  UI. `exportSchema = false` y `Migration(1,2)` escrito a mano
  (`AppDatabase.kt:33-37`) **no están verificados por nada**: no hay schemas
  exportados para diffear ni `MigrationTestHelper`. Lo único que los protege es
  un comentario.
- `tools/ui_lint.sh` no lo corre la CI (a menos que se lo agregues), y solo
  cubre 5 tells. No verifica contraste, ni accesibilidad, ni tokens de
  espaciado.
- El smoke test **abre** la app. No la usa: no toca catálogo, no toca el
  escáner, no sincroniza de verdad. Un bug de layout o de datos no lo ve.
- El tamaño del APK y el presupuesto de performance no se miden en ningún lado.

## Antes de dar por terminado

- [ ] ¿Hay un run verde que cubra el commit exacto? Anotá el número.
- [ ] ¿El test nuevo usa un valor del fixture, no uno inventado?
- [ ] ¿`bash tools/ui_lint.sh` pasa?
- [ ] ¿El cambio toca datos? ¿Preservé la regla de no sobrescribir filas y la
      de no mezclar Bs con CEC? Ver `ceco-precios-datos`.
- [ ] ¿El cambio toca UI? ¿Usó tokens existentes en vez de números sueltos, y
      no metí el naranja como texto? Ver `ceco-tablero-precios`.
- [ ] ¿R8? Release va minificado con full mode. Si tocás algo que se resuelve
      por reflexión o serialización, el release puede romper aunque debug pase.
      El smoke test es el que lo caza — por eso el merge final no es opcional.
