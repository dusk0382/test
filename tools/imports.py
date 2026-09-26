#!/usr/bin/env python3
"""
imports.py — coherencia de imports en el código Kotlin.

Motivo: en este proyecto no se puede compilar localmente (el SDK de Android es
x86-64 y el host es aarch64), así que cada error de import cuesta un ciclo
completo de CI de ~1.5 min. Los que mas salieron en el último commit:

- `TextAlign` importado de `androidx.compose.ui.text` en vez de
  `androidx.compose.ui.text.style`.
- `coil.compose` en vez de `coil3.compose` (el proyecto usa Coil 3, paquete
  `coil3`).
- Falta un import al mover código entre archivos (se movió la tarjeta del
  catálogo a ui/common y `Arrangement` se quedó atrás).

Los tres son detectables sin compilar. Lo que NO se puede detectar es si un
import está bien escrito pero no existe en la librería: eso lo decide el
compilador, no este script.

Salida: una línea por hallazgo, código 1 si hubo alguno.
"""
import re
import sys
from pathlib import Path

RAIZ = Path("app/src")
problemas: list[str] = []

# Packages que existen en el proyecto con prefijo distinto al que se suele
# escribir. El valor es el prefijo correcto.
PREFIJOS = {
    "androidx.compose.ui.text.TextAlign": "androidx.compose.ui.text.style.TextAlign",
    "androidx.compose.ui.text.FontWeight": "androidx.compose.ui.text.font.FontWeight",
    "androidx.compose.ui.text.TextStyle": "androidx.compose.ui.text.TextStyle",
    "coil.compose.": "coil3.compose.",
    "coil.request.": "coil3.request.",
    "coil.network.": "coil3.network.",
    "coil.size.": "coil3.size.",
}
# Prefijos que aparecen como import y sí existen (para no reportarlos).
PREFIXOS_VALIDOS = {
    "androidx.compose.material.icons.",
    "androidx.compose.foundation.layout.",
    "androidx.compose.foundation.text.",
    "androidx.compose.foundation.shape.",
    "androidx.compose.foundation.pager.",
    "androidx.compose.runtime.",
    "androidx.compose.ui.",
    "androidx.compose.material3.",
    "androidx.compose.material.",
    "androidx.lifecycle.",
    "androidx.navigation.",
    "androidx.room.",
    "androidx.work.",
    "androidx.hilt.",
    "androidx.datastore.",
    "androidx.camera.",
    "androidx.activity.",
    "androidx.core.",
    "androidx.sqlite.",
    "androidx.test.",
    "com.google.mlkit.",
    "com.google.dagger.",
    "com.google.devtools.ksp.",
    "com.dusk0382.cecosesolaprecios.",
    "coil3.",
    "kotlinx.",
    "java.",
    "javax.",
    "kotlin.",
    "okhttp3.",
    "dagger.",
}

# Símbolos Compose que se importan desde un subpaquete y se usan muy a menudo.
# La clave es el error tipográfico más común: el subpaquete se olvida.
SIMBOLO_A_PAQUETE = {
    "TextAlign": "androidx.compose.ui.text.style.",
    "FontWeight": "androidx.compose.ui.text.font.",
    "TextUnit": "androidx.compose.ui.unit.",
    "Dp": "androidx.compose.ui.unit.",
    "IntOffset": "androidx.compose.ui.unit.",
    "Arrangement": "androidx.compose.foundation.layout.",
    "Alignment": "androidx.compose.ui.",
    "ContentScale": "androidx.compose.ui.layout.",
    "TextOverflow": "androidx.compose.ui.text.style.",
    "KeyboardOptions": "androidx.compose.foundation.text.",
    "KeyboardActions": "androidx.compose.foundation.text.",
    "ImeAction": "androidx.compose.ui.text.input.",
    "KeyboardType": "androidx.compose.ui.text.input.",
    "LaunchedEffect": "androidx.compose.runtime.",
    "remember": "androidx.compose.runtime.",
    "mutableStateOf": "androidx.compose.runtime.",
    "derivedStateOf": "androidx.compose.runtime.",
    "collectAsStateWithLifecycle": "androidx.lifecycle.compose.",
    "verticalScroll": "androidx.compose.foundation.",
    "horizontalScroll": "androidx.compose.foundation.",
}


# Símbolos que se usan como identificador desnudo y cuyo import se olvida fácil
# al mover código de archivo. La lista es corta a propósito: cada entrada tiene
# que ser un símbolo de librería (nunca definible localmente en esta app) y que
# aparezca en el cuerpo sin estar importado.
SIMBOLOS_QUE_SE_OLVIDAN = [
    "Arrangement", "Alignment", "ContentScale", "TextOverflow", "TextAlign",
    "KeyboardOptions", "ImeAction", "collectAsStateWithLifecycle", "AsyncImage",
    "verticalScroll", "animateItem", "animateContentSize", "withTransaction",
    "mutableStateOf", "rememberSaveable", "LaunchedEffect", "stateIn",
    "combine", "debounce", "flatMapLatest", "distinctUntilChanged", "SharingStarted",
    "bottomSheet", "Crossfade", "scaleIn", "fadeIn", "slideInVertically",
]


def _sin_comentarios(txt: str) -> str:
    """Lo que cuenta es el código: un símbolo nombrado en un comentario no
    necesita import (y `debounce` aparece en varios)."""
    return "\n".join(
        l.split("//")[0] for l in txt.splitlines() if not l.startswith("import ")
    )


def revisar(path: Path) -> None:
    txt = path.read_text(encoding="utf-8")
    cuerpo = _sin_comentarios(txt)
    importados = {
        m.group(1).split(".")[-1] for m in re.finditer(r"^import ([\w.]+)$", txt, re.M)
    }
    for m in re.finditer(r"^import ([\w.]+)$", txt, re.M):
        fqn = m.group(1)
        if not fqn.startswith("androidx.") and not fqn.startswith("coil"):
            continue
        # 1. prefijo equivocado (coil vs coil3, subpaquete de text)
        for mal, bien in PREFIJOS.items():
            if fqn.startswith(mal) and mal != bien:
                problemas.append(
                    f"{path}:{txt[: m.start()].count(chr(10)) + 1}: "
                    f"import {fqn} — el paquete es {bien}{fqn[len(mal):]}"
                )
        # 2. símbolo conocido en el paquete equivocado
        simbolo = fqn.split(".")[-1]
        if simbolo in SIMBOLO_A_PAQUETE:
            esperado = SIMBOLO_A_PAQUETE[simbolo]
            if not fqn.startswith(esperado):
                problemas.append(
                    f"{path}:{txt[: m.start()].count(chr(10)) + 1}: "
                    f"{simbolo} viene de {esperado}, no de {fqn.rsplit('.', 1)[0]}"
                )
        # 3. paquete raíz desconocido
        raiz = ".".join(fqn.split(".")[:2])
        if raiz not in {"androidx.compose", "androidx.room", "coil.compose", "coil3.compose"}:
            if not any(fqn.startswith(p) for p in PREFIXOS_VALIDOS):
                problemas.append(
                    f"{path}:{txt[: m.start()].count(chr(10)) + 1}: "
                    f"paquete no reconocido: {fqn}"
                )
        # 4. import sin uso (los operators de delegación `by` no aparecen
        #    en el cuerpo, así que se exceptúan)
        nombre = simbolo
        if nombre in ("getValue", "setValue", "provideDelegate"):
            continue
        if not re.search(r"(?<![A-Za-z0-9_])" + re.escape(nombre) + r"(?![A-Za-z0-9_])", cuerpo):
            problemas.append(f"{path}:{txt[: m.start()].count(chr(10)) + 1}: import sin uso: {nombre}")

    # 5. símbolo usado, no importado y no definido en el archivo: el error que
    #    dejó la tarjeta sin `Arrangement` al moverla a ui/common.
    definidos_en_el_archivo = set(re.findall(r"\b(fun|val|var|class|object)\s+(\w+)", cuerpo))
    definidos_en_el_archivo = {d[1] for d in definidos_en_el_archivo}
    for simbolo in SIMBOLOS_QUE_SE_OLVIDAN:
        if simbolo in importados or simbolo in definidos_en_el_archivo:
            continue
        # Se usa como identificador desnudo, no como `algo.algo`
        if re.search(r"(?<![A-Za-z0-9_.])" + simbolo + r"(?![A-Za-z0-9_(])", cuerpo):
            problemas.append(f"{path}: {simbolo} se usa pero no está importado")


for p in sorted(RAIZ.rglob("*.kt")):
    revisar(p)

if not problemas:
    print("✓ imports coherentes: sin paquetes mal, sin símbolos del subpaquete equivocado, sin imports muertos")
    sys.exit(0)

for x in problemas:
    print(f"    {x}")
print("✗ imports: hay hallazgos (arriba)")
sys.exit(1)
