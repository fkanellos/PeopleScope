# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Retrofit
-dontwarn retrofit2.**
-dontwarn okio.**
-dontwarn com.squareup.okhttp.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Data models - Keep all DTO classes
-keep class gr.pkcoding.peoplescope.data.remote.dto.** { *; }
-keep class gr.pkcoding.peoplescope.domain.model.** { *; }
-keep class gr.pkcoding.peoplescope.data.local.entity.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Koin
-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }

# Paging 3
-keep class androidx.paging.** { *; }

# Coil
-keep class coil.** { *; }

# Timber
-keep class timber.log.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Navigation Compose
-keep class androidx.navigation.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel

# Keep all classes that have @Serializable annotation
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep `Companion` object fields of serializable classes.
-keepclassmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes
-keepclassmembers class **.*$serializer {
    *** INSTANCE;
}

# Remove Log calls
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Remove Timber log calls in release builds
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}