# Implementation Plan: R8 Obfuscation

## Overview

This plan enables R8 obfuscation for the ScanCard Android application to meet Google Play's 25% optimization target. The implementation covers enabling R8 in build.gradle.kts, updating ProGuard rules, and verifying build success and app functionality.

## Tasks

- [x] 1. Enable R8 in build.gradle.kts
  - Update release build type to enable code shrinking and resource shrinking
  - Set isMinifyEnabled = true
  - Set isShrinkResources = true
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 2. Update proguard-rules.pro with optimized keep rules
  - [x] 2.1 Add keepattributes for annotations and signatures
    - Add -keepattributes *Annotation*,Signature,EnclosingMethod
    - _Requirements: 4.2, 5.1_

  - [x] 2.2 Keep Application and Activity classes
    - Add -keep public class com.plath.scancard.ScanCardApplication { *; }
    - Add -keep public class com.plath.scancard.MainActivity { *; }
    - _Requirements: 2.1, 2.2_

  - [x] 2.3 Keep domain and data models (fields only)
    - Add -keep class com.plath.scancard.domain.model.** { <fields>; }
    - Add -keepclassmembers enum com.plath.scancard.domain.model.** (values/valueOf)
    - Add -keep class com.plath.scancard.data.ml.ExtractedCard { <fields>; }
    - Broad data.** { *; } removed (delegated to R8 reachability analysis)
    - _Requirements: 3.1, 3.2, 3.3_

  - [x] 2.4 Add Hilt-specific rules (consumer-rules delegation)
    - Broad dagger.hilt.** / javax.inject.** / dontwarn removed
    - Narrow generated-component keeps retained pending analyzeReleaseR8Config
    - Verified by Appium E2E launch + database flows (release vs debug parity)
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 2.5 Add Room-specific rules
    - Add -keep class * extends androidx.room.RoomDatabase
    - Add -keep @androidx.room.Entity class * { <fields>; }
    - Add -keep @androidx.room.Dao interface *
    - Add -keep class com.plath.scancard.data.local.Converters { *; }
    - _Requirements: 5.2, 5.3, 5.4, 5.5_

  - [x] 2.6 Add WorkManager rules
    - Add -keep class * extends androidx.work.Worker
    - Add -keep class * extends androidx.work.ListenableWorker
    - _Requirements: 8.2, 8.3_

  - [x] 2.7 Add LiteRT LM (Gemma) keep rules
    - Add -keep class com.google.ai.edge.litertlm.Engine { *; }
    - Add -keep class com.google.ai.edge.litertlm.EngineConfig { *; }
    - Add -keep class com.google.ai.edge.litertlm.Backend { *; }
    - Add -keep class com.google.ai.edge.litertlm.Backend$CPU { *; }
    - Add -keep class com.google.ai.edge.litertlm.SamplerConfig { *; }
    - Add -keep class com.google.ai.edge.litertlm.Conversation { *; }
    - Add -keep class com.google.ai.edge.litertlm.Contents { *; }
    - Add -keep class com.google.ai.edge.litertlm.MessageCallback { *; }
    - Add -keep class com.google.android.play.core.aipacks.AiPackManager { *; }
    - Add -keep class com.google.android.play.core.aipacks.AiPackManagerFactory { *; }
    - Add -keep class com.google.android.play.core.aipacks.AiPackState { *; }
    - Add -keep class com.google.android.play.core.aipacks.AiPackStateUpdateListener { *; }
    - Add -keep class com.google.android.play.core.aipacks.model.AiPackStatus { *; }
    - _Requirements: 7.2, 7.3, 7.4, 7.5, 7.9_

  - [x] 2.8 Add Kotlin and Compose rules (minimal)
    - Add -keepnames for kotlinx.coroutines MainDispatcherFactory / CoroutineExceptionHandler
    - Broad androidx.compose.** keep removed (delegated to Compose compiler + consumer-rules)
    - Verified by Appium E2E launch + deck create flows (release vs debug parity)
    - _Requirements: 6.1_

- [x] 3. Verify Build Success
  - [x] 3.1 Run release build
    - Execute ./gradlew :app:assembleRelease
    - Verify build completes without errors
    - _Requirements: 9.1_

  - [x] 3.2 Verify mapping.txt generation
    - Check app/build/outputs/mapping/release/mapping.txt exists
    - Verify file contains obfuscated class mappings
    - _Requirements: 1.4, 9.3_

  - [ ] 3.3 Run R8 config analysis
    - Execute ./gradlew :app:analyzeReleaseR8Config
    - Review output for kept, shrunk, and subsumed items
    - Remove narrow Hilt keeps if reported as subsumed
    - _Requirements: 9.5_

- [ ] 4. Verify App Functionality
  - [x] 4.1 Install and launch obfuscated APK
    - Install release APK on device or emulator
    - Verify app launches without crashes
    - _Requirements: 11.1_

  - [x] 4.2 Verify database operations
    - Test deck creation and retrieval via Room-backed list (verified on release)
    - _Requirements: 11.1_

  - [x] 4.3 Verify Hilt dependency injection
    - Verify launch and all injected flows (deck create, Room access) work on release
    - _Requirements: 11.3_

  - [ ] 4.4 Verify ML Kit functionality
    - Test document scanning
    - Test text recognition
    - _Requirements: 11.5_

  - [ ] 4.5 Verify data persistence across restart
    - Blocked: deck missing after force-stop restart reproduces identically on debug baseline (pre-existing issue, not an R8 regression)
    - _Requirements: 11.1_

  - [ ] 4.6 Verify WorkManager and LiteRT LM on release build
    - Blocked: existing specs require run-as (unavailable on non-debuggable release); needs a debuggable R8 verification build or run-as alternative
    - _Requirements: 11.4, 11.6_

- [x] 5. Verify Google Play Optimization
  - [x] 5.1 Compare APK sizes
    - Build debug APK: ./gradlew :app:assembleDebug
    - Build release APK: ./gradlew :app:assembleRelease
    - Calculate size reduction percentage
    - _Requirements: 10.1, 10.2, 10.3_

  - [ ] 5.2 Verify in Google Play Console
    - Upload AAB to internal testing track
    - Check optimization percentage in App Bundle Explorer
    - Verify optimization meets 25% target
    - _Requirements: 10.1_

## Notes

- Each task references specific requirements for traceability
- Release vs debug Appium parity is the R8 regression gate (Requirement 11.7)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["3.3"] },
    { "id": 4, "tasks": ["4.1", "4.2", "4.3", "4.4", "4.5", "4.6"] },
    { "id": 5, "tasks": ["5.1"] },
    { "id": 6, "tasks": ["5.2"] }
  ]
}
```