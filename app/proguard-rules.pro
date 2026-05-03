-keep class com.cryptolens.app.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class kotlinx.serialization.** { *; }
-keepnames class io.github.jan.supabase.** { *; }
