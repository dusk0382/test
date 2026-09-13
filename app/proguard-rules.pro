# Reglas de R8/ProGuard — Cecosesola Precios
# La mayoría de las libs (Compose, Room, OkHttp, kotlinx-serialization) traen
# sus propias reglas de consumidor. Solo se añaden excepciones reales aquí.

# kotlinx-serialization: mantener serializers generados por el plugin
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-keep interface kotlinx.serialization.<any> { *; }
