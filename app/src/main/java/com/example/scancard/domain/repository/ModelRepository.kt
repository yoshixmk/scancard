package com.example.scancard.domain.repository

import com.example.scancard.domain.model.ModelConfig
import com.example.scancard.domain.model.ModelState
import kotlinx.coroutines.flow.StateFlow

interface ModelRepository {
    val modelState: StateFlow<ModelState>
    suspend fun initializeModel()
    suspend fun downloadModel(config: ModelConfig)
    fun getModelPath(config: ModelConfig): String?
    fun getAvailableModels(): List<ModelConfig>
}
