# Strip unused Compose icons
-dontwarn org.jetbrains.annotations.**

# Keep everything we use (compose + wear)
-keep class androidx.compose.** { *; }
-keep class androidx.wear.** { *; }

# Our app classes
-keep class rocks.claudiusthebot.watertracker.** { *; }

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class rocks.claudiusthebot.watertracker.**$$serializer { *; }
-keepclassmembers class rocks.claudiusthebot.watertracker.** {
    *** Companion;
}
-keepclasseswithmembers class rocks.claudiusthebot.watertracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coroutines debug agent
-dontwarn kotlinx.coroutines.debug.**
