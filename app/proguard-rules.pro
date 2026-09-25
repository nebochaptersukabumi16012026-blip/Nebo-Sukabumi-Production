# ============================================================================
# ProGuard & R8 Configuration for Production Security & Anti-Reverse Engineering
# ============================================================================

# 1. Logcat Sanitization (Hapus Log di build release untuk cegah leak credential & token)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 2. General Obfuscation & Optimization
-repackageclasses ''
-allowaccessmodification
-dontusemixedcaseclassnames
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# 3. Retrofit & OkHttp Security Rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# 4. Moshi & Serialization Models Preservation
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @com.squareup.moshi.JsonClass *;
}

# 5. Application Data & Network Models
-keep class com.example.data.** { *; }
-keep class com.example.network.** { *; }

# 6. Room Database Rules
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase

# 7. Security Crypto
-keep class androidx.security.crypto.** { *; }
