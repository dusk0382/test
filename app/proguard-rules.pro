# ──────────────────────────────────────────────────────────────────────────────
# Retrofit & OkHttp
# ──────────────────────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ──────────────────────────────────────────────────────────────────────────────
# Gson
# ──────────────────────────────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }
-keep class com.cecosesola.coop.data.remote.** { *; }
-keep class com.cecosesola.coop.domain.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ──────────────────────────────────────────────────────────────────────────────
# Room
# ──────────────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.cecosesola.coop.data.local.** { *; }
-dontwarn androidx.room.paging.**

# ──────────────────────────────────────────────────────────────────────────────
# Coil
# ──────────────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ──────────────────────────────────────────────────────────────────────────────
# Coroutines
# ──────────────────────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ──────────────────────────────────────────────────────────────────────────────
# WorkManager
# ──────────────────────────────────────────────────────────────────────────────
-keep class androidx.work.** { *; }
-keep class com.cecosesola.coop.workers.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# Timber
# ──────────────────────────────────────────────────────────────────────────────
-dontwarn org.jetbrains.annotations.**
-keep class timber.log.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# Compose
# ──────────────────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ──────────────────────────────────────────────────────────────────────────────
# App específicas
# ──────────────────────────────────────────────────────────────────────────────
-keep class com.cecosesola.coop.MainActivity { *; }
-keep class com.cecosesola.coop.CecosesolaApp { *; }

# Mantener nombres de clase para debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
