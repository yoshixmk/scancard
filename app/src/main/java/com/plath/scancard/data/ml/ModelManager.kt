package com.plath.scancard.data.ml

import android.content.Context
import com.plath.scancard.domain.model.ModelConfig
import com.plath.scancard.domain.model.ModelState
import com.google.android.play.core.aipacks.AiPackManager
import com.google.android.play.core.aipacks.AiPackManagerFactory
import com.google.android.play.core.aipacks.AiPackState
import com.google.android.play.core.aipacks.AiPackStateUpdateListener
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

    @Volatile
    private var activeConfig: ModelConfig? = null
    internal var packListener: AiPackStateUpdateListener? = null
    private var listenerRegistered = false

    // Tasksのコールバックを呼んだスレッドで即時実行し、MainLooper依存をなくす
    // (unit testでも同期的に進捗が反映される; StateFlowはスレッドセーフ)。
    private val directExecutor = java.util.concurrent.Executor { it.run() }

    private fun ensureProgressListener() {
        if (listenerRegistered) return
        try {
            val listener = AiPackStateUpdateListener { state ->
                val config = activeConfig ?: return@AiPackStateUpdateListener
                if (state.name() !in config.aiPackNames) return@AiPackStateUpdateListener
                // Push型の進捗通知 → 分割pack全体を再クエリして集約進捗に反映する。
                // getPackStates の一発取得だけでは fetch 後の進捗が 0% のまま止まる。
                queryPackStates(config)
            }
            getAiPackManager().registerListener(listener)
            packListener = listener
            listenerRegistered = true
        } catch (e: Exception) {
            android.util.Log.w("ModelManager", "registerListener failed: ${e.message}")
        }
    }

    fun checkModelStatus(config: ModelConfig) {
        // Assembled file (release concat result) or DEBUG dummy counts as Ready in any build.
        if (File(context.filesDir, config.fileName).exists()) {
            android.util.Log.d("ModelManager", "Assembled/manual model file detected in filesDir. Marking as Ready.")
            _modelState.value = ModelState.Ready
            return
        }

        activeConfig = config
        ensureProgressListener()
        queryPackStates(config)
    }

    private fun queryPackStates(config: ModelConfig) {
        try {
            getAiPackManager().getPackStates(config.aiPackNames)
                .addOnSuccessListener(directExecutor) { result ->
                    val states = config.aiPackNames.mapNotNull { result.packStates()[it] }
                    if (states.size != config.aiPackNames.size) {
                        _modelState.value = ModelState.Idle
                        return@addOnSuccessListener
                    }
                    handlePackStates(config, states)
                }
                .addOnFailureListener(directExecutor) { e ->
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

    internal fun handlePackStates(config: ModelConfig, states: List<AiPackState>) {
        val statuses = states.map { it.status() }
        when {
            statuses.all { it == AiPackStatus.COMPLETED } -> {
                // Lazy assemble: report Ready, actual concat happens in getModelPath().
                // If parts are present, try assemble now so next getModelPath is instant.
                tryAssembleSplitModel(config)
                _modelState.value = ModelState.Ready
            }
            statuses.any { it == AiPackStatus.DOWNLOADING || it == AiPackStatus.PENDING || it == AiPackStatus.TRANSFERRING } -> {
                _modelState.value = ModelState.Downloading(computeAggregateProgress(states))
            }
            statuses.any { it == AiPackStatus.WAITING_FOR_WIFI || it == AiPackStatus.REQUIRES_USER_CONFIRMATION } -> {
                // 大容量DLの承認待ち・Wi-Fi待ち: Idleに戻すとUIが振り出しに戻るため、
                // 最後の進捗を保持したまま Downloading に留める (要 showConfirmationDialog)。
                android.util.Log.w("ModelManager", "AiPack waiting for confirmation/wifi: $statuses")
                _modelState.value = ModelState.Downloading(computeAggregateProgress(states))
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

    internal fun computeAggregateProgress(states: List<AiPackState>): Float {
        val done = states.sumOf { it.bytesDownloaded() }.toFloat()
        val total = states.sumOf { it.totalBytesToDownload() }.toFloat()
        if (total > 0) {
            return (done / total).coerceIn(0f, 1f)
        }
        // total不明(PENDING直後など): transferProgressPercentageをフォールバックにし、
        // それも0なら前回値を維持して 0% 張り付き・後退を防ぐ。
        val avgTransfer = states.map { it.transferProgressPercentage() }.average().toFloat() / 100f
        if (avgTransfer > 0f) return avgTransfer.coerceIn(0f, 1f)
        return (_modelState.value as? ModelState.Downloading)?.progress ?: 0f
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
        activeConfig = config
        ensureProgressListener()
        try {
            getAiPackManager().fetch(config.aiPackNames)
                .addOnSuccessListener(directExecutor) { result ->
                    // fetch成功時の初期状態をそのまま反映し、その後の進捗はlistener経由で更新される。
                    val states = config.aiPackNames.mapNotNull { result.packStates()[it] }
                    if (states.size == config.aiPackNames.size) {
                        handlePackStates(config, states)
                        // まだPENDINGで進捗0の場合もlistenerが後続更新を駆動するため、ここで上書きしない。
                        if (_modelState.value !is ModelState.Downloading && _modelState.value !is ModelState.Ready) {
                            _modelState.value = ModelState.Downloading(computeAggregateProgress(states))
                        }
                    } else {
                        val previous = (_modelState.value as? ModelState.Downloading)?.progress ?: 0f
                        _modelState.value = ModelState.Downloading(previous)
                    }
                    // スナップショット補完: listenerが届く前に最新状態を取り直す。
                    queryPackStates(config)
                }
                .addOnFailureListener(directExecutor) { e ->
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
