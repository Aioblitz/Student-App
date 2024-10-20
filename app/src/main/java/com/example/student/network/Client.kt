package com.example.student.network

import android.util.Log
import com.example.student.encryption.EncryptionDecryption
import com.example.student.models.ContentModel
import com.google.gson.Gson
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread

class Client(
    private val networkMessageInterface: NetworkMessageInterface,
    private val studentId: String
) {
    private lateinit var clientSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter
    private var authenticated = false
    private lateinit var aesKey: SecretKeySpec
    private lateinit var aesIV: IvParameterSpec
    private val encryptionDecryption = EncryptionDecryption()
    var ip: String = ""

    init {
        thread {
            try {
                connectToServer()
                authenticateWithServer()
                listenForMessages()
            } catch (e: Exception) {
                Log.e("CLIENT", "An error occurred in the client initialization", e)
            }
        }
    }

    private fun connectToServer() {
        clientSocket = Socket("192.168.49.1", Server.PORT)
        reader = clientSocket.inputStream.bufferedReader()
        writer = clientSocket.outputStream.bufferedWriter()
        ip = clientSocket.inetAddress.hostAddress!!
        Log.d("CLIENT", "Connected to server at 192.168.49.1")
    }

    private fun authenticateWithServer() {
        val initialMessage = ContentModel("I am here", ip, studentId)
        sendMessage(initialMessage)
        Log.d("CLIENT", "Sent initial 'I am here' message with student ID: $studentId")

        while (!authenticated) {
            val serverResponse = reader.readLine()
            if (serverResponse != null) {
                Log.d("CLIENT", "Received response from server: $serverResponse")
                val serverContent = Gson().fromJson(serverResponse, ContentModel::class.java)
                handleChallengeResponse(serverContent)
            }
        }
    }

    private fun handleChallengeResponse(serverContent: ContentModel) {
        try {
            val nonce = serverContent.message
            Log.d("CLIENT", "Received nonce from server: $nonce")

            // Generate AES key and IV using the hashed student ID (must match the server's method)
            val hashedID = encryptionDecryption.hashStrSha256(studentId)
            aesKey = encryptionDecryption.generateAESKey(hashedID)
            aesIV = encryptionDecryption.generateIV(hashedID)

            // Encrypt the nonce with AES and send it back to the server
            val encryptedNonce = encryptionDecryption.encryptMessage(nonce, aesKey, aesIV)
            val responseMessage = ContentModel(encryptedNonce, ip, studentId)
            sendMessage(responseMessage)

            Log.d("CLIENT", "Sent encrypted nonce response to server")

        } catch (e: Exception) {
            Log.e("CLIENT", "Error handling challenge response: ${e.message}")
        }
    }

    private fun listenForMessages() {
        while (clientSocket.isConnected) {
            try {
                val serverMessage = reader.readLine()
                if (serverMessage != null) {
                    Log.d("CLIENT", "Received message from server: $serverMessage")
                    val content = Gson().fromJson(serverMessage, ContentModel::class.java)
                    handleServerMessage(content)
                }
            } catch (e: Exception) {
                Log.e("CLIENT", "Error while listening for messages: ${e.message}")
            }
        }
    }

    private fun handleServerMessage(content: ContentModel) {
        try {
            // Decrypt the server's message using the AES key and IV
            val decryptedMessage = encryptionDecryption.decryptMessage(content.message, aesKey, aesIV)
            Log.d("CLIENT", "Decrypted message from server: $decryptedMessage")

            // Assuming the decrypted message is in JSON format, parse it
            val jsonObject = JSONObject(decryptedMessage)
            val message = jsonObject.getString("message")
            val senderIp = jsonObject.getString("senderIp")

            // Create a ContentModel object with the decrypted data
            val decryptedContent = ContentModel(message, senderIp)

            // Pass the ContentModel to the UI via the interface
            networkMessageInterface.onContent(decryptedContent)

        } catch (e: Exception) {
            Log.e("CLIENT", "Error decrypting message: ${e.message}")
        }
    }

    fun sendMessage(content: ContentModel) {
        val message = Gson().toJson(content)
        writer.write("$message\n")
        writer.flush()
        Log.d("CLIENT", "Sent message: $message")
    }

    fun close() {
        try {
            clientSocket.close()
            Log.d("CLIENT", "Client socket closed")
        } catch (e: Exception) {
            Log.e("CLIENT", "Error closing client socket", e)
        }
    }
}