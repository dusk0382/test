#!/usr/bin/env bash
# tools/smoke_all.sh — prueba de humo de arranque de los APKs disponibles en el emulador.
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

# Se prueba siempre lo que haya (aunque el release falle): interesa saber si el
# problema es solo del release minificado o de ambos. El release no tiene nombre
# fijo: sin keystore, AGP produce `app-release-unsigned.apk`, que no se instala.
RELEASE_APK="$(ls "$DIR"/app-release*.apk 2>/dev/null | head -n 1 || true)"
if [ -z "$RELEASE_APK" ]; then
    echo "### release: no hay app-release*.apk, se omite el smoke"
elif printf '%s' "$RELEASE_APK" | grep -qi 'unsigned'; then
    echo "### release: APK sin firmar, se omite el smoke (configurá el keystore)"
else
    bash tools/smoke_apk.sh "$RELEASE_APK" "$REC" /tmp/logcat-release.txt || RC=1
    adb uninstall "$REC" > /dev/null 2>&1 || true
fi
bash tools/smoke_apk.sh "$DIR/app-debug.apk" "$DBG" /tmp/logcat-debug.txt || RC=1

if [ "$RC" = 0 ]; then
    echo "### resultado: los APKs probados arrancan y siguen vivos"
else
    echo "### resultado: FALLO — ver la traza de arriba"
fi
exit "$RC"
