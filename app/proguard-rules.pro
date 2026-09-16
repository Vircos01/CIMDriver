# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.** { *; }

# Hilt
-keep,allowobfuscation,allowshrinking interface dagger.hilt.internal.GeneratedEntryPoint
-keep,allowobfuscation,allowshrinking @dagger.hilt.internal.ComponentInterfaces interface *

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# MapLibre
-keep class org.maplibre.gl.** { *; }
-keep class org.maplibre.android.** { *; }

# Models
-keep class com.cimdriver.app.data.local.entity.** { *; }

# Compose
-keep class androidx.compose.** { *; }

# Tracking Service & Diagnostics
-keep class com.cimdriver.app.service.ServiceState { *; }
-keep class com.cimdriver.app.service.TrackingStatusStore { *; }
