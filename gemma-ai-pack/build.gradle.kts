plugins {
    id("com.android.ai-pack")
}

aiPack {
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
