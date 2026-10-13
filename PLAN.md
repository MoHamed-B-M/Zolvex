# PLAN.md — Zolvex (Government Employee Database)

> Executor: OpenCode / Cursor / Claude Code. Read this file top to bottom before writing code. Tick every `- [ ]`. Report the build result after each phase. Never start a phase while the previous phase's build is red.
>
> All commands below are written as `./gradlew <task>`. On Windows/pwsh use `.\gradlew.bat <task>`.
> Display name: **Zolvex** (one `app_name` resource → rename in one place). Technical identifiers stay: package `com.gov.employeedb`, module `:app`, root project `Zolvex`.
> Deviations from `prompt.json` are explicit in **Appendix A**. Nothing in `prompt.json` is silently overridden.

**Progress**

- [x] Phase 1 — Scaffold
- [x] Phase 2 — Design system + i18n/RTL
- [x] Phase 3 — Navigation shell
- [ ] Phase 4 — Domain + Data
- [ ] Phase 5 — Excel engine
- [x] Phase 6 — Employee list UI
- [ ] Phase 7 — Import wizard UI
- [ ] Phase 8 — Detail / Edit / Create UI
- [ ] Phase 9 — Dashboard UI
- [ ] Phase 10 — Analytics UI
- [ ] Phase 11 — Settings UI
- [ ] Phase 12 — Motion, polish, accessibility
- [ ] Phase 13 — Testing & performance hardening
- [ ] Phase 14 — Release

---

## 1. Architecture Decision Record (ADR)

### ADR-1 — Language & runtime: Kotlin + Jetpack Compose (native Android)

| Criterion | **Kotlin + Compose (chosen)** | Flutter | Kotlin Multiplatform |
|---|---|---|---|
| SAF + `DocumentFile` + `CreateDocument` | First-class, no plugin | Needs `file_picker`/`flutter_file_dialog`, partial SAF | First-class |
| `.xlsx` parsing of 10k+ rows | Apache POI + SAX streaming, real `OPCPackage` | Dart xlsx parsers load the whole file in memory → OOM at 10k+ | POI via expect/actual, extra glue |
| Per-app locale + RTL (API 26→35) | `locales_config.xml`, `Configuration`, `LocalLayoutDirection` | Supported via ARB + own locator | Same as Kotlin |
| Size floor (15 MB budget) | ~2 MB empty app | +≈8–10 MB engine per ABI | ~2 MB + toolchain |
| M3 motion APIs | Same release cadence as Android | Material 3 lags | Compose MP lags (no `motionScheme` parity) |
| Tooling cost (Android-only) | 1 toolchain (Gradle/AGP) | 2 toolchains (Gradle + Dart/Pub) | 2 build systems for zero reuse benefit |

**Decision: Kotlin + Jetpack Compose.** Android-only target, hard < 15 MB budget, SAF + POI integration, and per-app locale/RTL are native concerns; Flutter's +8–10 MB engine and in-memory xlsx parsers violate two constraints, and KMP buys nothing without a second platform.

### ADR-2 — UI: Jetpack Compose + Material 3 **Expressive** (BOM 2026.06.01 → Compose 1.11.4, Material3 **1.5.0-alpha18** pinned outside the BOM, expressive channel)
Full Material 3 Expressive as the design language (user decision, 2026-10-11): `MaterialExpressiveTheme`, `MotionScheme.expressive()` (spring/bouncy motion), shape-morphing buttons, wavy progress indicators. Material3 **1.5.0-alpha18** is pinned explicitly **outside** the BOM (which still ships 1.4.0): in 1.4.0 the Expressive surface (`MaterialExpressiveTheme`, the expressive `Shapes` constructor, the `…Emphasized` type styles) is declared `internal`, so 1.4.0 cannot deliver Full Expressive *at all*; 1.5.0-alpha18 is the newest release that exposes it publicly while still declaring `aar-metadata` minCompileSdk 35 / minAGP 8.6.0 (alpha19+ jump to compileSdk 37 / AGP 9.1, contradicting A9). Its expressive markers are `@Material3ExpressiveApi` — a `@RestrictTo` annotation, **not** a `@RequiresOptIn` marker — so no `@OptIn` is needed and lint's public `RestrictedApi` check stays clean (see A13). This required a coordinated toolchain bump (Kotlin 2.4.21 / KSP 2.3.12 / AGP 8.13.2 / Gradle 8.14 / compileSdk 36 — see Appendix A, A9). M3 provides `MaterialTheme.motionScheme` (expressive motion tokens), `SwipeToDismissBox`, M3 `DatePickerDialog`, `PullToRefreshBox`; Compose 1.11.4 provides `SharedTransitionLayout` and `Modifier.animateItem()`. No XML layouts except the launch theme. BOM pinned at **2026.06.01** (not 2026.09.00): Compose **1.12.x** AARs require compileSdk 37 + AGP 9.1.0, which contradicts the AGP 8.13.2 / compileSdk 36 choice in A9 — 2026.06.01 is the newest BOM whose Compose core (1.11.4) still runs on AGP 8.6+ / compileSdk 35+, and it still ships Material3 1.4.0, which is exactly why material3 itself is pinned separately for the Expressive requirement (see Appendix A, A11 and A13).

### ADR-3 — DI: Hilt 2.58 (KSP)
First-party AndroidX integration (`hilt-navigation-compose` → `hiltViewModel()` in the NavHost), compile-time graph validation, KSP (not KAPT) keeps builds fast and R8 friendly. Bumped 2.52 → 2.58 (2026-10-11): KSP 2.3.x (required by Kotlin 2.4.21, see A9) removed `com.google.devtools.ksp.gradle.KspTaskJvm`, which Hilt 2.52's AGP plugin `Class.forName`s — phase-1 CI failed with `ClassNotFoundException`. Hilt 2.54+ targets `KspAATask`; **2.58 is the newest Hilt that still supports AGP 8.x** (2.59+ require AGP 9.0, which contradicts A9's AGP 8.13.2 choice) — see Appendix A, A10.

### ADR-4 — Database: Room 2.8.5 (KSP2, `Flow` DAOs, schema export)
Native `Flow` invalidation, `@Transaction` batch inserts, `LocalDate ↔ epochDay` converters, Robolectric-testable on the JVM, `exportSchema = true` for the migration tests. Bumped 2.6.1 → **2.8.5** (2026-10-12, user-approved, see A16): Room 2.6.1 predates KSP2 and the A9 toolchain (Kotlin 2.4.21 / KSP 2.3.12) is KSP2-only — Room codegen cannot compile under it; AndroidX added KSP2 support in 2.7.0, and 2.8.5 (Sep 2026) is the current stable, tested against compileSdk 36 / Kotlin 2.0+. No Paging 3: explicit `LIMIT/OFFSET` windowing plus SQL projections is enough for 10k rows and removes a dependency. No `fallbackToDestructiveMigration`: personal government data must survive schema changes (§5.6).

### ADR-5 — Excel: Apache POI Android port `com.github.SUPERCILEX.poi-android:poi:3.17` (as specified), streaming mode only
`OPCPackage` + `XSSFReader` + `XSSFSheetXMLHandler` (SAX) never build the sheet DOM; `ReadOnlySharedStringsTable` bounds memory; `SXSSFWorkbook` for writes. POI is the only library that reads real-world government xlsx (merged title rows, formula cells, styled dates) with no server. Size risk is real → **Appendix B** defines the trigger and the pre-approved fallbacks.

### ADR-6 — Charts: Vico 2.0.0-alpha.2 (`compose-m3`) for Cartesian charts + a hand-rolled Canvas donut
Vico is Compose-native and themes from M3. Vico 2's pie/donut API is new alpha surface; a ~45-line `DonutChart` (`Canvas` + `drawArc` + animated sweep) gives exact hole size, M3 palette, RTL-safe geometry and deterministic animation with zero alpha-API risk. One abstraction, two renderers (donut and pie via `innerRadiusRatio = 0f`).

### ADR-7 — Pattern: Clean-lite (domain / data / presentation) with MVVM + MVI-style unidirectional state
Per screen: immutable `UiState` + `StateFlow` + `onEvent(Event)` + one-shot `Channel<Effect>`. The domain layer is pure Kotlin (testable without Android); repositories expose `Flow`/`suspend`; ViewModels hold no `Context`. Single Gradle module `:app` with strict package boundaries — faster incremental builds, no ABI cost against the 15 MB budget, and no module-wiring rabbit holes for the agent.

### ADR-8 — Async/state: Coroutines 1.9.0 + Flow with injected dispatchers
`@IoDispatcher`, `@DefaultDispatcher`, `@ApplicationScope` qualifiers. No hardcoded `Dispatchers.IO` anywhere → every use case is deterministically testable with `StandardTestDispatcher`.

### ADR-9 — Runtime language switch: Compose-level locale provider, no `appcompat`
`AppTheme` provides `LocalContext` (locale-wrapped), `LocalConfiguration` and `LocalLayoutDirection` from a `StateFlow<AppLanguage>`. Instant switch, no Activity recreation, no flash, no extra dependency. `appcompat`'s `setApplicationLocales()` recreates the Activity (visible flash) and adds a dependency, so it is rejected. On API 33+ we *read* `LocaleManager.applicationLocales` at startup and mirror it into DataStore so the system "App language" screen stays authoritative.

### ADR-10 — Dependency addendum (pre-approved, all versioned)
Required by the spec but not listed in `prompt.json`. Rule 5 still requires flagging anything **else** before adding it.

| Dependency | Why |
|---|---|
| `org.jetbrains.kotlin.plugin.serialization:2.4.21` + `kotlinx-serialization-json:1.7.3` | `@Serializable` type-safe nav routes (Phase 3); `prompt.json`'s ProGuard section already anticipates it |
| `androidx.core:core-splashscreen:1.0.1` | Phase 12 splash; explicitly mentioned in `prompt.json` |
| `androidx.room:room-testing:2.8.5`, `org.robolectric:robolectric:4.14.1`, `androidx.test:core:1.6.1`, `androidx.test.ext:junit:1.2.1`, `androidx.test:runner:1.6.2`, `junit:junit:4.13.2` | Room/DataStore tests must run in `testDebugUnitTest` (JVM), not on an emulator |
| `app.cash.turbine:turbine:1.2.0`, `io.mockk:mockk:1.13.13`, `com.google.truth:truth:1.4.4`, `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0` | ViewModel / use-case tests |
| `androidx.compose.ui:ui-test-junit4`, `ui-test-manifest` (from BOM) | Compose UI + RTL tests (Phase 13) |

Explicitly **not** added: WorkManager (a 10k-row import completes in seconds; chunked Room transactions already make partial progress durable), Paging 3, appcompat, Hilt-Work, Timber, Material Components XML.

### ADR-11 — No `java.time` desugaring
`minSdk 26` ⇒ `java.time`, `java.nio.file`, `Period` are native. Saves ~200 KB and one plugin.

### ADR-12 — No `allowBackup`
`android:allowBackup="false"`: the database holds personal data (names, DOB, salary, phone, address). Cloud backup of that dataset is out of scope for a government tool; this also avoids Room restore/version mismatches.

---

## 2. Tech Stack & Exact Versions

| Layer | Choice | Version |
|---|---|---|
| Language | Kotlin | 2.4.21 (Compose compiler plugin ships with Kotlin) |
| Build | AGP / Gradle | 8.13.2 / 8.14 |
| JDK toolchain | Java | 17 target (JDK 21 installed; `jvmTarget = JVM_17`) |
| SDK | min / target / compile | 26 / 35 / 36 |
| UI | Compose BOM + Material3 | BOM 2026.06.01 (Compose 1.11.4) + material3 **1.5.0-alpha18**, pinned outside the BOM (A13) |
| Nav | navigation-compose (type-safe) | 2.8.5 |
| DI | Hilt (KSP) | 2.58 |
| DB | Room (KSP2, schema export) | 2.8.5 |
| Prefs | datastore-preferences | 1.1.1 |
| Excel | poi-android (JitPack) | 3.17 |
| Charts | Vico compose-m3 | 2.0.0-alpha.2 |
| SAF | documentfile | 1.0.1 |
| Serialization | kotlinx-serialization-json | 1.7.3 |
| Splash | core-splashscreen | 1.0.1 |
| KSP | com.google.devtools.ksp | 2.3.12 |
| Test | JUnit4 4.13.2, Robolectric 4.14.1, Turbine 1.2.0, MockK 1.13.13, Truth 1.4.4 |

### `gradle/wrapper/gradle-wrapper.properties`

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### `gradle/libs.versions.toml` (full)

```toml
[versions]
agp = "8.13.2"
kotlin = "2.4.21"
ksp = "2.3.12"
hilt = "2.58"
hiltNavigationCompose = "1.2.0"
coreKtx = "1.15.0"
activityCompose = "1.9.3"
composeBom = "2026.06.01"
# Pinned explicitly: the BOM's material3 1.4.0 keeps the Expressive API internal (A13).
material3 = "1.5.0-alpha18"
navigation = "2.8.5"
lifecycle = "2.8.7"
room = "2.6.1"
coroutines = "1.9.0"
datastore = "1.1.1"
documentfile = "1.0.1"
poiAndroid = "3.17"
vico = "2.0.0-alpha.2"
kotlinxSerialization = "1.7.3"
splashscreen = "1.0.1"
junit4 = "4.13.2"
androidxTestCore = "1.6.1"
androidxTestJunit = "1.2.1"
androidxTestRunner = "1.6.2"
espresso = "3.6.1"
robolectric = "4.14.1"
turbine = "1.2.0"
mockk = "1.13.13"
truth = "1.4.4"

[libraries]
# --- AndroidX core / lifecycle ---
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-core-splashscreen = { module = "androidx.core:core-splashscreen", version.ref = "splashscreen" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-process = { module = "androidx.lifecycle:lifecycle-process", version.ref = "lifecycle" }

# --- Compose (versions resolved by the BOM) ---
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-foundation = { module = "androidx.compose.foundation:foundation" }
compose-animation = { module = "androidx.compose.animation:animation" }
compose-material3 = { module = "androidx.compose.material3:material3", version.ref = "material3" }
compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }

# --- Navigation ---
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }

# --- DI ---
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# --- Room ---
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-room-testing = { module = "androidx.room:room-testing", version.ref = "room" }

# --- Coroutines / DataStore / Serialization ---
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }

# --- Excel / SAF / Charts ---
poi-android = { module = "com.github.SUPERCILEX.poi-android:poi", version.ref = "poiAndroid" }
androidx-documentfile = { module = "androidx.documentfile:documentfile", version.ref = "documentfile" }
vico-compose-m3 = { module = "com.patrykandpatrick.vico:compose-m3", version.ref = "vico" }

# --- Test ---
junit4 = { module = "junit:junit", version.ref = "junit4" }
androidx-test-core = { module = "androidx.test:core", version.ref = "androidxTestCore" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidxTestRunner" }
androidx-test-ext-junit = { module = "androidx.test.ext:junit", version.ref = "androidxTestJunit" }
androidx-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }

[bundles]
compose = ["compose-ui", "compose-ui-graphics", "compose-ui-tooling-preview", "compose-foundation", "compose-animation", "compose-material3", "compose-material-icons-extended"]
room = ["androidx-room-runtime", "androidx-room-ktx"]
test = ["junit4", "truth", "turbine", "mockk", "kotlinx-coroutines-test", "robolectric", "androidx-test-core", "androidx-test-ext-junit", "androidx-room-testing"]
androidTest = ["androidx-test-ext-junit", "androidx-test-runner", "androidx-espresso-core", "compose-ui-test-junit4"]
```

### `settings.gradle.kts` (full)

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.google\\.dagger.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") {
            content { includeGroup("com.github.SUPERCILEX.poi-android") }
        }
    }
}

rootProject.name = "Zolvex"
include(":app")
```

### Root `build.gradle.kts` (full)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

### `gradle.properties` (full)

```properties
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

android.useAndroidX=true
android.enableJetifier=false
android.nonTransitiveRClass=true
android.defaults.buildfeatures.buildconfig=false
android.nonFinalResIds=false

kotlin.code.style=official
ksp.incremental=true
```

### `app/build.gradle.kts` (full)

```kotlin
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
```

---

## 3. Folder Structure

```
Zolvex/
├── PLAN.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── local.properties                     # NOT committed (keystore creds)
├── .gitignore                           # build/, .gradle/, local.properties, *.jks, *.keystore, tools/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/{gradle-wrapper.jar,gradle-wrapper.properties}
├── tools/                               # bundletool.jar (Phase 14, git-ignored)
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    ├── schemas/                          # Room exported JSON (committed)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── assets/licenses/{OFL_Inter.txt,OFL_Cairo.txt,APACHE_2_0.txt}
        │   ├── java/com/gov/employeedb/
        │   │   ├── ZolvexApp.kt
        │   │   ├── MainActivity.kt
        │   │   ├── core/
        │   │   │   ├── AppLanguage.kt
        │   │   │   ├── AppLocale.kt              # LocaleManager, Context.withLocale()
        │   │   │   ├── AppThemeMode.kt
        │   │   │   ├── LocaleDigits.kt           # Arabic-Indic <-> ASCII digits
        │   │   │   ├── DateFormatters.kt
        │   │   │   ├── NumberFormatters.kt
        │   │   │   ├── AppResult.kt
        │   │   │   ├── AppError.kt
        │   │   │   └── FileNames.kt
        │   │   ├── di/
        │   │   │   ├── Qualifiers.kt
        │   │   │   ├── CoroutinesModule.kt
        │   │   │   ├── DatabaseModule.kt
        │   │   │   ├── RepositoryModule.kt
        │   │   │   └── ExcelModule.kt
        │   │   ├── domain/
        │   │   │   ├── model/
        │   │   │   │   ├── Employee.kt
        │   │   │   │   ├── EmployeeField.kt
        │   │   │   │   ├── EmployeeSummary.kt
        │   │   │   │   ├── AgeBucket.kt
        │   │   │   │   ├── ServiceBucket.kt
        │   │   │   │   ├── BucketCount.kt
        │   │   │   │   ├── WorkforcePolicy.kt
        │   │   │   │   ├── DashboardStats.kt
        │   │   │   │   └── AnalyticsSnapshot.kt
        │   │   │   ├── analytics/WorkforceAnalytics.kt      # pure functions
        │   │   │   ├── repository/EmployeeRepository.kt
        │   │   │   └── usecase/
        │   │   │       ├── ObserveEmployeesUseCase.kt
        │   │   │       ├── GetEmployeeByIdUseCase.kt
        │   │   │       ├── CreateEmployeeUseCase.kt
        │   │   │       ├── UpdateEmployeeUseCase.kt
        │   │   │       ├── DeleteEmployeeUseCase.kt
        │   │   │       ├── RestoreEmployeeUseCase.kt
        │   │   │       ├── DeleteAllEmployeesUseCase.kt
        │   │   │       ├── CountEmployeesUseCase.kt
        │   │   │       ├── ObserveDashboardStatsUseCase.kt
        │   │   │       ├── ObserveAnalyticsUseCase.kt
        │   │   │       ├── PreviewExcelUseCase.kt
        │   │   │       ├── ImportExcelUseCase.kt
        │   │   │       ├── ExportEmployeesUseCase.kt
        │   │   │       ├── ExportAnalyticsUseCase.kt
        │   │   │       ├── ExportErrorReportUseCase.kt
        │   │   │       └── SeedDebugDataUseCase.kt
        │   │   ├── data/
        │   │   │   ├── local/
        │   │   │   │   ├── AppDatabase.kt
        │   │   │   │   ├── Converters.kt
        │   │   │   │   ├── Migrations.kt
        │   │   │   │   ├── entity/EmployeeEntity.kt
        │   │   │   │   ├── projection/EmployeeListItemEntity.kt
        │   │   │   │   ├── projection/EmployeeStatRow.kt
        │   │   │   │   └── dao/EmployeeDao.kt
        │   │   │   ├── mapper/EmployeeMappers.kt
        │   │   │   ├── preferences/
        │   │   │   │   ├── PreferencesRepository.kt
        │   │   │   │   ├── SettingsKeys.kt
        │   │   │   │   └── MappingProfileStore.kt
        │   │   │   ├── excel/
        │   │   │   │   ├── ExcelMime.kt
        │   │   │   │   ├── SafDocumentHelper.kt
        │   │   │   │   ├── HeaderSynonyms.kt
        │   │   │   │   ├── HeaderNormalizer.kt
        │   │   │   │   ├── ColumnDetector.kt
        │   │   │   │   ├── DateParser.kt
        │   │   │   │   ├── ExcelPreview.kt
        │   │   │   │   ├── RowMapper.kt
        │   │   │   │   ├── ExcelReader.kt
        │   │   │   │   ├── ExcelWriter.kt
        │   │   │   │   ├── ImportProgress.kt
        │   │   │   │   ├── ImportResult.kt
        │   │   │   │   └── ImportRowError.kt
        │   │   │   └── repository/EmployeeRepositoryImpl.kt
        │   │   └── presentation/
        │   │       ├── main/{AppViewModel.kt,AppUiState.kt}
        │   │       ├── theme/{Color.kt,Type.kt,Shape.kt,Theme.kt,Motion.kt,Typefaces.kt,AppIcons.kt}
        │   │       ├── navigation/
        │   │       │   ├── Screen.kt
        │   │       │   ├── TopLevelDestination.kt
        │   │       │   ├── NavTransitions.kt
        │   │       │   ├── AppNavigation.kt
        │   │       │   └── components/AppBottomBar.kt
        │   │       ├── common/
        │   │       │   ├── BucketLabels.kt
        │   │       │   ├── preview/PreviewVariants.kt
        │   │       │   └── components/{AppTopBar.kt,ScreenScaffold.kt,StatCard.kt,DonutChart.kt,BarChartCard.kt,
        │   │       │       EmployeeCard.kt,EmployeeAvatar.kt,SearchBar.kt,FilterBar.kt,EmptyState.kt,
        │   │       │       ErrorState.kt,LoadingShimmer.kt,AnimatedCheckmark.kt,ConfirmDialog.kt,
        │   │       │       SectionCard.kt,LabeledValue.kt,CountUpText.kt,BucketLegend.kt}
        │   │       ├── dashboard/{DashboardScreen.kt,DashboardViewModel.kt,DashboardUiState.kt}
        │   │       ├── employees/
        │   │       │   ├── list/{EmployeeListScreen.kt,EmployeeListViewModel.kt,EmployeeListUiState.kt,EmployeeFilters.kt,EmployeeSort.kt}
        │   │       │   ├── detail/{EmployeeDetailScreen.kt,EmployeeDetailViewModel.kt,EmployeeDetailUiState.kt}
        │   │       │   └── edit/{EmployeeEditScreen.kt,EmployeeEditViewModel.kt,EmployeeEditUiState.kt,EmployeeValidator.kt}
        │   │       ├── importx/{ImportScreen.kt,ImportViewModel.kt,ImportUiState.kt,
        │   │       │       steps/{SelectFileStep.kt,MappingStep.kt,PreviewStep.kt,ImportingStep.kt,ResultStep.kt,PageDots.kt,MappingRow.kt}}
        │   │       ├── analytics/{AnalyticsScreen.kt,AnalyticsViewModel.kt,AnalyticsUiState.kt,
        │   │       │       tabs/{AgeTab.kt,ServiceTab.kt,DepartmentTab.kt,GradeTab.kt}}
        │   │       └── settings/{SettingsScreen.kt,SettingsViewModel.kt,SettingsUiState.kt}
        │   └── res/
        │       ├── font/{inter_variable.ttf,cairo_variable.ttf}
        │       ├── values/{strings.xml,colors.xml,themes.xml,dimens.xml}
        │       ├── values-b+fr/strings.xml
        │       ├── values-b+ar/strings.xml
        │       ├── values-night/{themes.xml,colors.xml}
        │       ├── values-ldrtl/dimens.xml
        │       ├── xml/{locales_config.xml}
        │       └── mipmap-anydpi-v26/{ic_launcher.xml,ic_launcher_round.xml}
        ├── test/java/com/gov/employeedb/
        │   ├── testutil/{MainDispatcherRule.kt,TestData.kt,XlsxFixtures.kt}
        │   ├── core/{LocaleDigitsTest.kt,DateFormattersTest.kt,LocaleManagerTest.kt}
        │   ├── domain/analytics/WorkforceAnalyticsTest.kt
        │   ├── data/local/EmployeeDaoTest.kt
        │   ├── data/excel/{HeaderNormalizerTest.kt,ColumnDetectorTest.kt,DateParserTest.kt,
        │   │       ExcelReaderTest.kt,ExcelWriterTest.kt,ImportExcelUseCaseTest.kt,
        │   │       LargeFileStreamingTest.kt}
        │   ├── presentation/{EmployeeListViewModelTest.kt,EmployeeEditViewModelTest.kt,
        │   │       ImportViewModelTest.kt,SettingsViewModelTest.kt,DashboardViewModelTest.kt,
        │   │       AnalyticsViewModelTest.kt,EmployeeValidatorTest.kt}
        │   └── i18n/StringResourceParityTest.kt
        ├── test/resources/excel/{en_sample.xlsx,fr_sample.xlsx,ar_sample.xlsx,malformed.xlsx,merged_headers.xlsx}
        └── androidTest/java/com/gov/employeedb/
            ├── NavigationSmokeTest.kt
            ├── EmployeeListScreenTest.kt
            ├── RtlLayoutTest.kt
            └── MigrationTest.kt
```

---

## 4. Phase Breakdown

**Global gate for every phase:** `./gradlew assembleDebug` must succeed before starting the next phase; commit with the listed message; tick the progress checkbox. Every phase leaves the app installable and runnable.

---

### Phase 1 — Scaffold

**Goal:** A buildable, installable Android app skeleton with Hilt, Compose, POI and R8 wiring in place.

- [ ] `settings.gradle.kts`, `build.gradle.kts` (root), `gradle.properties`, `gradle/libs.versions.toml` (exact content from §2).
- [ ] `gradle/wrapper/gradle-wrapper.properties` (from §2) + wrapper jar via `gradle wrapper --gradle-version 8.14`.
- [ ] `app/build.gradle.kts` (from §2). Keep the `signingConfigs` block guarded by `keystoreProps` null-checks so debug builds work without `local.properties` entries.
- [ ] `app/proguard-rules.pro` with the full content from §10.
- [ ] `.gitignore`: `build/`, `.gradle/`, `local.properties`, `*.jks`, `*.keystore`, `.idea/`, `tools/`.
- [ ] `git init` → `git add . && git commit -m "chore: initial scaffold"`.
- [ ] `app/src/main/AndroidManifest.xml`:
  - no permissions at all (SAF is permission-free; do **not** add `READ_EXTERNAL_STORAGE`).
  - `<application android:name=".ZolvexApp" android:allowBackup="false" android:supportsRtl="true" android:localeConfig="@xml/locales_config" android:label="@string/app_name" android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round" android:theme="@style/Theme.Zolvex">`
  - `<activity android:name=".MainActivity" android:exported="true" android:windowSoftInputMode="adjustResize" android:configChanges="locale|layoutDirection|orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden|uiMode|density" android:enableOnBackInvokedCallback="true">` + MAIN/LAUNCHER intent filter.
- [ ] `res/xml/locales_config.xml` → `<locale-config>` with `<locale android:name="en"/>`, `fr`, `ar`.
- [ ] `res/values/strings.xml` → only `app_name` = `Zolvex`.
- [ ] `res/values/themes.xml` → `Theme.Zolvex` parent `android:Theme.Material.Light.NoActionBar`, `android:windowBackground=@color/window_background`.
- [ ] `res/values-night/themes.xml` → same but parent `android:Theme.Material.NoActionBar`.
- [ ] `res/values/colors.xml` + `values-night/colors.xml` → `window_background` (`#FFFBFF` / `#121212`).
- [ ] `ZolvexApp.kt` → `@HiltAndroidApp class ZolvexApp : Application()`; in `onCreate()` set the three StAX properties **before any POI class loads**:
  - `org.apache.poi.javax.xml.stream.XMLInputFactory` → `com.fasterxml.aalto.stax.InputFactoryImpl`
  - `org.apache.poi.javax.xml.stream.XMLOutputFactory` → `com.fasterxml.aalto.stax.OutputFactoryImpl`
  - `org.apache.poi.javax.xml.stream.XMLEventFactory` → `com.fasterxml.aalto.stax.EventFactoryImpl`
- [ ] `MainActivity.kt` → `@AndroidEntryPoint`, `enableEdgeToEdge()`, `setContent { MaterialTheme { Surface { Text("Hello") } } }`.
- [ ] Placeholder adaptive icon `res/mipmap-anydpi-v26/{ic_launcher,ic_launcher_round}.xml` (+ `values/ic_launcher_background.xml`), finalized in Phase 12.

**Verify**

```bash
./gradlew assembleDebug
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -iE "poi|aalto|xmlbeans"
```
Expected: `BUILD SUCCESSFUL`; the grep lists `com.github.SUPERCILEX.poi-android:poi:3.17`, an xmlbeans artifact and an aalto artifact. If `com.fasterxml:aalto-xml` is **absent**, add `com.fasterxml:aalto-xml:1.2.2` to the catalog (pre-approved, Appendix B) and re-run. `./gradlew installDebug` then launches a screen showing `Hello` with no crash.

**Commit:** `feat(phase-1): scaffold Gradle, Hilt app, Compose activity, POI wiring`

---

### Phase 2 — Design System + i18n + RTL

**Goal:** Material 3 theme with Inter/Cairo typography, all three locales translated, an instant locale provider, and a parity gate that fails the build on any missing string.

- [ ] `res/font/inter_variable.ttf`, `res/font/cairo_variable.ttf` (OFL; license texts copied to `assets/licenses/`).
- [ ] `presentation/theme/Typefaces.kt` → `InterFamily` / `CairoFamily` built from one variable font each with `FontVariation.Settings(FontVariation.weight(w))` for 400/500/600/700.
- [ ] `presentation/theme/Type.kt` → `latinTypography` (Inter) and `arabicTypography` (Cairo, `lineHeight` ≈ ×1.15, `LineHeightStyle(alignment = Center, trim = None)`); `AppTypography.forLanguage(language)`.
- [ ] `presentation/theme/Color.kt` → full M3 light + dark schemes, seed `#1B5E5A`; `dynamicLightColorScheme` / `dynamicDarkColorScheme` on API 31+.
- [ ] `presentation/theme/Shape.kt` → M3 defaults + `extraLarge = 28.dp`.
- [ ] `presentation/theme/Motion.kt` → `AppMotion` with `spatial()`, `effects()`, `staggeredDelay(index)` delegating to `MaterialTheme.motionScheme` (`MotionScheme.expressive()`, bouncy spring defaults).
- [ ] `core/AppLanguage.kt` → `enum class AppLanguage(val tag: String, val isRtl: Boolean, @StringRes val nativeNameRes: Int) { EN, FR, AR }` + `fromTag(tag)`.
- [ ] `core/AppLocale.kt` → `Context.withLocale(language): Context` (`createConfigurationContext` with `setLocale`), and `LocaleManager` holding a `@Volatile` cached language for cold start + `syncFromSystemLocaleManager()` (API 33+).
- [ ] `core/AppThemeMode.kt` → `enum { LIGHT, DARK, SYSTEM }`.
- [ ] `data/preferences/SettingsKeys.kt` + `PreferencesRepository.kt` (DataStore Preferences: `language`, `theme_mode`, `last_import_at`, `mapping_profile`); `Flow<AppLanguage>`, `Flow<AppThemeMode>`, `suspend set*` functions.
- [ ] `presentation/main/AppViewModel.kt` + `AppUiState.kt` (`language`, `themeMode`, `isLoaded`) with `onEvent(SetLanguage/SetThemeMode)`.
- [ ] `presentation/theme/Theme.kt` → `AppTheme(uiState, content)`:

```kotlin
val ctx = LocalContext.current
val localized = remember(language) { ctx.withLocale(language) }
CompositionLocalProvider(
    LocalContext provides localized,
    LocalConfiguration provides localized.resources.configuration,
    LocalLayoutDirection provides if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
) {
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        motionScheme = motionScheme,
        content = content,
    )
}
```

(`@OptIn(ExperimentalMaterial3ExpressiveApi::class)`; `AppMotion.spatial()`/`effects()` delegate to `MaterialTheme.motionScheme` — expressive spring defaults come free.)

- [ ] `MainActivity` collects `AppViewModel.uiState`, wraps content in `AppTheme`, calls `installSplashScreen()`.
- [ ] `res/values/strings.xml`, `values-b+fr/strings.xml`, `values-b+ar/strings.xml` → **every key from §11**, fully translated (copy the table verbatim).
- [ ] `test/.../i18n/StringResourceParityTest.kt` → reads the three XML files from disk and asserts: identical key sets, no blank values, no value equal to its key, and every `%s`/`%1$d`-style placeholder present in all three.
- [ ] `@Preview` composables for typography/colors in **both** `LayoutDirection.Ltr` and `LayoutDirection.Rtl`, light and dark.

**Verify**

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest --tests "*StringResourceParityTest*"
```
Expected: `BUILD SUCCESSFUL` and a green parity test. Then set the device language to العربية and relaunch: all strings are Arabic and the layout is mirrored (RTL).

**Commit:** `feat(phase-2): M3 design system, Inter/Cairo typography, trilingual resources, locale provider`

---

### Phase 3 — Navigation Shell

**Goal:** Type-safe navigation with 4 top-level tabs, a bottom bar that hides on detail routes, and M3 motion transitions.

- [x] `presentation/navigation/Screen.kt` → `@Serializable sealed interface Screen`: `data object Dashboard`, `EmployeeList`, `Analytics`, `Settings`, `Import`, `data class EmployeeDetail(val id: Long)`, `data class EmployeeEdit(val id: Long? = null)`.
- [x] `presentation/navigation/TopLevelDestination.kt` → enum of the 4 tabs with `route`, `@StringRes labelRes`, `icon`, `selectedIcon`.
- [x] `presentation/navigation/NavTransitions.kt` → `enter/exit/popEnter/popExit` transitions from `MaterialTheme.motionScheme` spatial specs (fade + subtle scale), 220 ms.
- [x] `presentation/navigation/components/AppBottomBar.kt` → `NavigationBar` + `NavigationBarItem`s wrapped in `AnimatedVisibility(slideInVertically + fadeIn / slideOutVertically + fadeOut)`.
- [x] `presentation/navigation/AppNavigation.kt` → `SharedTransitionLayout { NavHost(...) }` with `startDestination = Screen.Dashboard`; `composable<Screen.X>` for all routes; `showBottomBar = currentDestination.isTopLevel`.
- [x] Real `Scaffold` placeholders: `dashboard/DashboardScreen.kt`, `employees/list/EmployeeListScreen.kt`, `analytics/AnalyticsScreen.kt`, `settings/SettingsScreen.kt`; stubs for `employees/detail`, `employees/edit`, `importx/ImportScreen.kt` (routed now, filled in later phases).
- [x] `presentation/common/components/AppTopBar.kt` (M3 `TopAppBar` with optional `Icons.AutoMirrored.Filled.ArrowBack`).
- [x] `MainActivity` renders `AppNavigation()`.

**Verify**

```bash
./gradlew assembleDebug && ./gradlew installDebug
```
Expected: app opens on Dashboard; the 4 tabs switch with no crash; navigating to `/employee/detail/1` (temporary debug button or `adb shell am start -a android.intent.action.VIEW -d "app://employee/detail/1"`) hides the bottom bar; device in Arabic mirrors the bar order.

**Commit:** `feat(phase-3): type-safe navigation, bottom bar, screen placeholders`

---

### Phase 4 — Domain + Data

**Goal:** Room database, mappers, repository, use cases and pure analytics — verified by JVM tests.

- [x] `domain/model/Employee.kt` (§5) with `ageAt(today)` / `serviceYearsAt(today)`.
- [x] `domain/model/WorkforcePolicy.kt` → `RETIREMENT_AGE = 60`, `RETIREMENT_SOON_YEARS = 5`.
- [x] `domain/model/EmployeeField.kt` → the 11-field enum + `isCore` + `REQUIRED_CORE = setOf(FULL_NAME, DATE_OF_BIRTH, HIRE_DATE)`.
- [x] `domain/model/{AgeBucket,ServiceBucket,BucketCount,EmployeeSummary,DashboardStats,AnalyticsSnapshot}.kt` (§5).
- [x] `domain/analytics/WorkforceAnalytics.kt` → pure `compute(rows: List<EmployeeStatRow>, today: LocalDate): AnalyticsSnapshot`.
- [x] `data/local/entity/EmployeeEntity.kt` (§5) with indices on `fullName`, `department`, `hire_date`, `birth_date`, `importBatchId` (+ `(fullNameKey, birth_date)`).
- [x] `data/local/Converters.kt` → `LocalDate ↔ Long` (epochDay), as members of `Converters` (A16).
- [x] `data/local/projection/{EmployeeListItemEntity,EmployeeStatRow}.kt`.
- [x] `data/local/dao/EmployeeDao.kt` (§5 query surface + Flow `observeById` for the detail screen).
- [x] `data/local/AppDatabase.kt` → `version = 1`, `exportSchema = true`, `.addMigrations(*Migrations.ALL)`; `Migrations.kt` with an empty-but-real `ALL` array.
- [x] `data/mapper/EmployeeMappers.kt`, `domain/repository/EmployeeRepository.kt`, `data/repository/EmployeeRepositoryImpl.kt`, `di/{Qualifiers,CoroutinesModule,DatabaseModule,RepositoryModule}.kt`.
- [x] Use cases: all non-Excel ones now; Excel use cases are added in Phase 5 so each phase compiles.
- [x] `test/.../testutil/{MainDispatcherRule.kt,TestData.kt}`.
- [x] `test/.../data/local/EmployeeDaoTest.kt` (Robolectric, in-memory Room): insert/update/delete/count, paging + ordering, filters, projections, dedupe `countMatching`.
- [x] `test/.../domain/analytics/WorkforceAnalyticsTest.kt`: boundaries 24/25/34/35/44/45/54/55 and 4/5/9/10/14/15/19/20; null DOB/hire counted in `total` but excluded from averages; retirement-soon = age ≥ 55; median with even and odd counts; 10k-row compute < 50 ms.

**Verify**

```bash
./gradlew testDebugUnitTest --tests "*EmployeeDaoTest*" --tests "*WorkforceAnalyticsTest*"
```
Expected: `BUILD SUCCESSFUL`, 0 failures. `app/schemas/com.gov.employeedb.data.local.AppDatabase/1.json` is generated by Room inside the CI workspace, but **not committed in this phase**: the file is only consumed by the Phase-12 migration tests, so it will be committed together with them (see A16; `room.schemaLocation` is already wired in `app/build.gradle.kts`).

**Commit:** `feat(phase-4): Room schema, repository, use cases, workforce analytics`

---

### Phase 5 — Excel Engine (read / detect / write)

**Goal:** Streaming xlsx reader, 3-language column detection, robust date parsing, streaming writer — all covered by fixture tests.

- [ ] Fixtures in `app/src/test/resources/excel/`: `en_sample.xlsx`, `fr_sample.xlsx`, `ar_sample.xlsx` (10 rows each; headers per §6.2), `merged_headers.xlsx` (title row + merged cells + 2 blank rows above the real header), `malformed.xlsx` (text in a date column, one row with no name, one salary with `€`). Spec in Appendix C.
- [ ] `data/excel/ExcelMime.kt` → `XLSX` mime, `OPEN_TYPES = arrayOf(XLSX, "application/octet-stream", "*/*")`.
- [ ] `data/excel/SafDocumentHelper.kt` → `openInput(context, uri)`, `createDocument(context, name)`, `displayName(context, uri)` (via `DocumentFile`/`OpenableColumns`).
- [ ] `data/excel/HeaderNormalizer.kt` → trim → NFD → strip `\p{Mn}` → strip Arabic tashkeel `\u064B-\u0652`, `\u0670`, tatweel `\u0640` → lowercase → remove punctuation/digits → collapse spaces → strip leading `ال` → trim.
- [ ] `data/excel/HeaderSynonyms.kt` → `Map<EmployeeField, List<String>>` with the exact EN/FR/AR terms from §6.2.
- [ ] `data/excel/ColumnDetector.kt` → `detect(headers, sampleRows): DetectionResult(mapping: Map<Int, EmployeeField>, confidence: Float, unmapped: List<Int>)` using the scoring, sniffing and greedy assignment rules of §6.2.
- [ ] `data/excel/DateParser.kt` → `parse(raw: String?, cell: Cell?): LocalDate?`; patterns `dd/MM/yyyy`, `d/M/yyyy`, `dd-MM-yyyy`, `yyyy-MM-dd`, `dd.MM.yyyy`, `MM/dd/yyyy` (only when day-first is impossible) + Excel serials via `DateUtil.isCellDateFormatted` / `cell.localDateTimeCellValue`; rejects future DOB and pre-1900 dates.
- [ ] `data/excel/ExcelPreview.kt` → `ExcelPreview(sheetName, headerRowIndex, headers, detectedMapping, sampleRows, totalRowEstimate)`.
- [ ] `data/excel/ExcelReader.kt` → `preview(uri): AppResult<ExcelPreview>` and `stream(uri, mapping, skipDuplicates): Flow<ImportProgress>` on `@IoDispatcher`, built on `OPCPackage.open` + `XSSFReader` + `ReadOnlySharedStringsTable` + `XSSFSheetXMLHandler` (SAX); `delay(1)` every 100 rows; everything closed in `finally`.
- [ ] `data/excel/RowMapper.kt` → header-index → `Employee`; trim, NFC-normalize, convert Arabic-Indic digits, validate `REQUIRED_CORE`, produce `ImportRowError`s.
- [ ] `data/excel/ImportProgress.kt` (§6.3), `ImportResult.kt`, `ImportRowError.kt`.
- [ ] `data/excel/ExcelWriter.kt` → `writeEmployees(out, employees, headersFor)` and `writeAnalytics(out, snapshot, labels)` using `SXSSFWorkbook(null, 100, false, false)`: bold header + freeze pane + autofilter, date `CellStyle`, blanks for nulls.
- [ ] Excel use cases: `PreviewExcelUseCase`, `ImportExcelUseCase` (Flow; 500-row `@Transaction` chunks; dedupe by `fullName.lowercase()+":"+birthEpochDay`), `ExportEmployeesUseCase`, `ExportAnalyticsUseCase`, `ExportErrorReportUseCase`.
- [ ] `data/preferences/MappingProfileStore.kt` → persist the confirmed mapping keyed by `sha256(headers.joinToString("|"))`.
- [ ] Tests: `HeaderNormalizerTest`, `ColumnDetectorTest` (all 3 fixtures detect the 3 core fields; merged-headers fixture finds header row 3), `DateParserTest`, `ExcelReaderTest` (preview/stream + `malformed.xlsx` yields exactly 3 errors), `ExcelWriterTest` (write → re-read → assert), `ImportExcelUseCaseTest` (dedupe is idempotent), `LargeFileStreamingTest` (generates 10,000 rows, asserts completion and heap delta < 64 MB).

**Verify**

```bash
./gradlew testDebugUnitTest --tests "*Excel*" --tests "*ColumnDetector*" --tests "*DateParser*" --tests "*HeaderNormalizer*" --tests "*LargeFileStreaming*"
```
Expected: `BUILD SUCCESSFUL`, 0 failures; `LargeFileStreamingTest` prints `peakDeltaMB < 64`; `ColumnDetectorTest` asserts FULL_NAME/DATE_OF_BIRTH/HIRE_DATE for en/fr/ar.

**Commit:** `feat(phase-5): streaming xlsx reader/writer, trilingual column detection, date parser`

---

### Phase 6 — Employee List UI

**Goal:** A real list backed by Room with search, filters, sort, swipe-delete + undo, an expanding FAB, and a seeded 10k dataset.

- [x] `presentation/employees/list/EmployeeFilters.kt` (query, `departments: Set<String>`, `ageBuckets: Set<AgeBucket>`, `serviceBuckets: Set<ServiceBucket>`) and `EmployeeSort.kt` (`NAME, AGE, HIRE_DATE, DEPARTMENT` × `ASC/DESC`).
- [x] `EmployeeListUiState.kt` → `items, total, isLoading, isLoadingMore, query, filters, sort, canLoadMore, error, isSeeding`.
- [x] `EmployeeListViewModel.kt` → windowed loading (`pageSize = 100`), `MutableStateFlow<String>` query with `debounce(200)`, `flatMapLatest` over filters/sort, page accumulation, and `onEvent(Search / ApplyFilter / SetSort / LoadMore / Delete / UndoDelete / SeedDebugData / AddClick / ImportClick)`; delete retains the `Employee` for undo within the 5 s Snackbar window.
- [x] `presentation/common/components/EmployeeAvatar.kt` → initials + deterministic color from `abs(fullName.hashCode())` over an 8-color M3 palette.
- [x] `EmployeeCard.kt` → avatar, name, position, department, `AssistChip`s for age and service years.
- [x] `SearchBar.kt` (custom `OutlinedTextField`, leading search icon, trailing clear, `TextAlign.Start`), `FilterBar.kt` (department `FilterChip`s in a `LazyRow` + filter bottom sheet), `EmptyState.kt`, `ErrorState.kt`, `LoadingShimmer.kt`, `CountUpText.kt`, `ConfirmDialog.kt` (deferred to Phase 8, where it first has a caller).
- [x] `EmployeeListScreen.kt` → `Scaffold`; top bar (sort menu; seed button only `if (BuildConfig.DEBUG)`); search pinned as a sticky header; filter chips; `LazyColumn(key = { it.id })` with `Modifier.animateItem()` and staggered entry for the first 8 items; `SwipeToDismissBox` for `EndToStart` **and** `StartToEnd`; Snackbar with Undo; expandable FAB (`scaleIn(transformOrigin = TransformOrigin(1f, 1f))`) revealing *Add employee* and *Import Excel*; empty state with an Import CTA.
- [x] `SeedDebugDataUseCase` → 10,000 deterministic employees (seed 42), departments from a fixed list, DOB 1958–2003, hire 1990–2024.
- [x] Wire `Screen.EmployeeList` to the real screen.

**Verify**

```bash
./gradlew assembleDebug && ./gradlew testDebugUnitTest --tests "*EmployeeListViewModelTest*"
```
Expected: tests green (debounce emits once, ordering per sort, undo restores). Manual: seeding logs `ZolvexPerf seed_ms=<8 s`; scrolling shows **< 1 % janky frames** via `adb shell dumpsys gfxinfo com.gov.employeedb.debug framestats`; typing filters after ~200 ms; swipe deletes and Undo restores; FAB expands smoothly.

**Commit:** `feat(phase-6): employee list with search, filters, sort, swipe-delete, expanding FAB`

---

### Phase 7 — Import Wizard UI

**Goal:** End-to-end SAF import — pick → map → preview → stream → result, with a downloadable error report.

- [ ] `ImportUiState.kt` → `step: ImportStep { SELECT, MAPPING, PREVIEW, IMPORTING, RESULT }`, `page`, `uri`, `fileName`, `preview`, `mapping: Map<Int, EmployeeField>`, `skipDuplicates`, `progress: ImportProgress?`, `result: ImportResult?`, `error: AppError?`.
- [ ] `ImportViewModel.kt` → `onEvent(FileSelected / MappingChanged / Next / Back / Import / CancelImport / DownloadErrorReport / ViewEmployees)`. `FileSelected` runs `PreviewExcelUseCase`, applies the stored mapping profile, jumps to `PREVIEW` when `confidence ≥ 0.8` and all core fields mapped, else `MAPPING`. `Import` collects `ImportExcelUseCase` into progress; `CancelImport` cancels the job; state lives in the ViewModel so it survives steps.
- [ ] `ImportScreen.kt` → `Scaffold` + `HorizontalPager(pageCount = 4, userScrollEnabled = step != IMPORTING)`, `animateScrollToPage(spring(...))`, animated page dots, top bar with RTL-mirrored back and a Cancel-import action while importing.
- [ ] `steps/SelectFileStep.kt` → dashed-border card (`Modifier.drawBehind` + `PathEffect.dashPathEffect`) + upload icon; `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())` with `ExcelMime.OPEN_TYPES`; shows the chosen file name; hints `.xlsx` only.
- [ ] `steps/MappingRow.kt` → raw header + first sample value on the left; `ExposedDropdownMenuBox` with the 11 fields + *Ignore* on the right; auto-detected rows highlighted.
- [ ] `steps/MappingStep.kt` → list of `MappingRow`s, required-fields banner, `skip_duplicates` `Switch`, `Next` enabled only when core fields are mapped **and** no field is used twice (duplicate ⇒ inline error + shake).
- [ ] `steps/PreviewStep.kt` → horizontally scrollable table of the first 10 rows × mapped columns, mapped columns tinted, row estimate, `Import` button.
- [ ] `steps/ImportingStep.kt` → circular progress (`animateFloatAsState(processed / total)`), percentage count-up, live imported-row count, Cancel.
- [ ] `steps/ResultStep.kt` → `AnimatedCheckmark` (Canvas + `PathMeasure.getSegment`, 600 ms) or error icon; counts of imported / duplicates skipped / failed; `View employees` → popBackStack to `Screen.EmployeeList`; `Download error report` only when `failed > 0`; retry on error.
- [ ] `presentation/common/components/AnimatedCheckmark.kt`, `LabeledValue.kt`, `presentation/common/BucketLabels.kt` (`AgeBucket`/`ServiceBucket` → `@StringRes`; keeps `R` out of `domain`).
- [ ] `test/.../presentation/ImportViewModelTest.kt` → happy path, low confidence ⇒ MAPPING, duplicate mapping blocked, cancel mid-import returns to PREVIEW, error ⇒ RESULT with failures.

**Verify**

```bash
./gradlew assembleDebug && ./gradlew testDebugUnitTest --tests "*ImportViewModelTest*"
```
Manual: `adb push sample.xlsx /sdcard/Download/`, tap FAB → Import Excel, pick it; the wizard auto-detects and lands on Preview. Force the mapping path once by renaming a header in a copy of `ar_sample.xlsx` and fixing it in Mapping. Import completes with progress animation; `malformed.xlsx` produces failures and a saved `.xlsx` error report openable from Files.

**Commit:** `feat(phase-7): Excel import wizard with mapping, streaming progress, error report`

---

### Phase 8 — Employee Detail / Edit / Create UI

**Goal:** Full CRUD with validation, delete confirmation, and a shared-element transition from the list avatar.

- [ ] `EmployeeDetailUiState.kt` / `EmployeeDetailViewModel.kt` → `Load(id)`, `Delete`, `OnDeleted` effect, `Employee?` from `GetEmployeeByIdUseCase`.
- [ ] `EmployeeDetailScreen.kt` → large avatar header, name, position; `SectionCard`s: Personal (DOB, age, email, phone, address), Professional (hire date, service years, department, grade, salary), Notes; top bar with RTL-mirrored back / edit / delete; delete `AlertDialog`; extended FAB → `Screen.EmployeeEdit(id)`.
- [ ] `EmployeeValidator.kt` (pure, unit-tested): name non-blank; DOB not future and not before 1900; hire date after DOB; hire date not future; salary ≥ 0; email light-regex when non-blank → `Map<Field, @StringRes ValidationError>`.
- [ ] `EmployeeEditUiState.kt` / `EmployeeEditViewModel.kt` → form state, per-field errors, derived `isValid`, `isSaving`, `Save` (create vs update by null `id`), `Saved` one-shot effect.
- [ ] `EmployeeEditScreen.kt` → `OutlinedTextField`s for all 12 fields with per-field `KeyboardOptions`; **phone/email/salary wrapped in `CompositionLocalProvider(LocalLayoutDirection provides Ltr)` + `textAlign = TextAlign.Start`**; M3 `DatePickerDialog` for DOB and hire date; salary filters non-digits and converts Arabic-Indic digits via `core/LocaleDigits.kt`; first invalid Save triggers a `keyframes` shake (`Animatable`, ±8 dp ×3, 300 ms); Save disabled until valid; success check animation then `popBackStack`.
- [ ] Shared elements: `Modifier.sharedElement(rememberSharedContentState("employee-avatar-$id"), animatedVisibilityScope)` on the list card avatar and the detail avatar; `Modifier.sharedBounds` for the name; spec `MaterialTheme.motionScheme.slowSpatialSpec()`.
- [ ] Routes: `composable<Screen.EmployeeDetail>` with `navArgument("id") { type = NavType.LongType }`; `composable<Screen.EmployeeEdit>` with nullable id (`-1` sentinel).
- [ ] `test/.../presentation/{EmployeeEditViewModelTest,EmployeeValidatorTest}.kt`.

**Verify**

```bash
./gradlew assembleDebug && ./gradlew testDebugUnitTest --tests "*EmployeeEditViewModelTest*" --tests "*EmployeeValidatorTest*"
```
Manual: create (all four validations rejected with the shake), edit, delete (dialog → list refreshes), `adb shell am force-stop com.gov.employeedb.debug` then relaunch → changes persisted; tapping a list card shows the avatar travelling from the card to the detail header in both LTR and RTL.

**Commit:** `feat(phase-8): employee detail/edit/create with validation and shared element transition`

---

### Phase 9 — Dashboard UI

**Goal:** Live workforce stats with count-up numbers and two animated charts.

- [ ] `DashboardUiState.kt` → `isLoading, total, averageAge, averageService, retirementSoon, ageBuckets, serviceBuckets, recent, error`.
- [ ] `DashboardViewModel.kt` → `ObserveDashboardStatsUseCase` (stat projection rows) → `WorkforceAnalytics.compute(rows, LocalDate.now())` on `@DefaultDispatcher`; logs `ZolvexPerf analytics_ms=…`; exposes Refresh.
- [ ] `presentation/common/components/StatCard.kt` → M3 card, icon, label, big number via `animateIntAsState(spring(DampingRatioMediumBouncy, StiffnessMediumLow))` (+ `animateFloatAsState` for decimals).
- [ ] `DonutChart.kt` → Canvas donut with animated sweep (`animateFloatAsState` + spring), hole radius ratio, M3 palette, center label, `onSliceClick`, **wrapped in `LocalLayoutDirection provides Ltr`** so geometry never mirrors.
- [ ] `BarChartCard.kt` → Vico `CartesianChartHost` + `rememberCartesianChart(rememberColumnCartesianLayer(), startAxis, bottomAxis)` with `CartesianChartModelProducer.runTransaction` on data change; forced LTR.
- [ ] `BucketLegend.kt` → dot + label + count + share %.
- [ ] `DashboardScreen.kt` → `LazyColumn` 16 dp padding: header, 2×2 `StatCard` grid (Total, Average Age, Average Service, Retirement Soon), age `DonutChart`, service `BarChartCard`, legends, "Recent 5" `EmployeeCard`s; staggered `AnimatedVisibility(tween(400, delayMillis = index * 60))`; `PullToRefreshBox`.
- [ ] Move the debug "Seed test data" action here as well.
- [ ] `test/.../presentation/DashboardViewModelTest.kt` with a fake stat source.

**Verify**

```bash
./gradlew assembleDebug && ./gradlew testDebugUnitTest --tests "*DashboardViewModelTest*"
```
Manual: with 10,000 seated rows the dashboard renders in < 400 ms and logs `analytics_ms < 100`; counts animate up; both charts draw in; rotation keeps state; `retirement_soon` matches the Phase 4 DAO test expectation.

**Commit:** `feat(phase-9): dashboard with count-up stats, donut and bar charts, recent employees`

---

### Phase 10 — Analytics UI

**Goal:** Four-tab analysis with department filtering and xlsx export of the analysis.

- [ ] `AnalyticsUiState.kt` → `isLoading, selectedDepartment: String?, snapshot: AnalyticsSnapshot?, error`.
- [ ] `AnalyticsViewModel.kt` → `ObserveAnalyticsUseCase(selectedDepartment)`, `onEvent(SelectDepartment / ExportAnalysis / Retry)`; export via `ExportAnalyticsUseCase` + SAF `CreateDocument`.
- [ ] `AnalyticsScreen.kt` → `PrimaryTabRow` (Age | Service | Department | Grade) + `AnimatedContent(slideInHorizontally + fadeIn togetherWith slideOutHorizontally + fadeOut, sizeTransform = null)` with `MaterialTheme.motionScheme.fastEffectsSpec()`; department filter chips; export action in the top bar.
- [ ] `tabs/AgeTab.kt` → `DonutChart` + `BucketLegend` + average + **median** + youngest/oldest chips.
- [ ] `tabs/ServiceTab.kt` → Vico column chart + average + median + "Top 5 longest serving".
- [ ] `tabs/DepartmentTab.kt` → Vico horizontal bar chart (column layer with `VerticalAxis` at bottom, `HorizontalAxis` at start — numeric bars stay LTR even in Arabic), top 10 + *Other*, tapping a bar sets the filter.
- [ ] `tabs/GradeTab.kt` → the same `DonutChart` with `innerRadiusRatio = 0f` (pie) + legend + counts.
- [ ] Empty state (`empty_state_analytics` + Import CTA) and shimmer loading.
- [ ] `test/.../presentation/AnalyticsViewModelTest.kt` (filter recompute; export writes a non-empty stream).

**Verify**

```bash
./gradlew assembleDebug && ./gradlew testDebugUnitTest --tests "*AnalyticsViewModelTest*"
```
Manual: each tab renders with 10k rows in < 300 ms; tab switching slides without flicker; department selection filters all four tabs; the exported file re-opens with `ExcelReader` (also asserted in `ExcelWriterTest`); Arabic UI still draws charts LTR.

**Commit:** `feat(phase-10): analytics with four animated tabs and analysis export`

---

### Phase 11 — Settings UI

**Goal:** Language, theme, export-all, replace-all import and destructive delete — all with instant effect.

- [ ] `SettingsUiState.kt` → `language, themeMode, employeeCount, isBusy, lastExportUri, error, dialog: SettingsDialog? { NONE, IMPORT_REPLACE, DELETE_ALL_1, DELETE_ALL_2 }`.
- [ ] `SettingsViewModel.kt` → `onEvent(SetLanguage / SetThemeMode / ExportAll / ImportReplaceConfirmed / DeleteAllRequested / DeleteAllConfirmed / ClearError)`; export/import use SAF launchers supplied by the screen; `Share`/`Message` effects via `Channel`.
- [ ] `SettingsScreen.kt` → sections:
  - **Language**: three `FilterChip`s (English / Français / العربية) with native-name strings; selection writes DataStore and applies instantly.
  - **Theme**: `SingleChoiceSegmentedButtonRow` Light / Dark / System.
  - **Data**: "Import (replaces all)" → `ConfirmDialog` (`import_replace_confirm`); "Export all employees" → `CreateDocument` + `ExportEmployeesUseCase`; "Delete all" → double confirm (`delete_all_confirm` → `delete_all_message`).
  - **About**: name, `version` from `BuildConfig.VERSION_NAME`, `licenses` → `assets/licenses` viewer (simple scrollable text screen), privacy note about `allowBackup=false`.
- [ ] Theme/language crossfade: wrap the app shell in `AnimatedContent(targetState = uiState.themeMode)` and, inside `AppTheme`, `AnimatedContent(targetState = language)` with `tween(220)`.
- [ ] `test/.../presentation/SettingsViewModelTest.kt` (language/theme writes, double-confirm gating, export path called once, delete-all clears count).

**Verify**

```bash
./gradlew assembleDebug && ./gradlew testDebugUnitTest --tests "*SettingsViewModelTest*"
```
Manual: switching to العربية flips the whole app to RTL instantly with a crossfade and no restart; Theme → Dark animates; Export all produces a valid `.xlsx`; Delete all requires two confirmations and empties the list.

**Commit:** `feat(phase-11): settings with runtime language, theme, import/export, delete-all`

---

### Phase 12 — Motion, Polish, Accessibility

**Goal:** Every screen's animation plan implemented, haptics, skeletons, adaptive icon, splash, a11y, and lint clean.

- [ ] Audit and implement every row of the §8 animation table; confirm each has a `MaterialTheme.motionScheme`-derived duration.
- [ ] Haptics: `HapticFeedback` on FAB expand, swipe-delete commit, save success, mapping duplicate error (`HapticFeedbackType.LongPress` / `TextHandleMove` as appropriate).
- [ ] Shimmer skeletons replace spinners on Dashboard, List, Analytics first load.
- [ ] Adaptive icon: `AnycubicIcon`-style foreground vector + `ic_launcher_background` color + monochrome layer for themed icons (API 33+).
- [ ] `core-splashscreen`: `Theme.Zolvex.Splash` with `windowSplashScreenBackground` + `windowSplashScreenAnimatedIcon`, `setKeepOnScreenCondition { !appUiState.isLoaded }`, `installSplashScreen()` before `setContent`.
- [ ] Content descriptions on every icon-only control using the `cd_*` keys; `Modifier.semantics { role = Role.Button }` where the role is not implied; touch targets ≥ 48 dp.
- [ ] Typography/dynamic-type pass: verify with font scale 1.3 without clipping (especially Arabic).
- [ ] Preview every component in light/dark × LTR/RTL.
- [ ] Delete all unused drawables; check `BuildConfig.DEBUG`-only UI is not referenced in release code paths.
- [ ] Run `./gradlew lint` and fix every warning that is an error or a genuine defect.

**Verify**

```bash
./gradlew assembleDebug && ./gradlew lint
```
Expected: `BUILD SUCCESSFUL`; **0 errors** from lint (warnings reviewed and either fixed or explicitly justified). Manual: install on a Pixel 6 (API 34), a small API 26 phone and an API 34 tablet; walk every screen; rotate; enable system font scale 1.3; toggle Arabic; enable "Remove animations" and confirm no crashes and instant transitions.

**Commit:** `feat(phase-12): motion pass, haptics, skeletons, adaptive icon, splash, a11y`

---

### Phase 13 — Testing & Performance Hardening

**Goal:** Full test pyramid green, performance budgets measured and enforced, no flaky tests.

- [ ] Complete the suite per §9 (every layer with the stated coverage).
- [ ] `androidTest/.../MigrationTest.kt` → `MigrationTestHelper` scaffold with assertions for the current schema (keeps future migrations honest).
- [ ] `androidTest/.../NavigationSmokeTest.kt` → all 4 tabs + detail/edit/import routes reachable, back stack correct.
- [ ] `androidTest/.../EmployeeListScreenTest.kt` → search filters rows, empty state appears for a nonsense query, swipe-delete shows Undo.
- [ ] `androidTest/.../RtlLayoutTest.kt` → render `EmployeeListScreen` under `LayoutDirection.Rtl` and assert `onNodeWithText(employee.name).assertPositionInRootIsEqualTo(...)`-style invariants: avatar is on the **right**, back arrow is mirrored, and Arabic strings are used.
- [ ] Performance gates (record results in the phase commit message):
  - cold start `adb shell am start -W -n com.gov.employeedb.debug/com.gov.employeedb.MainActivity` → `TotalTime < 1500 ms`;
  - import of a 10,000-row file → `< 20 s`, no OOM, peak Java heap < 128 MB (log `ZolvexPerf`);
  - dashboard analytics compute → `< 100 ms`; analytics tab compute → `< 200 ms`;
  - list scroll → `adb shell dumpsys gfxinfo com.gov.employeedb.debug framestats` → janky frames `< 1 %`;
  - export of 10,000 rows → `< 8 s`, peak heap < 128 MB.
- [ ] Fix any flakiness: no `Thread.sleep`, use `MainDispatcherRule` + `StandardTestDispatcher` + `advanceUntilIdle`, and Robolectric `@Config(sdk = [34])` pinned everywhere.
- [ ] Re-run with app data cleared and with 0 employees to prove empty states.

**Verify**

```bash
./gradlew testDebugUnitTest
./gradlew lint
./gradlew connectedDebugAndroidTest   # only after `./gradlew installDebug` on a running device/emulator
```
Expected: `testDebugUnitTest` 0 failures and 0 skipped; lint 0 errors; connected tests 0 failures; all five performance gates met and pasted into the commit message.

**Commit:** `test(phase-13): full test pyramid, instrumentation tests, performance gates`

---

### Phase 14 — Release

**Goal:** Signed, minified AAB under 15 MB that installs and works on API 26 and API 34.

- [ ] Create a keystore: `keytool -genkeypair -v -keystore zolvex-release.jks -alias zolvex -keyalg RSA -keysize 4096 -validity 10000` (store it outside the repo, e.g. `%USERPROFILE%\.android-keys\`).
- [ ] Add to `local.properties` (never committed): `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.
- [ ] Confirm `release { isMinifyEnabled = true; isShrinkResources = true }` and that `proguard-rules.pro` contains every rule from §10.
- [ ] Verify the POI keep rules actually took effect by launching an import on the **release** build (an over-aggressive shrink shows up as `ClassNotFoundException: com.fasterxml.aalto...` or a missing xmlbeans `TypeSystemHolder`).
- [ ] Build the bundle and measure it:

```bash
./gradlew clean
./gradlew bundleRelease
# size gate
$f = Get-Item app/build/outputs/bundle/release/app-release.aab
"{0:N2} MB" -f ((($f.Length) / 1MB))
```
Expected: `app-release.aab` exists and is **< 15.00 MB**.

- [ ] Secondary gate — real install size on a device, using bundletool (download `bundletool-all-1.17.2.jar` into `tools/`, git-ignored):

```bash
java -jar tools/bundletool-all-1.17.2.jar build-apks --bundle=app/build/outputs/bundle/release/app-release.aab --output=tools/zolvex.apks --mode=universal --ks=%USERPROFILE%\.android-keys\zolvex-release.jks --ks-key-alias=zolvex
java -jar tools/bundletool-all-1.17.2.jar install-apks --apks=tools/zolvex.apks
```
Expected: installs and launches; `flutter`-free, POI-free-of-crashes.

- [ ] Full smoke test on the **release** build: import en/fr/ar fixtures, CRUD, dashboard stats, all four analytics tabs, export employees + analysis, switch language to Arabic (RTL), delete all, restart and confirm persistence.
- [ ] Bump `versionCode`/`versionName` if this is not the first release; commit the final `PLAN.md` tick state.
- [ ] Tag: `git tag v1.0.0`.

**Verify**

```bash
./gradlew bundleRelease
./gradlew lint
```
Expected: `BUILD SUCCESSFUL`, AAB < 15 MB, lint 0 errors, and the 30-minute manual smoke test (DoD item 11) completes without a crash.

**Commit:** `chore(phase-14): release signing, R8 shrinking, v1.0.0 bundle`

---

## 5. Data Model

### 5.1 Domain model — `domain/model/Employee.kt`

```kotlin
data class Employee(
    val id: Long = 0L,
    val fullName: String,
    val dateOfBirth: LocalDate? = null,
    val hireDate: LocalDate? = null,
    val department: String? = null,
    val position: String? = null,
    val grade: String? = null,
    val salary: Double? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val importBatchId: String? = null,
    val sourceRow: Int? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    fun ageAt(today: LocalDate): Int? = dateOfBirth?.let { Period.between(it, today).years }

    fun serviceYearsAt(today: LocalDate): Int? =
        hireDate?.let { ChronoUnit.MONTHS.between(it, today).coerceAtLeast(0) / 12 }

    val age: Int? get() = ageAt(LocalDate.now())
    val yearsOfService: Int? get() = serviceYearsAt(LocalDate.now())

    val isRetirementSoon: Boolean
        get() = (age ?: return false) >=
            WorkforcePolicy.RETIREMENT_AGE - WorkforcePolicy.RETIREMENT_SOON_YEARS
}
```

`Clock`-free by design: everything derivable takes `today: LocalDate`, so tests are deterministic and `domain` stays pure.

### 5.2 `domain/model/EmployeeField.kt`

```kotlin
enum class EmployeeField {
    FULL_NAME, DATE_OF_BIRTH, HIRE_DATE, DEPARTMENT, POSITION,
    GRADE, SALARY, EMAIL, PHONE, ADDRESS, NOTES;

    val isCore: Boolean get() = this in REQUIRED_CORE

    companion object {
        val REQUIRED_CORE: Set<EmployeeField> = setOf(FULL_NAME, DATE_OF_BIRTH, HIRE_DATE)
    }
}
```

### 5.3 Control tables (single source of truth for labels, buckets, tests)

| Bucket | Interval | Policy |
|---|---|---|
| `AgeBucket.UNDER_25` | `[0, 25)` | `WorkforcePolicy.RETIREMENT_AGE = 60` |
| `AgeBucket.B25_34` | `[25, 35)` | `RETIREMENT_SOON_YEARS = 5` |
| `AgeBucket.B35_44` | `[35, 45)` | ⇒ retirement-soon ⇔ `age ≥ 55` |
| `AgeBucket.B45_54` | `[45, 55)` | rows with null DOB/hire count in `total` only |
| `AgeBucket.B55_PLUS` | `[55, ∞)` | `yearsOfService = floor(months / 12)` |
| `ServiceBucket.LESS_5` | `[0, 5)` | |
| `ServiceBucket.B5_10` | `[5, 10)` | |
| `ServiceBucket.B10_15` | `[10, 15)` | |
| `ServiceBucket.B15_20` | `[15, 20)` | |
| `ServiceBucket.B20_PLUS` | `[20, ∞)` | |

Half-open intervals prevent double counting at 24/25, 34/35, …; `WorkforceAnalyticsTest` asserts every boundary.

```kotlin
enum class AgeBucket(val minInclusive: Int, val maxExclusive: Int) {
    UNDER_25(0, 25), B25_34(25, 35), B35_44(35, 45), B45_54(45, 55), B55_PLUS(55, Int.MAX_VALUE);
    companion object { fun of(age: Int): AgeBucket = entries.first { age in it.minInclusive until it.maxExclusive } }
}
enum class ServiceBucket(val minInclusive: Int, val maxExclusive: Int) {
    LESS_5(0, 5), B5_10(5, 10), B10_15(10, 15), B15_20(15, 20), B20_PLUS(20, Int.MAX_VALUE);
    companion object { fun of(years: Int): ServiceBucket = entries.first { years in it.minInclusive until it.maxExclusive } }
}
```

### 5.4 Analysis models

```kotlin
data class BucketCount(val label: String, val count: Int, val share: Float) // share 0f..1f

data class EmployeeSummary(
    val id: Long,
    val fullName: String,
    val department: String?,
    val position: String?,
    val age: Int?,
    val serviceYears: Int?,
)

data class AnalyticsSnapshot(
    val total: Int,
    val averageAge: Double?,
    val medianAge: Double?,
    val averageService: Double?,
    val medianService: Double?,
    val retirementSoon: Int,
    val ageBuckets: List<BucketCount>,
    val serviceBuckets: List<BucketCount>,
    val departmentCounts: List<BucketCount>,  // sorted desc; "Other" appended when > 10
    val gradeCounts: List<BucketCount>,
    val longestServing: List<EmployeeSummary>, // top 5
)

data class DashboardStats(
    val snapshot: AnalyticsSnapshot,
    val recent: List<EmployeeSummary>,         // latest 5 by createdAt desc
)
```

`WorkforceAnalytics.compute(rows: List<EmployeeStatRow>, today: LocalDate): AnalyticsSnapshot` is a pure function: no Android, no I/O, unit-testable, and reused by both the dashboard and analytics screens. `BucketCount.label` holds the **raw** bucket/department/grade value; presentation maps `AgeBucket`/`ServiceBucket` to `@StringRes` via `BucketLabels.kt`, and department/grade strings pass through untouched.

### 5.5 Room entity — `data/local/entity/EmployeeEntity.kt`

```kotlin
@Entity(
    tableName = "employees",
    indices = [
        Index("fullName"), Index("department"),
        Index("hire_date"), Index("birth_date"), Index("importBatchId"),
        Index(value = ["fullNameKey", "birth_date"]),
    ],
)
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "fullName") val fullName: String,
    @ColumnInfo(name = "fullNameKey") val fullNameKey: String,
    @ColumnInfo(name = "birth_date") val dateOfBirth: LocalDate?,
    @ColumnInfo(name = "hire_date") val hireDate: LocalDate?,
    val department: String?,
    val position: String?,
    val grade: String?,
    val salary: Double?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val notes: String?,
    val importBatchId: String?,
    val sourceRow: Int?,
    val createdAt: Long,
    val updatedAt: Long,
)
```

`data/local/Converters.kt` (class members — Room only discovers `@TypeConverter`s on a class/object referenced via `@TypeConverters`, never top-level functions; Room 2.8.5's KSP2 processor cannot read the `@Database(typeConverters = …)` parameter at all — "No parameter with name 'typeConverters' found" — so the parameter is omitted and converters are declared on the entity, the DAO and both projections via `@TypeConverters(Converters::class)` — see A16; signatures are non-null per the 2.7+ TypeConverter analyzer recommendation, b/193437407):

```kotlin
class Converters {
    @TypeConverter fun toEpochDay(date: LocalDate): Long = date.toEpochDay()
    @TypeConverter fun fromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)
}
```

### 5.6 DAO & projections — `data/local/dao/EmployeeDao.kt`

```kotlin
@Query("""
    SELECT id, fullName, department, position, birth_date AS dateOfBirth, hire_date AS hireDate
    FROM employees
    WHERE (:q = '' OR fullName LIKE '%' || :q || '%'
                   OR position LIKE '%' || :q || '%'
                   OR department LIKE '%' || :q || '%')
      AND (:department = '' OR department = :department)
      AND (:birthFrom IS NULL OR birth_date >= :birthFrom)
      AND (:birthTo   IS NULL OR birth_date <  :birthTo)
      AND (:hireFrom  IS NULL OR hire_date  >= :hireFrom)
      AND (:hireTo    IS NULL OR hire_date  <  :hireTo)
    ORDER BY
      CASE WHEN :sortKey = 'NAME'       AND :ascending THEN fullName   END ASC,
      CASE WHEN :sortKey = 'NAME'       AND NOT :ascending THEN fullName END DESC,
      CASE WHEN :sortKey = 'AGE'        AND :ascending THEN birth_date END DESC,
      CASE WHEN :sortKey = 'AGE'        AND NOT :ascending THEN birth_date END ASC,
      CASE WHEN :sortKey = 'HIRE_DATE'  AND :ascending THEN hire_date  END ASC,
      CASE WHEN :sortKey = 'HIRE_DATE'  AND NOT :ascending THEN hire_date END DESC,
      CASE WHEN :sortKey = 'DEPARTMENT' AND :ascending THEN department END ASC,
      CASE WHEN :sortKey = 'DEPARTMENT' AND NOT :ascending THEN department END DESC,
      id ASC
    LIMIT :limit OFFSET :offset
""")
fun observePage(
    q: String, department: String,
    birthFrom: Long?, birthTo: Long?, hireFrom: Long?, hireTo: Long?,
    sortKey: String, ascending: Boolean, limit: Int, offset: Int,
): Flow<List<EmployeeListItemEntity>>
```

```kotlin
@Query("SELECT COUNT(*) FROM employees") fun observeCount(): Flow<Int>
@Query("SELECT COUNT(*) FROM employees WHERE fullNameKey = :key AND birth_date IS :epochDay")
suspend fun countMatching(key: String, epochDay: Long?): Int
@Query("SELECT id, fullName, position, birth_date AS birthDate, hire_date AS hireDate, department, grade FROM employees") fun observeStatRows(): Flow<List<EmployeeStatRow>>
@Query("SELECT id, fullName, department, position, birth_date, hire_date FROM employees ORDER BY createdAt DESC LIMIT :limit") fun observeRecent(limit: Int): Flow<List<EmployeeListItemEntity>>
@Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(entity: EmployeeEntity): Long
@Insert suspend fun insertAll(entities: List<EmployeeEntity>): List<Long>
@Update suspend fun update(entity: EmployeeEntity)
@Delete suspend fun delete(entity: EmployeeEntity)
@Query("DELETE FROM employees WHERE id = :id") suspend fun deleteById(id: Long)
@Query("DELETE FROM employees") suspend fun deleteAll()
@Query("SELECT * FROM employees WHERE id = :id") suspend fun getById(id: Long): EmployeeEntity?
```

Notes:
- `fullNameKey` is a stored, normalized column (`trim().lowercase(Locale.ROOT)`) on `EmployeeEntity` (with a composite `(fullNameKey, birth_date)` index) added purely for the dedupe lookup — `countMatching` is a single indexed, null-safe query, and the key never surfaces in the domain model.
- `EmployeeStatRow` also selects `id`, `fullName`, `position` (date columns aliased `birth_date AS birthDate`, `hire_date AS hireDate`) so `WorkforceAnalytics.compute` can build `AnalyticsSnapshot.longestServing` from the same projection.
- Bucket filters are translated to `birthFrom/birthTo/hireFrom/hireTo` epochDay ranges in the ViewModel, so filtering always happens in SQL; never load 10k rows to filter in Kotlin.
- No `fallbackToDestructiveMigration` — data loss is unacceptable for this app.

---

## 6. Excel Import Flow

### 6.1 User flow (text diagram)

```
 [EmployeeList FAB] or [empty-state CTA]
        |  tap "Import Excel"
        v
 +--------------------------- Step 1 - SELECT ---------------------------+
 | SAF OpenDocument(mime = ...spreadsheetml.sheet, fallback */*)         |
 | -> ContentResolver.openInputStream(uri)     (no storage permission)   |
 +--------------------------------+--------------------------------------+
                                  v
                     PreviewExcelUseCase(uri)      [@IoDispatcher]
                     +-------------------------------------------+
                     | OPCPackage.open(stream)                   |
                     |  +- XSSFReader -> sheet 0                 |
                     |  +- ReadOnlySharedStringsTable            |
                     |  +- scan rows 0..9 -> header row locator  |
                     |  +- ColumnDetector(headers, sampleRows)   |
                     |  +- ExcelPreview(headers, mapping,        |
                     |        sampleRows, totalRowEstimate)      |
                     +-------------------------------------------+
                                  |
             confidence >= 0.8 && REQUIRED_CORE fully mapped ?
                     +------------+------------+
                    YES                        NO
                     |                          v
                     |      +---------- Step 2 - MAPPING ----------+
                     |      | MappingRow per header:              |
                     |      |   ExposedDropdownMenu(11 + Ignore)  |
                     |      |   auto-detected value highlighted   |
                     |      |   required badges, duplicate block  |
                     |      |   Switch: skip_duplicates           |
                     |      |   [Next] enabled iff core mapped    |
                     |      +----------------+--------------------+
                     |                       | user confirms
                     +-----------+-----------+
                                 v
 +---------------------------- Step 3 - PREVIEW ------------------------------+
 | first 10 rows x mapped columns, horizontal scroll, mapped cols tinted      |
 | "about N rows" estimate      [Import]   [Back]                             |
 +--------------------------------+-------------------------------------------+
                                  v
 +---------------------------- Step 4 - IMPORTING ---------------------------+
 | XSSFSheetXMLHandler (SAX) row by row                                       |
 |   -> RowMapper -> Employee | ImportRowError                                 |
 |   -> dedupe (fullNameKey + birth_date) against existing keys                |
 |   -> buffer 500 -> dao.insertAll in @Transaction  (durable per chunk)       |
 |   -> emit Progress(processed, total)   delay(1) every 100 rows              |
 |   -> cancellable: committed chunks stay committed                           |
 +--------------------------------+-------------------------------------------+
                                  v
 +---------------------------- RESULT (page 4) ------------------------------+
 | animated check - imported / duplicates skipped / failed counts              |
 | [View employees] -> popBackStack(EmployeeList)                             |
 | if failed > 0: [Download error report]                                     |
 |    -> ExportErrorReportUseCase -> SAF CreateDocument("..._errors.xlsx")     |
 |    -> sheet "Errors": source row | column | raw value | reason              |
 +----------------------------------------------------------------------------+
```

### 6.2 Column auto-detection across 3 languages

Pipeline: `HeaderNormalizer` → `HeaderSynonyms` → `ColumnDetector`.

`HeaderNormalizer.normalize(raw)`: trim → `Normalizer.normalize(NFD)` → drop `\p{Mn}` (é→e, à→a) → drop Arabic tashkeel `\u064B–\u0652`, `\u0670`, tatweel `\u0640` → lowercase → replace punctuation/digits with space → collapse whitespace → strip leading `ال` → trim.

| Field | EN | FR | AR |
|---|---|---|---|
| `FULL_NAME` | name, full name, employee name, employee | nom, nom complet, nom et prenom, employe | الاسم, الاسم الكامل, اسم الموظف, الموظف |
| `DATE_OF_BIRTH` | dob, date of birth, birth date, born | date de naissance, naissance, date naissance | تاريخ الميلاد, الميلاد, تاريخ الولادة |
| `HIRE_DATE` | hire date, hiring date, date of hire, start date, recruitment date | date d'embauche, embauche, recrutement, date de recrutement | تاريخ التعيين, التعيين, تاريخ التوظيف, التوظيف |
| `DEPARTMENT` | department, dept, division, directorate | departement, service, direction, division | الإدارة, القسم, الدائرة, المديرية, المصلحة |
| `POSITION` | position, job title, title, post, function | poste, fonction, emploi, intitule | الوظيفة, المنصب, المسمى الوظيفي |
| `GRADE` | grade, class, classification, scale, rank | grade, echelle, categorie, classe | الدرجة, الرتبة, الصنف, الفئة |
| `SALARY` | salary, wage, pay, remuneration, gross salary | salaire, traitement, remuneration, paie | الأجر, الراتب, المعاش, الأجر الإجمالي |
| `EMAIL` | email, e-mail, mail, courriel | email, courriel, mail | البريد الإلكتروني, الإيميل, البريد |
| `PHONE` | phone, mobile, telephone, tel, cell | telephone, tel, portable, mobile | الهاتف, الجوال, رقم الهاتف, الهاتف المحمول |
| `ADDRESS` | address, residence, domicile | adresse, residence, domicile | العنوان, السكن, محل الإقامة |
| `NOTES` | notes, note, remarks, comments, observations | notes, remarque, remarques, observations | ملاحظات, تعليق, تعليقات, ملحوظة |

Scoring and assignment:
1. Normalize both sides of every pair; **exact** match ⇒ 2 points, **token/`contains`** match ⇒ 1 point, else 0.
2. Data sniffing (only for 0-score headers): every sample value parses as a date ⇒ +1 to the leftmost of `DATE_OF_BIRTH`/`HIRE_DATE` (leftmost date column → DOB, next → HIRE_DATE); contains `@` + `.` ⇒ +1 `EMAIL`; `+` or ≥ 8 digits with separators ⇒ +1 `PHONE`; numeric with ≤ 2 decimals ⇒ +1 `SALARY`.
3. Greedy assignment: highest score first, one header per field, one field per header; ties broken by column order.
4. `confidence = mappedCoreFields / 3f`. `< 0.8` or any missing core field ⇒ **force Step 2 (MAPPING)**.
5. Header row discovery: score rows `0..9` by non-empty cells of length 2–40 that match ≥ 1 synonym; pick the highest, require ≥ 2 matches, else default to row 0 and force low confidence (covers government files with title rows and merged cells).
6. User-confirmed mappings are persisted by `MappingProfileStore` keyed by `sha256(headers)` and silently re-applied for the same layout next time.

### 6.3 Memory-safe parsing strategy

- **SAX only:** `OPCPackage.open(inputStream)` → `XSSFReader` → `XSSFSheetXMLHandler(styles, sharedStrings, handler, false)`. `XSSFWorkbook`/`WorkbookFactory`/HSSF are never used for reading — they build the full sheet DOM (≈1–2 kB/row ⇒ 10k rows ≈ 20 MB of XMLBeans objects; 100k rows = OOM).
- `ReadOnlySharedStringsTable` holds only the shared-string table (bounded, typically a few hundred KB).
- One `Employee` is materialized per row and folded immediately into a 500-row buffer flushed inside one Room `@Transaction`; only aggregates and capped errors are retained. Peak heap for 10k rows is asserted < 64 MB in `LargeFileStreamingTest`.
- All work on `@IoDispatcher`; `delay(1)` every 100 rows and `yield()` per batch keep the UI responsive and make cancellation prompt.
- Progress emissions are rate-limited (≤ 20/s) to keep recomposition cheap.
- Writes use `SXSSFWorkbook(null, 100, false, false)` (100-row window, no temp files) with `dispose()`/`close()` in `finally`.
- **Only `.xlsx`** is supported; `.xls`/`.xlsm` raise the localized `unsupported_file_type` error (HSSF would add ≈1.5 MB for a format the brief does not require).
- `OPCPackage` is closed in `finally` on every path; a failure to open/parse yields `AppError.FileRead` and returns the wizard to Step 1 with retry.

```kotlin
sealed interface ImportProgress {
    data class Started(val totalRows: Int) : ImportProgress
    data class Progress(val processed: Int, val totalRows: Int) : ImportProgress
    data class RowFailed(val error: ImportRowError) : ImportProgress
    data class Done(
        val imported: Int,
        val skippedDuplicates: Int,
        val failed: Int,
        val errors: List<ImportRowError>,
        val batchId: String,
    ) : ImportProgress
}

data class ImportRowError(
    val sourceRow: Int,          // 1-based spreadsheet row
    val column: String?,         // raw header name, null when whole-row
    val rawValue: String,
    val reason: ImportErrorReason,
)

enum class ImportErrorReason { MISSING_REQUIRED, INVALID_DATE, INVALID_NUMBER, DUPLICATE, TOO_LONG, UNPARSEABLE }
```

### 6.4 Error handling for malformed rows

- A malformed row is **skipped, not fatal**: `failed++`, append an `ImportRowError`, continue streaming.
- Errors are capped at 5,000 in memory; beyond that only the count is tracked and the report states the cap.
- Whole-row problems report `column = null`; cell problems report the raw header name.
- `REQUIRED_CORE` missing ⇒ `MISSING_REQUIRED`; unparseable date ⇒ `INVALID_DATE` (raw value preserved for the report); `nan`/currency-only salary ⇒ `INVALID_NUMBER` (value kept, salary `null`, row still imported — a missing salary is not a fatal row error).
- Duplicate policy is explicit: `skip_duplicates` (default ON) skips rows whose `fullNameKey + birth_date` already exists in the DB **or** repeats earlier in the same file; existing keys are loaded once into a `HashSet<Long>` of hashes.
- Whole-file failures (`FileRead`, `UnsupportedFile`) abort into the error state; already-committed chunks remain (durable partial import) and the Result screen reports the committed count.
- The `Result` step writes the report as a real `.xlsx` (sheet `Errors`) via SAF `CreateDocument`, so Excel/LibreOffice open it without a CSV encoding round-trip.

---

## 7. Localization & RTL Strategy

### 7.1 Resource layout

```
res/values/strings.xml           # en (default, complete)
res/values-b+fr/strings.xml      # fr (complete)
res/values-b+ar/strings.xml      # ar (complete)
res/xml/locales_config.xml       # en, fr, ar  -> enables Android 13 "App language"
res/values/themes.xml + values-night/themes.xml
res/values-ldrtl/dimens.xml      # RTL-only size overrides (reserved, usually empty)
app/src/test/.../i18n/StringResourceParityTest.kt   # fails the build on any key drift
```

Manifest: `android:supportsRtl="true"`, `android:localeConfig="@xml/locales_config"`. `MainActivity` declares `android:configChanges="…|locale|layoutDirection|…"` so a locale change never recreates the Activity.

### 7.2 Font strategy (Latin + Arabic)

- `res/font/inter_variable.ttf` (Latin) and `res/font/cairo_variable.ttf` (Arabic with Latin fallback), both **variable** and both OFL (license text in `assets/licenses/`).
- `Typefaces.kt` builds `InterFamily` / `CairoFamily` with `FontVariation.Settings(FontVariation.weight(w))` for 400/500/600/700 — one file per family (~1.2 MB total) instead of seven static weights (~3 MB).
- `AppTypography.forLanguage(language)`: Inter for en/fr, Cairo for ar with `lineHeight` ≈ ×1.15 and `LineHeightStyle(alignment = Center, trim = None)` so diacritics and ascenders are never clipped.
- Glyphs are bundled ⇒ identical rendering on API 26 → 35, independent of OEM system fonts.

### 7.3 Runtime language switch — exact mechanism

1. `AppLanguage` is stored in DataStore (`PreferencesRepository.language`) and cached in `LocaleManager`'s `@Volatile` field so cold start never flashes the system locale while DataStore's first read is in flight.
2. `AppViewModel` exposes `StateFlow<AppUiState>`; `SettingsScreen` dispatches `SetLanguage`.
3. `AppTheme` re-provides `LocalContext` (locale-wrapped `Context`), `LocalConfiguration`, and `LocalLayoutDirection`. `stringResource`, `pluralStringResource`, `stringArrayResource` and every M3 internal that reads `LocalContext`/`LocalConfiguration` (e.g. `DatePickerDialog`) switch immediately.
4. No Activity recreation, no restart, no flash. `AnimatedContent(targetState = language, tween(220))` smooths the swap; with system animations disabled the swap is instant.
5. On API 33+, `LocaleManager.syncFromSystemLocaleManager()` mirrors a locale chosen in the system *App language* screen into DataStore on next launch so both entry points agree.
6. Dates/numbers are formatted with the **app** locale (`DateFormatters`, `NumberFormatters`), never `Locale.getDefault()`.

### 7.4 RTL pitfalls and how to avoid them

| # | Pitfall | Mitigation |
|---|---|---|
| 1 | `left`/`right` padding, `Arrangement.Left`, `TextAlign.Left` | Use `start`/`end`, `Arrangement.Start/End`, `TextAlign.Start/End` everywhere. Rule: grep for `PaddingValues`/`Offset` with `left`/`right` ⇒ must be empty. |
| 2 | `Modifier.offset(x = …)` and `graphicsLayer.translationX` do not mirror | Use `offset` only for symmetric values, or derive the sign from `LocalLayoutDirection`; prefer `Arrangement`/`Spacer(Modifier.weight(1f))`. `absoluteOffset` only for physically fixed elements, always with a comment. |
| 3 | Directional icons don't flip | Use `Icons.AutoMirrored.*` (e.g. `AutoMirrored.Filled.ArrowBack`) or `Modifier.scale(scaleX = -1f)` for custom vectors. **Never** flip non-directional icons (clock, user, save, chart, trash). |
| 4 | Canvas/figure geometry mirrored (charts read backwards) | Wrap every chart in `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)`; chart axes are numeric and stay LTR in Arabic. |
| 5 | Arabic-Indic digits (٠١٢٣) break parsing and inputs | `core/LocaleDigits.kt` `toAsciiDigits()` on every numeric parse; editable fields format with a fixed ASCII-digit `DecimalFormatSymbols`. |
| 6 | Email/phone/salary text mirrors | Wrap those `OutlinedTextField`s in `LocalLayoutDirection provides Ltr` + `textAlign = TextAlign.Start`, with `KeyboardType.Email/Phone/Number`. |
| 7 | `HorizontalPager` reverses page order in RTL | Expected: page 0 sits on the right. Keep page dots derived from `LocalLayoutDirection` so indicator order matches the visible pages. |
| 8 | `SwipeToDismissBox` direction hardcoded | Use `SwipeToDismissBoxValue.EndToStart`/`StartToEnd` — M3 mirrors them automatically. |
| 9 | Shared-element geometry glitches in RTL | Use `sharedElement`/`sharedBounds` on symmetric bounds; never combine with `absoluteOffset`. |
| 10 | Splash/adaptive icon direction | `android:autoMirrored="true"` on directional vector drawables; keep the launch icon symmetric. |
| 11 | Arabic shaping broken by string concatenation | Never concatenate translatable fragments; one resource per sentence with positional args (`%1$s`), never `stringResource(a) + b`. |
| 12 | Arabic text clipped by default line height | Cairo typography with a larger `lineHeight` + `LineHeightStyle.Trim.None` (§7.2). |
| 13 | French narrow no-break spaces / Arabic digit grouping break assertions | Format with `NumberFormat.getInstance(appLocale)`; tests assert formatted output explicitly for en/fr/ar. |
| 14 | Snackbar/Toast gravity | `SnackbarHost` inside `Scaffold` only; no manual `Gravity`. |
| 15 | RTL regressions slip through review | `RtlLayoutTest` (Phase 13) plus RTL `@Preview`s on every component (Phase 12) make regressions visible. |

---

## 8. Animation & Motion Plan

Global rules: every duration/easing comes from `MaterialTheme.motionScheme` via `AppMotion` (§2, `Motion.kt`) — `spatial()` for position/size, `effects()` for alpha/color. Compose honours the system animation scale, so "Remove animations" makes everything instant. Animations are decorative: no information is conveyed by motion alone.

| # | Screen / element | Animation | Implementation |
|---|---|---|---|
| 1 | Bottom bar show/hide | slide + fade | `AnimatedVisibility(slideInVertically { it } + fadeIn(), slideOutVertically { it } + fadeOut())` |
| 2 | Navigation between destinations | fade + subtle scale, 220 ms | `NavHost` transitions from `NavTransitions.kt` using `AppMotion.spatial()` |
| 3 | Dashboard stat numbers | count-up 0 → value, medium-bouncy spring | `animateIntAsState(spring(DampingRatioMediumBouncy, StiffnessMediumLow))`; decimals via `animateFloatAsState` + `NumberFormatters` |
| 4 | Dashboard age donut | sweep 0° → 360°, 700 ms, 40 ms per-slice stagger | `DonutChart` `animateFloatAsState` + `Canvas.drawArc` |
| 5 | Dashboard service bars | bars grow from the axis | Vico `CartesianChartModelProducer.runTransaction` after `LaunchedEffect` + 60 ms |
| 6 | Dashboard section entry | staggered | `MutableTransitionState(false) → true` + `AnimatedVisibility(tween(400, delayMillis = index * 60))`, capped at 8 items |
| 7 | List item insert/reorder/remove | fade + slide 12 dp, interruption-aware | `Modifier.animateItem()` (Compose 1.11.4) |
| 8 | List FAB expand | 2 mini-FABs `scaleIn(transformOrigin = TransformOrigin(1f, 1f))` + `fadeIn`, 180 ms; icon rotates +45° | `AnimatedVisibility` + `AnimatedContent` |
| 9 | Swipe-to-delete | drag + background icon scale | M3 `SwipeToDismissBox` + `animateFloatAsState` on icon alpha |
| 10 | Undo restore | item re-inserts via `animateItem()`; snackbar slides | `SnackbarHost` + `RestoreEmployeeUseCase` |
| 11 | List card → detail (avatar + name) | shared element / shared bounds | `SharedTransitionLayout` + `rememberSharedContentState("employee-avatar-$id")`, `slowSpatialSpec()` |
| 12 | Detail section entry | sequential reveal | `LazyColumn` + `animateItem()` |
| 13 | Edit: invalid field | shake ±8 dp × 3 over 300 ms, then inline error | `Animatable` + `keyframes { 0f at 0; -8f at 60; 8f at 120; -8f at 180; 8f at 240; 0f at 300 }` |
| 14 | Edit: save success | checkmark draw-on, 600 ms, then pop | `AnimatedCheckmark` + `PathMeasure.getSegment` |
| 15 | Import wizard page change | spring scroll | `HorizontalPager.animateScrollToPage(spring(StiffnessLow))` |
| 16 | Import wizard page dots | animated width + alpha | `PageDots` with `animateDpAsState` / `animateFloatAsState`, order from `LocalLayoutDirection` |
| 17 | Import: Next becomes enabled | button scale 0.96 → 1.0 + container colour | `animateFloatAsState` + M3 `ButtonDefaults` |
| 18 | Import progress | ring sweep + percentage count-up + pulse | `animateFloatAsState` + `infiniteRepeatable(tween(1200), RepeatMode.Reverse)` for the pulse |
| 19 | Import result | checkmark draw-on (or error icon pop); counts count-up | `AnimatedCheckmark`, `CountUpText` |
| 20 | Analytics tab switch | horizontal slide + fade, no size animation | `AnimatedContent(..., sizeTransform = null)` + `fastEffectsSpec()` |
| 21 | Analytics re-filter | arcs/bars re-animate from previous values | `animateFloatAsState` per slice; Vico model transition |
| 22 | Analytics department filter chip | selection scale + colour | M3 `FilterChip` + `animateFloatAsState` |
| 23 | Settings theme switch | crossfade of the shell, 220 ms | `AnimatedContent(targetState = themeMode)` |
| 24 | Settings language switch | crossfade, 220 ms, RTL flip | `AnimatedContent(targetState = language)` inside `AppTheme` |
| 25 | Loading | shimmer sweep | `LoadingShimmer` with `Brush.linearGradient` + `rememberInfiniteTransition` offset |
| 26 | Splash → content | icon scale + alpha keep-on-screen, M3 crossfade | `androidx.core.splashscreen` + `setKeepOnScreenCondition { !uiState.isLoaded }` |
| 27 | Dialogs / bottom sheets | M3 defaults (scrim fade + scale) | `AlertDialog`, `ModalBottomSheet` |
| 28 | Detail delete → list | pop transition + list `animateItem()` removal | `NavHost` pop transition + Room flow refresh |

---

## 9. Testing Strategy

### 9.1 Coverage per layer

| Layer | Type | Tooling / location | Target | What must be asserted |
|---|---|---|---|---|
| `core/` (digits, formatters, locale) | JVM unit | JUnit4 + Truth — `test/core/` | 90 % | Arabic-Indic digit round-trip; fr/ar number + date formatting; `withLocale` returns an RTL configuration for `ar`; `LocaleManager` cache wins before DataStore emits |
| `domain/analytics` | JVM unit | Truth — `test/domain/analytics/` | 95 % | every bucket boundary (24/25/34/35/44/45/54/55, 4/5/9/10/14/15/19/20); null DOB/hire in `total` only; medians (even/odd); retirement-soon; department `Other` rollup > 10; 10k-row compute < 50 ms |
| `domain/usecase` | JVM unit | MockK + coroutines-test + Turbine | 85 % | dedupe logic, restore, delete-all, export orchestration, dispatcher correctness |
| `data/local` (DAO, converters, `fullNameKey`) | JVM unit on Robolectric | in-memory Room, `@Config(sdk = [34])` | 90 % | CRUD; paging/ordering per sort key; department + epochDay range filters; projections return only selected columns; `countMatching` dedupe |
| `data/preferences` | JVM unit on Robolectric | DataStore test scope | 80 % | language/theme set+read; mapping profile round-trip; corrupted value falls back to default |
| `data/excel` | JVM unit + fixtures | JUnit4 — `test/data/excel/` + `test/resources/excel/` | 90 % | detection in en/fr/ar; merged-header row discovery; every date format; `malformed.xlsx` ⇒ exactly 3 errors with correct reasons; write→read round-trip; 10k-row heap ceiling |
| `presentation/*ViewModel` | JVM unit | Turbine + `MainDispatcherRule(StandardTestDispatcher)` + MockK | 85 % | state transitions; debounce emits once per 200 ms; cancel mid-import; validation gating; filter/sort recompute; one-shot effects fired once |
| i18n | JVM unit | `StringResourceParityTest` (reads `res/values*/strings.xml` from disk) | 100 % | key parity across en/fr/ar; no blank values; no value == key; identical placeholder sets |
| UI (Compose) | Instrumented | `createAndroidComposeRule`, `ui-test-junit4` — `androidTest/` | smoke | navigation routes; list search/empty state; swipe + undo; mapping validation blocks Next; RTL invariants |
| Persistence/migration | Instrumented | `MigrationTestHelper` | schema | current schema opens; `1.json` matches the entity |

### 9.2 Performance budgets (enforced gates, measured on a Pixel 6 / API 34)

| Scenario | Budget | How it is measured |
|---|---|---|
| Cold start | `TotalTime < 1500 ms` | `adb shell am start -W -n com.gov.employeedb.debug/com.gov.employeedb.MainActivity` |
| Import 10,000 rows | `< 20 s`, peak heap `< 128 MB` | `ZolvexPerf` log lines emitted by `ImportExcelUseCase` |
| Import of one row batch | no dropped frames on the wizard screen | `adb shell dumpsys gfxinfo … framestats` |
| Dashboard analytics | `< 100 ms` | `ZolvexPerf analytics_ms=…` |
| Analytics tab compute | `< 200 ms` | `ZolvexPerf tab_ms=…` |
| List scroll (10k items) | janky frames `< 1 %` | `adb shell dumpsys gfxinfo com.gov.employeedb.debug framestats` |
| Export 10,000 rows | `< 8 s`, peak heap `< 128 MB` | `ZolvexPerf export_ms=…` |
| Locale switch | `< 1 frame` visible change (no recreate) | `adb shell dumpsys activity activities | grep -c "MainActivity"` stays 1 |
| Release AAB | `< 15 MB` | file size of `app-release.aab` (Phase 14) |

`ZolvexPerf` is a single `private const val TAG = "ZolvexPerf"` used with `Log.i`; it is compiled out of release builds by wrapping the calls in `if (BuildConfig.DEBUG)`.

### 9.3 Test hygiene rules

- No `Thread.sleep`; use `MainDispatcherRule`, `StandardTestDispatcher`, `advanceUntilIdle()`, `Turbine`.
- Every Robolectric test pins `@Config(sdk = [34])`.
- Fixtures are generated once and committed (`test/resources/excel/`), never downloaded at test time.
- A test that depends on the current date must accept `today` as a parameter — `domain` never calls `LocalDate.now()` internally.
- Any new test failure on `main` blocks the next phase (Rule 7).

---

## 10. ProGuard / R8 Rules

### `app/proguard-rules.pro` (complete)

```proguard
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
```

Verification of these rules happens in Phase 14 by running a **release** import: an over-aggressive shrink surfaces as `ClassNotFoundException` for aalto/xmlbeans or a POI parse failure.

---

## 11. Full String Key List

### 11.1 Rules

- Every user-facing string is a key; no literal text in Kotlin (lint error via `HardcodedText`-equivalent custom check in code review + the grep in Rule 3).
- Key names are `snake_case`, no prefix, one canonical name per concept.
- Placeholders use positional form (`%1$s`, `%2$d`) — never bare `%s` when more than one argument.
- The `cd_` prefix marks accessibility content descriptions.
- `StringResourceParityTest` (Phase 2) fails the build if any key is missing in any locale.

### 11.2 Flat key list

```
app_name, dashboard, employees, analytics, settings,
import_excel, export_excel, export_all, export_analysis, import_success, import_failed,
add_employee, edit_employee, delete_employee, employee_detail,
search_hint, search_no_results, clear_search, filter, apply, reset, sort_by,
sort_name, sort_age, sort_hire_date, sort_department, sort_ascending, sort_descending,
total_employees, average_age, average_service, median_age, retirement_soon,
retirement_soon_hint, longest_serving, top_departments, distribution, count, percentage,
age, years, service_years, department, position, grade, salary,
full_name, date_of_birth, hire_date, email, phone, address, notes, notes_hint, address_hint,
personal_info, professional_info, additional_info, required, optional,
delete, edit, save, saving, cancel, confirm, undo, done, next, back, close, ok, retry,
delete_confirm_title, delete_confirm_message, delete_all, delete_all_message, delete_all_confirm,
language, language_english, language_french, language_arabic,
theme, theme_light, theme_dark, theme_system,
select_file, select_excel_file, unsupported_file_type, file_read_error,
column_mapping, mapping_required_fields_hint, skip_duplicates, preview, total_rows,
rows_to_import, rows_imported, rows_skipped, rows_failed, import_progress,
error_report, download_error_report, error_report_saved, import_cancelled, cancel_import,
view_employees, import_step_select, import_step_map, import_step_preview, import_step_result,
analytics_age, analytics_service, analytics_department, analytics_grade,
age_under_25, age_25_34, age_35_44, age_45_54, age_55_plus,
service_less_5, service_5_10, service_10_15, service_15_20, service_20_plus,
empty_state_employees, empty_state_analytics, empty_state_search, no_data,
loading, error_generic, seed_test_data, about, version, licenses, backup_disabled_note,
validation_name_required, validation_dob_invalid, validation_hire_after_dob,
validation_salary_negative, validation_email_invalid,
cd_back, cd_edit, cd_delete, cd_add, cd_import, cd_search, cd_filter, cd_sort,
cd_language, cd_chart, cd_clear, cd_avatar, cd_export, cd_confirm
```

Count: **154 keys** (each × 3 locales = 462 string values). §11.2 and §11.3 agree exactly — 154 distinct keys each, verified by parsing this file — and the earlier "146" was an arithmetic slip in the prose (see Appendix A, A14).

### 11.3 Full translations (en / fr / ar)

| key | en | fr | ar |
|---|---|---|---|
| `app_name` | Zolvex | Zolvex | Zolvex |
| `dashboard` | Dashboard | Tableau de bord | لوحة التحكم |
| `employees` | Employees | Employés | الموظفون |
| `analytics` | Analytics | Analytique | التحليلات |
| `settings` | Settings | Paramètres | الإعدادات |
| `import_excel` | Import Excel | Importer Excel | استيراد إكسل |
| `export_excel` | Export Excel | Exporter Excel | تصدير إكسل |
| `export_all` | Export all employees | Exporter tous les employés | تصدير جميع الموظفين |
| `export_analysis` | Export analysis | Exporter l'analyse | تصدير التحليل |
| `import_success` | Import completed | Importation réussie | تم الاستيراد بنجاح |
| `import_failed` | Import failed | Échec de l'importation | فشل الاستيراد |
| `add_employee` | Add employee | Ajouter un employé | إضافة موظف |
| `edit_employee` | Edit employee | Modifier l'employé | تعديل الموظف |
| `delete_employee` | Delete employee | Supprimer l'employé | حذف الموظف |
| `employee_detail` | Employee | Employé | الموظف |
| `search_hint` | Search name, position or department | Rechercher un nom, un poste ou un service | ابحث بالاسم أو الوظيفة أو القسم |
| `search_no_results` | No matching employees | Aucun employé correspondant | لا يوجد موظفون مطابقون |
| `clear_search` | Clear | Effacer | مسح |
| `filter` | Filter | Filtrer | تصفية |
| `apply` | Apply | Appliquer | تطبيق |
| `reset` | Reset | Réinitialiser | إعادة تعيين |
| `sort_by` | Sort by | Trier par | الترتيب حسب |
| `sort_name` | Name | Nom | الاسم |
| `sort_age` | Age | Âge | العمر |
| `sort_hire_date` | Hire date | Date d'embauche | تاريخ التعيين |
| `sort_department` | Department | Département | القسم |
| `sort_ascending` | Ascending | Croissant | تصاعدي |
| `sort_descending` | Descending | Décroissant | تنازلي |
| `total_employees` | Total Employees | Total Employés | إجمالي الموظفين |
| `average_age` | Average Age | Âge moyen | متوسط العمر |
| `average_service` | Average Service | Ancienneté moyenne | متوسط سنوات الخدمة |
| `median_age` | Median age | Âge médian | العمر الوسيط |
| `retirement_soon` | Retirement soon | Départs imminents | قريبو التقاعد |
| `retirement_soon_hint` | Within 5 years of retirement age | Dans les 5 ans précédant l'âge de la retraite | خلال 5 سنوات من سن التقاعد |
| `longest_serving` | Longest serving | Les plus anciens | الأطول خدمة |
| `top_departments` | Largest departments | Principaux départements | أكبر الأقسام |
| `distribution` | Distribution | Répartition | التوزيع |
| `count` | Count | Nombre | العدد |
| `percentage` | Share | Part | النسبة |
| `age` | Age | Âge | العمر |
| `years` | years | ans | سنة |
| `service_years` | Years of service | Années de service | سنوات الخدمة |
| `department` | Department | Département | القسم |
| `position` | Position | Poste | الوظيفة |
| `grade` | Grade | Grade | الدرجة |
| `salary` | Salary | Salaire | الراتب |
| `full_name` | Full name | Nom complet | الاسم الكامل |
| `date_of_birth` | Date of birth | Date de naissance | تاريخ الميلاد |
| `hire_date` | Hire date | Date d'embauche | تاريخ التعيين |
| `email` | Email | E-mail | البريد الإلكتروني |
| `phone` | Phone | Téléphone | الهاتف |
| `address` | Address | Adresse | العنوان |
| `notes` | Notes | Notes | ملاحظات |
| `notes_hint` | Additional notes | Notes complémentaires | ملاحظات إضافية |
| `address_hint` | Street, city | Rue, ville | الشارع، المدينة |
| `personal_info` | Personal | Personnel | الشخصية |
| `professional_info` | Professional | Professionnel | المهنية |
| `additional_info` | Additional | Complémentaire | إضافية |
| `required` | Required | Obligatoire | مطلوب |
| `optional` | Optional | Facultatif | اختياري |
| `delete` | Delete | Supprimer | حذف |
| `edit` | Edit | Modifier | تعديل |
| `save` | Save | Enregistrer | حفظ |
| `saving` | Saving… | Enregistrement… | جارٍ الحفظ… |
| `cancel` | Cancel | Annuler | إلغاء |
| `confirm` | Confirm | Confirmer | تأكيد |
| `undo` | Undo | Annuler | تراجع |
| `done` | Done | Terminé | تم |
| `next` | Next | Suivant | التالي |
| `back` | Back | Retour | رجوع |
| `close` | Close | Fermer | إغلاق |
| `ok` | OK | OK | موافق |
| `retry` | Retry | Réessayer | إعادة المحاولة |
| `delete_confirm_title` | Confirm deletion | Confirmer la suppression | تأكيد الحذف |
| `delete_confirm_message` | Delete this employee? This cannot be undone. | Supprimer cet employé ? Cette action est irréversible. | هل تريد حذف هذا الموظف؟ لا يمكن التراجع عن هذا الإجراء. |
| `delete_all` | Delete all employees | Supprimer tous les employés | حذف جميع الموظفين |
| `delete_all_message` | This permanently deletes every employee record. Continue? | Cela supprime définitivement tous les employés. Continuer ? | سيتم حذف جميع سجلات الموظفين نهائياً. هل تريد المتابعة؟ |
| `delete_all_confirm` | Delete all | Tout supprimer | حذف الكل |
| `language` | Language | Langue | اللغة |
| `language_english` | English | Anglais | الإنجليزية |
| `language_french` | French | Français | الفرنسية |
| `language_arabic` | Arabic | Arabe | العربية |
| `theme` | Theme | Thème | المظهر |
| `theme_light` | Light | Clair | فاتح |
| `theme_dark` | Dark | Sombre | داكن |
| `theme_system` | System | Système | حسب النظام |
| `select_file` | Choose a file | Choisir un fichier | اختيار ملف |
| `select_excel_file` | Choose an Excel file | Choisir un fichier Excel | اختيار ملف إكسل |
| `unsupported_file_type` | Only .xlsx files are supported | Seuls les fichiers .xlsx sont pris en charge | يُدعم ملفات .xlsx فقط |
| `file_read_error` | Could not read the selected file | Impossible de lire le fichier sélectionné | تعذر قراءة الملف المحدد |
| `column_mapping` | Column mapping | Association des colonnes | ربط الأعمدة |
| `mapping_required_fields_hint` | Map Name, Date of birth and Hire date to continue | Associez Nom, Date de naissance et Date d'embauche pour continuer | اربط الاسم وتاريخ الميلاد وتاريخ التعيين للمتابعة |
| `skip_duplicates` | Skip duplicates | Ignorer les doublons | تخطي المكرر |
| `preview` | Preview | Aperçu | معاينة |
| `total_rows` | Total rows | Total des lignes | إجمالي الصفوف |
| `rows_to_import` | Rows to import | Lignes à importer | صفوف للاستيراد |
| `rows_imported` | Rows imported | Lignes importées | الصفوف المستوردة |
| `rows_skipped` | Duplicates skipped | Doublons ignorés | الصفوف المتخطاة |
| `rows_failed` | Rows with errors | Lignes en erreur | الصفوف الفاشلة |
| `import_progress` | Importing… | Importation… | جارٍ الاستيراد… |
| `error_report` | Error report | Rapport d'erreurs | تقرير الأخطاء |
| `download_error_report` | Download error report | Télécharger le rapport d'erreurs | تنزيل تقرير الأخطاء |
| `error_report_saved` | Error report saved | Rapport d'erreurs enregistré | تم حفظ تقرير الأخطاء |
| `import_cancelled` | Import cancelled | Importation annulée | تم إلغاء الاستيراد |
| `cancel_import` | Cancel import | Annuler l'importation | إلغاء الاستيراد |
| `view_employees` | View employees | Voir les employés | عرض الموظفين |
| `import_step_select` | File | Fichier | الملف |
| `import_step_map` | Mapping | Association | الربط |
| `import_step_preview` | Preview | Aperçu | المعاينة |
| `import_step_result` | Result | Résultat | النتيجة |
| `analytics_age` | Age distribution | Répartition par âge | توزيع الأعمار |
| `analytics_service` | Years of service | Ancienneté | سنوات الخدمة |
| `analytics_department` | Departments | Départements | الأقسام |
| `analytics_grade` | Grades | Grades | الدرجات |
| `age_under_25` | Under 25 | Moins de 25 | أقل من 25 |
| `age_25_34` | 25–34 | 25–34 | 25–34 |
| `age_35_44` | 35–44 | 35–44 | 35–44 |
| `age_45_54` | 45–54 | 45–54 | 45–54 |
| `age_55_plus` | 55 and over | 55 et plus | 55 وأكثر |
| `service_less_5` | Under 5 | Moins de 5 | أقل من 5 |
| `service_5_10` | 5–10 | 5–10 | 5–10 |
| `service_10_15` | 10–15 | 10–15 | 10–15 |
| `service_15_20` | 15–20 | 15–20 | 15–20 |
| `service_20_plus` | 20 and over | 20 et plus | 20 وأكثر |
| `empty_state_employees` | No employees yet | Aucun employé pour le moment | لا يوجد موظفون بعد |
| `empty_state_analytics` | No data to analyse | Aucune donnée à analyser | لا توجد بيانات للتحليل |
| `empty_state_search` | Nothing matches your search | Aucun résultat pour cette recherche | لا نتائج مطابقة لبحثك |
| `no_data` | No data | Aucune donnée | لا توجد بيانات |
| `loading` | Loading | Chargement | جارٍ التحميل |
| `error_generic` | Something went wrong. Please try again. | Une erreur est survenue. Veuillez réessayer. | حدث خطأ ما. يرجى المحاولة مرة أخرى. |
| `seed_test_data` | Seed test data | Données de test | بيانات تجريبية |
| `about` | About | À propos | حول |
| `version` | Version %1$s | Version %1$s | الإصدار %1$s |
| `licenses` | Open-source licences | Licences open source | تراخيص المصادر المفتوحة |
| `backup_disabled_note` | Backups are disabled for this app to protect personal data. | Les sauvegardes sont désactivées pour protéger les données personnelles. | النسخ الاحتياطي معطّل لحماية البيانات الشخصية. |
| `validation_name_required` | Full name is required | Le nom complet est obligatoire | الاسم الكامل مطلوب |
| `validation_dob_invalid` | Invalid date of birth | Date de naissance invalide | تاريخ ميلاد غير صالح |
| `validation_hire_after_dob` | Hire date must be after the date of birth | La date d'embauche doit être postérieure à la date de naissance | يجب أن يكون تاريخ التعيين بعد تاريخ الميلاد |
| `validation_salary_negative` | Salary must be zero or greater | Le salaire doit être positif ou nul | يجب أن يكون الراتب صفراً أو أكثر |
| `validation_email_invalid` | Invalid email address | Adresse e-mail invalide | عنوان البريد الإلكتروني غير صالح |
| `cd_back` | Back | Retour | رجوع |
| `cd_edit` | Edit employee | Modifier l'employé | تعديل الموظف |
| `cd_delete` | Delete employee | Supprimer l'employé | حذف الموظف |
| `cd_add` | Add employee | Ajouter un employé | إضافة موظف |
| `cd_import` | Import Excel file | Importer un fichier Excel | استيراد ملف إكسل |
| `cd_search` | Search | Rechercher | بحث |
| `cd_filter` | Filters | Filtres | عوامل التصفية |
| `cd_sort` | Sort options | Options de tri | خيارات الترتيب |
| `cd_language` | Change language | Changer de langue | تغيير اللغة |
| `cd_chart` | Chart | Graphique | مخطط |
| `cd_clear` | Clear | Effacer | مسح |
| `cd_avatar` | Initials avatar | Avatar | صورة رمزية |
| `cd_export` | Export | Exporter | تصدير |
| `cd_confirm` | Confirm | Confirmer | تأكيد |

### 11.4 String formatting rules

- Counts: `pluralStringResource` where a sentence embeds a number (e.g. `rows_imported` shows `%1$d` via a dedicated plural key if a full sentence is needed; the table above keeps count labels because they are used as `label + count` in stat cards).
- All placeholders use positional form: `Version %1$s`.
- Never build sentences by concatenating resources — each row above is a complete sentence.

---

## 12. Definition of Done

Final checklist the agent must verify before declaring success. Every item must be *observed*, not assumed.

- [ ] Installs and launches on **API 26** and **API 34** without a crash.
- [ ] `.xlsx` import works for EN, FR and AR header files; auto-detection maps the 3 core fields in all three.
- [ ] Mapping UI correctly fixes a deliberately broken header layout; `Next` stays disabled while a core field is unmapped or a field is duplicated.
- [ ] A malformed file imports the good rows, reports the bad ones, and produces a downloadable `.xlsx` error report.
- [ ] All CRUD operations persist across a full app restart (`adb shell am force-stop` then relaunch).
- [ ] Dashboard shows live stats (total, average age, average service, retirement-soon) plus two animated charts.
- [ ] Analytics has 4 tabs (Age, Service, Department, Grade); every chart renders and department filtering affects all tabs.
- [ ] Export produces valid `.xlsx` for employees **and** for the analysis, both re-readable by `ExcelReader`.
- [ ] Language switching EN/FR/AR is instant, with a complete RTL flip, and no Activity restart (single `MainActivity` instance in `dumpsys activity activities`).
- [ ] All strings localized: `StringResourceParityTest` green, zero hardcoded user-facing text.
- [ ] Swipe-to-delete with a working Undo.
- [ ] Animations implemented per §8: stat count-up, list entry, FAB expand, shared element, chart draw, wizard progress, tab slide, theme/language crossfade.
- [ ] Release AAB **< 15 MB** (and the release build passes the same smoke test as debug — proving the R8 rules).
- [ ] Performance budgets of §9.2 all met and pasted into the Phase 13 commit message.
- [ ] `./gradlew lint` → **0 errors**; `./gradlew testDebugUnitTest` → **0 failures**.
- [ ] 30-minute manual test with 10,000 rows: import, browse, filter, edit, delete, analyse, export, switch language twice, rotate — no crash, no visible jank.
- [ ] `PLAN.md` progress checkboxes all ticked; `git tag v1.0.0`.

---

## 13. Agent Execution Rules

1. **Build after every phase.** Never start phase N+1 while `./gradlew assembleDebug` is red for phase N. Report the build result (and test result) after each phase.
2. **One commit per phase**, message exactly `feat(phase-N): <desc>` (or `test(...)` / `chore(...)` where §4 says so). Never mix two phases in one commit.
3. **No hardcoded user-facing strings, ever.** Every label goes through `stringResource(R.string.…)` and exists in `values/`, `values-b+fr/`, `values-b+ar/`. `StringResourceParityTest` must stay green.
4. **Always `start`/`end`, never `left`/`right`** in Compose (`PaddingValues`, `Arrangement`, `TextAlign`, `Offset`). Before finishing a phase, grep the diff for `left =`/`right =` in Compose modifiers and fix any hit.
5. **Ask before adding any dependency** not already in `gradle/libs.versions.toml`. The Phase-1..14 set in §2 plus Appendix A is pre-approved; anything else needs explicit confirmation. Never add a dependency without a version.
6. **No placeholders.** No `TODO()`, no `NotImplementedError`, no commented-out code, no unused imports/fields. If something cannot be implemented yet, it must not be committed (finish it or leave the file out of that phase).
7. **Run the phase gate before reporting success:** `./gradlew assembleDebug` always; `./gradlew testDebugUnitTest` whenever the phase lists tests; `./gradlew lint` in Phases 12–14. Zero failures.
8. **Dispatchers are injected.** Never write `Dispatchers.IO`/`Dispatchers.Default` inside a ViewModel or use case; use `@IoDispatcher` / `@DefaultDispatcher`. Never use `GlobalScope`; use `@ApplicationScope`.
9. **Close every resource.** POI `OPCPackage`/`Workbook` and every `InputStream`/`OutputStream` in `try/finally` (`use {}`). A leaked `OPCPackage` locks the workbook and leaks native memory.
10. **Files under ~300 lines.** Extract composables, keep one public composable per file (plus private helpers), and give every component an `@Preview` in light/dark × LTR/RTL.
11. **Domain stays pure.** `domain/` may use `java.time` but never `android.*`, `Context`, `R`, or `Log`. Presentation may not import Room entities or POI types; only `domain` models cross layers.
12. **Data integrity first.** Never mutate `Employee.id`; updates preserve `createdAt` and set `updatedAt`; deletes are single-row by id; `deleteAll` requires the double confirmation from Phase 11.
13. **Never commit secrets.** `local.properties`, `*.jks`, `*.keystore` and `tools/` stay git-ignored. Release signing config reads only from `local.properties`.
14. **Verify visually in both directions.** After any UI phase, launch the app, switch to Arabic, and confirm the new screen mirrors correctly before committing.
15. **Update `PLAN.md`.** Tick the phase checkbox and record measured numbers (perf, sizes) in the commit message as you go; the plan is the source of truth for the next phase.

---

## Appendix A — Deviations from `prompt.json` (explicit, intentional)

| # | `prompt.json` says | This plan does | Why |
|---|---|---|---|
| A1 | Charts via Vico, `components/AgeChart.kt` (Vico donut) | Vico for Cartesian charts; `DonutChart` (Canvas) for donut/pie, wrapped by `AgeChart`/`GradeTab` | Vico 2's pie API is brand-new alpha surface; a ~45-line Canvas donut removes that risk and keeps exact M3 styling + deterministic animation |
| A2 | `data/excel/EmployeeField.kt` | `domain/model/EmployeeField.kt` | The mapping UI (presentation) needs the enum; keeping it in `data/` would make presentation depend on the data layer |
| A3 | Phase 2 defines nav routes | Phase 2 = design system/i18n, Phase 3 = navigation | Keeps Phase 2 focused and each phase independently verifiable |
| A4 | 12 phases | 14 phases (split nav out, split polish/testing/release) | Each phase must be independently buildable and verifiable; the two extra gates reduce end-stage risk |
| A5 | kotlinx.serialization only implied (ProGuard section) | Explicit plugin + `kotlinx-serialization-json:1.7.3` | Type-safe nav routes require it (ADR-10) |
| A6 | "WorkManager" mentioned in the brief | Not used; streaming Flow + chunked Room transactions | A 10k-row import takes seconds; chunks are already durable, so WorkManager adds a dependency and IPC/state complexity for no reliability gain |
| A7 | No `appcompat` specified | No `appcompat` (Compose-level locale provider) | Avoids Activity recreation/flash on language switch (ADR-9) |
| A8 | `androidx.compose.material:material-icons-extended` | Kept, with a size fallback in Appendix B | Full icon set is convenient; R8 strips unused icons, and the fallback is defined if the 15 MB gate fails |
| A9 | Pinned toolchain: Kotlin 2.0.21 / KSP 2.0.21-1.0.28 / AGP 8.7.3 / Gradle 8.9 / BOM 2024.12.01 / compileSdk 35 | **Kotlin 2.4.21 / KSP 2.3.12 / AGP 8.13.2 / Gradle 8.14 / BOM 2026.06.01 / compileSdk 36** (targetSdk 35, minSdk 26 unchanged) | User chose **Full Material 3 Expressive** (2026-10-11): the Expressive API surface (`MaterialExpressiveTheme`, `MotionScheme.expressive()`, wavy indicators) exists **only** in the material3 1.4.0+ / Compose 1.8+ line, and Compose 1.11-era artifacts are compiled with a newer Kotlin metadata than 2.0.21 can consume. AGP 8.13.2 (last stable 8.x) because AGP 9.x removes the `com.android.application` + `org.jetbrains.kotlin.android` plugin model (built-in Kotlin). JDK 21 runtime, `jvmTarget = 17`. Per Phase-1 rule "ask before adding any dependency not in the approved set": no dependency **names** were added or removed; only pinned **versions** were bumped, and only where the chosen Expressive stack requires it |
| A10 | Hilt 2.52 | **Hilt 2.58** (`hilt-android`, `hilt-compiler`, `hilt-android-gradle-plugin`) | KSP 2.3.12 (forced by A9) removed `com.google.devtools.ksp.gradle.KspTaskJvm`; Hilt 2.52's AGP plugin `Class.forName`s that exact class, so the phase-1 CI build failed with `ClassNotFoundException: com.google.devtools.ksp.gradle.KspTaskJvm` ("The KSP plugin was detected to be applied but its task class could not be found"). Hilt 2.54 is the first release that targets the new `KspAATask`; **2.58 is the newest Hilt with AGP 8.x support** — 2.59+ hard-require AGP 9.0, which A9 excludes. Same artifact names — version bump only |
| A11 | Compose BOM 2026.09.00 (the A9 pin) | **Compose BOM 2026.06.01** → Compose **1.11.4** core (Material3 **1.4.0** unchanged) | Compose **1.12.x** AARs (`ui`, `foundation-layout`, `animation-core`, …) carry `aar-metadata` requiring **compileSdk ≥ 37** and **AGP ≥ 9.1.0** — both contradict A9 (AGP 8.13.2 last stable 8.x, compileSdk 36). `checkDebugAarMetadata` failed with 22 issues on BOM 2026.09.00 (Compose 1.12.1); **2026.06.01 is the newest BOM whose Compose core (1.11.4) still runs on AGP 8.6+ / compileSdk 35+** (verified from published aar-metadata: minCompileSdk 35, min AGP 8.6.0) while still shipping Material3 **1.4.0** (the Expressive stable; its own aar-metadata: minCompileSdk 35, min AGP 8.6.0). Version pin only — no dependency names added or removed |
| A12 | Hilt 2.58's transitive `org.jetbrains.kotlin:kotlin-metadata-jvm:2.2.20` (pinned by `dagger-compiler`, resolved on Hilt's own `hiltCompileOnly` configuration and the `ksp*` configurations) | Force `kotlin-metadata-jvm` to **2.4.21** (the Kotlin version, A9) on **all** configurations via `resolutionStrategy.force` | Dagger's `KotlinMetadata` reader throws on Kotlin 2.4 metadata: "Provided Metadata instance has version 2.4.0, while maximum supported version is 2.3.0" — failing `:app:hiltJavaCompile<Variant>`. Verified from the published jars that `kotlin-metadata-jvm:2.2.20` has `MetadataVersion.INSTANCE=[2,2,0]` (max 2.3.0) while `:2.4.21` has `INSTANCE=[2,4,0]` (max 2.5.0, reads 2.4.0). A **version force**, not a new dependency name; `kotlin-metadata-jvm` is processor-classpath-only and never packaged into the APK. Verified Hilt **2.59** also pins 2.2.20 (same failure) and 2.59.1+ require AGP 9 (A10), so staying on 2.58 + forced metadata reader is the minimal path |
| A13 | Material3 as delivered by Compose BOM 2026.06.01 (i.e. **1.4.0**, per A11) | **material3 1.5.0-alpha18**, declared in the version catalog and pinned with `version.ref = "material3"` on `compose-material3` so it overrides the BOM | Full Material 3 Expressive is a hard user decision (2026-10-11), and **material3 1.4.0 cannot deliver it**: in 1.4.0 the expressive surface (`MaterialExpressiveTheme`, the expressive `Shapes` constructor with `largeIncreased` / `extraLargeIncreased` / `extraExtraLarge`, the 14 `…Emphasized` type styles) is declared `internal`. 1.5.0-alpha18 exposes it publicly and still declares `aar-metadata` minCompileSdk 35 / minAGP 8.6.0, so it runs on the A9 toolchain (AGP 8.13.2 / compileSdk 36). Verified from published AAR metadata: alpha15–18 → 35 / 8.6.0, while **alpha19 and every later build → compileSdk 37 / AGP 9.1.0**, which A9 excludes — so alpha18 is the last usable release on this toolchain and this pin can only move together with an AGP 9 migration. Compose core stays 1.11.4 from the BOM (alpha15–18 require 1.11.0-beta02+, satisfied). **Same artifact name — version pin only.** Its expressive markers are `@Material3ExpressiveApi`, i.e. `@RestrictTo(LIBRARY_GROUP_PREFIX)` and **not** `@RequiresOptIn`, so no `@OptIn` is needed anywhere (adding one would be an "unnecessary opt-in" warning) and lint's public `RestrictedApi` check stays satisfiable: that detector reacts only to a *direct* `@RestrictTo` / `@VisibleForTesting` on the referenced element, and neither `MaterialExpressiveTheme` nor either `Shapes` constructor carries one — verified against the alpha18 sources |
| A14 | — (Phase 2 implementation notes; no `prompt.json` change) | See the notes in this cell | **Key count:** §11.2 and §11.3 contain **154** keys, not the 146 the prose claimed, and the flat list and the translation table agree exactly (154 distinct keys each). The generated `values`, `values-b+fr`, `values-b+ar` files and the `StringResourceParityTest` lower bound are therefore built on 154 (462 values). **Parity gate:** the "no value may equal its key" rule needs one documented exception — `years` is the English unit label ("12 years"), so its value is legitimately spelled exactly like its key; the test compares case-sensitively and allowlists that single key. **Construction:** `Typography` and `Shapes` each ship two `copy` overloads with identical signatures (ambiguous at the call site), so both are built through constructors with the full 29- / 8-argument parameter lists; for the same reason `Shapes(extraLarge = …)` alone is avoided (two constructors stay applicable). **Deferred keys:** `SettingsKeys` defines only `language` and `theme_mode`; `last_import_at` and `mapping_profile` move to Phase 5 together with their first consumer (Rule 6, no dead constants). **System-language sync:** `LocaleManager.systemLanguage()` reads the Android 13+ per-app language and `AppViewModel` persists it, instead of `LocaleManager.syncFromSystemLocaleManager()`, which would make `core` depend on `data`. **Colours:** both schemes are derived from the seed `#1B5E5A` with the Material 3 tonal model (CIELAB L\* tones, the baseline chroma ratios per palette, gamut-clamped per tone); the procedure reproduces the published baseline palette exactly (`#6750A4` → tone 40, `#EADDFF` → tone 90, `#21005D` → tone 10), which is the calibration check recorded in `Color.kt`. Error roles are deliberately left on the Material 3 baseline red so an error can never read as a brand-teal state |
| A15 | — (Phase 3 implementation notes; no `prompt.json` change) | See the notes in this cell | **Transitions:** `NavHost` takes non-composable transition callbacks, so `NavTransitions` captures the `MaterialTheme.motionScheme` specs once, in composition (`rememberNavTransitions`), and hands the resulting lambda holder to the host. There is no hardcoded 220 ms: §8's global rule makes *every* duration and easing come from the motion scheme via `AppMotion`, and the expressive spatial spring settles in that ~200 ms band. **Deep link:** the plan's verify step needs `app://employee/detail/1`, so `EmployeeDetail` carries `navDeepLink("app://employee/detail")` and `MainActivity` gained an `<intent-filter>` for scheme `app` / host `employee`; the "temporary debug button" alternative was rejected as placeholder code (Rule 6). **Inset contract:** the shell Scaffold owns the bottom inset (`systemBars.only(Bottom)`) and the bottom bar; each screen owns top + horizontal via `ScreenScaffold`. When the bar hides, the alpha18 Scaffold pads the content with the leftover inset (its `contentPadding.paddingHolder` branches on the bar measuring 0×0 — verified in the alpha18 sources), so detail screens never sit under the navigation bar and nothing is double-padded. **Files beyond the checklist:** `common/components/ScreenScaffold.kt` (the shared screen frame that owns the top bar + inset contract), `common/components/EmptyState.kt` (from the §3 component list — the truthful body of every placeholder, reused by Phase 6), `common/preview/PreviewVariants.kt` (the Rule-10 light/dark × LTR/RTL `@PreviewParameter` harness used by every new component). **Bottom-bar selection:** highlighted only while a top-level route is current; `selected` is `null` during the exit animation. **`EmployeeEdit` reachability:** routed now but unreachable from the UI until Phase 8; its optional `id: Long?` maps to the built-in `LongNullableType` when omitted (verified in navigation-common 2.8.5 `NavTypeConverter`) |

| A16 | Room pinned at 2.6.1 (ADR-4, §2, ADR-10, version catalog) and the §5.5 entity / §5.6 stat-row shapes | **Room bumped to 2.8.5** (user-approved 2026-10-12) + the §5.5/§5.6 corrections recorded here | Room 2.6.1 predates KSP2; the A9 toolchain (Kotlin 2.4.21 / KSP 2.3.12) runs KSP2 only, and AndroidX added KSP2 support in Room 2.7.0 ("Support for KSP2 is also added", 2.7.0 release notes). 2.8.5 (Sep 2026) is the current stable, built against compileSdk 36 / Kotlin 2.0+, and carries the 2025–26 wrapper fixes. Same artifact names — version bump only; the plan's Room Gradle plugin is still not used, schema export stays on the `ksp { arg("room.schemaLocation", …) }` argument. Companion §5 corrections: (1) `@TypeConverter`s are **class members** of `Converters` — Room only discovers converters on a class/object referenced via `@TypeConverters`, never top-level functions — declared on the entity, the DAO and both projections (the `@Database(typeConverters = …)` parameter is unusable under 2.8.5's KSP2 processor; hardening note below); they are **non-null receiver/return** (`LocalDate → Long`, `Long → LocalDate`) per the 2.7+ TypeConverter analyzer recommendation (b/193437407). **Post-hoc hardening (2026-10-12, CI):** Room 2.8.5's KSP2 processor did **not** apply the database-scope converters in CI — `kspDebugKotlin` failed with "Cannot figure out how to save/read this property" on every `LocalDate` field (entity + both projections) regardless of signature and despite the class compiling fine. As a result `Converters` is registered via `@TypeConverters(Converters::class)` on **each** consumer — the entity, the DAO, both projections — which is the only registration the 2.8.5 processor honors. The `@Database(typeConverters = …)` parameter is **omitted entirely**: the processor cannot read it, failing with `No parameter with name 'typeConverters' found` — reproduced with the plain `[Converters::class]` and with a fully-qualified literal (both variants tried/recorded here). Once discovery worked, CI also surfaced pre-existing `ChronoUnit.MONTHS.between(...) / 12` **Long**-result compile errors (masked by the earlier KSP failure) — fixed with `.toInt()` at all four sites (Employee, mapper, analytics ×2), keeping `serviceYears` `Int?`; (2) `EmployeeEntity` gains `fullNameKey` (a stored `trim().lowercase(Locale.ROOT)` column) plus the composite `(fullNameKey, birth_date)` index — §5.6's `countMatching` dedupe needs it to stay a single indexed, null-safe lookup, and the key never surfaces in the domain model; (3) `EmployeeStatRow` carries `id`, `fullName`, `position` in addition to the stat columns (and `observeStatRows` selects them) because `AnalyticsSnapshot.longestServing` needs identity + a display name from the same projection; (4) `GetEmployeeByIdUseCase` returns `Flow<Employee?>` via the DAO's `observeById` so the Phase-7 detail screen live-updates, while the one-shot §5.6 `getById` stays on the DAO for later phases; (5) projections bind through the SQL aliases (§5.6) so neither projection class needs `@ColumnInfo`. |

---

## Appendix B — Risk Register & Contingencies

| Risk | Trigger | Action (in order) |
|---|---|---|
| POI keeps the AAB over 15 MB | Phase 14 AAB ≥ 15 MB | 1) drop `material-icons-extended` → hand-written `AppIcons.kt` vectors (≈ −2 MB); 2) replace bundled Inter/Cairo with system fonts (≈ −1.2 MB); 3) confirm `androidResources.localeFilters` is active; 4) if still over, swap POI for `org.dhatim:fastexcel:0.18.4` (streaming reader + writer, ≈1 MB) and keep the `ExcelReader`/`ExcelWriter` APIs identical so only the data layer changes |
| Aalto classes missing at runtime | `ClassNotFoundException: com.fasterxml.aalto…` | Add `com.fasterxml:aalto-xml:1.2.2` to the catalog (pre-approved) and re-run the import; also confirm the three StAX system properties are set in `ZolvexApp.onCreate` **before** first POI use |
| R8 strips XMLBeans metadata | release-only import crash (`TypeSystemHolder`, schema classes) | Rules already keep `schemaorg_apache_xmlbeans.system.**` and `org.apache.xmlbeans.impl.schema.**`; if still failing, add `-keep class org.apache.xmlbeans.impl.schema.TypeSystemHolder` and re-test the release import |
| `SXSSFWorkbook` missing from the POI android port | compile error in `ExcelWriter` | Fall back to `XSSFWorkbook` with `workbook.close()` in `finally`, cap export at the currently filtered set, and add `android:largeHeap="true"` to the manifest for the export path; document the ceiling (≈50k rows) |
| Vico alpha API drift | compile error in `BarChartCard`/`DepartmentTab` | The used surface is confined to `CartesianChartHost`, `rememberCartesianChart`, `rememberColumnCartesianLayer`, `rememberStartAxis`, `rememberBottomAxis`, `CartesianChartModelProducer`, `runTransaction`; if a symbol moved, replace **only** `BarChartCard` with a Canvas bar chart (same public API), keeping the rest untouched |
| Robolectric + Room flakiness on CI | intermittent `EmployeeDaoTest` failures | Pin `@Config(sdk = [34])`, disable parallel test execution for that class, and if it still flakes move DAO tests to `androidTest/EmployeeDaoTest.kt` (keeping `testDebugUnitTest` free of DAO tests) |
| Export heap spike | `export_ms` > 8 s or OOM on a low-RAM device | Keep `SXSSFWorkbook(null, 100, false, false)`; reduce the window to 50; export only the filtered set; enable `largeHeap` as a last resort |
| Arabic text clipping | visual check at font scale 1.3 | Increase Cairo `lineHeight` to ×1.2 and `LineHeightStyle.Trim.None`; re-check the detail header and stat cards |

---

## Appendix C — Test fixture specification (`app/src/test/resources/excel/`)

| File | Sheet | Content |
|---|---|---|
| `en_sample.xlsx` | `Employees` | Row 1 headers: `Full Name, Date of Birth, Hire Date, Department, Position, Grade, Salary, Email, Phone, Address, Notes`; rows 2–11 = 10 valid employees |
| `fr_sample.xlsx` | `Employés` | Row 1 headers: `Nom complet, Date de naissance, Date d'embauche, Département, Poste, Grade, Salaire, E-mail, Téléphone, Adresse, Notes`; 10 valid rows |
| `ar_sample.xlsx` | `الموظفون` | Row 1 headers: `الاسم الكامل, تاريخ الميلاد, تاريخ التعيين, القسم, الوظيفة, الدرجة, الراتب, البريد الإلكتروني, الهاتف, العنوان, ملاحظات`; 10 valid rows |
| `merged_headers.xlsx` | `Sheet1` | Row 1 = merged title `وزارة — قائمة الموظفين`; rows 2–3 empty; row 4 = real headers (the AR set); rows 5–14 = 10 valid rows. Header row must be detected as index 3 |
| `malformed.xlsx` | `Employees` | 8 rows: 5 valid; row 4 = `Notes` column containing a date string; row 6 = empty `Full Name`; row 8 = `Salary` = `€1 234,56`. Expected: `imported == 6`, `failed == 3`, errors = `INVALID_DATE` (col Notes), `MISSING_REQUIRED` (col Full Name), `INVALID_NUMBER` (col Salary) |

Fixture expectations the tests assert:
- `en/fr/ar_sample.xlsx` → `confidence == 1.0f`, core fields mapped, `totalRowEstimate == 10`.
- `merged_headers.xlsx` → `headerRowIndex == 3`, core fields mapped.
- `malformed.xlsx` → exact counts above and each error's `sourceRow` (1-based sheet row).
- `LargeFileStreamingTest` generates a 10,000-row workbook in a temp file at test runtime (no fixture committed) and asserts completion, `imported == 10000`, and heap delta < 64 MB.

---

**End of plan.** Phase 1 starts at §4 → "Phase 1 — Scaffold", task 1. Report the `assembleDebug` result before proceeding to Phase 2.

