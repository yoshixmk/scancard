# ScanCard ProGuard Rules

# Preserve annotations, signatures required by Hilt, Room and Play deobfuscation
-keepattributes *Annotation*,Signature,EnclosingMethod,SourceFile,LineNumberTable

# Application and Activity classes
-keep public class com.plath.scancard.ScanCardApplication { *; }
-keep public class com.plath.scancard.MainActivity { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { <fields>; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.plath.scancard.data.local.Converters { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# LiteRT LM (Gemma)
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

# Domain models - field names only
-keep class com.plath.scancard.domain.model.** { <fields>; }
-keepclassmembers enum com.plath.scancard.domain.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Manual JSON parse target
-keep class com.plath.scancard.data.ml.ExtractedCard { <fields>; }

# Hilt/Dagger
-keep public class * extends dagger.hilt.internal.GeneratedComponentManager
-keep public class * extends dagger.hilt.internal.ComponentManager
-keep class com.plath.scancard.**_HiltModules { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltWrapper_DefaultViewModelFactories { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# JNI native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

