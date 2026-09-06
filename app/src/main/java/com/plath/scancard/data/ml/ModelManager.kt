package com.plath.scancard.data.ml

import android.content.Context
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.model.ModelState
import com.google.android.play.core.aipacks.AiPackManager
import com.google.android.play.core.aipacks.AiPackManagerFactory
import com.google.android.play.core.aipacks.model.AiPackStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val _modelState = MutableStateFlow<ModelState>(ModelState.Idle)
    val modelState = _modelState.asStateFlow()

    // Visible for testing: allow injection in unit tests
    internal var aiPackManager: AiPackManager? = null
    private fun getAiPackManager(): AiPackManager = aiPackManager ?: AiPackManagerFactory.getInstance(context).also { aiPackManager = it }


    fun checkModelStatus(config: ModelConfig) {
        // Developer Fallback: Only enabled in DEBUG builds
        if (com.plath.scancard.BuildConfig.DEBUG) {
            if (File(context.filesDir, config.fileName).exists()) {
                android.util.Log.d("ModelManager", "Manual model file detected in filesDir. Marking as Ready.")
                _modelState.value = ModelState.Ready
                return
            }
        }

        try {
            getAiPackManager().getPackStates(listOf(config.aiPackName))
                .addOnSuccessListener { result ->
                    val state = result.packStates()[config.aiPackName]
                    when (state?.status()) {
                        AiPackStatus.COMPLETED -> _modelState.value = ModelState.Ready
                        AiPackStatus.DOWNLOADING -> {
                            val progress = state.bytesDownloaded().toFloat() / state.totalBytesToDownload().coerceAtLeast(1)
                            _modelState.value = ModelState.Downloading(progress)
                        }
                        AiPackStatus.FAILED -> {
                            if (com.plath.scancard.BuildConfig.DEBUG) {
                                android.util.Log.w("ModelManager", "AiPack FAILED in DEBUG (assetPacks disabled) -> Idle: ${config.aiPackName}")
                                _modelState.value = ModelState.Idle
                            } else {
                                _modelState.value = ModelState.Error("Pack status check failed")
                            }
                        }
                        else -> _modelState.value = ModelState.Idle
                    }
                }
                .addOnFailureListener { e ->
                    val msg = e.message ?: "Failed to check status"
                    // PACK_UNAVAILABLE(-2) = pack自体がPlay上に存在しない (.aabに未同梱が最多原因)
                    val hint = if (msg.contains("-2") || msg.contains("PACK_UNAVAILABLE")) {
                        "$msg (pack未配信: .aabにassetPacksが含まれているか、Play Consoleの配布トラックを確認)"
                    } else msg
                    if (com.plath.scancard.BuildConfig.DEBUG) {
                        android.util.Log.w("ModelManager", "AiPack unavailable in DEBUG (assetPacks disabled) -> Idle: $hint")
                        _modelState.value = ModelState.Idle
                    } else {
                        _modelState.value = ModelState.Error(hint)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.w("ModelManager", "AiPack check exception -> Idle in DEBUG: ${e.message}")
            _modelState.value = if (com.plath.scancard.BuildConfig.DEBUG) ModelState.Idle else ModelState.Error(e.message ?: "Failed to check status")
        }
    }

    fun isModelDownloaded(config: ModelConfig): Boolean {
        if (com.plath.scancard.BuildConfig.DEBUG) {
            if (File(context.filesDir, config.fileName).exists()) return true
        }
        return try {
            val location = getAiPackManager().getPackLocation(config.aiPackName)
            location != null
        } catch (e: Exception) {
            android.util.Log.w("ModelManager", "getPackLocation failed -> not downloaded: ${e.message}")
            false
        }
    }

    fun getModelPath(config: ModelConfig): String {
        if (com.plath.scancard.BuildConfig.DEBUG) {
            val manualFile = File(context.filesDir, config.fileName)
            if (manualFile.exists()) return manualFile.absolutePath
        }
        return try {
            val location = getAiPackManager().getPackLocation(config.aiPackName)
            val assetsPath = location?.assetsPath() ?: ""
            File(assetsPath, config.fileName).absolutePath
        } catch (e: Exception) {
            android.util.Log.w("ModelManager", "getModelPath failed: ${e.message}")
            ""
        }
    }

    fun downloadModel(config: ModelConfig) {
        try {
            getAiPackManager().fetch(listOf(config.aiPackName))
                .addOnSuccessListener {
                    _modelState.value = ModelState.Downloading(0f)
                }
                .addOnFailureListener { e ->
                    if (com.plath.scancard.BuildConfig.DEBUG) {
                        android.util.Log.w("ModelManager", "AiPack fetch unavailable in DEBUG -> Idle: ${e.message}")
                        _modelState.value = ModelState.Idle
                    } else {
                        _modelState.value = ModelState.Error("Download trigger failed: ${e.message}")
                    }
                }
        } catch (e: Exception) {
            android.util.Log.w("ModelManager", "AiPack fetch exception -> Idle in DEBUG: ${e.message}")
            _modelState.value = if (com.plath.scancard.BuildConfig.DEBUG) ModelState.Idle else ModelState.Error(e.message ?: "Failed")
        }
    }
}
