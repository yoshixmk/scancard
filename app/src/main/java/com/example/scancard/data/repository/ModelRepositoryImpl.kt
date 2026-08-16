package com.example.scancard.data.repository

import com.example.scancard.data.ml.ModelManager
import com.example.scancard.domain.model.ModelConfig
import com.example.scancard.domain.model.ModelState
import com.example.scancard.domain.repository.ModelRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepositoryImpl @Inject constructor(
    private val modelManager: ModelManager
) : ModelRepository {
    override val modelState: StateFlow<ModelState> = modelManager.modelState

    override suspend fun initializeModel() {
        // Initialization logic for LLM
    }

    override suspend fun downloadModel(config: ModelConfig) {
        modelManager.downloadModel(config)
    }

    override fun getModelPath(config: ModelConfig): String? {
        return if (modelManager.isModelDownloaded(config)) modelManager.getModelPath(config) else null
    }

    override fun getAvailableModels(): List<ModelConfig> = ModelConfig.AVAILABLE_MODELS
}
