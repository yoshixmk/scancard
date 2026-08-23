package com.plath.scancard.data.repository

import com.plath.scancard.data.ml.ModelManager
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.model.ModelState
import com.plath.scancard.domain.repository.ModelRepository
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

    override fun checkModelStatus(config: ModelConfig) {
        modelManager.checkModelStatus(config)
    }

    override fun getModelPath(config: ModelConfig): String? {
        return if (modelManager.isModelDownloaded(config)) modelManager.getModelPath(config) else null
    }

    override fun getAvailableModels(): List<ModelConfig> = ModelConfig.AVAILABLE_MODELS
}
