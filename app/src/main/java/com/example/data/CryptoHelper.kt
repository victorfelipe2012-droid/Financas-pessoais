package com.example.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 1000
    private const val KEY_LENGTH = 256
    private const val SALT = "PrivaFinSalt123#" // Standard salt for local device key derivation

    fun deriveKey(password: String): SecretKeySpec {
        val saltBytes = SALT.toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun encrypt(plainText: String, keySpec: SecretKeySpec): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val secureRandom = SecureRandom()
        val iv = ByteArray(12) // GCM standard IV size is 12 bytes
        secureRandom.nextBytes(iv)
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Combine IV and CipherText
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(cipherTextB64: String, keySpec: SecretKeySpec): String {
        val combined = Base64.decode(cipherTextB64, Base64.DEFAULT)
        if (combined.size < 12) {
            throw IllegalArgumentException("Invalid encrypted backup data size.")
        }
        val iv = ByteArray(12)
        val cipherText = ByteArray(combined.size - 12)
        System.arraycopy(combined, 0, iv, 0, 12)
        System.arraycopy(combined, 12, cipherText, 0, cipherText.size)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
