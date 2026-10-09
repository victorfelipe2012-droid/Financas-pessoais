package com.example.data

/**
 * Interface injetável para operações de criptografia de auto-backups e snapshots.
 * Em produção, delega para o AndroidKeyStoreHelper diretamente, utilizando hardware de segurança.
 * Em ambientes de teste unitário sem provider AndroidKeyStore, permite a injeção de provedor alternativo.
 */
interface BackupCryptoProvider {
    fun encrypt(plainText: String): String
    fun decrypt(encryptedString: String): String
}

class AndroidKeyStoreCryptoProvider : BackupCryptoProvider {
    override fun encrypt(plainText: String): String = AndroidKeyStoreHelper.encrypt(plainText)
    override fun decrypt(encryptedString: String): String = AndroidKeyStoreHelper.decrypt(encryptedString)
}
