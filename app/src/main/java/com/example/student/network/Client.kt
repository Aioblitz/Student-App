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
        val initialMessage = ContentModel("I am here", ip, null)
        sendMessage(initialMessage)
        Log.d("CLIENT", "Sent initial 'I am here' message")

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

            val hashedID = encryptionDecryption.hashStrSha256(studentId)
            aesKey = encryptionDecryption.generateAESKey(hashedID)
            aesIV = encryptionDecryption.generateIV(hashedID)

            val encryptedNonce = encryptionDecryption.encryptMessage(nonce, aesKey, aesIV)
            val responseMessage = ContentModel(encryptedNonce, ip, null)
            sendMessage(responseMessage)

            Log.d("CLIENT", "Sent encrypted nonce response to server")
            authenticated = true

            // Notify the UI that the client is authenticated
            runOnUiThread {
                networkMessageInterface.onContent(serverContent)
            }

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
            val decryptedMessage = encryptionDecryption.decryptMessage(content.message, aesKey, aesIV)
            Log.d("CLIENT", "Decrypted message from server: $decryptedMessage")

            val jsonObject = JSONObject(decryptedMessage)
            val message = jsonObject.getString("message")
            val senderIp = jsonObject.getString("senderIp")

            val decryptedContent = ContentModel(message, senderIp)
            runOnUiThread {
                networkMessageInterface.onContent(decryptedContent)
            }

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

    // Helper function to run code on the main thread
    private fun runOnUiThread(action: () -> Unit) {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        mainHandler.post(action)
    }
}