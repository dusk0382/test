#!/usr/bin/env bash
# ui_lint.sh — grep determinista sobre el código Compose (DESIGN.md §6.4).
# No juzga el diseño: cuenta tells medibles. Falla si encuentra alguno.
#
# Lo corre la CI antes de los tests, así que un tell introducido en un PR se ve
# sin tener que instalar el APK.
set -uo pipefail
cd "$(dirname "$0")/.."

UI=app/src/main/java/com/dusk0382/cecosesolaprecios/ui
fail=0

chk() { # chk <descripcion> <patron-egrep> [archivos...]
    local desc="$1" pat="$2"; shift 2
    local hits
    # Se excluyen líneas de comentario (path:line:contenido): los tells
    # cuentan en código ejecutable/strings, no en documentación.
    hits=$(grep -rnE "$pat" "$@" 2>/dev/null | grep -vE ':[0-9]+: */?(\*|/|$)' || true)
    if [ -n "$hits" ]; then
        echo "✗ $desc"
        echo "$hits" | sed 's/^/    /'
        fail=1
    else
        echo "✓ $desc"
    fi
}

UI_FILES=$(find "$UI" -name '*.kt' -not -path '*/theme/*')

# §3.1 — sin emojis en código de UI
emoji=$(grep -rnP "[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}\x{2B00}-\x{2BFF}]" "$UI" 2>/dev/null || true)
if [ -n "$emoji" ]; then echo "✗ Emoji en UI:"; echo "$emoji" | sed 's/^/    /'; fail=1; else echo "✓ sin emojis en UI"; fi

# §3.2 — sin border como estructura de tarjeta
chk "sin BorderStroke en tarjetas" "BorderStroke" "$UI"

# §3.3 — sin literales en MAYÚSCULAS para labels (4+ letras, excluye constantes)
chk "sin labels MAYÚSCULAS en strings de UI" "\"[A-ZÁÉÍÓÚÑ]{4,}[^\"]*\"" \
    "$UI/catalog" "$UI/cart" "$UI/detail" "$UI/settings" "$UI/favorites" 2>/dev/null || true

# colores fuera del tema (solo theme/ define hex)
chk "sin Color(0x...) fuera de theme/" "Color\(0x" \
    "$UI" --include="*.kt" --exclude-dir=theme 2>/dev/null || true

# §5 — formas por token, no radios manuales (theme/Forma.kt es LA definición)
chk "sin RoundedCornerShape sueltos (usar MaterialTheme.shapes)" "RoundedCornerShape\(" \
    $UI_FILES 2>/dev/null || true

# §5 — espaciado solo por token. Espacio.kt es la única fuente (DESIGN.md §5:
# "si un número de separación aparece suelto … es un defecto").
#
# El patrón solo mira argumentos de ESPACIADO a propósito: `padding`, `spacedBy`,
# `Spacer` y `offset`. Un `.dp` en `size()` o en `height()` de una imagen NO es
# espaciado, es una métrica de componente (alto de la foto, ancho de columna,
# tamaño de ícono, stroke), y esos sí son números propios.
chk "sin .dp de espaciado fuera de los tokens" \
    "(padding\([[:space:]]*[A-Za-z0-9_.]*[0-9]+\.dp|spacedBy\([0-9]+\.dp|Spacer\([^)]*[0-9]+\.dp|offset\([^)]*[0-9]+\.dp)" \
    $UI_FILES 2>/dev/null || true

# §3.6 — sin shadows de color. Elevation 0 en tarjetas: la jerarquía la da el tono.
chk "sin elevation distinta de 0 en tarjetas" "cardElevation\([^)]*[1-9][0-9]*\.dp" \
    $UI_FILES 2>/dev/null || true

# Coherencia de imports: aquí no se compila, así que un paquete equivocado
# cuesta un ciclo entero de CI.
python3 tools/imports.py || fail=1

# Accesibilidad y precio: checks que necesitan mirar varias líneas a la vez.
# Van en Python porque grep no puede seguir una llamada a Icon( abierta y
# buscar el contentDescription de las líneas siguientes.
python3 tools/ui_checks.py || fail=1

# Accesibilidad: el área táctil mínima. 40dp por debajo de 48dp no cumple.
chk "sin áreas táctiles de 40dp (el mínimo es 48)" "size\(40\.dp\)" \
    $UI_FILES 2>/dev/null || true

if [ "$fail" -eq 0 ]; then
    echo "---"
    echo "ui_lint: limpio"
    exit 0
else
    echo "---"
    echo "ui_lint: tells encontrados (ver DESIGN.md §3)"
    exit 1
fi
