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
        // TODO(IMP-08): targetSdk = 36 昇格 (AppFunctions要件) — 実数値は JVM8 ビルド制約のため 35 のまま。昇格時は下記手順で 36 に変更する。
        // 要件: AppFunctions は targetSdk 36+ かつ compileSdk 37+ (Android 16) が前提。現行 compileSdk=37 は充足、targetSdk のみ未達。
        //       skill: .kiro/skills/appfunctions/SKILL.md Prerequisites / references/context.md 参照。
        // 昇格手順:
        //   1. 本行を targetSdk = 36 に変更 (数値のみ変更、本コメントは除去)
        //   2. compileSdk=37 が維持されていることを確認
        //   3. JVM17 + AGP 9.3.1 環境で ./gradlew :app:assembleDebug を実行し SUCCESS を確認
        //   4. Android 16+ (API 36) エミュ/実機でインストール検証: adb shell getprop ro.build.version.sdk が 36 以上
        //   5. adb shell cmd app_function list-app-functions で AppFunctions 登録を確認 (docs/appfunctions-discovery.md 8-6 参照)
        // 注意点 — targetSdk 35→36 Behavior Changes (要 Release Notes 照合):
        //   - 通知(POST_NOTIFICATIONS): 33+ で導入済みだが 36 では通知チャネル重要度・フォアグラウンドサービス通知の厳格化に注意。
        //     BackgroundTaskManager / NotificationHelper / ExtractionPreviewScreen(権限リクエスト)のフローを再検証。権限未付与時の WorkManager 通知フォールバック確認。
        //   - ストレージ/メディア: READ_EXTERNAL_STORAGE(maxSdkVersion=32) / READ_MEDIA_* は維持。36 で追加された権限・PhotoPicker 拡張があれば要対応。本アプリは SAF/Clipboard に委譲。
        //   - Edge-to-Edge: 35+ で強制される edge-to-edge が 36 でも継続。MainActivity.enableEdgeToEdge() + WindowInsets 消費が正しいか再スクショ (IMP-03)。
        //   - ForegroundService: 34+ で foregroundServiceType 必須。現状 FOREGROUND_SERVICE のみ宣言、WorkManager 使用のため影響小だが将来 FGS 使用時は type 明示。
        //   - プライバシー/セキュリティ: 36 のプライバシー変更 (Health Connect, Intent フィルタ厳格化等) は本アプリ非対象だが Play Console Target API 要件を満たすこと。
        //   - AppFunctions: targetSdk 36 未満では AppSearch への schema 登録が行われず Gemini/Agent から発見されない。昇格後に service/ScanCardAppFunctionService.kt の KSP 生成物を確認。
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.plath.scancard.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // assetPacks.add(":gemma-ai-pack") // Comment out for faster local dev/testing

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
    kotlinOptions {
        jvmTarget = "17"
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

// Jacoco report stub – AGP 8+ では Android カバレッジ連携が複雑なため雛形のみ。
// 要手動対応: 実カバレッジ計測には buildTypes.debug.enableUnitTestCoverage / android Jacoco 連携が必要。
// ビルドを壊さないようデフォルトは無効化し、タスク存在のみ保証する。
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
    // 雛形: 将来有効化する際は以下を有効化
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

// TODO(IMP-06) 6-1 Compose Preview Screenshot Testing 導入手順（JVM8制約でコメントのみ、gradleフル実行禁止のため適用はコメント留め）
// 手順A: catalogありの場合（推奨）
//   1. `gradle/libs.versions.toml` にバージョン追加:
//      [versions]
//      compose-screenshot = "0.0.1-alpha08" # AGP 9.3.1 / Kotlin 2.1.10 / Compose BOM 2026.08.00 と整合する最新を使う
//      [plugins]
//      compose-screenshotTest = { id = "com.android.compose.screenshot", version.ref = "compose-screenshot" }
//   2. `app/build.gradle.kts` plugins ブロックに alias 追加:
//      plugins {
//          alias(libs.plugins.compose.screenshotTest) // TODO(IMP-06): 有効化時はコメントを外す
//      }
// 手順B: catalogなしの場合（本プロジェクトは直書きのためこちらでも可）
//   plugins {
//       id("com.android.compose.screenshot") version "0.0.1-alpha08" // TODO(IMP-06): 有効化時はコメントを外す（versionは AGP と整合）
//   }
// 手順C: screenshotTest sourceSet 有効化後に自動生成されるタスク:
//   ./gradlew :app:validateScreenshotTest --info   # 差分検証（CI用、閾値チェック）
//   ./gradlew :app:updateScreenshotTest --info     # 参照画像更新（ローカルで目視承認後に実行）
//   成果物: `app/src/screenshotTest/resources/` or `app/build/screenshots/` 配下に 9サイズ×theme×fontScale の参照 PNG が生成される想定
// 要件: compileSdk 37 / JVM17 / AGP 9.3.1 で実行（JVM8では screenshotTest タスク実行不可）。有効化前に `./gradlew :app:assembleDebug` が SUCCESS であることを確認。
// 参考: .kiro/skills/testing-setup/SKILL.md Step8, adaptive/SKILL.md Step1, docs/screenshot-testing.md 参照。

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
    
    // ML Kit
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("com.google.mlkit:text-recognition-japanese:16.0.1")
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

    // Navigation (Navigation 2 - 現行)
    implementation("androidx.navigation:navigation-compose:2.9.8")
    // TODO(IMP-02): Navigation3 移行用依存追加手順 (JVM8制約でコメント留め、gradleフル実行禁止のためコメントのみ)
    // 背景: .kiro/skills/navigation-3/SKILL.md migration-guide / .kiro/skills/adaptive/SKILL.md Navigation3必須
    //       現行 compileSdk=37 / minSdk=26 は Navigation3 要件 (compileSdk 36+ / minSdk 23+) を充足。
    //       JVM8環境では Navigation3 依存追加でビルド不可のため、本ブロックはコメントのまま。JVM17 + AGP 9.3.1 で有効化すること。
    // 手順1: Kotlin Serialization plugin 追加 (NavKeyの @Serializable に必須)
    // TODO(IMP-02): Kotlin Serialization 導入手順 (JVM8制約でコメントのみ)
    //   Option A: libs.versions.toml に追加する場合
    //     [versions]
    //     kotlin = "2.1.10"
    //     kotlinxSerializationJson = "1.8.1"
    //     nav3Core = "1.0.0" # navigation3-runtime/ui のバージョン。alphaを利用する場合は "1.0.0-alpha10" 等を指定
    //     lifecycleViewmodelNav3 = "2.9.0" # ViewModel連携が必要な場合のみ
    //     [libraries]
    //     kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
    //     androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "nav3Core" }
    //     androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "nav3Core" }
    //     androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycleViewmodelNav3" }
    //     [plugins]
    //     kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
    //   Option B: 直書きする場合 (app/build.gradle.kts 先頭 plugins ブロックに追加):
    //     // TODO(IMP-02): id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
    //     // TODO(IMP-02): implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1") // @Serializable に必須
    //   有効化時は上記コメントを外し、JVM17で Sync + ./gradlew :app:assembleDebug を実行
    // 手順2: app/build.gradle.kts dependencies に追記 (コメントを外して Sync)
    // TODO(IMP-02): implementation("androidx.navigation3:navigation3-runtime:1.0.0-alpha")
    // TODO(IMP-02): implementation("androidx.navigation3:navigation3-ui:1.0.0-alpha")
    // TODO(IMP-02): implementation("androidx.navigation3:navigation3-runtime:1.0.0") // 安定版 (推奨)
    // TODO(IMP-02): implementation("androidx.navigation3:navigation3-ui:1.0.0") // 安定版 (推奨)
    //   // ViewModelで SavedStateHandle / toRoute() を使う場合のみ:
    //   // TODO(IMP-02): implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.9.0")
    //   // または BOM利用時:
    //   // TODO(IMP-02): implementation(platform("androidx.navigation3:navigation3-bom:1.0.0"))
    //   // TODO(IMP-02): implementation("androidx.navigation3:navigation3-runtime")
    //   // TODO(IMP-02): implementation("androidx.navigation3:navigation3-ui")
    //   // Navigation3 移行の alpha を明示したい場合はタスク指定の以下を使用 (ただし最新安定版 1.0.0 を推奨):
    //   // TODO(IMP-02): implementation("androidx.navigation3:navigation3-runtime:1.0.0-alpha10")
    //   // TODO(IMP-02): implementation("androidx.navigation3:navigation3-ui:1.0.0-alpha10")
    // 手順3: 有効化後の検証
    //   1. ./gradlew :app:assembleDebug が SUCCESS であることを確認 (JVM17必須)
    //   2. ui/navigation/NavKeys.kt の `: NavKey` コメントを外し import androidx.navigation3.runtime.NavKey が解決されることを確認
    //   3. docs/navigation3-migration.md の手順で ScanCardNavHost.kt を NavDisplay に置換
    // 注意: Navigation2 の navigation-compose:2.9.8 は移行完了まで併存可。完全移行後に削除: implementation("androidx.navigation:navigation-compose:2.9.8")
    // 参考: .kiro/skills/navigation-3/references/android/guide/navigation/navigation-3/migration-guide.md Step1, get-started.md

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Network
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    
    // WorkManager
    val workVersion = "2.11.2"
    implementation("androidx.work:work-runtime-ktx:$workVersion")

    // IMP-07 7-4 DataStore導入手順（JVM8制約でコメントのみ、gradleフル実行禁止のため依存追加はコメントに留める）
    // 手順: implementation("androidx.datastore:datastore-preferences:1.1.1")
    // 用途: StudyViewModel の filter_type 永続化（DataStore<Preferences>）。Req9.6 "filter persists across sessions" 対応。
    // 有効化時: StudyViewModelのTODOコメント（クラス冒頭/setFilter/init）を実装し、HiltでDataStoreを注入する。

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

    // TODO(IMP-05): Adaptive UI 依存 — JVM8 gradle制約でコメント留め、IMP-02 Navigation3移行後に有効化
    // 手順: 下記3行のコメントを外し Sync する。versions は BOM 2026.08.00 と整合する 1.1.0 系を使用
    // implementation("androidx.compose.material3.adaptive:adaptive:1.1.0")
    // implementation("androidx.compose.material3.adaptive:adaptive-layout:1.1.0")
    // implementation("androidx.compose.material3.adaptive:adaptive-navigation3:1.1.0")

    // TODO(IMP-09): Styles API 実験導入手順 — gradleフル実行禁止のためコメント雛形のみ (P3任意, experimental)
    // 背景: .kiro/skills/styles/SKILL.md は foundation:1.12.0-alpha01 / BOM 2026.04.01 + compileSdk 37 + ExperimentalFoundationStyleApi opt-in を要求。
    //       本プロジェクトは app/build.gradle.kts:12 compileSdk=37 は充足。BOM 2026.08.00 は 2026.04.01 より新しくfoundationも含むが、
    //       明示的に foundation:1.12.0-alpha01 を要求する場合は下記 Option B を併用できる。TextField等の Material3 は Styles対象外。
    // 前提チェック:
    //   - compileSdk = 37 充足 (本ファイル android{ compileSdk=37 } を維持)
    //   - kotlin jvmTarget = 17 充足 (kotlinOptions.jvmTarget="17" / compilerOptions.jvmTarget 参照)
    // 有効化手順 (JVM17 + AGP 9.3.1 で ./gradlew :app:assembleDebug が SUCCESS であることを事前確認):
    //   Option A — BOM 更新で foundation を間接取得 (推奨, BOM 2026.04.01+ は foundation 1.12.0-alpha01 を内包):
    //     dependencies {
    //         implementation(platform("androidx.compose:compose-bom:2026.04.01")) // 現行 2026.08.00 は既に 2026.04.01 より新しいため維持でも可
    //         // BOM更新時は 2026.08.00 → 2026.04.01 以上へ (ただし Downgradeにならないよう最新を維持するなら変更不要)
    //     }
    //   Option B — foundation を明示追加 (alpha を直接指定したい場合, BOMと併用可):
    //     dependencies {
    //         implementation("androidx.compose.foundation:foundation:1.12.0-alpha01")
    //         // 注意: BOMと併用時は BOMのバージョン解決が優先されるため、必要なら BOM を 2026.04.01 以上に上げる
    //     }
    //   Option C — コンパイラ opt-in (必須, experimental API):
    //     // build.gradle.kts androidブロック外 または kotlinブロック内で有効化:
    //     kotlin {
    //         compilerOptions {
    //             jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    //             freeCompilerArgs.add("-opt-in=androidx.compose.foundation.style.ExperimentalFoundationStyleApi")
    //         }
    //     }
    //     // 代替 (AGP 9.3.1 + Kotlin 2.1系で kotlinOptions 使用時):
    //     // kotlinOptions { freeCompilerArgs += "-opt-in=androidx.compose.foundation.style.ExperimentalFoundationStyleApi" }
    //     // 既存の kotlinOptions { jvmTarget="17" } と併存させる場合は kotlin { compilerOptions { ... } } に一本化推奨
    //   検証:
    //     1. 上記 Option A/B + C をアンコメントし Sync
    //     2. import androidx.compose.foundation.style.Style が解決できることを確認 (IDEで赤波線が消える)
    //     3. ui/theme/ComponentStyles.kt のコメント雛形を有効化しビルド
    //     4. ./gradlew :app:assembleDebug で SUCCESS、スクショ差分0を確認 (IMP-09 9-4 参照)
    //   ロールバック: 上記3行を再コメントし Sync すれば現行 Material3直書きに復帰 (Theme.kt は無変更のため影響なし)
    //   リスク: foundation 1.12.0-alpha01 は alpha/experimental。Material3コンポーネント(Card/Button等)は Styles非対応、カスタムコンポーネントのみ適用可
    //   参考: .kiro/skills/styles/SKILL.md Prerequisites / Step 2-3、docs/styles-migration.md

    // TODO(IMP-01): testImplementation("io.mockk:mockk:1.13.8") — 既存と整合: 現行 1.13.13 に更新済み (Kotlin 2.x 互換のため 1.13.8→1.13.13)
    //   有効化手順: 上記 testImplementation 行のコメントを外す。MockKはFake優先で必要な場合のみ使用。
    // TODO(IMP-01): androidTestImplementation("com.google.dagger:hilt-android-testing:2.60.1") — Hilt Instrumented Test用
    //   手順: androidTest sourceSet に追記、KSP とペアで有効化。HiltTestRunner / HiltAndroidRule と併用。
    // TODO(IMP-01): kspAndroidTest("com.google.dagger:hilt-compiler:2.60.1") — HiltのKSPコード生成 (androidTest)
    //   手順: kspAndroidTest 構成で hilt-compiler を追加。HiltTestApplication生成に必須。
    // TODO(IMP-01): testImplementation("io.kotest:kotest-property:5.9.1") — Property-based Testing
    //   手順: test sourceSet に追記。Kotest Property 5.9.1 は BOMなし直指定。現行は JUnit4 Parameterizedで代替しつつ依存は導入済み。
    // TODO(IMP-01): testImplementation("org.robolectric:robolectric:4.12.2") — JVM上でAndroid FrameworkをFake
    //   手順: testImplementation に追記 + testOptions.unitTests.isIncludeAndroidResources=true で有効化。
    // TODO(IMP-01): testImplementation("androidx.test:core:1.6.1") — AndroidX Test Core (Robolectric併用)
    //   手順: testImplementation に追記。既存は 1.7.0 に更新済み (1.6.1と互換、最新で整合)。core-testing / espresso 等と併用。
    // TODO(IMP-01): jacoco 導入手順 — plugins { id("jacoco") } + jacoco { toolVersion = "0.8.12" } + tasks.register<JacocoReport>("jacocoTestReport") { isEnabled=false } (スタブ)
    //   詳細は 1-7 のコメントブロックを参照。AGP 8+ では android.buildTypes.debug.enableUnitTestCoverage 連携が別途必要。

    // TODO(IMP-01) 1-7 jacoco導入コメント:
    // 手順1: plugins { id("jacoco") } — 既に app/build.gradle.kts:7 に適用済み
    // 手順2: jacoco { toolVersion = "0.8.12" } — 本ファイル 62-64行に配置済み (0.8.12 は AGP 8.x / JDK17 と整合)
    // 手順3: tasks.register<JacocoReport>("jacocoTestReport") { isEnabled=false; reports{ xml, html } } — 69-94行にスタブ登録済み
    // 手順4: 有効化時は isEnabled=true + dependsOn("testDebugUnitTest") + classDirectories/sourceDirectories/executionData を正しく紐付け、CI閾値 80% (business logic) を `jacocoTestCoverageVerification` で設定
    // 注意: JVM8制約下では実際のコバンテージレポート生成は不可のためスタブ留め。JVM17で `./gradlew :app:jacocoTestReport` 実行。

    // TODO(IMP-08): AppFunctions 依存 — JVM8 gradle制約でコメント留め、targetSdk 36 昇格時に有効化
    // 要件: targetSdk 36+ / compileSdk 37+ / KSP / Hilt。skill: .kiro/skills/appfunctions/references/implementation-configuration.md Step1 参照。
    // 手順:
    //   1. app/build.gradle.kts:14 の targetSdk を 36 に昇格 (本ファイル先頭の TODO(IMP-08) コメント参照)
    //   2. 下記2行のコメントを外し Sync (version は maven.google.com の最新 alpha10+ を推奨、タスク指定の alpha01 は legacy):
    //      implementation("androidx.appfunctions:appfunctions:1.0.0-alpha01")
    //      ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha01")
    //      推奨最新: implementation("androidx.appfunctions:appfunctions:1.0.0-alpha10") + ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha10")
    //   3. KSP 引数を確認: ksp { arg("appfunctions.aggregateAppFunctions", "true") } が必要に応じて自動付与されることを確認 (AGP 9.3.1)
    //   4. Hilt 連携: ScanCardAppFunctionService.kt は @AndroidEntryPoint + @AppFunctionServiceEntryPoint を併用。Hilt 2.60.1 / KSP 依存は既存で充足。
    //   5. ビルド検証: JVM17 で ./gradlew :app:assembleDebug を実行。app/build/generated/ksp/debug/ 配下に schema XML が生成されることを確認
    //   6. AndroidManifest.xml に <service android:permission="android.permission.BIND_APP_FUNCTION_SERVICE" ...> が KSP により自動/手動登録されることを確認
    // 注意: 有効化前は本コメントのみ。依存を外したまま app/src/main/java/com/example/scancard/service/ScanCardAppFunctionService.kt はコメント雛形でコンパイルエラーを回避。
    // TODO(IMP-08): implementation("androidx.appfunctions:appfunctions:1.0.0-alpha01")
    // TODO(IMP-08): ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha01")
}
