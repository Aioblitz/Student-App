package com.example.student.network

import android.util.Log
import com.example.student.encryption.EncryptionDecryption
import com.example.student.models.ContentModel
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread

class Client(private val networkMessageInterface: NetworkMessageInterface, private val studentID: String) {
    private lateinit var clientSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter
    private lateinit var aesKey: SecretKeySpec
    private lateinit var aesIV: IvParameterSpec
    private val encryptionDecryption = EncryptionDecryption()
    var ip: String = ""

    init {
        connectToServer()
    }

    private fun connectToServer() {
        thread {
            try {
                Log.d("CLIENT", "Attempting to connect to server...")
                clientSocket = Socket("192.168.49.1", Server.PORT)
                reader = clientSocket.inputStream.bufferedReader()
                writer = clientSocket.outputStream.bufferedWriter()
                ip = clientSocket.inetAddress.hostAddress!!
                Log.d("CLIENT", "Connected to server at $ip")

                // Send initial message to server
                sendMessage(ContentModel("I am here", ip, studentID))
                Log.d("CLIENT", "Sending 'I am here' message with student ID: $studentID")

                while (true) {
                    val serverResponse = reader.readLine()
                    if (serverResponse != null) {
                        Log.d("CLIENT", "Received response from server: $serverResponse")
                        handleServerResponse(serverResponse)
                    }
                }
            } catch (e: Exception) {
                Log.e("CLIENT", "An error has occurred in the client")
                e.printStackTrace()
            }
        }
    }

    private fun handleServerResponse(serverResponse: String) {
        try {
            val serverContent = Gson().fromJson(serverResponse, ContentModel::class.java)
            if (serverContent.message != null && serverContent.message.length == 32) {
                // Received nonce from server, encrypt it with student ID
                Log.d("CLIENT", "Received nonce (R) from server: $serverContent")
                val hashedID = encryptionDecryption.hashStrSha256(studentID)
                aesKey = encryptionDecryption.generateAESKey(hashedID)
                aesIV = encryptionDecryption.generateIV(hashedID)
                val encryptedNonce = encryptionDecryption.encryptMessage(serverContent.message, aesKey, aesIV)
                sendMessage(ContentModel(encryptedNonce, ip, studentID))
                Log.d("CLIENT", "Sending encrypted nonce (R) back to server")
            } else {
                // Handle regular messages
                val decryptedMessage = encryptionDecryption.decryptMessage(serverContent.message, aesKey, aesIV)
                val decryptedContent = Gson().fromJson(decryptedMessage, ContentModel::class.java)
                networkMessageInterface.onContent(decryptedContent)
            }
        } catch (e: Exception) {
            Log.e("CLIENT", "Error handling server response")
            e.printStackTrace()
        }
    }

    fun sendMessage(content: ContentModel) {
        thread {
            try {
                if (!clientSocket.isConnected) {
                    throw Exception("We aren't currently connected to the server!")
                }
                val contentAsStr: String = Gson().toJson(content)
                writer.write("$contentAsStr\n")
                writer.flush()
            } catch (e: Exception) {
                Log.e("CLIENT", "Failed to send message")
                e.printStackTrace()
            }
        }
    }

    fun close() {
        try {
            clientSocket.close()
        } catch (e: Exception) {
            Log.e("CLIENT", "Error closing client socket")
            e.printStackTrace()
        }
    }
}