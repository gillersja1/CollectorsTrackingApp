# Room Database entities and DAOs
-keep class com.example.collectorstrackingapp.model.** { *; }
-keep class com.example.collectorstrackingapp.data.** { *; }

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# JSON / Gson
-keepclassmembers class * {
    @org.json.JSONObject *;
}
