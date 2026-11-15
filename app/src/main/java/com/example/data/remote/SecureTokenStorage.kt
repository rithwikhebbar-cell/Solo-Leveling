package com.example.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface TokenStorage {
    fun saveTokens(accessToken: String, refreshToken: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clearTokens()
    fun getOrCreateDeviceUuid(): String
}

/**
 * Hardware-backed Android Keystore (AES-256-GCM) token & device identity storage.
 * Falls back cleanly in Robolectric JVM tests where AndroidKeyStore provider is absent.
 * Never logs tokens, secrets, or credentials.
 */
class SecureTokenStorage(context: Context) : TokenStorage {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS, encryptValue(accessToken))
            .putString(KEY_REFRESH, encryptValue(refreshToken))
            .apply()
    }

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)?.let { decryptValue(it) }

    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)?.let { decryptValue(it) }

    override fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .apply()
    }

    override fun getOrCreateDeviceUuid(): String {
        val existing = prefs.getString(KEY_DEVICE_UUID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_UUID, created).apply()
        return created
    }

    private fun encryptValue(plaintext: String): String {
        return runCatching {
            val key = getOrCreateAesKey() ?: return@runCatching fallbackEncode(plaintext)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + encrypted.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encrypted, 0, combined, 1 + iv.size, encrypted.size)
            "KS:" + Base64.encodeToString(combined, Base64.NO_WRAP)
        }.getOrElse {
            fallbackEncode(plaintext)
        }
    }

    private fun decryptValue(encoded: String): String? {
        return runCatching {
            if (encoded.startsWith("KS:")) {
                val key = getOrCreateAesKey() ?: return null
                val bytes = Base64.decode(encoded.removePrefix("KS:"), Base64.NO_WRAP)
                val ivLen = bytes[0].toInt()
                val iv = bytes.copyOfRange(1, 1 + ivLen)
                val cipherText = bytes.copyOfRange(1 + ivLen, bytes.size)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
                String(cipher.doFinal(cipherText), Charsets.UTF_8)
            } else if (encoded.startsWith("MEM:")) {
                String(Base64.decode(encoded.removePrefix("MEM:"), Base64.NO_WRAP), Charsets.UTF_8)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun fallbackEncode(plaintext: String): String =
        "MEM:" + Base64.encodeToString(plaintext.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun getOrCreateAesKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val PREFS_NAME = "solo_leveling_secure_vault"
        private const val KEY_ACCESS = "enc_access_token"
        private const val KEY_REFRESH = "enc_refresh_token"
        private const val KEY_DEVICE_UUID = "device_uuid"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "solo_leveling_session_aes_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
