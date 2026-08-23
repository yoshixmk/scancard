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

    private val aiPackManager: AiPackManager = AiPackManagerFactory.getInstance(context)

    fun checkModelStatus(config: ModelConfig) {
        // Developer Fallback: Only enabled in DEBUG builds
        if (com.plath.scancard.BuildConfig.DEBUG) {
            if (File(context.filesDir, config.fileName).exists()) {
                android.util.Log.d("ModelManager", "Manual model file detected in filesDir. Marking as Ready.")
                _modelState.value = ModelState.Ready
                return
            }
        }

        aiPackManager.getPackStates(listOf(config.aiPackName))
            .addOnSuccessListener { result ->
                val state = result.packStates()[config.aiPackName]
                when (state?.status()) {
                    AiPackStatus.COMPLETED -> _modelState.value = ModelState.Ready
                    AiPackStatus.DOWNLOADING -> {
                        val progress = state.bytesDownloaded().toFloat() / state.totalBytesToDownload().coerceAtLeast(1)
                        _modelState.value = ModelState.Downloading(progress)
                    }
                    AiPackStatus.FAILED -> _modelState.value = ModelState.Error("Pack status check failed")
                    else -> _modelState.value = ModelState.Idle
                }
            }
            .addOnFailureListener { e ->
                _modelState.value = ModelState.Error(e.message ?: "Failed to check status")
            }
    }

    fun isModelDownloaded(config: ModelConfig): Boolean {
        // Check manual placement in DEBUG builds only
        if (com.plath.scancard.BuildConfig.DEBUG) {
            if (File(context.filesDir, config.fileName).exists()) return true
        }
        
        val location = aiPackManager.getPackLocation(config.aiPackName)
        return location != null
    }

    fun getModelPath(config: ModelConfig): String {
        // Check manual placement in DEBUG builds only
        if (com.plath.scancard.BuildConfig.DEBUG) {
            val manualFile = File(context.filesDir, config.fileName)
            if (manualFile.exists()) return manualFile.absolutePath
        }

        val location = aiPackManager.getPackLocation(config.aiPackName)
        val assetsPath = location?.assetsPath() ?: ""
        return File(assetsPath, config.fileName).absolutePath
    }

    fun downloadModel(config: ModelConfig) {
        aiPackManager.fetch(listOf(config.aiPackName))
            .addOnSuccessListener {
                _modelState.value = ModelState.Downloading(0f)
            }
            .addOnFailureListener { e ->
                _modelState.value = ModelState.Error("Download trigger failed: ${e.message}")
            }
    }
}
