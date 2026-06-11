# Gson keeping rules
-keepattributes *Annotation*
-keepattributes Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room keeping rules
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomDatabase { *; }

# General Gemini keep rules
-keep class com.google.ai.client.generativeai.** { *; }
