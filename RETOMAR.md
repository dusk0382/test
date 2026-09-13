# RETOMAR.md — App "Cecosesola Precios" (Android nativa Kotlin)

> Documento de handoff completo. Escrito 2026-09-13. Léelo entero antes de tocar nada;
> está ordenado de "qué es esto" a "qué falta exactamente".

## 1. Qué se está construyendo

App Android **nativa (Kotlin + Jetpack Compose)** para consultar los precios de víveres
de Cecosesola (cooperativa venezolana), paridad con la PWA https://precios.cecosesola.coop/ :
catálogo con búsqueda, categorías, favoritos, carrito con compartir, escáner de código de
barras, offline-first. **Uso personal** (sin distribute, sin avisos legales).

**Restricción dura**: debe moverse fluida en un teléfono con **MediaTek Helio G25**
(8× Cortex-A53, Mali-G52 MC1, 2–3GB RAM, a menudo Android Go) — que es la máquina de
desarrollo: **Termux + PRoot Debian aarch64 en ese mismo teléfono**.

## 2. Entorno de desarrollo — LEER ANTES DE INTENTAR NADA

- Working dir del proyecto: `/root/frebufftesteos/Ceco` (repo git propio, rama `main`).
- **NO se puede compilar Android localmente**: los binarios de `/opt/android-sdk`
  (aapt2, zipalign, adb) son x86-64 y este host es aarch64 sin binfmt/box64 → fallan
  con "required file not found". Java 21, d8.jar y apksigner.jar SÍ corren, pero
  Gradle 4.4.1 del sistema es demasiado viejo y AGP necesita aapt2 de todas formas.
- **El build loop es CI de GitHub Actions**: cada push a `main` corre
  `.github/workflows/build.yml` (tests + assembleDebug + assembleRelease) y sube los
  APKs como artefactos descargables desde la pestaña Actions de GitHub. El usuario
  instala el APK en el teléfono a mano (sin adb).
- **El repo todavía no tiene remote.** Falta `git remote add origin …` + push — ver §9 paso 0.
- **El PAT que pasó el usuario está REVOCADO/INVÁLIDO** (401 "Bad credentials" contra
  api.github.com/user). Pedir uno nuevo o que el usuario haga el push él mismo.
- Los endpoints de red funcionan bien desde este teléfono (curl OK).

## 3. Fuentes de datos (verificadas en vivo 2026-09-13)

### A. Mirror del usuario (fuente BASE — rápida, CDN)
`https://raw.githubusercontent.com/dusk0382/cecosesola-data/main/precios.json`
```json
{ "fecha_actualizacion": "2026-09-12 18:05:59", "total_productos": 518,
  "productos": [{ "id": "1", "nombre": "Galleta Maria Puig", "precio": 1365,
                  "imagen": "https://raw.githubusercontent.com/…/images/1_518a0dfc.jpg" }] }
```
~100KB. El scraper es Python+Playwright en GitHub Actions del propio usuario,
cron Jue–Dom 3×/día. NO tiene categorías, barcodes ni precio anterior.
En el teléfono viejo también existe el repo local en `/root/frebufftesteos/Ceco` — no,
ojo: `Ceco` es NUESTRApp; el scraper está solo en GitHub.

### B. API oficial (fuente de ENRIQUECIMIENTO — lenta, la usa la PWA)
`POST https://kana.imk.cecosesola.coop/graphql` con body `{"query":"query { downloadFile }"}`
→ `{"data":{"downloadFile":"<JSON de 1MB como string>"}}`. Sin auth, sin rate-limit visible.
**Lentísima medida hoy: 7–40 segundos, ~1MB, sin gzip, server Caddy+Express.**
El JSON interno tiene:
- `priceList.model` → `{version: 3988, productsIncluded: 527, rateId, …}` (la `version` es el marcador de cambio)
- `officialRate.model` → `{base:"CEC", forSales:[{destination:"USD",value:{$numberDecimal:"1"}},{destination:"VED",value:{$numberDecimal:"832.49"}}]}` ← **la tasa VED/CEC es Bs por unidad CEC**
- `branches` → 4 ferias (`name`: "Feria Del Centro", …). `_id` a veces int, a veces string → se parsea como JsonElement.
- `products` → 527 `{"_ns":"pdt","model":{…}}` con `name, brand, presentation, barcode` (EAN-13, **hoy 527/527 presentes**), `tags` (ej. `["confiteria"]`, solo 366/527), `images` (URLs, algunas `http://` del host kana → red Security config lo permite solo en ese host), `status`, y `pricePublished{priceBase:{amount:{$numberDecimal:"1.6815801"},currencyCode:"CEC"}, oldPrice:{…} `← precio anterior en CEC, updatedAt}`.
- Montos siempre `{"$numberDecimal": "..."}` — decimales CEC, NO Bs.
- Introspección GraphQL abierta (hay `listProduct`, `currentPriceList`, etc.) pero `downloadFile` da todo de una.

### Decisión de fusión (ya implementada en MergeEngine)
- Cada fila `ProductEntity` tiene `repoId`, `apiId` y `fuente` ("repo"/"api"/"ambas").
- El precio **visible** (`precioBs`) es canónicamente el del mirror; la API aporta
  `precioCec`/`precioAnteriorCec` (para el % de variación **CEC↔CEC, nunca mezclar
  monedas**) + categoria/marca/presentacion/barcode/imagen-grande.
- Matching entre fuentes: `apiId` → `barcode` → nombre normalizado (`normalizarNombre`:
  minúsculas, sin acentos NFD, espacios colapsados — en `domain/Normaliza.kt`).
- Upsert masivo; nunca `DELETE all` (el proyecto viejo perdía favoritos así).

## 4. Estado del código (commit fcffdd2 + working tree SUCIO con ~15 archivos)

### Committeado y presumiblemente sólido
- **Scaffold**: Gradle 8.13 wrapper (copiado de `keiyoushi/fork-extensions`, jar 8.x),
  `settings.gradle.kts`, version catalog con AGP 8.13.2 + Kotlin 2.2.21 + KSP
  2.2.21-2.0.5 + Compose BOM 2025.12.01 + Hilt 2.56.2 + Room 2.7.2 + nav 2.9.8 +
  Coil 3.6.2 + okhttp 4.12 + work 2.11.2 (versiones verificadas hoy contra maven
  metadata; NO tocar sin razón).
- **Tema M3**: semilla `#FD4902`, light+dark completo (`PriceUpRedDark` etc.),
  tipografía rampa M3, dinámico OFF a propósito.
- **Room**: `AppDatabase`, 4 entidades, 4 DAOs (ProductDao con `searchFlow(query,
  categoria, orden)` por CASE-WHEN, FavoriteDao con join `favoritosConProductosFlow`,
  CartDao con `CartLine` join + `quantityOfFlow` + `countFlow`, MetaDao).
  `exportSchema=false`, DB v1.
- **DTOs**: `RepoDtos` (mirror) y `OfficialDtos` (GraphQL) con `FlexIdSerializer`
  (acepta int/string/null), `Wrapper<T>{model}`, `DecimalAmount{"$numberDecimal"}`,
  `Json { ignoreUnknownKeys; isLenient; coerceInputValues }`.
- **HTTP**: sin Retrofit — `HttpClients` (OkHttp `fast` 10/20s y `slow` 20/120/150s),
  `RepoApi.getPrecios()`, `OfficialApi.downloadFile()` (devuelve String? interno).
- **Sync**: `BaseSyncWorker`/`EnrichSyncWorker` (@HiltWorker) + `SyncScheduler`
  (periódicas 6h / 1día, `KEEP`, backoff exponencial). `ProductRepository.requestEnrich()`
  encola one-time "sync_enrich_manual". `syncBase()` salta si `fecha_actualizacion` no cambió;
  `syncEnrich()` salta si `priceList.version` no cambió.
- **Tests JVM con FIXTURES REALES** (`app/src/test/resources/fixtures/`):
  `precios.json` y `downloadfile_graphql.json` capturados hoy — ParserTest (anti-drift)
  + MergeTest (3 casos: match por nombre conserva fila/precio repo, inserción api-only,
  merge mirror no destruye enriquecimiento). `assertEquals` de asserts específicos
  (Galleta Maria Puig: barcode 7591082000307, categoria confiteria, precioCec 1.6815801,
  tasa 832.49) — estos son la red de seguridad del parsing.
- **CI** `.github/workflows/build.yml`: JDK 21 temurin, gradle/actions/setup-gradle@v4
  con cache, keystore OPCIONAL desde secrets (`KEYSTORE_B64` + `KEYSTORE_PROPERTIES`
  con placeholder `__WORKDIR__`), `testDebugUnitTest` → `assembleDebug` →
  `assembleRelease` → artefacto `apks`. Release lee `keystore.properties` si existe.

### En el working tree, SIN commitear (el corte de la sesión)
- **UI completa**: `CatalogScreen` (BasicTextField custom, chips categoría+orden,
  PullToRefreshBox, staggered grid `Adaptive(170dp)`, `ProductoCard` flat-border sin
  elevación, icono de escáner vectorial propio en el buscador), `CatalogViewModel`
  (debounce 220 + flatMapLatest + stateIn WhileSubscribed(5s)), `DetailScreen`
  (TopAppBar, imagen 220dp, datos key/value, stepper/agregar), `FavoritesScreen`,
  `CartScreen` (SwipeToDismissBox EndToStart, steppers, total tabular, Intent ACTION_SEND
  con texto del carrito, dialog vaciar), `DeltaBadge` (vectores propios ic_trending_*,
  no material-icons-extended), `Format.kt` (DecimalFormat `#,##0.##` separadores fijos
  es-VE "1.365,5" + `PriceText` con tnum), `nav/Routes.kt` (3 Dest con iconos filled/outlined),
  `MainActivity` reescrito con Scaffold+NavigationBar+BadgedBox+NavHost (4 rutas),
  `CecosesolaApp` ahora implementa `SingletonImageLoader.Factory` (Coil global con el
  OkHttp `fast` compartido, memoryCache 12MB, crossfade OFF).
- `data/prefs/AppPrefs.kt` (DataStore Preferences: themeMode SYSTEM/LIGHT/DARK + boolean
  usd) y `ui/MainViewModel.kt` (expone ambos como StateFlow).
- `ui/common/PrecioVisible.kt`: `precioVisible(usd)` devuelve (precio, "Bs"|"USD") —
  **CREADO pero TODAVÍA NO CONECTADO** en las pantallas.
- Deps nuevas en build.gradle/toml: CameraX 1.6.2 (core/camera2/lifecycle/view) +
  ML Kit barcode bundled 17.3.0.

### ⚠️ LO QUE NO ESTÁ BIEN (a revisar primero, en orden)
1. **`ui/scanner/ScannerScreen.kt` es un BORRADOR ROTO** — la última llamada antes del
   corte iba a ser "rewrite cleanly". Defectos concretos a arreglar:
   - `private fun kotlinx.coroutines.CoroutineScope.launch(...)` — extensión shadow
     absurda; reemplazar por `viewModelScope.launch` (necesita scope: pasar el
     `CoroutineScope` o usar `viewModelScope` inyectando launch desde fuera; lo más
     simple: `init` con un scope propio o `SupervisorJob()+Dispatchers.Main`).
   - `collectAsStateWithLifecycleSafe()` y `collectNothing()` son basura — usar
     `androidx.lifecycle.compose.collectAsStateWithLifecycle` normal sobre `vm.resultado`.
   - `import androidx.lifecycle.ViewModel` pero falta `dagger`/`viewModelScope`; revisar imports.
   - `LaunchedEffect(lifecycleOwner)` para configurar CameraProvider: funcional pero
     el listener de `future.addListener` puede fugarse tras dispose → guardar y
     cancelar; o usar `await()` en coroutine.
   - FALTA: el callback `onBack` nunca se usa (no hay UI para volver — el sistema back
     sirve, pero añadir un IconButton de cierre queda mejor).
   - La resolución `Size(1280,720)` en `ResolutionStrategy` para ImageAnalysis está bien
     elegida a propósito para el G25 (mantener).
2. **`MainActivity` usa `hiltViewModel<CartViewModel>()` en el Scaffold raíz** — funciona
   (misma Activity-referenced store) pero el badge solo vive si el NavHost no destruye
   el VM… verificar: los VM de tabs se destruyen al cambiar de tab; `CartViewModel`
   se pide en `AppNav` (scope de la Activity) → OK, no se destruye. Pero `CartScreen`
   TAMBIÉN pide su propio `hiltViewModel()` (scope de backstack entry) → dos instancias;
   el badge lee una y la pantalla otra. **Unificar: que `CartScreen` reciba el VM o
   usar `viewModel(viewModelStoreOwner = LocalContext as ComponentActivity)`** — o
   mover el badge a leer `db.cartDao().countFlow()` vía un VM de la Activity.
3. **DeltaBadge recibe (`p.precioCec ?: p.precioBs`, `p.precioAnteriorCec`)** en
   CatalogScreen y DetailScreen: si `precioCec` es null, compara Bs contra CEC-anterior
   → % basura. **Corregir: solo pintar el badge si `precioCec != null && precioAnteriorCec != null`**
   (mover la guarda adentro: `if (actual==null) return` tomando Doubles nullables, o
   pasar un booleano). MergeTest/ParserTest no cubren esto.
4. **`Detalle` necesita el argumento Long**: `Routes.DETALLE="detalle/{productId}"`,
   `navArgument LongType` puesto — OK — pero la función `Detalle` con
   `savedStateHandle["productId"]` devuelve `Long?`: checkNotNull está; validar que
   navigation-compose inyecte Long de ruta string (sí lo hace con NavType.LongType).
5. **`CatalogViewModel.fechaRepo`** usa el truco `MutableStateFlow.also{launch}` —
   se calcula una sola vez en la creación del VM y nunca se actualiza tras un refresh.
   Cambiar por `flatMapLatest` sobre un trigger de refresh o recargarlo al terminar
   `refresh()`.
6. **`precioVisible`/`MainViewModel.usd`/toggle Bs⇄USD todavía no están cableados** a
   las pantallas (ver §5.6). `MainActivity` tampoco usa `MainViewModel.themeMode` aún —
   el Theme ignora el override manual (solo `isSystemInDarkTheme`).
7. **Ajustes (Settings) NO existe como pantalla** — estaba en el plan (fechas de sync,
   ferias, toggle tema/moneda, verificar datos). `Routes.AJUSTES`/`ESCANER` definidos
   en `nav/Routes.kt` pero ninguna ruta los registra en el NavHost (escáner sí está en
   el plan del paso siguiente).
8. **Tests nuevos sin escribir para lo último** (prefs/visible). Parser/Merge siguen
   corriendo y protegen datos.
9. **Nada de esto ha COMPILADO nunca** — ni local (imposible) ni en CI (no hay remote).
   Asumir errores de compilación en la primera corrida; el plan explícito era iterar
   contra CI. Los sospechosos habituales: imports en scanner, `PlatformContext` cast en
   `newImageLoader(context: PlatformContext)` (la firma de `SingletonImageLoader.Factory`
   en Coil 3.6 es `newImageLoader(context: PlatformContext)`, OK, pero verificar),
   `BorderStroke` importado inline como `androidx.compose.foundation.BorderStroke` en dos
   screens (compila, feo), `Alignment` en `Column(..., horizontalAlignment=...)` en
   CatalogScreen importado? (sí, `import Alignment` está), `FilterChip(selected=…)` sobre
   `List<ProductEntity>`… — en fin: primero push, luego leer los errores del log.

## 5. Plan original (aprobado por el usuario) — pasos y dónde vamos

Paso 1 Scaffold+CI ✅ · Paso 2 Data+tests ✅ · Paso 3 Catálogo 🟡 (UI escrita, sin CI) ·
Paso 4 Detalle+Favoritos 🟡 (UI escrita, cableado pendiente §4.2) · Paso 5 Carrito 🟡
(idem) · Paso 6 Enriquecimiento: backend ✅ pero DeltaBadge bug §4.3 y USD toggle sin
UI · Paso 7 Escáner 🔴 borrador roto §4.1 · Paso 8 Perf/polish/ajustes ❌ no empezado.

Cosas del plan aún NO hechas:
- Ajustes/Settings screen completa (§4.7).
- Tema manual (SYSTEM/LIGHT/DARK) aplicado en MainActivity desde MainViewModel.
- Toggle Bs⇄USD en la UI (las piezas existen: AppPrefs.usd, MainViewModel.usd,
  PrecioVisible.kt — solo falta consumirlas en cards/detalle/carrito/ajustes).
- WorkManager one-time "verificar datos" desde Ajustes (el método existe:
  `repo.requestEnrich()`; exponer botón).
- `ferias()` existe en ProductRepository pero ninguna UI lo muestra.
- README de instalación para el usuario.
- baseline profile: solo 8 líneas de arranque; falta enriquecer al final (Paso 8).
- El usuario preguntó por `npx autoskills` como segunda opinión — ofrecido, no hecho.
- Skills de Google planeados (`camerax`, `r8-analyzer`, `edge-to-edge`, `testing-setup`,
  `gradle-build-performance` vía `npx skills add android/skills --skill …`) — no instalados;
  `Skill(camerax)` falló ("Unknown skill") porque nunca se corrió la instalación. Se puede
  trabajar sin ellos (recetas estándar conocidas), pero para el paso 8 instalar
  `r8-analyzer`/`edge-to-edge` vale la pena.

## 6. Cómo iterar (build loop)

```bash
cd /root/frebufftesteos/Ceco
git add -A && git commit -m "…"      # messages terminan con: Co-Authored-By: Claude Code <noreply@anthropic.com>
# falta remote:  git remote add origin https://github.com/dusk0382/cecosesola-precios.git
#                git push -u origin main      (necesita credenciales válidas)
```
CI corre solo (`build.yml`). Ver resultados: `gh run list`/`gh run watch` si hay
CLI+token, o el usuario mira la pestaña Actions / descarga el artefacto `apks`.
Instalación: descargar `app-debug.apk` del artefacto y abrirlo en el teléfono.
Ritmo: el API provider del usuario de Claude Code tiene ~10 RPM / 5 paralelo —
espaciar polls a CI, trabajar en lotes de escritura.

Keystore para releases actualizables: `keytool -genkey -keystore release.keystore -alias ceco -keyalg RSA -keysize 2048 -validity 10000 -storepass <x> -keypass <x> -dname "CN=Cecosesola Precios"` (keytool funciona aquí, es Java). Subir a secrets:
`KEYSTORE_B64=$(base64 -w0 release.keystore)` y `KEYSTORE_PROPERTIES` con la ruta
`storeFile=__WORKDIR__/release.keystore` (el workflow sustituye `__WORKDIR__`).

## 7. Datos de referencia

- Sitio web PWA (referencia de UX): https://precios.cecosesola.coop/ — su bundle JS usa
  los mismos GraphQL `downloadFile` + dolarRate + localStorage offline (confirmado leyendo
  su `index.51b8d20d.mjs` en /tmp/app.mjs si aún existe; si no, recapturable).
- Repo viejo del usuario (Kotlin, deficiente, ya analizaday minada):
  https://github.com/dusk0382/test (público). Bugs de ahí que NO repetimos:
  `replaceAll` borraba filas/favoritos en cada sync; DTO esperaba `categoria`/`presentacion`
  que precios.json NUNCA tuvo (categorías en realidad vienen de la API oficial vía `tags`);
  CI regeneraba keystore efímero con pass "android" (perdía updates); targetSdk 35 sin
  edge-to-edge; Room+kapt+Moshi+Retrofit (ahora KSP + serialization + OkHttp pelado).
  Cosas buenas que SÍ copiamos: flat cards sin icons-extended, cache-first Room,
  WorkManager sin requiresCharging (lección en su comentario), formatter es-VE.
- Fixtures: `app/src/test/resources/fixtures/*.json` — regenerables hoy: mirror siempre;
  GraphQL solo si el schema no cambió (ParserTest lo dice).
- `keiyoushi/fork-extensions` (en `/root/frebufftesteos/keiyoushi`) = proyecto Android
  Kotlin preexistente del usuario; de ahí se copió el `gradlew` + wrapper jar 8.x.

## 8. Orden sugerido para retomar

1. Arreglar §4.1 (scanner) y §4.3 (guarda del DeltaBadge) — 20 min de edición.
2. Unificar VM del carrito para el badge (§4.2).
3. Cablear MainViewModel (tema + USD) en MainActivity y pantallas (§4.6).
4. Pantalla Ajustes (§4.7): lista simple — fechas sync, ferias, toggle USD (solo
   visible si `repo.tasaOficial()!=null`), tema, botón "Verificar datos"
   (`repo.requestEnrich()` + refresh base inline), crédito "Datos: precios.cecosesola.coop".
5. Commit TODO, pedir al usuario remote/credenciales válidas (el PAT viejo está muerto),
   push, mirar CI, iterar errores de compilación uno por lote (no uno por uno —
   rate limit).
6. Paso 8 del plan: R8/baseline/edge-to-edge (opcional: instalar `r8-analyzer`,
   `edge-to-edge` de android/skills antes).
7. README.

## 9. Cosas que el usuario ya decidió (no re-litigar)

- Kotlin nativo (no Expo/RN — el hype Appllama no aplica aquí; solo copiamos el "bar").
- Híbrido mirror+API con el mirror como base canónica.
- CI compila; instalación manual.
- Uso personal: sin marca "no oficial", nombre "Cecosesola Precios".
- Optimización G25 explícita y desde el día 1 (baseline profile, crossfade off,
  cache 12MB, cards sin elevación, 720p análisis, animaciones mínimas).
- Rate limit ~10 RPM/5 paralelo del PROVEEDOR DE CLAUDE DEL USUARIO (no de Cecosesola):
  espaciar tool calls, lotes grandes de escritura, polls escasos a CI.
