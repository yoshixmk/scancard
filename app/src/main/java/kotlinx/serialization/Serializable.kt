package kotlinx.serialization

/**
 * IMP-02 Navigation3 雛形用スタブ (ビルドを壊さないため)
 *
 * 本来は `org.jetbrains.kotlinx:kotlinx-serialization-json` により提供される annotation だが、
 * JVM8制約で依存をコメント留めしているため、NavKeys.kt の `import kotlinx.serialization.Serializable`
 * が解決せずビルドエラーになるのを防ぐための一時スタブ。
 *
 * 有効化手順 (JVM17で Navigation3 移行時):
 *   1. app/build.gradle.kts の TODO(IMP-02) をアンコメントし、下記を有効化:
 *      - implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
 *      - plugins { id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10" }
 *   2. 本スタブファイルを削除する (実ライブラリの Serializable と重複するため)
 *      - `rm app/src/main/java/kotlinx/serialization/Serializable.kt`
 *   3. `./gradlew :app:assembleDebug` で重複エラーが解消されたことを確認
 *
 * 注意: このスタブは `KSerializer` / `serializer<T>()` 等は提供しない。Navigation3 の `NavKeySerializer`
 *       が必要な場合は実ライブラリが必須。現状の NavKeys.kt は `@Serializable` の付与のみで
 *       シリアライザ生成は行わないため本スタブで十分。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Serializable
