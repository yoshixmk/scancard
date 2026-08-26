package kotlinx.serialization

/**
 * IMP-02 Navigation3 Template Stub (to avoid breaking the build)
 *
 * This annotation is originally provided by `org.jetbrains.kotlinx:kotlinx-serialization-json`,
 * but because dependencies are commented out due to JVM8 constraints, this is a temporary stub
 * to prevent build errors caused by unresolved `import kotlinx.serialization.Serializable`
 * in `NavKeys.kt`.
 *
 * Activation Procedure (when migrating to Navigation3 with JVM17):
 *   1. Uncomment TODO(IMP-02) in `app/build.gradle.kts` and enable the following:
 *      - implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
 *      - plugins { id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10" }
 *   2. Delete this stub file (to avoid duplication with the actual library's Serializable):
 *      - `rm app/src/main/java/kotlinx/serialization/Serializable.kt`
 *   3. Confirm that duplication errors are resolved with `./gradlew :app:assembleDebug`.
 *
 * Note: This stub does not provide `KSerializer` or `serializer<T>()`. If Navigation3's `NavKeySerializer`
 *       is required, the actual library must be used. Current `NavKeys.kt` only applies `@Serializable`
 *       and does not generate serializers, so this stub is sufficient.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Serializable
