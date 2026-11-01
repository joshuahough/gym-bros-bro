# kotlinx.serialization: keep generated serializers for @Serializable classes (seed data, nav routes).
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.gymbrosbro.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
