package com.plath.scancard

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.plath.scancard.domain.usecase.ResumePendingExtractionsUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ScanCardApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var resumePendingExtractions: ResumePendingExtractionsUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Resume extractions interrupted by process death or system cancellation (Req12.10)
        applicationScope.launch {
            runCatching { resumePendingExtractions() }
                .onFailure { android.util.Log.e("ScanCardApp", "Failed to resume pending extractions", it) }
        }
    }
}
