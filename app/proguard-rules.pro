# ScanCard ProGuard Rules
# NOTE: 本ファイルは IMP-04 で広域keepを精緻化。ビルド非破壊のため広域keepはコメントアウト＋縮小案をコメント併記に留める。
# JVM17で ./gradlew :app:analyzeReleaseR8Config 実行後に冗長確定したルールは完全削除予定。isMinifyEnabled=false のため現状コメントアウトでも挙動不変。

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
# -keep public class com.example.scancard.ScanCardApplication { *; }
# 検証: ./gradlew :app:analyzeReleaseR8Config で subsumed 判定を確認。

# Room
# TODO(R8): 要 analyzeReleaseR8Config で冗長判定 – Room 2.8.4 はAARがconsumer-rules提供 (REDUNDANT-RULES.md Case: Room Database)。手動keep不要。
# -keep class * extends androidx.room.RoomDatabase
# 推奨: 削除 (Room consumer-rulesに委譲)。AppDatabase_Impl はR8が自動保持。
# -keep class com.example.scancard.data.local.entities.** { *; }
# 推奨縮小案 (広域 {*; } → アノテーション or フィールド限定):
# -keep @androidx.room.Entity class * { *; }
# またはクラス単位:
# -keep class com.example.scancard.data.local.entities.Card { <fields>; }
# -keep class com.example.scancard.data.local.entities.Deck { <fields>; }
# -keep class com.example.scancard.data.local.entities.Scan { <fields>; }
# 現PRはコメントアウト＋縮小案併記 (将来 analyzeReleaseR8Config で Keeps件数を確認し確定)。

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
# Keep internal AI components that use reflection or JNI
# TODO(R8): 広域 → 使用クラス限定 – ModelManager.kt で使用は AiPackManager / AiPackManagerFactory / AiPackStatus のみ。
# -keep class com.google.android.play.core.aipacks.** { *; }
# 推奨縮小案:
# -keep class com.google.android.play.core.aipacks.AiPackManager { *; }
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
# -keep class com.example.scancard.data.ml.ExtractedCard { *; }
# 推奨縮小案:
# -keep class com.example.scancard.data.ml.ExtractedCard { <fields>; }
# -keep class com.example.scancard.domain.model.** { *; }
# 推奨縮小案:
# -keep class com.example.scancard.domain.model.** { <fields>; }
# または allowobfuscation で縮小: -keep,allowobfuscation class com.example.scancard.domain.model.** { <fields>; }

# Hilt/Dagger specific (often needed if default rules are missing)
# TODO(R8): 要 analyzeReleaseR8Config で冗長判定 – Hilt 2.60.1 / androidx.hilt:hilt-work:1.4.0 はAARがconsumer-rules同梱。REDUNDANT-RULES.mdライブラリ冗長ケース。
# keepattributes は proguard-android-optimize.txt が同等を含むため縮小可。GeneratedComponentManager/_HiltModulesはsubsumed候補。
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
# 推奨縮小案: -keepattributes *Annotation*,Signature  # InnerClasses/EnclosingMethodが不要かは分析後に判定
-keep public class * extends dagger.hilt.internal.GeneratedComponentManager
-keep public class * extends dagger.hilt.internal.ComponentManager
-keep class com.example.scancard.**_HiltModules { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltWrapper_DefaultViewModelFactories { *; }
# 上記4ルールは analyzeReleaseR8Config で subsumed と判定されれば削除。現PRは注記のみ残し有効維持。
