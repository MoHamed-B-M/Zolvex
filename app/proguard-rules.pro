# =============================================================================
# Zolvex - R8 rules
# Release: isMinifyEnabled = true, isShrinkResources = true
# =============================================================================

# --- Keep useful crash-report metadata ---------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*,AnnotationDefault

# =============================================================================
# Apache POI (poi-android 3.17) + XMLBeans + StAX (Aalto)
# POI reflects heavily on XMLBeans generated classes; without these rules the
# app crashes at runtime with ClassNotFoundException on the TypeSystemHolder.
# =============================================================================
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class com.fasterxml.aalto.** { *; }
-keep class org.apache.commons.** { *; }
-keep class javax.xml.stream.** { *; }
-keep class org.w3c.dom.** { *; }
-keep class org.xml.sax.** { *; }

# XMLBeans loads the generated TypeSystemHolder by name from this package.
-keep class schemaorg_apache_xmlbeans.system.** { *; }
-keep class org.apache.xmlbeans.impl.schema.** { *; }
-keep class org.apache.xmlbeans.impl.store.** { *; }

# Keep the StAX service registrations POI depends on.
-keep class * implements javax.xml.stream.XMLInputFactory { <init>(...); }
-keep class * implements javax.xml.stream.XMLOutputFactory { <init>(...); }
-keep class * implements javax.xml.stream.XMLEventFactory { <init>(...); }
-keepnames class com.fasterxml.aalto.stax.** { *; }

# POI touches these JDK/desktop APIs that do not exist on Android.
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn com.microsoft.schemas.**
-dontwarn org.openxmlformats.**
-dontwarn com.fasterxml.aalto.**
-dontwarn org.apache.commons.**
-dontwarn javax.xml.**
-dontwarn javax.imageio.**
-dontwarn javax.naming.**
-dontwarn javax.annotation.**
-dontwarn java.applet.**
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn java.lang.management.**
-dontwarn sun.misc.**
-dontwarn org.w3c.dom.**
-dontwarn org.xml.sax.**
-dontwarn org.ietf.jgss.**
-dontwarn stax.**
-dontwarn com.graphbuilder.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.slf4j.**

# POI's OOXML digital-signature code (org.apache.poi.poifs.crypt.dsig) references XAdES
# XMLBeans types under the ETSI namespace that are not bundled in poi-android 3.17 and
# are unused by this app (we never sign documents). Quiet R8's missing-class check so
# minifyReleaseWithR8 / hiltJavaCompileRelease succeed — R8 otherwise reports
# org.etsi.uri.x01903.v13.SignaturePolicyIdType et al. as missing and fails the build.
-dontwarn org.etsi.**

# =============================================================================
# Room (Room ships consumer rules; these are belt-and-braces for generated code)
# =============================================================================
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomDatabase$Callback { *; }
-keep class androidx.room.util.** { *; }
-dontwarn androidx.room.paging.**

# =============================================================================
# Hilt / Dagger (Hilt ships consumer rules; keep the wiring entry points)
# =============================================================================
-keep class dagger.hilt.android.internal.** { *; }
-keep,allowobfuscation @interface dagger.hilt.android.lifecycle.HiltViewModel
-keep,allowobfuscation @interface dagger.hilt.android.AndroidEntryPoint
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep class javax.inject.** { *; }
-keepattributes RuntimeVisibleAnnotations
-dontwarn dagger.hilt.**
-dontwarn dagger.internal.codegen.**
-dontwarn javax.inject.**

# =============================================================================
# kotlinx.serialization (type-safe navigation routes)
# =============================================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.gov.employeedb.**$$serializer { *; }
-keepclassmembers class com.gov.employeedb.** { *** Companion; }
-keepclasseswithmembers class com.gov.employeedb.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.gov.employeedb.presentation.navigation.Screen { *; }
-keep class com.gov.employeedb.presentation.navigation.Screen$* { *; }

# =============================================================================
# Kotlin / coroutines
# =============================================================================
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
-dontnote kotlin.coroutines.**
-keep class kotlin.Metadata { *; }

# =============================================================================
# Compose (tooling/preview never ship; nothing extra needed for runtime)
# =============================================================================
-dontwarn androidx.compose.**

# =============================================================================
# DataStore
# =============================================================================
-keep class androidx.datastore.preferences.protobuf.** { *; }
-dontwarn androidx.datastore.**