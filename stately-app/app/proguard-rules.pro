# Why: kotlinx.serialization generates a companion `serializer()` per @Serializable class and
# looks it up reflectively. Without these keeps, R8 strips them and every parse fails at runtime
# in release builds only -- exactly the class of defect spec §1.2 forbids shipping.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class dev.cazh0.stately.** {
    *** Companion;
}
-keepclasseswithmembers class dev.cazh0.stately.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.cazh0.stately.**$$serializer { *; }

# OkHttp ships its own consumer rules; these silence known-safe reflective references.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
