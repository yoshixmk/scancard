package com.example.scancard.data.ml

import android.content.Context
import com.example.scancard.domain.model.ModelState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _modelState = MutableStateFlow<ModelState>(ModelState.Idle)
    val modelState = _modelState.asStateFlow()

    private val modelFile = File(context.filesDir, "gemma-2b-it-cpu-int4.bin")

    fun isModelDownloaded(): Boolean = modelFile.exists()

    fun getModelPath(): String = modelFile.absolutePath

    suspend fun downloadModelStub() {
        // In a real app, this would download from a URL.
        // For this task, we assume the user might have to place it or we simulate download.
        if (isModelDownloaded()) {
            _modelState.value = ModelState.Ready
            return
        }

        _modelState.value = ModelState.Downloading(0.1f)
        // Simulate download
        kotlinx.coroutines.delay(1000)
        _modelState.value = ModelState.Downloading(0.5f)
        kotlinx.coroutines.delay(1000)
        _modelState.value = ModelState.Ready
    }
}
