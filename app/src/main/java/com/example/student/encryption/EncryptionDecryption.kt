package com.example.student.encryption

import android.os.Build
import androidx.annotation.RequiresApi
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.util.Base64

class EncryptionDecryption {

    @RequiresApi(Build.VERSION_CODES.O)
    fun hashStrSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }

    fun generateAESKey(hashedID: String): SecretKeySpec {
        val keyBytes = hashedID.toByteArray(Charsets.UTF_8).copyOf(16)
        return SecretKeySpec(keyBytes, "AES")
    }

    fun generateIV(hashedID: String): IvParameterSpec {
        val ivBytes = hashedID.toByteArray(Charsets.UTF_8).copyOf(16)
        return IvParameterSpec(ivBytes)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun encryptMessage(message: String, key: SecretKeySpec, iv: IvParameterSpec): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val encrypted = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encrypted)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun decryptMessage(encryptedMessage: String, key: SecretKeySpec, iv: IvParameterSpec): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val decodedBytes = Base64.getDecoder().decode(encryptedMessage)
        val decrypted = cipher.doFinal(decodedBytes)
        return String(decrypted, Charsets.UTF_8)
    }
}