#!/usr/bin/env bash
# tools/smoke_apk.sh — prueba de humo de arranque: instala un APK en el
# dispositivo/emulador conectado por adb, abre la app y decide si arranca o crashea.
# Uso: tools/smoke_apk.sh <apk> <applicationId> [logcat-destino]
#
# Un FATAL EXCEPTION solo cuenta si es de NUESTRO proceso: en un emulador el
# buffer trae crashes de otros (Gmail, sistema) que no son asunto del APK.
# Sale 0 si la app sigue viva sin crash propio, 1 si crashea y 2 si no se pudo probar.
set -u

APK="$1"
PKG="$2"
LOG="${3:-/tmp/smoke-logcat.txt}"
ESPERA="${ESPERA_SEGUNDOS:-30}"

echo "=== [$PKG] instalando $APK"
adb logcat -c
if ! adb install -r "$APK"; then
    echo "FALLO: no se pudo instalar $APK"
    exit 2
fi

# El componente real es <applicationId>/<clase>: la clase NO lleva el sufijo del
# applicationId del debug (.debug), así que se pregunta al sistema en vez de
# construir el nombre a mano.
COMPONENTE=$(adb shell cmd package resolve-activity --brief "$PKG" 2> /dev/null | tr -d '\r' | tail -n 1)
if [ -z "$COMPONENTE" ] || echo "$COMPONENTE" | grep -qi "no activity"; then
    echo "FALLO: no se pudo resolver la actividad launcher de $PKG"
    exit 2
fi

echo "=== [$PKG] abriendo $COMPONENTE"
if ! adb shell am start -W -n "$COMPONENTE"; then
    echo "FALLO: am start devolvio error para $COMPONENTE"
    exit 2
fi
PID=$(adb shell pidof -s "$PKG" 2> /dev/null | tr -d '\r' || true)

echo "=== [$PKG] esperando ${ESPERA}s (primer frame + sync inicial)"
sleep "$ESPERA"

adb logcat -d > "$LOG"
# Además del buffer completo, la traza aislada del proceso (si aún se conoce el PID).
if [ -n "$PID" ]; then
    adb logcat -d --pid="$PID" >> "$LOG" 2> /dev/null || true
fi

# Bloque de traza cuyo `Process:` es el nuestro (va pegado a la cabecera).
TRAZA=$(awk -v pkg="Process: $PKG" '
    /FATAL EXCEPTION/ { buf = $0; n = 1; next }
    n > 0 { buf = buf "\n" $0; n++; if (index($0, pkg) > 0) { print buf; exit } }
' "$LOG")

if [ -n "$TRAZA" ]; then
    echo "### CRASH en $PKG"
    echo "$TRAZA"
    echo "### logcat completo: $LOG"
    exit 1
fi

if grep -q "ANR in $PKG" "$LOG"; then
    echo "### ANR en $PKG"
    grep -m1 -A 40 "ANR in $PKG" "$LOG"
    exit 1
fi

if ! adb shell pidof "$PKG" > /dev/null 2>&1; then
    echo "### CRASH en $PKG: el proceso no está vivo tras ${ESPERA}s"
    tail -n 120 "$LOG"
    exit 1
fi

otros=$(grep -c "FATAL EXCEPTION" "$LOG" || true)
echo "=== [$PKG] OK: arrancó y sigue vivo (logcat en $LOG; crashes ajenos en el buffer: $otros)"
exit 0
