package com.example.jarvis.core.ai

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.jarvis.core.auth.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val memoryDao: MemoryDao,
    private val authManager: AuthManager
) {
    private val TAG = "SyncManager"

    /**
     * Orchestrates a full sync: Push local changes -> Pull remote changes.
     */
    suspend fun sync(): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = authManager.userId ?: return@withContext Result.failure(Exception("User not logged in"))

        try {
            pushLocalChanges(userId)
            pullRemoteChanges(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Pushes unsynced local data to Firestore using Batch Writes for efficiency.
     */
    private suspend fun pushLocalChanges(userId: String) {
        val unsyncedMemories = memoryDao.getUnsyncedMemories()
        if (unsyncedMemories.isEmpty()) return

        val userMemoriesRef = firestore.collection("users").document(userId).collection("memories")

        // Firestore batches are limited to 500 operations
        unsyncedMemories.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { memory ->
                val docRef = if (!memory.firestoreId.isNullOrEmpty()) {
                    userMemoriesRef.document(memory.firestoreId)
                } else {
                    userMemoriesRef.document() // Generate new ID
                }

                val data = mapOf(
                    "text" to memory.text,
                    "timestamp" to memory.timestamp,
                    "isDeleted" to memory.isDeleted,
                    "userId" to userId
                )

                batch.set(docRef, data, SetOptions.merge())

                // Update local firestoreId if it was newly generated
                if (memory.firestoreId == null) {
                    memoryDao.updateMemory(memory.copy(firestoreId = docRef.id))
                }
            }
            batch.commit().await()
        }

        // Mark all as synced after successful batch commit
        unsyncedMemories.forEach {
            memoryDao.updateMemory(it.copy(isSynced = true))
        }
        Log.d(TAG, "Pushed ${unsyncedMemories.size} memories to cloud")
    }

    /**
     * Fetches remote updates since the last sync timestamp.
     * Conflict Resolution: Latest Wins (based on Firestore timestamp).
     */
    private suspend fun pullRemoteChanges(userId: String) {
        val lastSyncTimestamp = memoryDao.getLastSyncTimestamp() ?: 0L

        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("memories")
            .whereGreaterThan("timestamp", lastSyncTimestamp)
            .get()
            .await()

        if (snapshot.isEmpty) return

        snapshot.documents.forEach { doc ->
            val remoteTimestamp = doc.getLong("timestamp") ?: 0L
            val existingLocal = memoryDao.getByFirestoreId(doc.id)

            // Conflict resolution: Latest Wins
            if (existingLocal == null || remoteTimestamp > existingLocal.timestamp) {
                val newMemory = MemoryEntity(
                    id = existingLocal?.id ?: 0, // Preserve local ID if exists
                    firestoreId = doc.id,
                    text = doc.getString("text") ?: "",
                    embedding = floatArrayOf(), // Assume background task handles embeddings
                    timestamp = remoteTimestamp,
                    isSynced = true,
                    isDeleted = doc.getBoolean("isDeleted") ?: false
                )
                memoryDao.insertMemory(newMemory)
            }
        }
        Log.d(TAG, "Pulled ${snapshot.size()} memories from cloud")
    }

    /**
     * Schedules periodic background sync using WorkManager.
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "CloudSyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Immediate one-time sync (e.g., on app start or manual refresh).
     */
    fun scheduleImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "ImmediateSyncWorker",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
