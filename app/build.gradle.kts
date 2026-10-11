import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.gov.employeedb"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gov.employeedb"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            storeFile = keystoreProps.getProperty("RELEASE_STORE_FILE")?.let { file(it) }
            storePassword = keystoreProps.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = keystoreProps.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = keystoreProps.getProperty("RELEASE_KEY_PASSWORD")
            enableV1Signing = false
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // Single source of truth for locale restriction (APK size).
    androidResources { localeFilters += listOf("en", "fr", "ar") }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/AL2.0",
                "/META-INF/LGPL2.1",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/NOTICE",
                "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            )
            pickFirsts += setOf("/META-INF/services/javax.xml.stream.*")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true

            all { test ->
                test.maxHeapSize = "1g"
            }
        }
    }

    lint {
        abortOnError = true
        checkDependencies = true
        warningsAsErrors = false
    }
}

kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

// Hilt 2.58/2.59 — the newest Hilt that still supports AGP 8.x (2.59.1+ hard-require
// AGP 9, see PLAN.md Appendix A A10/A12) — pins kotlin-metadata-jvm 2.2.20 transitively
// via dagger-compiler; its max readable metadata version is 2.3.0 (MetadataVersion
// INSTANCE [2,2,0]). Kotlin 2.4.21 (A9) emits metadata 2.4.0, so Dagger fails on the
// :app:hiltJavaCompile<Variant> task ("Provided Metadata instance has version 2.4.0,
// while maximum supported version is 2.3.0"). Force the metadata reader to the Kotlin
// version on every configuration: kotlin-metadata-jvm lives only on the Hilt/KSP
// processor classpaths (Hilt's own `hiltCompileOnly`, plus `ksp*`) and is never
// packaged into the APK. kotlin-metadata-jvm 2.4.21 (INSTANCE [2,4,0]) reads 2.4.0.
configurations.configureEach {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlin.get()}")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.poi.android)
    implementation(libs.androidx.documentfile)
    implementation(libs.vico.compose.m3)

    testImplementation(libs.bundles.test)
    testImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.androidTest)
    androidTestImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.test.manifest)
}

// POI on the JVM (unit tests) needs the same StAX factory overrides as the app.
tasks.withType<Test>().configureEach {
    maxHeapSize = "1g"
    systemProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")
    systemProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl")
    systemProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl")
}