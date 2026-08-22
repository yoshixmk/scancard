# ScanCard ProGuard Rules

# Hilt - Rules are typically provided by the library, but keeping generic ones for safety
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.scancard.data.local.entities.** { *; }

# LiteRT LM (Gemma)
-keep class com.google.ai.edge.litertlm.** { *; }
# Keep internal AI components that use reflection or JNI
-keep class com.google.android.play.core.aipacks.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# General Data Models (if any use reflection/JSON)
-keep class com.example.scancard.data.ml.ExtractedCard { *; }
-keep class com.example.scancard.domain.model.** { *; }

# Hilt/Dagger specific (often needed if default rules are missing)
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep public class * extends dagger.hilt.internal.GeneratedComponentManager
-keep public class * extends dagger.hilt.internal.ComponentManager
-keep class com.example.scancard.**_HiltModules { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltWrapper_DefaultViewModelFactories { *; }
