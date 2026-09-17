# Reglas de R8/ProGuard — Cecosesola Precios
# La mayoría de las libs (Compose, Room, OkHttp, kotlinx-serialization) traen
# sus propias reglas de consumidor. Solo se añaden excepciones reales aquí.

# kotlinx-serialization: los serializers los genera el plugin en compilación y
# kotlinx-serialization-json ya trae sus propias reglas de consumidor, así que
# no hace falta ninguna regla aquí. (Una regla con `kotlinx.serialization.<any>`
# no es sintaxis válida de R8 y con enableR8.fullMode=true rompe el release.)
