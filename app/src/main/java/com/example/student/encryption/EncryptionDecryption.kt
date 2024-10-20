package com.example.student.encryption

import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.SecretKey
import javax.crypto.Cipher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import java.security.SecureRandom

class EncryptionDecryption {

    fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun getFirstNChars(input: String, n: Int): String {
        return if (input.length < n) {
            input.padEnd(n, '0') // Pad with zeros if the input is shorter than required
        } else {
            input.substring(0, n)
        }
    }

    fun hashStrSha256(str: String): String {
        val algorithm = "SHA-256"
        val hashedString = MessageDigest.getInstance(algorithm).digest(str.toByteArray(UTF_8))
        return hashedString.toHex()
    }

    fun generateAESKey(seed: String): SecretKeySpec {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val hashedKey = sha256.digest(seed.toByteArray(UTF_8))
        return SecretKeySpec(hashedKey.copyOf(16), "AES")  // Use first 16 bytes for AES-128
    }

    fun generateIV(seed: String): IvParameterSpec {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val hashedIv = sha256.digest(seed.toByteArray(UTF_8))
        return IvParameterSpec(hashedIv.copyOf(16))  // AES requires a 16-byte IV
    }


    @OptIn(ExperimentalEncodingApi::class)
    fun encryptMessage(plaintext: String, aesKey: SecretKey, aesIv: IvParameterSpec): String {
        val plainTextByteArr = plaintext.toByteArray()

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")

        cipher.init(Cipher.ENCRYPT_MODE, aesKey, aesIv)

        val encrypt = cipher.doFinal(plainTextByteArr)
        return Base64.Default.encode(encrypt)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decryptMessage(encryptedText: String, aesKey: SecretKey, aesIv: IvParameterSpec): String {
        val textToDecrypt = Base64.Default.decode(encryptedText)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE, aesKey, aesIv)

        val decrypt = cipher.doFinal(textToDecrypt)
        return String(decrypt)
    }
}