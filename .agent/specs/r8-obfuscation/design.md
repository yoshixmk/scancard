# Design Document

## Overview

This design defines the configuration for R8 obfuscation to meet Google Play's optimization target of 25% or higher. The implementation enables R8 in build.gradle.kts and configures appropriate keep rules in proguard-rules.pro to preserve application functionality after obfuscation.

## Architecture

### R8 Build Pipeline

```
Source Code → Kotlin Compiler → R8 Shrinker → Obfuscated APK/AAB
                         ↓
                  mapping.txt (for deobfuscation)
```

R8 performs three main operations:
1. **Shrinking** - Removes unused classes, methods, and fields
2. **Obfuscation** - Renames code elements to make reverse engineering difficult
3. **Optimization** - Applies code optimizations (inlining, dead code elimination)

## Design Decisions

### Decision 1: Enable R8 in build.gradle.kts

**Selection:** Enable `isMinifyEnabled = true` and `isShrinkResources = true` in the release build type.

**Rationale:**
- Required to meet Google Play optimization requirements (25%+)
- R8 performs shrinking, obfuscation, and optimization together
- `isShrinkResources = true` removes unused resources

**Changes to app/build.gradle.kts:**
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### Decision 2: ProGuard Rules Structure

**Selection:** Update proguard-rules.pro with optimized keep rules, removing redundant rules that conflict with consumer-rules.

**Rationale:**
- Existing file contains commented-out redundant rules
- Rules that can be delegated to consumer-rules should be removed
- App-specific classes (Application, Activity, Models) must be explicitly kept

**Structure:**
1. Keep Application and MainActivity (AndroidManifest.xml references)
2. Keep model/entity fields only (domain.model `<fields>`, Room `@Entity <fields>`, Converters, ExtractedCard); repositories and workers delegated to R8
3. Keep annotations and signatures (required by Hilt/Room) plus SourceFile/LineNumberTable for Play deobfuscation
4. Delegate library rules to consumer-rules where possible

### Decision 3: Library Rules Delegation

**Selection:** Delegate Hilt, Room, ML Kit, LiteRT LM keep rules to consumer-rules provided by libraries, avoiding broad keep rules.

**Rationale:**
- These libraries bundle consumer-rules in their AAR files
- Broad keep rules (e.g., `-keep class com.google.mlkit.**`) keep hundreds of classes, blocking size optimization
- Adding specific rules only when build errors occur is more efficient

## Implementation Details

### 1. build.gradle.kts Updates

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### 2. proguard-rules.pro Structure

The optimized proguard-rules.pro file follows this structure:

#### 2.1 Attributes and Signatures

```proguard
-keepattributes *Annotation*,Signature
-keepattributes EnclosingMethod
```

Required for Hilt and Room annotation processing.

#### 2.2 Application Components (AndroidManifest.xml references)

```proguard
-keep public class com.plath.scancard.ScanCardApplication { *; }
-keep public class com.plath.scancard.MainActivity { *; }
```

#### 2.3 Domain and Data Models

```proguard
-keep class com.plath.scancard.domain.model.** { <fields>; }
-keepclassmembers enum com.plath.scancard.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class com.plath.scancard.data.ml.ExtractedCard { <fields>; }
-keep class com.plath.scancard.data.local.Converters { *; }
```

Broad `data.** { *; }` is prohibited (delegated to R8 reachability analysis).

#### 2.4 Hilt Specific Rules

Hilt internals are delegated to the hilt-android consumer-rules. No broad
`dagger.hilt.**` / `javax.inject.**` keeps. Narrow generated-component keeps are
retained pending `analyzeReleaseR8Config` subsumed verdict.

#### 2.5 Room Specific Rules

```proguard
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { <fields>; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.plath.scancard.data.local.Converters { *; }
```

Note: Room 2.8.4 provides consumer-rules via room-runtime AAR. Entity `<fields>`
are kept because manual Migration SQL references column names as strings.

#### 2.6 WorkManager Rules

```proguard
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}
```

#### 2.7 Kotlin Coroutines

```proguard
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
```

#### 2.8 Compose

No broad Compose keep. The Compose compiler and library consumer-rules handle
preservation. Verified by Appium E2E launch and deck-create flows (release vs
debug parity).

### Decision 4: Verification Strategy

**Selection:** Run `./gradlew :app:assembleRelease` to verify the build succeeds and obfuscation is applied.

**Rationale:**
- Production build verification is most accurate
- If mapping.txt is generated, obfuscation has been applied

**Verification Steps:**
1. Run `./gradlew :app:assembleRelease`
2. Verify build completes without errors
3. Check that `app/build/outputs/mapping/release/mapping.txt` is generated
4. Verify APK/AAB size reduction

## Components and Interfaces

### Components Requiring Keep Rules

The following application components must be preserved in the obfuscated build:

1. **ScanCardApplication** - Entry point referenced in AndroidManifest.xml
   - Required for app initialization
   - Contains Hilt application annotation

2. **MainActivity** - Main launcher activity referenced in AndroidManifest.xml
   - Required for app launch
   - Contains Compose UI initialization

3. **Services** - Background services if any
   - Must be explicitly kept if referenced in manifest

### Interfaces Requiring Preservation

1. **Hilt Component Interfaces** - Required for dependency injection
   - Interfaces annotated with @Component or @Module
   - Must be preserved for DI to function

2. **Room Database** - Abstract class extending RoomDatabase
   - Required for database operations
   - Must not be obfuscated

3. **WorkManager Workers** - Classes extending Worker or ListenableWorker
   - Required for background task execution
   - Constructor signature must be preserved

4. **Compose Composables** - UI functions with @Composable annotation
   - May be obfuscated but should keep names for debugging
   - Compose compiler handles most preservation automatically

## Data Models

### Domain Models

The following domain model classes must be preserved for app functionality:

```kotlin
package com.plath.scancard.domain.model

// Card-related models
class Card { ... }
class CardScore { ... }
class CardIssuer { ... }

// Domain result types
class Result<out T> { ... }
class ScanResult { ... }
```

Preservation requirement: These classes are used for business logic and must keep their field names for JSON serialization.

### Data Transfer Objects (DTOs)

Network serialization DTOs that require preservation:

```kotlin
package com.plath.scancard.data.remote.dto

class CardDto { ... }
class ScanResponseDto { ... }
class ScoreDto { ... }
```

Preservation requirement: Field names must match API JSON response structure.

### Room Entities and DAOs

Database entities that require Room annotation preservation:

```kotlin
package com.plath.scancard.data.local.entity

@Entity(tableName = "cards")
class CardEntity { ... }

@Dao
interface CardDao { ... }
```

Preservation requirement:
- Entity class names and field names must be preserved
- DAO interfaces must not be obfuscated for Room to function
- Room annotations (@Entity, @Dao, @PrimaryKey) must be retained

### Preserved Packages

The following package patterns are preserved in proguard-rules.pro:

```proguard
-keep class com.plath.scancard.domain.model.** { *; }
-keep class com.plath.scancard.data.** { *; }
```

This ensures all domain models, DTOs, entities, and DAOs are preserved.

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Build Success After Obfuscation

*For any* release build configuration with R8 enabled, the build SHALL complete successfully without errors.

**Validates: Requirements 1.1, 1.2, 1.5, 9.1**

### Property 2: Mapping File Generation

*For any* successful release build with R8 enabled, the system SHALL generate a mapping.txt file in the outputs directory.

**Validates: Requirements 1.4, 9.3**

### Property 3: Application Class Preservation

*For any* obfuscated release build, the ScanCardApplication and MainActivity classes SHALL be preserved and accessible.

**Validates: Requirements 2.1, 2.2**

### Property 4: Domain Model Preservation

*For any* obfuscated release build, domain model classes SHALL be preserved for JSON serialization and database operations.

**Validates: Requirements 3.1, 3.2, 3.3**

### Property 5: Library Functionality Preservation

*For any* obfuscated release build, Hilt, Room, WorkManager, ML Kit, and LiteRT LM SHALL function correctly at runtime.

**Validates: Requirements 4.1, 4.2, 5.1, 5.2, 6.1, 7.1, 8.1**

## Error Handling

### Error: Missing Keep Rules

If R8 reports a class is removed that should be preserved, add specific keep rules:

```proguard
-keep class com.example.MyClass { *; }
```

### Error: Runtime Reflection Failures

If runtime errors occur due to obfuscation:
1. Use mapping.txt to deobfuscate stack traces
2. Add specific keep rules for affected classes
3. Consider using `-dontobfuscate` for specific classes as temporary workaround

### Error: Consumer-Rules Delegation Issues

If library functionality breaks after delegation:
1. Run `./gradlew :app:analyzeReleaseR8Config` to analyze
2. Add specific keep rules for affected classes
3. Do NOT use broad keep rules (e.g., `-keep class com.google.mlkit.**`)

## Testing Strategy

### Build Verification

To verify that R8 configuration works correctly, run the following build command:

```bash
./gradlew :app:assembleRelease
```

Expected results:
- Build completes without errors
- APK or AAB file is generated in `app/build/outputs/apk/release/` or `app/build/outputs/bundle/release/`

### Mapping File Verification

The mapping.txt file is the primary indicator that obfuscation has been applied:

1. Run release build: `./gradlew :app:assembleRelease`
2. Verify file exists: `app/build/outputs/mapping/release/mapping.txt`
3. Check contents: File should contain original-to-obfuscated class name mappings

If mapping.txt is generated, R8 has successfully:
- Shrunk unused code
- Obfuscated class and method names
- Generated deobfuscation mappings

### Runtime Verification Steps

After installing the obfuscated APK:

1. **Basic Functionality Test**
   - Launch the app
   - Verify main screen loads
   - Test card scanning functionality

2. **Database Operations**
   - Create new cards
   - Verify saved cards persist after app restart
   - Test card deletion

3. **Background Tasks**
   - Verify any scheduled work (if applicable) runs correctly
   - Check that notifications still work

4. **Network Operations**
   - Test API calls
   - Verify data serialization/deserialization works

### Size Reduction Verification

To verify the 25% Google Play optimization target:

1. Build debug APK: `./gradlew :app:assembleDebug`
2. Build release APK: `./gradlew :app:assembleRelease`
3. Compare sizes:
   - Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
   - Release APK: `app/build/outputs/apk/release/app-release.apk`

Release APK should be at least 25% smaller than debug APK.

### Troubleshooting Failed Verification

If build or runtime verification fails:

1. **Build fails with R8 errors**
   - Check proguard-rules.pro for missing keep rules
   - Add specific keep rules for missing classes
   - Run `./gradlew :app:analyzeReleaseR8Config` for analysis

2. **Runtime crashes**
   - Use mapping.txt to deobfuscate crash logs
   - Add keep rules for affected classes
   - Check consumer-rules are properly loaded

3. **Size reduction below 25%**
   - Enable `isShrinkResources = true`
   - Add resource shrinking rules
   - Consider removing unused dependencies

## Risk Analysis

### Risk 1: Runtime Reflection Issues

**Description:** Code using reflection (Hilt, Room, WorkManager) may break after obfuscation.

**Mitigation:**
- Add keepattributes rules for annotations and signatures
- Delegate Hilt/Room rules to consumer-rules
- Add specific keep rules when issues occur

### Risk 2: Build Time Increase

**Description:** R8 processing increases release build time.

**Impact:** Only affects release builds, not development builds. Acceptable for production releases.

### Risk 3: App Size Reduction Below 25%

**Description:** Optimization may not reach 25% target.

**Mitigation:**
- Enable `isShrinkResources = true` for resource removal
- Add additional optimization rules if needed
- Consider removing unused dependencies

## Acceptance Criteria Mapping

| Requirement | Implementation |
|-------------|----------------|
| Requirement 1.1 | build.gradle.kts: isMinifyEnabled = true |
| Requirement 1.2 | build.gradle.kts: isShrinkResources = true |
| Requirement 1.3 | build.gradle.kts: isDebuggable = false (explicit) |
| Requirement 1.4 | R8 generates mapping.txt by default |
| Requirement 2.1-2.3 | proguard-rules.pro: Application and MainActivity keeps, no broad wildcards |
| Requirement 3.1-3.3 | proguard-rules.pro: field-only keeps, no broad data keep |
| Requirement 4.1-4.3 | proguard-rules.pro: Hilt consumer-rules delegation, no broad keeps |
| Requirement 5.1-5.5 | proguard-rules.pro: Room keeps (database, entity fields, DAO, Converters) |
| Requirement 6.1-6.3 | ML Kit consumer-rules delegation, no broad keeps |
| Requirement 7 | proguard-rules.pro: LiteRT LM and AiPack class keeps |
| Requirement 8 | proguard-rules.pro: WorkManager keeps |
| Requirement 9 | assembleRelease success and mapping.txt verification |
| Requirement 10 | Google Play Console verification |
| Requirement 11.1-11.7 | Appium E2E release vs debug parity |

## Files to Modify

1. `app/build.gradle.kts` - Enable R8
2. `app/proguard-rules.pro` - Update keep rules