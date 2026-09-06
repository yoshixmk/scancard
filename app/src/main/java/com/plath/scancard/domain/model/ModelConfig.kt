package com.plath.scancard.domain.model

data class ModelConfig(
    val id: String,
    val name: String,
    val description: String,
    val sizeGb: Double,
    val fileName: String,
    val aiPackName: String
) {
    companion object {
        const val DEFAULT_ID = "gemma-4-e2b"

        // Gemma 4 E2B only (single-model policy to reduce app size)
        val GEMMA_4_E2B = ModelConfig(
            id = "gemma-4-e2b",
            name = "Gemma 4 E2B (Universal CPU)",
            description = "Standard dense model. Works on most phones.",
            sizeGb = 2.6,
            fileName = "gemma-4-E2B-it.litertlm",
            aiPackName = "gemma_ai_pack"
        )
        
        val AVAILABLE_MODELS = listOf(GEMMA_4_E2B)
    }
}
