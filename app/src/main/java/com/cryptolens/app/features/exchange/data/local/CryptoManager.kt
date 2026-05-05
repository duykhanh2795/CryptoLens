package com.cryptolens.app.features.exchange.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM encryption using Android Keystore.
 * Keys never leave the secure hardware — no key material stored in app.
 *
 * Format of encrypted output (base64): [12-byte IV][ciphertext]
 */
@Singleton
class CryptoManager @Inject constructor() {

    companion object {
        private const val KEY_ALIAS      = "cryptolens_exchange_key"
        private const val KEYSTORE       = "AndroidKeyStore"
        private const val ALGORITHM      = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE     = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING        = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128  // bits
        private const val IV_LENGTH      = 12   // bytes
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).also { it.load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        return KeyGenerator.getInstance(ALGORITHM, KEYSTORE)
            .also { it.init(spec) }
            .generateKey()
    }

    /**
     * Encrypt [plainText] and return base64-encoded [IV + ciphertext].
     */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv         = cipher.iv                          // 12 bytes GCM IV
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined   = iv + ciphertext                    // prepend IV
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt base64-encoded [encryptedText] (produced by [encrypt]).
     * Returns null if decryption fails (corrupted data / wrong key).
     */
    fun decrypt(encryptedText: String): String? {
        return try {
            val combined   = Base64.decode(encryptedText, Base64.NO_WRAP)
            val iv         = combined.copyOfRange(0, IV_LENGTH)
            val ciphertext = combined.copyOfRange(IV_LENGTH, combined.size)
            val cipher     = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
