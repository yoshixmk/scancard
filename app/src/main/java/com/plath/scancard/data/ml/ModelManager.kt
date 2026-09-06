package com.plath.scancard.data.ml

import android.content.Context
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.model.ModelState
import com.google.android.play.core.aipacks.AiPackManager
import com.google.android.play.core.aipacks.AiPackManagerFactory
import com.google.android.play.core.aipacks.model.AiPackStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val _modelState = MutableStateFlow<ModelState>(ModelState.Idle)
    val modelState = _modelState.asStateFlow()

    // Visible for testing: allow injection in unit tests
    internal var aiPackManager: AiPackManager? = null
    private fun getAiPackManager(): AiPackManager = aiPackManager ?: AiPackManagerFactory.getInstance(context).also { aiPackManager = it }


    fun checkModelStatus(config: ModelConfig) {
        // Assembled file (release concat result) or DEBUG dummy counts as Ready in any build.
        if (File(context.filesDir, config.fileName).exists()) {
            android.util.Log.d("ModelManager", "Assembled/manual model file detected in filesDir. Marking as Ready.")
            _modelState.value = ModelState.Ready
            return
        }

        try {
            getAiPackManager().getPackStates(config.aiPackNames)
                .addOnSuccessListener { result ->
                    val states = config.aiPackNames.mapNotNull { result.packStates()[it] }
                    if (states.size != config.aiPackNames.size) {
                        _modelState.value = ModelState.Idle
                        return@addOnSuccessListener
                    }
                    val statuses = states.map { it.status() }
                    when {
                        statuses.all { it == AiPackStatus.COMPLETED } -> {
                            // Lazy assemble: report Ready, actual concat happens in getModelPath().
                            // If parts are present, try assemble now so next getModelPath is instant.
                            val assembled = tryAssembleSplitModel(config)
                            if (assembled != null || hasAllPartFiles(config)) {
                                _modelState.value = ModelState.Ready
                            } else {
                                _modelState.value = ModelState.Ready
                            }
                        }
                        statuses.any { it == AiPackStatus.DOWNLOADING || it == AiPackStatus.PENDING } -> {
                            val done = states.sumOf { it.bytesDownloaded() }.toFloat()
                            val total = states.sumOf { it.totalBytesToDownload() }.coerceAtLeast(1).toFloat()
                            _modelState.value = ModelState.Downloading((done / total).coerceIn(0f, 1f))
                        }
                        statuses.any { it == AiPackStatus.FAILED } -> {
                            if (com.plath.scancard.BuildConfig.DEBUG) {
                                android.util.Log.w("ModelManager", "AiPack FAILED in DEBUG (assetPacks disabled) -> Idle: ${config.aiPackNames}")
                                _modelState.value = ModelState.Idle
                            } else {
                                _modelState.value = ModelState.Error("Pack status check failed")
                            }
                        }
                        else -> _modelState.value = ModelState.Idle
                    }
                }
                .addOnFailureListener { e ->
                    val msg = e.message ?: "Failed to check status"
                    // PACK_UNAVAILABLE(-2) = pack自体がPlay上に存在しない (.aabに未同梱が最多原因)
                    val hint = if (msg.contains("-2") || msg.contains("PACK_UNAVAILABLE")) {
                        "$msg (pack未配信: .aabにassetPacksが含まれているか、Play Consoleの配布トラックを確認)"
                    } else msg
                    if (com.plath.scancard.BuildConfig.DEBUG) {
                        android.util.Log.w("ModelManager", "AiPack unavailable in DEBUG (assetPacks disabled) -> Idle: $hint")
                        _modelState.value = ModelState.Idle
                    } else {
                        _modelState.value = ModelState.Error(hint)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.w("ModelManager", "AiPack check exception -> Idle in DEBUG: ${e.message}")
            _modelState.value = if (com.plath.scancard.BuildConfig.DEBUG) ModelState.Idle else ModelState.Error(e.message ?: "Failed to check status")
        }
    }

    fun isModelDownloaded(config: ModelConfig): Boolean {
        if (File(context.filesDir, config.fileName).exists()) return true
        return try {
            hasAllPartFiles(config)
        } catch (e: Exception) {
            android.util.Log.w("ModelManager", "getPackLocation failed -> not downloaded: ${e.message}")
            false
        }
    }

    fun getModelPath(config: ModelConfig): String {
        val manualFile = File(context.filesDir, config.fileName)
        if (manualFile.exists()) return manualFile.absolutePath
        return try {
            tryAssembleSplitModel(config)?.absolutePath ?: ""
        } catch (e: Exception) {
            android.util.Log.w("ModelManager", "getModelPath failed: ${e.message}")
            ""
        }
    }

    /** Returns assembled file if present or successfully concatenated, else null. */
    internal fun tryAssembleSplitModel(config: ModelConfig): File? {
        val out = File(context.filesDir, config.fileName)
        if (out.exists()) return out
        val parts = config.aiPackNames.mapIndexedNotNull { index, packName ->
            val location = try {
                getAiPackManager().getPackLocation(packName)
            } catch (_: Exception) { null } ?: return null
            val assetsPath = location.assetsPath() ?: return null
            val partName = config.partFileNames.getOrNull(index) ?: return null
            val f = File(assetsPath, partName)
            if (!f.exists()) return null
            f
        }
        if (parts.size != config.aiPackNames.size) return null
        return assembleParts(parts, out)
    }

    private fun hasAllPartFiles(config: ModelConfig): Boolean {
        config.aiPackNames.forEachIndexed { index, packName ->
            val location = getAiPackManager().getPackLocation(packName) ?: return false
            val assetsPath = location.assetsPath() ?: return false
            val partName = config.partFileNames.getOrNull(index) ?: return false
            if (!File(assetsPath, partName).exists()) return false
        }
        return true
    }

    internal fun assembleParts(parts: List<File>, out: File): File? {
        val tmp = File(out.parent, "${out.name}.tmp")
        try {
            tmp.outputStream().buffered(8 * 1024 * 1024).use { output ->
                parts.forEach { part ->
                    part.inputStream().buffered(8 * 1024 * 1024).use { input ->
                        input.copyTo(output)
                    }
                }
            }
            val expected = parts.sumOf { it.length() }
            if (tmp.length() != expected) {
                android.util.Log.w("ModelManager", "Assembled size mismatch: ${tmp.length()} vs $expected")
                tmp.delete()
                return null
            }
            if (!tmp.renameTo(out)) {
                tmp.copyTo(out, overwrite = true)
                tmp.delete()
            }
            return out
        } catch (e: Exception) {
            android.util.Log.w("ModelManager", "assembleParts failed: ${e.message}")
            tmp.delete()
            return null
        }
    }

    fun downloadModel(config: ModelConfig) {
        try {
            getAiPackManager().fetch(config.aiPackNames)
                .addOnSuccessListener {
                    _modelState.value = ModelState.Downloading(0f)
                }
                .addOnFailureListener { e ->
                    if (com.plath.scancard.BuildConfig.DEBUG) {
                        android.util.Log.w("ModelManager", "AiPack fetch unavailable in DEBUG -> Idle: ${e.message}")
                        _modelState.value = ModelState.Idle
                    } else {
                        _modelState.value = ModelState.Error("Download trigger failed: ${e.message}")
                    }
                }
        } catch (e: Exception) {
            android.util.Log.w("ModelManager", "AiPack fetch exception -> Idle in DEBUG: ${e.message}")
            _modelState.value = if (com.plath.scancard.BuildConfig.DEBUG) ModelState.Idle else ModelState.Error(e.message ?: "Failed")
        }
    }
}
