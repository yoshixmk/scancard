package com.example.scancard.data.ml

import android.content.Context
import androidx.work.*
import com.example.scancard.data.auth.AuthManager
import com.example.scancard.data.worker.ModelDownloadWorker
import com.example.scancard.domain.model.ModelConfig
import com.example.scancard.domain.model.ModelState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authManager: AuthManager
) {
    private val _modelState = MutableStateFlow<ModelState>(ModelState.Idle)
    val modelState = _modelState.asStateFlow()

    fun checkModelStatus(config: ModelConfig) {
        if (isModelDownloaded(config)) {
            _modelState.value = ModelState.Ready
        } else {
            _modelState.value = ModelState.Idle
        }
    }

    private val workManager = WorkManager.getInstance(context)

    fun isModelDownloaded(config: ModelConfig): Boolean {
        return File(context.filesDir, config.fileName).exists()
    }

    fun getModelPath(config: ModelConfig): String {
        return File(context.filesDir, config.fileName).absolutePath
    }

    suspend fun downloadModel(config: ModelConfig) {
        val authToken = authManager.getAccessToken() ?: run {
            _modelState.value = ModelState.Error("Not authenticated with Hugging Face")
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workDataOf(
                "url" to config.downloadUrl,
                "fileName" to config.fileName,
                "authToken" to authToken
            ))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        workManager.enqueueUniqueWork(
            "download_${config.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        // Observe progress
        workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
            when (workInfo?.state) {
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getFloat("progress", 0f)
                    _modelState.value = ModelState.Downloading(progress)
                }
                WorkInfo.State.SUCCEEDED -> {
                    _modelState.value = ModelState.Ready
                }
                WorkInfo.State.FAILED -> {
                    _modelState.value = ModelState.Error("Download failed")
                }
                else -> {}
            }
        }
    }
}
