# ScanCard ProGuard Rules
# NOTE: release ビルドは isMinifyEnabled=true + isShrinkResources=true。
# 広域keepは削減効率を下げるため使用禁止。consumer-rules に委譲できるものは委譲し、
# 不足分のみクラス単位で追加する。変更後は ./gradlew :app:assembleRelease と
# Appium E2E (APK_PATH=release APK) で動作確認すること。

# Preserve annotations, signatures and crash-report info required by Hilt, Room and Play deobfuscation
-keepattributes *Annotation*,Signature,EnclosingMethod,SourceFile,LineNumberTable

# Hilt - Rules are typically provided by the library, but keeping generic ones for safety
# TODO(R8): 要 analyzeReleaseR8Config で冗長判定 – REDUNDANT-RULES.md Case: Android Components / AAPT2が自動keep。Hilt/Room/AGPがconsumer-rules提供。
# 以下7ルールは AAPT2/R8 が AndroidManifest.xml から自動保持するため冗長候補。現状コメントアウト＋注記に留める (将来削除)。
# -keep public class * extends android.app.Service
# -keep public class * extends android.app.Application
# -keep public class * extends android.app.Activity
# -keep public class * extends android.content.BroadcastReceiver
# -keep public class * extends android.content.ContentProvider
# -keep public class * extends android.app.backup.BackupAgentHelper
# -keep public class * extends android.preference.Preference
# 推奨縮小案: 削除 (AAPT2委譲)。特定クラスが必要なら最小keep例:
-keep public class com.plath.scancard.ScanCardApplication { *; }
-keep public class com.plath.scancard.MainActivity { *; }
# 検証: ./gradlew :app:analyzeReleaseR8Config で subsumed 判定を確認。

# Room
# TODO(R8): 要 analyzeReleaseR8Config で冗長判定 – Room 2.8.4 はAARがconsumer-rules提供 (REDUNDANT-RULES.md Case: Room Database)。手動keep不要。
# -keep class * extends androidx.room.RoomDatabase
# 推奨: 削除 (Room consumer-rulesに委譲)。AppDatabase_Impl はR8が自動保持。
# -keep class com.plath.scancard.data.local.entities.** { *; }
# 推奨縮小案 (広域 {*; } → アノテーション or フィールド限定):
# -keep @androidx.room.Entity class * { *; }
# またはクラス単位:
# -keep class com.plath.scancard.data.local.entities.Card { <fields>; }
# -keep class com.plath.scancard.data.local.entities.Deck { <fields>; }
# -keep class com.plath.scancard.data.local.entities.Scan { <fields>; }
# 現PRはコメントアウト＋縮小案併記 (将来 analyzeReleaseR8Config で Keeps件数を確認し確定)。
# Room (consumer-rules に委譲 + Migration SQL の列名安定のため Entity フィールド保持)
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { <fields>; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.plath.scancard.data.local.Converters { *; }

# WorkManager (Task 2.6)
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# LiteRT LM (Gemma)
# TODO(R8): 広域keepをクラス単位に精緻化 – GemmaCardExtractor.kt で使用は Engine/EngineConfig/Backend/Conversation 等10クラス程度のみ。Lv1 Package-Wide Wildcardsは最悪影響。
# -keep class com.google.ai.edge.litertlm.** { *; }
# 推奨縮小案 (実使用クラスに限定 – AAR展開でクラス名を javap で検証):
# -keep class com.google.ai.edge.litertlm.Engine { *; }
# -keep class com.google.ai.edge.litertlm.EngineConfig { *; }
# -keep class com.google.ai.edge.litertlm.Conversation { *; }
# -keep class com.google.ai.edge.litertlm.ConversationConfig { *; }
# -keep class com.google.ai.edge.litertlm.SamplerConfig { *; }
# -keep class com.google.ai.edge.litertlm.Backend { *; }
# -keep class com.google.ai.edge.litertlm.Backend$CPU { *; }
# -keep class com.google.ai.edge.litertlm.Contents { *; }
# -keep class com.google.ai.edge.litertlm.Content { *; }
# -keep class com.google.ai.edge.litertlm.MessageCallback { *; }
# -keep class com.google.ai.edge.litertlm.Message { *; }
# さらに縮小可能なら allowobfuscation 修飾: -keep,allowobfuscation class com.google.ai.edge.litertlm.Engine { <init>(...); }
# JNIリフレクションがあれば追加: -keep class com.google.ai.edge.litertlm.LlmModel { *; } # 存在は AAR確認要
# LiteRT LM (Gemma) - Task 2.7: GemmaCardExtractor.kt / ModelManager.kt の実使用クラスのみ
-keep class com.google.ai.edge.litertlm.Engine { *; }
-keep class com.google.ai.edge.litertlm.EngineConfig { *; }
-keep class com.google.ai.edge.litertlm.Backend { *; }
-keep class com.google.ai.edge.litertlm.Backend$CPU { *; }
-keep class com.google.ai.edge.litertlm.SamplerConfig { *; }
-keep class com.google.ai.edge.litertlm.Conversation { *; }
-keep class com.google.ai.edge.litertlm.ConversationConfig { *; }
-keep class com.google.ai.edge.litertlm.Contents { *; }
-keep class com.google.ai.edge.litertlm.Content { *; }
-keep class com.google.ai.edge.litertlm.MessageCallback { *; }
-keep class com.google.ai.edge.litertlm.Message { *; }
-keep class com.google.android.play.core.aipacks.AiPackManager { *; }
-keep class com.google.android.play.core.aipacks.AiPackManagerFactory { *; }
-keep class com.google.android.play.core.aipacks.AiPackState { *; }
-keep class com.google.android.play.core.aipacks.AiPackStateUpdateListener { *; }
-keep class com.google.android.play.core.aipacks.model.AiPackStatus { *; }
# Keep internal AI components that use reflection or JNI
# TODO(R8): 広域 → 使用クラス限定 – ModelManager.kt で使用は AiPackManager / AiPackManagerFactory / AiPackStatus のみ。
# -keep class com.google.android.play.core.aipacks.** { *; }
# 推奨縮小案:
# -keep class com.google.android.play.core.aipacks.AiPackManagerFactory { *; }
# -keep class com.google.android.play.core.aipacks.model.AiPackStatus { *; }
# または consumer-rules に委譲し削除。

# ML Kit
# TODO(R8): 冗長 – MLKitはAARがconsumer-rules提供のため委譲可能。com.google.mlkit.** {*; } はPlay Services数百クラスを保持しShrinking阻害最大。
# -keep class com.google.mlkit.** { *; }
# 推奨: 削除 (consumer-rulesに委譲)。リリースで破損した場合のみ最小keep:
# -keep class com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions { *; }
# -keep class com.google.mlkit.vision.documentscanner.GmsDocumentScanning { *; }
# -keep class com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult { *; }
# -keep class com.google.mlkit.vision.text.TextRecognition { *; }
# -keep class com.google.mlkit.vision.common.InputImage { *; }
# -keep class com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions { *; }
# -keep class com.google.android.gms.tasks.** { *; }
# 推奨: 削除 – play-services-tasks は consumer-rules 提供、kotlinx-coroutines-play-services の await() はリフレクション不使用。
# 検証: DocumentScannerManager.kt / TextRecognitionManager.kt / ModelManager.kt の Task.await() がR8後も動作することを確認。

# General Data Models (if any use reflection/JSON)
# TODO(R8): 精緻化 – ExtractedCard は手動JSONパース (CardResponseParser) でリフレクション不要。domain/modelはGson不使用。
# -keep class com.plath.scancard.data.ml.ExtractedCard { *; }
# 推奨縮小案:
# -keep class com.plath.scancard.data.ml.ExtractedCard { <fields>; }
# -keep class com.plath.scancard.domain.model.** { *; }
# 推奨縮小案:
# -keep class com.plath.scancard.domain.model.** { <fields>; }
# または allowobfuscation で縮小: -keep,allowobfuscation class com.plath.scancard.domain.model.** { <fields>; }

# Domain models (Task 2.3): フィールド名のみ保持。メソッドは shrink/難読化対象。
# -keep class com.plath.scancard.domain.model.** { *; } は過剰 (削減阻害) のため廃止。
-keep class com.plath.scancard.domain.model.** { <fields>; }
-keepclassmembers enum com.plath.scancard.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
# Manual JSON parse target (CardResponseParser はリフレクション不使用だがフィールド名安定のため)
-keep class com.plath.scancard.data.ml.ExtractedCard { <fields>; }
# NOTE: -keep class com.plath.scancard.data.** { *; } は過剰のため削除。
# repository/worker/ml-manager は consumer-rules + R8 到達可能性解析に委譲する。

# Hilt/Dagger specific (often needed if default rules are missing)
# TODO(R8): 要 analyzeReleaseR8Config で冗長判定 – Hilt 2.60.1 / androidx.hilt:hilt-work:1.4.0 はAARがconsumer-rules同梱。REDUNDANT-RULES.mdライブラリ冗長ケース。
# keepattributes は proguard-android-optimize.txt が同等を含むため縮小可。GeneratedComponentManager/_HiltModulesはsubsumed候補。
# 推奨縮小案: -keepattributes *Annotation*,Signature  # InnerClasses/EnclosingMethodが不要かは分析後に判定
-keep public class * extends dagger.hilt.internal.GeneratedComponentManager
-keep public class * extends dagger.hilt.internal.ComponentManager
-keep class com.plath.scancard.**_HiltModules { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltWrapper_DefaultViewModelFactories { *; }
# 上記4ルールは analyzeReleaseR8Config で subsumed と判定されれば削除。現PRは注記のみ残し有効維持。
# NOTE: -keep class dagger.hilt.** / javax.inject.** / ComponentSupplier は広域過剰のため削除。
# Hilt 2.60.1 の consumer-rules に委譲し、Appium E2E (起動+DB+Worker) で検証する。
# Kotlin Coroutines (Task 2.8)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# JNI: LiteRT 等の native メソッド名は難読化しない
-keepclasseswithmembernames class * {
    native <methods>;
}

# NOTE: -keep class androidx.compose.** { *; } は削減阻害最大のため削除。
# Compose コンパイラ + ライブラリ consumer-rules に委譲し、Appium E2E で検証する。