#!/usr/bin/env bash
# ui_lint.sh — grep determinista sobre el código Compose (DESIGN.md §6.4).
# No juzga el diseño: cuenta tells medibles. Falla si encuentra alguno.
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
    $(find "$UI" -name '*.kt' -not -path '*/theme/*') 2>/dev/null || true

# iconos con contentDescription null permitido solo en imagen decorativa:
# contar Icon( sin segundo argumento es difícil en grep; se deja al code review.

if [ "$fail" -eq 0 ]; then
    echo "---"
    echo "ui_lint: limpio"
    exit 0
else
    echo "---"
    echo "ui_lint: tells encontrados (ver DESIGN.md §3)"
    exit 1
fi
