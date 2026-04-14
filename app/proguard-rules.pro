# ──────────────────────────────────────────────────────────────────────────────
# Reglas mínimas y quirúrgicas.
#
# Coil, OkHttp, Compose y Kotlin Coroutines incluyen sus propias consumer rules
# en el artefacto — R8 las aplica automáticamente sin que las repitas aquí.
# Las reglas "-keep class X.** { *; }" amplias que estaban antes impedían que
# R8 eliminara código no usado de esas librerías.
# ──────────────────────────────────────────────────────────────────────────────

# Retrofit — necesita mantener tipos de retorno genéricos para la reflexión de interfaces
-keepattributes Signature, Exceptions, *Annotation*, InnerClasses, EnclosingMethod
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Moshi codegen — los adaptadores se generan en compile-time, no necesitan keep amplio
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Modelos de datos (accedidos por nombre en serialización y Room)
-keep class com.cecosesola.coop.data.remote.** { *; }
-keep class com.cecosesola.coop.domain.model.** { *; }

# Room
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.cecosesola.coop.data.local.** { *; }
-dontwarn androidx.room.paging.**

# WorkManager
-keep class com.cecosesola.coop.workers.** { *; }

# Coroutines — campos volátiles internos usados por reflection
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Eliminar logs de Timber en release (incluso la construcción del string)
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Mantener info de línea para stack traces legibles
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
