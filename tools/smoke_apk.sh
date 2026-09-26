#!/usr/bin/env bash
# tools/smoke_apk.sh — prueba de humo de arranque: instala un APK en el
# dispositivo/emulador conectado por adb, abre la app y decide si arranca o crashea.
# Uso: tools/smoke_apk.sh <apk> <applicationId> <actividad> [logcat-destino]
#
# Sale 0 si el proceso sigue vivo sin FATAL EXCEPTION, 1 si crashea y 2 si no instala.
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

echo "=== [$PKG] esperando ${ESPERA}s (primer frame + sync inicial)"
sleep "$ESPERA"

adb logcat -d > "$LOG"

if grep -q "FATAL EXCEPTION" "$LOG"; then
    echo "### CRASH en $PKG"
    grep -m1 -A 80 "FATAL EXCEPTION" "$LOG"
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

echo "=== [$PKG] OK: arrancó y sigue vivo (logcat en $LOG)"
exit 0
