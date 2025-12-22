# Preserve ExoPlayer classes
-keep class com.google.android.exoplayer2.** { *; }
-keep class androidx.media3.** { *; }
-dontwarn com.google.android.exoplayer2.**
-dontwarn androidx.media3.**

# Preserve DataStore classes
-keep class androidx.datastore.** { *; }
-keep class * extends androidx.datastore.core.Serializer { *; }

# Preserve Kotlin serialization
-keepattributes *Annotation*
-dontwarn kotlinx.serialization.**
-keep,includedescriptorclasses class com.lsj.mp7.**$$serializer { *; }
-keepclassmembers class com.lsj.mp7.** {
    *** Companion;
}
-keepclasseswithmembers class com.lsj.mp7.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Preserve video player data classes
-keep class com.lsj.mp7.data.** { *; }
-keep class com.lsj.mp7.util.** { *; }

# MediaMetadataRetriever
-keep class android.media.MediaMetadataRetriever { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimize and obfuscate
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-repackageclasses ''