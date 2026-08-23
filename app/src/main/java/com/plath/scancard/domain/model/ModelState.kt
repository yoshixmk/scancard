package com.plath.scancard.domain.model

sealed class ModelState {
    object Idle : ModelState()
    data class Downloading(val progress: Float) : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}
