package com.plath.scancard.domain.model

data class ModelConfig(
    val id: String,
    val name: String,
    val description: String,
    val sizeGb: Double,
    val fileName: String,
    // Play 1.5GB/pack 上限のため 2.6GB モデルは 2 pack 分割配信。結合後は filesDir/fileName に復元。
    val aiPackNames: List<String>,
    val partFileNames: List<String>
) {
    // Single-pack legacy accessor (first pack) for logging/compat.
    val aiPackName: String get() = aiPackNames.first()

    companion object {
        const val DEFAULT_ID = "gemma-4-e2b"

        // Gemma 4 E2B only (single-model policy to reduce app size)
        val GEMMA_4_E2B = ModelConfig(
            id = "gemma-4-e2b",
            name = "Gemma 4 E2B (Universal CPU)",
            description = "Standard dense model. Works on most phones.",
            sizeGb = 2.6,
            fileName = "gemma-4-E2B-it.litertlm",
            aiPackNames = listOf("gemma_ai_pack", "gemma_ai_pack_2"),
            partFileNames = listOf(
                "gemma-4-E2B-it.litertlm.part0",
                "gemma-4-E2B-it.litertlm.part1"
            )
        )

        val AVAILABLE_MODELS = listOf(GEMMA_4_E2B)
    }
}
