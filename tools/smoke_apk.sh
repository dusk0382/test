#!/usr/bin/env bash
# tools/smoke_apk.sh — prueba de humo de arranque: instala un APK en el
# dispositivo/emulador conectado por adb, abre la app y decide si arranca o crashea.
# Uso: tools/smoke_apk.sh <apk> <applicationId> <actividad> [logcat-destino]
#
# Un FATAL EXCEPTION solo cuenta si es de NUESTRO proceso: en un emulador el
# buffer trae crashes de otros (Gmail, sistema) que no son asunto del APK.
# Sale 0 si la app sigue viva sin crash propio, 1 si crashea y 2 si no instala.
set -u

APK="$1"
PKG="$2"
ACTIVIDAD="$3"
LOG="${4:-/tmp/smoke-logcat.txt}"
ESPERA="${ESPERA_SEGUNDOS:-30}"

echo "=== [$PKG] instalando $APK"
adb logcat -c
if ! adb install -r "$APK"; then
    echo "FALLO: no se pudo instalar $APK"
    exit 2
fi

echo "=== [$PKG] abriendo $PKG/$ACTIVIDAD"
adb shell am start -W -n "$PKG/$ACTIVIDAD" || true
PID=$(adb shell pidof -s "$PKG" 2> /dev/null | tr -d '\r' || true)

echo "=== [$PKG] esperando ${ESPERA}s (primer frame + sync inicial)"
sleep "$ESPERA"

adb logcat -d > "$LOG"
# Además del buffer completo, la traza aislada del proceso (si aún se conoce el PID).
if [ -n "$PID" ]; then
    adb logcat -d --pid="$PID" >> "$LOG" 2> /dev/null || true
fi

# Bloque de traza cuyo `Process:` es el nuestro (va pegado a la cabecera).
TRaza=$(awk -v pkg="Process: $PKG" '
    /FATAL EXCEPTION/ { buf = $0; n = 1; next }
    n > 0 { buf = buf "\n" $0; n++; if (index($0, pkg) > 0) { print buf; exit } }
' "$LOG")

if [ -n "$TRaza" ]; then
    echo "### CRASH en $PKG"
    echo "$TRaza"
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
