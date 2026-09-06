plugins {
    id("com.android.ai-pack")
}

aiPack {
    // Holds gemma-4-E2B-it.litertlm.part0 (first half). Empty pack => PACK_UNAVAILABLE(-2) on Play.
    packName = "gemma_ai_pack"

    dynamicDelivery {
        deliveryType = "on-demand"
    }
    
    // In a real project, you would place model files in src/main/assets
    // and potentially use device targeting here.
    /*
    deviceTargeting {
        ram {
            // Deliver E4B to high-end devices
            group("high_ram") {
                minMemoryAtInstallGb = 8
            }
        }
    }
    */
}
