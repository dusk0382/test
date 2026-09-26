#!/usr/bin/env bash
# tools/smoke_all.sh — prueba de humo de arranque de los dos APKs en el emulador.
# Uso: tools/smoke_all.sh <directorio-con-apks>
#
# Existe porque `reactivecircus/android-emulator-runner` ejecuta el `script` línea
# por línea, cada una en un shell nuevo: variables como `release=$?` se pierden
# entre líneas. Por eso la orquestación vive aquí, en un solo proceso.
set -u

DIR="${1:-apks}"
REC="com.dusk0382.cecosesolaprecios"
DBG="$REC.debug"
RC=0

# Se prueban los dos SIEMPRE (aunque el primero falle): interesa saber si el
# problema es solo del release minificado o de ambos.
bash tools/smoke_apk.sh "$DIR/app-release.apk" "$REC" /tmp/logcat-release.txt || RC=1
adb uninstall "$REC" > /dev/null 2>&1 || true
bash tools/smoke_apk.sh "$DIR/app-debug.apk" "$DBG" /tmp/logcat-debug.txt || RC=1

if [ "$RC" = 0 ]; then
    echo "### resultado: release y debug arrancan y siguen vivos"
else
    echo "### resultado: FALLO — ver la traza de arriba"
fi
exit "$RC"
