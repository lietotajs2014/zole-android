# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class lv.zole.**$$serializer { *; }
-keepclassmembers class lv.zole.** { *** Companion; }
-keepclasseswithmembers class lv.zole.** { kotlinx.serialization.KSerializer serializer(...); }
