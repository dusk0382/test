#!/usr/bin/env python3
"""
ui_checks.py — tells estructurales del código Compose (DESIGN.md §6.4).

Lo que `ui_lint.sh` no puede hacer con grep: mirar una llamada a `Icon(` y
comprobar si en las líneas siguientes hay un `contentDescription`. Casi todos los
Icon de esta app ocupan varias líneas, así que un grep línea a línea marca
falsos positivos en todos.

Salida: una línea por hallazgo y código de salida 1 si hubo alguno.
"""
import re
import sys
from pathlib import Path

UI = Path("app/src/main/java/com/dusk0382/cecosesolaprecios/ui")
# theme/ es donde se declaran los valores: ahí los números y los hex son el token.
archivos = sorted(p for p in UI.rglob("*.kt") if "theme" not in p.parts)

HALLAZGOS: list[tuple[Path, int, str]] = []

# El call site es `Icon(`, no `Icons.Filled.Add`. La versión anterior saltaba la
# línea entera si contenía `Icons.`, y como casi todo Icon de esta app pasa una
# imagen de Icons por dentro, ese filtro se comía los casos que había que ver.
LLAMADA_ICON = re.compile(r"(?<![A-Za-z])Icon\s*\(")



def quitar_comentario(linea: str) -> str:
    """Lo que cuenta es el código, no lo documentado."""
    return re.sub(r"//.*$", "", linea)


def _llamada_completa(lineas: list[str], i: int, desde: int) -> str:
    """
    Reúne el texto de una llamada que puede ocupar varias líneas, contando
    paréntesis para no cortar en el primer `)` de un anidado.

    `desde` es el offset del call site dentro de la línea: sin él, una llamada
    anidada como `IconButton(onBack) { Icon(..., "Atrás") }` empezaba a contar
    en el paréntesis del IconButton y el segundo argumento salía mal.
    """
    texto = ""
    profundidad = 0
    for j in range(i, min(i + 20, len(lineas))):
        trozo = lineas[j][desde:] if j == i else lineas[j]
        texto += trozo
        profundidad += trozo.count("(") - trozo.count(")")
        # Solo el balance de parentesis. La condicion anterior tambien exigia que
        # la linea terminara en ")": con una lambda final
        # (`IconButton(onClick = { … }) { … }`) la linea cierra en ")" pero
        # TERMINA en "{", asi que no cortaba ahi y se comia el `modifier` del Icon
        # de adentro, reportando un boton que no tenia size() propio.
        if profundidad <= 0:
            return texto
        desde = 0
    return texto


def _segundo_argumento(texto: str) -> str | None:
    """El segundo argumento de la llamada, o None si no hay."""
    inicio = texto.index("(") + 1  # el texto ya arranca en el call site de Icon(
    profundidad = 0
    for k, ch in enumerate(texto[inicio:], start=inicio):
        if ch == "(":
            profundidad += 1
        elif ch == ")":
            if profundidad == 0:
                return None
            profundidad -= 1
        elif ch == "," and profundidad == 0:
            return texto[k + 1 :].lstrip()
    return None


def revisar_icon(archivo: Path, lineas: list[str]) -> None:
    """
    La etiqueta de un `Icon` puede venir de dos formas válidas, y esta app usa
    las dos: con nombre (`contentDescription = "Filtros"`) o como segundo
    argumento posicional (`Icon(painter, "Filtros")`). Aceptar solo la primera
    daba 4 falsos positivos; rechazar las dos deja iconos de verdad sin
    etiquetar.
    """
    for i, linea in enumerate(lineas):
        m = LLAMADA_ICON.search(linea)
        if not m:
            continue
        texto = _llamada_completa(lineas, i, m.start())
        if "contentDescription" in texto:
            continue
        segundo = _segundo_argumento(texto)
        if segundo and segundo.startswith(('"', "null")):
            continue  # etiqueta posicional: string literal o null decorativo
        HALLAZGOS.append(
            (archivo, i + 1, "Icon(...) sin contentDescription (usá null si es decorativo)")
        )


# Un precio se pinta con PriceText. Un Text suelto que llama a formatBs es el
# mecanismo por el que el precio del detalle quedó en el naranja de marca
# (3.26:1) mientras el de la tarjeta usaba el token de acento (5.66:1).
PATRON_PRECIO = re.compile(r"\bText\s*\(", re.M)
PATRON_FORMATO = re.compile(r"formatBs\s*\(")


def revisar_precio(archivo: Path, lineas: list[str]) -> None:
    for i, linea in enumerate(lineas):
        if not PATRON_PRECIO.search(linea):
            continue
        ventana = " ".join(lineas[i : i + 6])
        if PATRON_FORMATO.search(ventana):
            HALLAZGOS.append((archivo, i + 1, "precio pintado con un Text propio; usá PriceText"))


# Botones de icono: no se les pone `size()` propio.
#
# `IconButton` y familia ya aplican `minimumInteractiveComponentSize()` y luego su
# propio `size(40.dp)`, así que el objetivo tactil ya es de 48dp sin que el caller
# intervenga. Poner `size(48.dp)` desde el caller no cambia el dibujo —el `size` del
# caller llega antes en la cadena y el de M3 se aplica al final— pero si reserva
# 48dp de fila para dibujar 40. En la tarjeta de catalogo (unos 158dp) el precio mas
# el stepper median 215dp y el boton "+" quedaba cortado contra el borde: en el
# screenshot se veia como una astilla vertical, y el producto era casi inagregable.
CONTROLES_ICONO = re.compile(
    r"(?<![A-Za-z])(?:IconButton|OutlinedIconButton|FilledIconButton|IconToggleButton)\s*\("
)


def revisar_boton(archivo: Path, lineas: list[str]) -> None:
    for i, linea in enumerate(lineas):
        m = CONTROLES_ICONO.search(linea)
        if not m:
            continue
        texto = _llamada_completa(lineas, i, m.start())
        # Solo el primer argumento con nombre `modifier`, que es el del control.
        propio = re.search(r"modifier\s*=\s*([^,)]*)", texto)
        if propio and re.search(r"\.size\s*\(", propio.group(1)):
            HALLAZGOS.append(
                (
                    archivo,
                    i + 1,
                    "size() explicito en un boton de icono: M3 ya pone el tamano y "
                    "el objetivo tactil; fijarlo reserva mas fila de la que dibuja",
                )
            )


for archivo in archivos:
    crudo = archivo.read_text(encoding="utf-8")
    lineas = [quitar_comentario(l) for l in crudo.splitlines()]
    revisar_icon(archivo, lineas)
    revisar_precio(archivo, lineas)
    revisar_boton(archivo, lineas)

if not HALLAZGOS:
    print("✓ precios con PriceText, Icon con etiqueta, botones sin size() propio")
    sys.exit(0)

for archivo, linea, msg in HALLAZGOS:
    print(f"    {archivo}:{linea}: {msg}")
print("✗ checks estructurales de UI: hay hallazgos (arriba)")
sys.exit(1)
