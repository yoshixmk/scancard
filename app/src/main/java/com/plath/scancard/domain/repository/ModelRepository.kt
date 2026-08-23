package com.plath.scancard.domain.repository

import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.model.ModelState
import kotlinx.coroutines.flow.StateFlow

interface ModelRepository {
    val modelState: StateFlow<ModelState>
    suspend fun initializeModel()
    suspend fun downloadModel(config: ModelConfig)
    fun checkModelStatus(config: ModelConfig)
    fun getModelPath(config: ModelConfig): String?
    fun getAvailableModels(): List<ModelConfig>
}
