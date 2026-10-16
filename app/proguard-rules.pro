# Proguard rules for CinePulse

# Keep data models used by Moshi and Retrofit
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Keep Room database entities, DAOs, and database
-keep class com.example.data.local.** { *; }
-keepclassmembers class com.example.data.local.** { *; }

# Keep Retrofit API interfaces
-keep interface com.example.data.remote.** { *; }

# Moshi rules
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes Signature
-keepattributes *Annotation*
