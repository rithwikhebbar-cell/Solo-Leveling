package com.example.service

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.model.SocialArtifactValidationState
import com.example.domain.model.SocialMissionEvidenceType
import com.example.domain.model.SocialMissionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Milestone 12 - Phase 5.1.1: Real-World Mission Artifact Storage & Integrity Verifier.
 * Responsibilities:
 * - Enforces explicit account-scoped local vault isolation (<root>/<safeOwner>/).
 * - Validates attachment streams (non-empty, <= 10 MB, SHA-256 content digest).
 * - Prevents cross-account file path traversal and duplicate hash spoofing.
 * - Verifies physical retrievability and digest integrity before mastery promotion.
 */
class SocialArtifactStorageManager(
    private val context: Context? = null,
    private val storageRootDir: File = File(
        requireNotNull(context) { "Context or storageRootDir is required" }.filesDir,
        DIRECTORY_NAME
    )
) {
    companion object {
        const val DIRECTORY_NAME = "social_artifacts"
        const val MAX_ARTIFACT_BYTES = 10L * 1024L * 1024L // 10 MB
    }

    data class StoredArtifactResult(
        val artifactUri: String,
        val artifactOwnerId: String,
        val artifactContentHash: String,
        val displayFileName: String,
        val byteSize: Long,
        val validationState: SocialArtifactValidationState
    )

    private fun safeOwnerSegment(accountOwnerId: String): String {
        val cleaned = accountOwnerId.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        require(cleaned.isNotEmpty()) { "Invalid accountOwnerId for artifact storage" }
        return cleaned
    }

    private fun getAccountArtifactDir(accountOwnerId: String): File {
        val dir = File(storageRootDir, safeOwnerSegment(accountOwnerId))
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun sanitizeFileName(rawName: String?): String {
        val base = rawName?.substringAfterLast('/')?.substringAfterLast('\\')
            ?.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            ?.trim('.')
            .orEmpty()
        return if (base.isBlank()) "artifact.bin" else base.take(80)
    }

    suspend fun ingestFromUri(
        accountOwnerId: String,
        recordId: String,
        uri: Uri,
        declaredFileName: String? = null
    ): Result<StoredArtifactResult> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context?.contentResolver
                ?: throw IllegalStateException("Android ContentResolver unavailable for URI ingestion.")
            val displayName = declaredFileName ?: queryDisplayName(uri) ?: "artifact.bin"
            resolver.openInputStream(uri)?.use { stream ->
                val bytes = readBoundedBytes(stream)
                storeBytesInternal(accountOwnerId, recordId, displayName, bytes)
            } ?: throw IllegalStateException("Attached URI could not be opened or is no longer readable.")
        }
    }

    suspend fun ingestFromBytes(
        accountOwnerId: String,
        recordId: String,
        fileName: String,
        bytes: ByteArray
    ): Result<StoredArtifactResult> = withContext(Dispatchers.IO) {
        runCatching {
            storeBytesInternal(accountOwnerId, recordId, fileName, bytes)
        }
    }

    private fun storeBytesInternal(
        accountOwnerId: String,
        recordId: String,
        fileName: String,
        bytes: ByteArray
    ): StoredArtifactResult {
        if (bytes.isEmpty()) {
            throw IllegalArgumentException("Attached artifact is empty (0 bytes).")
        }
        if (bytes.size.toLong() > MAX_ARTIFACT_BYTES) {
            throw IllegalArgumentException("Attached artifact exceeds 10 MB limit.")
        }

        val safeOwner = safeOwnerSegment(accountOwnerId)
        val safeRecordId = recordId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(48)
        val safeName = sanitizeFileName(fileName)
        val accountDir = getAccountArtifactDir(safeOwner)
        val targetFile = File(accountDir, "${safeRecordId}_${System.currentTimeMillis()}_$safeName")

        val canonicalRoot = accountDir.canonicalPath + File.separator
        val canonicalTarget = targetFile.canonicalPath
        if (!canonicalTarget.startsWith(canonicalRoot)) {
            throw SecurityException("Artifact path escapes account-scoped storage directory.")
        }

        targetFile.writeBytes(bytes)
        val storedHash = sha256Hex(bytes)

        // Verify round-trip read integrity before reporting ARTIFACT_RETRIEVABLE
        if (!targetFile.exists() || !targetFile.canRead() || targetFile.length() != bytes.size.toLong()) {
            targetFile.delete()
            throw IllegalStateException("Stored artifact failed round-trip read verification.")
        }
        val readBackHash = sha256Hex(targetFile.readBytes())
        if (readBackHash != storedHash) {
            targetFile.delete()
            throw IllegalStateException("Stored artifact checksum mismatch.")
        }

        return StoredArtifactResult(
            artifactUri = targetFile.canonicalPath,
            artifactOwnerId = accountOwnerId,
            artifactContentHash = storedHash,
            displayFileName = safeName,
            byteSize = bytes.size.toLong(),
            validationState = SocialArtifactValidationState.ARTIFACT_RETRIEVABLE
        )
    }

    fun verifyRetrievable(mission: SocialMissionRecord): Boolean {
        if (mission.evidenceType != SocialMissionEvidenceType.ARTIFACT_ATTACHED) return false
        if (!mission.artifactValidationState.qualifiesForRealWorldMastery) return false
        val uriString = mission.artifactUri?.trim().orEmpty()
        val ownerId = mission.artifactOwnerId?.trim().orEmpty()
        val expectedHash = mission.artifactContentHash?.trim().orEmpty()
        if (uriString.isBlank() || ownerId.isBlank() || expectedHash.isBlank()) return false
        if (ownerId != mission.accountOwnerId) return false

        return if (uriString.startsWith("content://", ignoreCase = true)) {
            val resolver = context?.contentResolver ?: return false
            runCatching {
                resolver.openInputStream(Uri.parse(uriString))?.use { stream ->
                    val bytes = readBoundedBytes(stream)
                    bytes.isNotEmpty() && sha256Hex(bytes) == expectedHash
                } ?: false
            }.getOrDefault(false)
        } else {
            runCatching {
                val accountDir = getAccountArtifactDir(ownerId)
                val file = File(uriString)
                val canonicalRoot = accountDir.canonicalPath + File.separator
                val canonicalFile = file.canonicalPath
                if (!canonicalFile.startsWith(canonicalRoot)) return false
                if (!file.exists() || !file.isFile || !file.canRead() || file.length() <= 0L) return false
                sha256Hex(file.readBytes()) == expectedHash
            }.getOrDefault(false)
        }
    }

    fun validateSourcePathAllowedForAccount(accountOwnerId: String, sourceFile: File) {
        val canonicalSource = sourceFile.canonicalPath
        val canonicalVaultRoot = storageRootDir.canonicalPath + File.separator
        val canonicalOwnDir = getAccountArtifactDir(accountOwnerId).canonicalPath + File.separator
        if (canonicalSource.startsWith(canonicalVaultRoot) && !canonicalSource.startsWith(canonicalOwnDir)) {
            throw SecurityException("Cross-account artifact attachment reference is forbidden.")
        }
        val safeCurrentOwner = safeOwnerSegment(accountOwnerId)
        val parentDirName = sourceFile.parentFile?.name
        if (canonicalSource.contains(File.separator + DIRECTORY_NAME + File.separator) &&
            parentDirName != safeCurrentOwner
        ) {
            throw SecurityException("Cross-account artifact attachment reference is forbidden.")
        }
    }

    suspend fun deleteArtifact(accountOwnerId: String, artifactUri: String?): Boolean = withContext(Dispatchers.IO) {
        if (artifactUri.isNullOrBlank() || artifactUri.startsWith("content://", ignoreCase = true)) {
            return@withContext true
        }
        runCatching {
            val accountDir = getAccountArtifactDir(accountOwnerId)
            val file = File(artifactUri)
            val canonicalRoot = accountDir.canonicalPath + File.separator
            if (file.canonicalPath.startsWith(canonicalRoot) && file.exists()) {
                file.delete()
            } else {
                false
            }
        }.getOrDefault(false)
    }

    private fun queryDisplayName(uri: Uri): String? {
        val resolver = context?.contentResolver ?: return null
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }

    private fun readBoundedBytes(input: InputStream): ByteArray {
        val buffer = ByteArray(8192)
        val out = ByteArrayOutputStream()
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            if (total > MAX_ARTIFACT_BYTES) {
                throw IllegalArgumentException("Attached artifact exceeds 10 MB limit.")
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
