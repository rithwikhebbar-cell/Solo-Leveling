package com.example.service

import android.content.Context
import com.example.data.local.SoloLevelingDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Milestone 12: Application-layer Audio Lifecycle Manager.
 * Responsibilities:
 * - Creates, validates and finalizes .m4a recordings in account-scoped directory.
 * - Enforces 50 MB hard storage quota per account owner.
 * - Enforces 180s duration limits.
 * - Reconciles orphaned audio files on app launch.
 * - Cascades file deletions safely without failing transactions.
 * - Never uploads raw audio to Supabase.
 */
class AudioLifecycleManager(
    private val context: Context,
    private val database: SoloLevelingDatabase
) {
    companion object {
        const val MAX_RECORDING_SECONDS = 180
        const val MAX_ACCOUNT_QUOTA_BYTES = 50 * 1024 * 1024L // 50 MB
        const val DIRECTORY_NAME = "social_recordings"
    }

    private fun getAccountRecordingDir(accountOwnerId: String): File {
        val safeOwner = accountOwnerId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val dir = File(context.filesDir, "$DIRECTORY_NAME/$safeOwner")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun getAccountStorageUsageBytes(accountOwnerId: String): Long = withContext(Dispatchers.IO) {
        val dir = getAccountRecordingDir(accountOwnerId)
        dir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    suspend fun prepareNewRecordingFile(accountOwnerId: String, sessionId: String): Result<File> = withContext(Dispatchers.IO) {
        val currentUsage = getAccountStorageUsageBytes(accountOwnerId)
        if (currentUsage >= MAX_ACCOUNT_QUOTA_BYTES) {
            pruneOldestRecordings(accountOwnerId, (MAX_ACCOUNT_QUOTA_BYTES * 0.8).toLong())
        }

        val safeOwner = accountOwnerId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val dir = getAccountRecordingDir(accountOwnerId)
        val fileName = "${safeOwner}_${sessionId}_${System.currentTimeMillis()}.m4a"
        val targetFile = File(dir, fileName)
        try {
            if (targetFile.exists()) targetFile.delete()
            targetFile.createNewFile()
            Result.success(targetFile)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    suspend fun validateAndFinalizeRecording(
        accountOwnerId: String,
        file: File,
        durationSeconds: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            if (file.exists()) file.delete()
            return@withContext Result.failure(IllegalStateException("Audio recording file is empty or missing."))
        }
        if (durationSeconds <= 0 || durationSeconds > MAX_RECORDING_SECONDS + 5) {
            file.delete()
            return@withContext Result.failure(IllegalArgumentException("Invalid recording duration: $durationSeconds seconds."))
        }

        val usage = getAccountStorageUsageBytes(accountOwnerId)
        if (usage > MAX_ACCOUNT_QUOTA_BYTES) {
            pruneOldestRecordings(accountOwnerId, MAX_ACCOUNT_QUOTA_BYTES)
        }

        Result.success(file.name)
    }

    suspend fun deleteRecording(accountOwnerId: String, fileName: String?): Boolean = withContext(Dispatchers.IO) {
        if (fileName.isNullOrBlank()) return@withContext true
        val dir = getAccountRecordingDir(accountOwnerId)
        val file = File(dir, fileName)
        if (file.exists()) {
            file.delete()
        } else {
            true
        }
    }

    suspend fun purgeAccountRecordings(accountOwnerId: String): Boolean = withContext(Dispatchers.IO) {
        val dir = getAccountRecordingDir(accountOwnerId)
        if (dir.exists()) {
            dir.deleteRecursively()
        } else {
            true
        }
    }

    suspend fun reconcileOrphanFiles(accountOwnerId: String) = withContext(Dispatchers.IO) {
        val dir = getAccountRecordingDir(accountOwnerId)
        val filesOnDisk = dir.listFiles()?.filter { it.isFile } ?: return@withContext
        val sessionsWithAudio = database.socialPracticeSessionDao().getSessionsWithAudio(accountOwnerId)
        val activeAudioFileNames = sessionsWithAudio.mapNotNull { it.audioFileName }.toSet()

        // 1. Delete physical files that have no matching DB record
        for (file in filesOnDisk) {
            if (file.name !in activeAudioFileNames) {
                file.delete()
            }
        }

        // 2. Clear hasAudioRecording flag for DB records whose files were lost on disk
        for (session in sessionsWithAudio) {
            val fileName = session.audioFileName
            if (fileName == null || !File(dir, fileName).exists()) {
                database.socialPracticeSessionDao().updateSession(
                    session.copy(hasAudioRecording = false, audioFileName = null)
                )
            }
        }
    }

    private suspend fun pruneOldestRecordings(accountOwnerId: String, targetMaxBytes: Long) = withContext(Dispatchers.IO) {
        val dir = getAccountRecordingDir(accountOwnerId)
        val files = dir.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() } ?: return@withContext
        var currentBytes = files.sumOf { it.length() }

        for (file in files) {
            if (currentBytes <= targetMaxBytes) break
            val length = file.length()
            if (file.delete()) {
                currentBytes -= length
            }
        }
    }
}
