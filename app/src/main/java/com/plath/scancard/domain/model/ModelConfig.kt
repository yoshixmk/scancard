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

        // Gemma 4 family - Delivered via Google Play AI Packs
        val GEMMA_4_E2B = ModelConfig(
            id = "gemma-4-e2b",
            name = "Gemma 4 E2B (Universal CPU)",
            description = "Standard dense model. Works on most phones.",
            sizeGb = 2.6,
            fileName = "gemma-4-E2B-it.litertlm",
            aiPackName = "gemma_ai_pack"
        )
        
        val GEMMA_4_E4B = ModelConfig(
            id = "gemma-4-e4b",
            name = "Gemma 4 E4B (Balanced)",
            description = "Higher reasoning capability while remaining mobile-friendly.",
            sizeGb = 3.7,
            fileName = "gemma-4-E4B-it.litertlm",
            aiPackName = "gemma_ai_pack"
        )

        // Previous generation Gemma 2
        val GEMMA_2_2B = ModelConfig(
            id = "gemma-2-2b",
            name = "Gemma 2 2B (Legacy)",
            description = "Proven 2B model, very low resource usage.",
            sizeGb = 1.6,
            fileName = "gemma-2-2b-it-cpu-int4.bin",
            aiPackName = "gemma_2_2b_pack"
        )
        
        val AVAILABLE_MODELS = listOf(GEMMA_4_E2B, GEMMA_4_E4B, GEMMA_2_2B)
    }
}
