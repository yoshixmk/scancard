package com.example.scancard.domain.model

data class ModelConfig(
    val id: String,
    val name: String,
    val description: String,
    val sizeGb: Double,
    val fileName: String,
    val downloadUrl: String
) {
    companion object {
        // Gemma 4 family - Optimized for mobile
        val GEMMA_4_E2B = ModelConfig(
            id = "gemma-4-e2b",
            name = "Gemma 4 E2B (Ultra Fast)",
            description = "Highly efficient dense model with PLE. Best for most phones.",
            sizeGb = 1.3,
            fileName = "gemma-4-e2b-it-cpu-int4.bin",
            downloadUrl = "https://huggingface.co/google/gemma-4-e2b-it/resolve/main/gemma-4-e2b-it-cpu-int4.bin"
        )
        
        val GEMMA_4_E4B = ModelConfig(
            id = "gemma-4-e4b",
            name = "Gemma 4 E4B (Balanced)",
            description = "Higher reasoning capability while remaining mobile-friendly.",
            sizeGb = 2.5,
            fileName = "gemma-4-e4b-it-cpu-int4.bin",
            downloadUrl = "https://huggingface.co/google/gemma-4-e4b-it/resolve/main/gemma-4-e4b-it-cpu-int4.bin"
        )

        // Previous generation Gemma 2
        val GEMMA_2_2B = ModelConfig(
            id = "gemma-2-2b",
            name = "Gemma 2 2B (Legacy)",
            description = "Proven 2B model, very low resource usage.",
            sizeGb = 1.6,
            fileName = "gemma-2-2b-it-cpu-int4.bin",
            downloadUrl = "https://huggingface.co/google/gemma-2-2b-it/resolve/main/gemma-2-2b-it-cpu-int4.bin"
        )
        
        val AVAILABLE_MODELS = listOf(GEMMA_4_E2B, GEMMA_4_E4B, GEMMA_2_2B)
    }
}
