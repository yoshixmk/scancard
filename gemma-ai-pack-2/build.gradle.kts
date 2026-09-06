plugins {
    id("com.android.ai-pack")
}

aiPack {
    // Holds gemma-4-E2B-it.litertlm.part1 (second half). Empty pack => PACK_UNAVAILABLE(-2) on Play.
    packName = "gemma_ai_pack_2"

    dynamicDelivery {
        deliveryType = "on-demand"
    }
}
