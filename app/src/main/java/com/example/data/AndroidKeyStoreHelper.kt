package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Gerenciador de chaves de criptografia no Android Keystore para backups automáticos locais.
 * As chaves nunca saem do hardware de segurança do dispositivo (TEE / StrongBox).
 */
object AndroidKeyStoreHelper {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "PrivaFin_Local_AutoBackup_Key_v2"
    private const val ALGORITHM = "AES/GCM/NoPadding"

    @Synchronized
    fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): String {
        val key = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Prefixo + IV (12 bytes) + CipherText
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        return "KEYSTORE_V2:" + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedString: String): String {
        if (!encryptedString.startsWith("KEYSTORE_V2:")) {
            throw IllegalArgumentException("Arquivo de auto-backup não está no formato do Keystore.")
        }
        val b64 = encryptedString.removePrefix("KEYSTORE_V2:")
        val combined = Base64.decode(b64, Base64.NO_WRAP)
        if (combined.size < 12) {
            throw IllegalArgumentException("Dados de backup corrompidos ou incompletos.")
        }

        val iv = ByteArray(12)
        val cipherText = ByteArray(combined.size - 12)
        System.arraycopy(combined, 0, iv, 0, 12)
        System.arraycopy(combined, 12, cipherText, 0, cipherText.size)

        val key = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
