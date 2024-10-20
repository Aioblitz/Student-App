package com.example.student.encryption

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class EncryptionDecryption {

    fun hashStrSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateAESKey(hashedID: String): SecretKeySpec {
        val keyBytes = hashedID.toByteArray().copyOf(16) // Use the first 16 bytes for AES-128
        return SecretKeySpec(keyBytes, "AES")
    }

    fun generateIV(hashedID: String): IvParameterSpec {
        val ivBytes = hashedID.toByteArray().copyOf(16) // Use the first 16 bytes for IV
        return IvParameterSpec(ivBytes)
    }

    fun encryptMessage(message: String, key: SecretKeySpec, iv: IvParameterSpec): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val encryptedBytes = cipher.doFinal(message.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    fun decryptMessage(encryptedMessage: String, key: SecretKeySpec, iv: IvParameterSpec): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val decodedBytes = Base64.decode(encryptedMessage, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes)
    }
}