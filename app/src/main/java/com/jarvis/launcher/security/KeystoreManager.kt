package com.jarvis.launcher.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keystore Manager - Secure encryption for sensitive data
 *
 * Uses Android Keystore System to encrypt/decrypt API keys
 * Keys never leave the secure hardware
 *
 * Features:
 * - AES-256 encryption
 * - GCM mode for authenticated encryption
 * - Hardware-backed security (when available)
 * - Automatic key generation
 */
class KeystoreManager(private val context: Context) {

    companion object {
        private const val TAG = "KeystoreManager"
        private const val KEY_ALIAS = "jarvis_api_key_encryption"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SEPARATOR = "]" // Separator between IV and ciphertext
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    init {
        // Generate key if it doesn't exist
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }

    /**
     * Encrypt sensitive data (e.g., API keys)
     */
    fun encrypt(plaintext: String): String {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            // Encrypt the data
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // Get the IV (Initialization Vector)
            val iv = cipher.iv

            // Combine IV and ciphertext for storage
            // Format: [Base64(IV)][Base64(Ciphertext)]
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

            return "$ivBase64$IV_SEPARATOR$ciphertextBase64"

        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw EncryptionException("Failed to encrypt data", e)
        }
    }

    /**
     * Decrypt sensitive data
     */
    fun decrypt(encryptedData: String): String {
        try {
            // Split IV and ciphertext
            val parts = encryptedData.split(IV_SEPARATOR)
            if (parts.size != 2) {
                throw EncryptionException("Invalid encrypted data format")
            }

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)

            // Decrypt
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val plaintext = cipher.doFinal(ciphertext)
            return String(plaintext, Charsets.UTF_8)

        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw EncryptionException("Failed to decrypt data", e)
        }
    }

    /**
     * Check if data is encrypted (contains IV separator)
     */
    fun isEncrypted(data: String): Boolean {
        return data.contains(IV_SEPARATOR) && data.split(IV_SEPARATOR).size == 2
    }

    /**
     * Generate encryption key in Keystore
     */
    private fun generateKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false) // No biometric required
                .setRandomizedEncryptionRequired(true) // Random IV for each encryption
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()

            Log.d(TAG, "Encryption key generated successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Key generation failed", e)
            throw EncryptionException("Failed to generate encryption key", e)
        }
    }

    /**
     * Get the secret key from Keystore
     */
    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Delete encryption key (for security reset)
     */
    fun deleteKey() {
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
                Log.d(TAG, "Encryption key deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Key deletion failed", e)
        }
    }

    /**
     * Check if encryption is available
     */
    fun isAvailable(): Boolean {
        return try {
            keyStore.containsAlias(KEY_ALIAS) || generateKey() != null
            true
        } catch (e: Exception) {
            Log.e(TAG, "Keystore not available", e)
            false
        }
    }
}

/**
 * Custom exception for encryption errors
 */
class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
