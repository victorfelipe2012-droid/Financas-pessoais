package com.example.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utilitário de criptografia para exportação e importação manual de backups protegidos por senha.
 * - Versão 2: PBKDF2-HMAC-SHA256 (65.536 iterações) + Salt aleatório de 16 bytes + IV aleatório de 12 bytes + AES-256-GCM.
 * - Leitor legado compatível com backups antigos v1.
 */
object CryptoHelper {

    private const val GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val MODERN_KDF = "PBKDF2WithHmacSHA256"
    private const val MODERN_ITERATIONS = 65536
    private const val KEY_LENGTH = 256

    // Constantes do formato legado v1 (mantidas estritamente para leitura)
    private const val LEGACY_KDF = "PBKDF2WithHmacSHA1"
    private const val LEGACY_ITERATIONS = 1000
    private const val LEGACY_SALT = "PrivaFinSalt123#"

    /**
     * Criptografa dados em formato modernizado v2 com salt e IV aleatórios.
     * Formato de saída: "PRIVAFIN2:<iterations>:<saltB64>:<ivB64>:<cipherTextB64>"
     */
    fun encryptModern(plainText: String, password: CharArray): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val iv = ByteArray(12)
        random.nextBytes(iv)

        val spec = PBEKeySpec(password, salt, MODERN_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(MODERN_KDF)
        val keyBytes = factory.generateSecret(spec).encoded
        val keySpec = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(GCM_ALGORITHM)
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)

        return "PRIVAFIN2:$MODERN_ITERATIONS:$saltB64:$ivB64:$cipherB64"
    }

    /**
     * Descriptografa arquivo detectando automaticamente se é o formato moderno v2 ou legado v1.
     */
    fun decrypt(encryptedContent: String, password: CharArray): String {
        val trimmed = encryptedContent.trim()

        return if (trimmed.startsWith("PRIVAFIN2:")) {
            decryptModern(trimmed, password)
        } else {
            decryptLegacy(trimmed, password)
        }
    }

    private fun decryptModern(content: String, password: CharArray): String {
        val parts = content.split(":")
        if (parts.size != 5) {
            throw IllegalArgumentException("Cabeçalho do backup moderno corrompido.")
        }

        val iterations = parts[1].toIntOrNull() ?: MODERN_ITERATIONS
        val salt = Base64.decode(parts[2], Base64.NO_WRAP)
        val iv = Base64.decode(parts[3], Base64.NO_WRAP)
        val cipherText = Base64.decode(parts[4], Base64.NO_WRAP)

        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(MODERN_KDF)
        val keyBytes = factory.generateSecret(spec).encoded
        val keySpec = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(GCM_ALGORITHM)
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        val decrypted = cipher.doFinal(cipherText)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun decryptLegacy(cipherTextB64: String, password: CharArray): String {
        val saltBytes = LEGACY_SALT.toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(password, saltBytes, LEGACY_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(LEGACY_KDF)
        val keyBytes = factory.generateSecret(spec).encoded
        val keySpec = SecretKeySpec(keyBytes, "AES")

        val combined = Base64.decode(cipherTextB64, Base64.DEFAULT)
        if (combined.size < 12) {
            throw IllegalArgumentException("Arquivo de backup legado corrompido ou inválido.")
        }
        val iv = ByteArray(12)
        val cipherText = ByteArray(combined.size - 12)
        System.arraycopy(combined, 0, iv, 0, 12)
        System.arraycopy(combined, 12, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance(GCM_ALGORITHM)
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
