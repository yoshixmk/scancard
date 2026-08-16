package com.example.scancard.data.repository

import com.example.scancard.data.ml.ModelManager
import com.example.scancard.domain.model.ModelState
import com.example.scancard.domain.repository.ModelRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ModelRepositoryImpl @Inject constructor(
    private val modelManager: ModelManager
) : ModelRepository {
    override val modelState: StateFlow<ModelState> = modelManager.modelState

    override suspend fun initializeModel() {
        if (modelManager.isModelDownloaded()) {
            // Initialization logic for LLM
        }
    }

    override suspend fun downloadModel() {
        modelManager.downloadModelStub()
    }
}
