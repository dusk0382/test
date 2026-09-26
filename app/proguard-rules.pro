# Reglas de R8/ProGuard — Cecosesola Precios
# La mayoría de las libs (Compose, Room, OkHttp, kotlinx-serialization) traen
# sus propias reglas de consumidor. Solo se añaden excepciones reales aquí.

# Sin esto el release no dice en qué archivo/línea murió: mapping.txt retraza el
# nombre del método, pero la línea se pierde. Coste en tamaño: despreciable.
-keepattributes SourceFile,LineNumberTable

# kotlinx-serialization: los serializers los genera el plugin en compilación y
# kotlinx-serialization-json ya trae sus propias reglas de consumidor, así que
# no hace falta ninguna regla aquí. (Una regla con `kotlinx.serialization.<any>`
# no es sintaxis válida de R8 y con enableR8.fullMode=true rompe el release.)
