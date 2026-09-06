plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("jacoco")
    id("androidx.room") version "2.8.4"
}

android {
    namespace = "com.plath.scancard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.plath.scancard"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "com.plath.scancard.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // AI Pack配信に必須: コメントアウトすると .aab にpackが含まれず Play上で PACK_UNAVAILABLE(-2) になる
    assetPacks.add(":gemma-ai-pack")

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

jacoco {
    toolVersion = "0.8.13"
}

// IMP-01: Disable Jacoco instrumentation for JDK25 (major 69) until Jacoco supports it; reports remain stub
tasks.withType<Test> {
    extensions.findByType<JacocoTaskExtension>()?.isEnabled = false
}

// Jacoco report stub – Template only as Android coverage integration is complex in AGP 8+.
// Manual action required: Real coverage measurement requires buildTypes.debug.enableUnitTestCoverage / android Jacoco integration.
// Defaulted to disabled to avoid breaking the build, ensuring only task existence.
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Stub Jacoco report – manual configuration required for AGP 8+ (see docs/testing.md)"
    isEnabled = false
    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(file("${layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/html"))
        xml.outputLocation.set(file("${layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"))
    }
    // Template: Enable the following when enabling in the future
    // dependsOn("testDebugUnitTest")
    // classDirectories.setFrom(files("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug"))
    // sourceDirectories.setFrom(files("src/main/java"))
    // executionData.setFrom(fileTree(layout.buildDirectory.get().asFile) { include("**/*.exec", "**/*.ec") })
}

tasks.register<JacocoReport>("jacocoTestDebugUnitTestReport") {
    group = "verification"
    description = "Stub Jacoco report for debug unit tests – manual configuration required for AGP 8+"
    isEnabled = false
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// TODO(IMP-06) 6-1 Compose Preview Screenshot Testing implementation procedure (Comments only due to JVM8 constraints; application commented out due to gradle full execution prohibition)
// Option A: With version catalog (Recommended)
//   1. Add version to `gradle/libs.versions.toml`:
//      [versions]
//      compose-screenshot = "0.0.1-alpha08" # Use latest compatible with AGP 9.3.1 / Kotlin 2.1.10 / Compose BOM 2026.08.00
//      [plugins]
//      compose-screenshotTest = { id = "com.android.compose.screenshot", version.ref = "compose-screenshot" }
//   2. Add alias to `app/build.gradle.kts` plugins block:
//      plugins {
//          alias(libs.plugins.compose.screenshotTest) // TODO(IMP-06): Uncomment when enabling
//      }
// Option B: Without catalog (As this project uses direct declaration, this is also fine)
//   plugins {
//       id("com.android.compose.screenshot") version "0.0.1-alpha08" // TODO(IMP-06): Uncomment when enabling (version matches AGP)
//   }
// Option C: Tasks auto-generated after enabling screenshotTest sourceSet:
//   ./gradlew :app:validateScreenshotTest --info   # Difference verification (CI, threshold check)
//   ./gradlew :app:updateScreenshotTest --info     # Reference image update (run after local visual approval)
//   Output: Expected 9 sizes x theme x fontScale reference PNGs under `app/src/screenshotTest/resources/` or `app/build/screenshots/`
// Requirements: Run with compileSdk 37 / JVM17 / AGP 9.3.1 (screenshotTest task unavailable on JVM8). Ensure `./gradlew :app:assembleDebug` SUCCESS before enabling.
// References: See .kiro/skills/testing-setup/SKILL.md Step 8, adaptive/SKILL.md Step 1, docs/screenshot-testing.md.

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.compose.material:material-icons-extended")
    
    // ML Kit (unbundled Japanese OCR via Play Services)
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("com.google.mlkit:text-recognition-japanese:16.0.1")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")
    
    // LiteRT LM (Modern on-device AI)
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.16.1")
    implementation("com.google.android.play:ai-delivery:0.2.0-beta01")
    
    // Room
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-android-compiler:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0")
    implementation("androidx.hilt:hilt-work:1.4.0")
    ksp("androidx.hilt:hilt-compiler:1.4.0")

    // Navigation (Navigation 2 - Current)
    implementation("androidx.navigation:navigation-compose:2.9.8")
    // TODO(IMP-02): Navigation3 migration dependency addition procedure (Commented out due to JVM8 constraints; comments only due to gradle full execution prohibition)
    // Background: .kiro/skills/navigation-3/SKILL.md migration-guide / .kiro/skills/adaptive/SKILL.md Navigation3 mandatory
    //       Current compileSdk=37 / minSdk=26 meet Navigation3 requirements (compileSdk 36+ / minSdk 23+).
    //       Build impossible with Navigation3 dependencies in JVM8; this block remains commented. Enable with JVM17 + AGP 9.3.1.
    // Step 1: Add Kotlin Serialization plugin (Mandatory for @Serializable in NavKey)
    // TODO(IMP-02): Kotlin Serialization implementation procedure (Comments only due to JVM8 constraints)
    //   Option A: When adding to libs.versions.toml
    //     [versions]
    //     kotlin = "2.1.10"
    //     kotlinxSerializationJson = "1.8.1"
    //     nav3Core = "1.0.0" # version of navigation3-runtime/ui. Specify "1.0.0-alpha10" etc. if using alpha
    //     lifecycleViewmodelNav3 = "2.9.0" # Only if ViewModel integration is required
    //     [libraries]
    //     kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
    //     androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "nav3Core" }
    //     androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "nav3Core" }
    //     androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycleViewmodelNav3" }
    //     [plugins]
    //     kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
    //   Option B: Direct declaration (Add to top plugins block in app/build.gradle.kts):
    //     // TODO(IMP-02): id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
    //     // TODO(IMP-02): implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1") // Mandatory for @Serializable
    //   Uncomment the above when enabling, then Sync with JVM17 and run ./gradlew :app:assembleDebug
    // Step 2: Add to app/build.gradle.kts dependencies (Uncomment and Sync)
    // TODO(IMP-02): implementation("androidx.navigation3:navigation3-runtime:1.0.0-alpha")
    // TODO(IMP-02): implementation("androidx.navigation3:navigation3-ui:1.0.0-alpha")
    // TODO(IMP-02): implementation("androidx.navigation3:navigation3-runtime:1.0.0") // Stable (Recommended)
    // TODO(IMP-02): implementation("androidx.navigation3:navigation3-ui:1.0.0") // Stable (Recommended)
    //   // Only if using SavedStateHandle / toRoute() in ViewModel:
    //   // TODO(IMP-02): implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.9.0")
    //   // Or with BOM:
    //   // TODO(IMP-02): implementation(platform("androidx.navigation3:navigation3-bom:1.0.0"))
    //   // TODO(IMP-02): implementation("androidx.navigation3:navigation3-runtime")
    //   // TODO(IMP-02): implementation("androidx.navigation3:navigation3-ui")
    //   // If you want to specify alpha for Navigation3 migration, use the following:
    //   // TODO(IMP-02): implementation("androidx.navigation3:navigation3-runtime:1.0.0-alpha10")
    //   // TODO(IMP-02): implementation("androidx.navigation3:navigation3-ui:1.0.0-alpha10")
    // Step 3: Post-activation verification
    //   1. Confirm ./gradlew :app:assembleDebug SUCCESS (JVM17 mandatory)
    //   2. Uncomment `: NavKey` in ui/navigation/NavKeys.kt and verify import androidx.navigation3.runtime.NavKey resolves
    //   3. Replace ScanCardNavHost.kt with NavDisplay using steps in docs/navigation3-migration.md
    // Note: Navigation 2's navigation-compose:2.9.8 can coexist until migration is complete. Remove after full migration: implementation("androidx.navigation:navigation-compose:2.9.8")
    // References: See .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/migration-guide.md Step 1, get-started.md

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Network
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    
    // WorkManager
    val workVersion = "2.11.2"
    implementation("androidx.work:work-runtime-ktx:$workVersion")

    // IMP-07 7-4 DataStore implementation procedure (Comments only due to JVM8 constraints; dependency addition commented out due to gradle full execution prohibition)
    // Procedure: implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Use: filter_type persistence in StudyViewModel (DataStore<Preferences>). Addresses Req9.6 "filter persists across sessions".

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Tests - Unit
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("org.json:json:20231013")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("io.kotest:kotest-property:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("androidx.room:room-testing:2.8.4")
    testImplementation(platform("androidx.compose:compose-bom:2026.08.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.compose.ui:ui-test-manifest")

    // Tests - Instrumented / Hilt
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.60.1")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.60.1")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // TODO(IMP-05): Adaptive UI dependencies — Commented out due to JVM8 gradle constraints; enable after IMP-02 Navigation3 migration
    // Procedure: Uncomment the following 3 lines and Sync. Use 1.1.0 series compatible with BOM 2026.08.00
    // implementation("androidx.compose.material3.adaptive:adaptive:1.1.0")
    // implementation("androidx.compose.material3.adaptive:adaptive-layout:1.1.0")
    // implementation("androidx.compose.material3.adaptive:adaptive-navigation3:1.1.0")

    // IMP-09 Styles API experimental - see .kiro/skills/styles

    // IMP-01 done: deps implemented (mockk 1.13.13 etc) - see .kiro/skills/testing-setup

    // TODO(IMP-08): AppFunctions dependencies — targetSdk 36 satisfied, enable with JVM17 + AGP 9.3.2
    // Requirements: targetSdk 36+ / compileSdk 37+ / KSP / Hilt. (targetSdk=36, compileSdk=37 met)
    // Procedure:
    //   1. [Done] targetSdk 36 in app/build.gradle.kts
    //   2. Uncomment the following 2 lines and Sync (latest alpha10+ from maven.google.com recommended; alpha01 is legacy):
    //      implementation("androidx.appfunctions:appfunctions:1.0.0-alpha01")
    //      ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha01")
    //      Recommended: implementation("androidx.appfunctions:appfunctions:1.0.0-alpha10") + ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha10")
    //   3. Check KSP arguments: Confirm ksp { arg("appfunctions.aggregateAppFunctions", "true") } is added automatically if needed (AGP 9.3.1)
    //   4. Hilt integration: ScanCardAppFunctionService.kt uses @AndroidEntryPoint + @AppFunctionServiceEntryPoint. Existing Hilt 2.60.1 / KSP dependencies suffice.
    //   5. Build verification: Run ./gradlew :app:assembleDebug on JVM17. Confirm schema XML generation in app/build/generated/ksp/debug/
    //   6. Confirm automatic/manual registration of <service android:permission="android.permission.BIND_APP_FUNCTION_SERVICE" ...> in AndroidManifest.xml by KSP
    // Note: Before activation, only this comment exists. service/ScanCardAppFunctionService.kt uses comment templates to avoid compilation errors without dependencies.
    // TODO(IMP-08): implementation("androidx.appfunctions:appfunctions:1.0.0-alpha01")
    // TODO(IMP-08): ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha01")
}
