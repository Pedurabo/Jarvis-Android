package com.example.jarvis.core.ai

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result {
        Log.d("SyncWorker", "Starting background sync...")
        return try {
            val result = syncManager.sync()
            if (result.isSuccess) {
                Log.d("SyncWorker", "Background sync successful")
                ListenableWorker.Result.success()
            } else {
                Log.e("SyncWorker", "Background sync failed: ${result.exceptionOrNull()?.message}")
                ListenableWorker.Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Critical failure in background sync", e)
            ListenableWorker.Result.failure()
        }
    }
}
