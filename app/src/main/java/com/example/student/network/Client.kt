package com.example.student.network

import android.util.Log
import com.google.gson.Gson
import com.example.student.models.ContentModel
import com.example.student.encryption.EncryptionDecryption
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread

class Client (private val networkMessageInterface: NetworkMessageInterface,studentID: String){
    private lateinit var clientSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter
    private lateinit var aesKey: SecretKeySpec
    private lateinit var aesIV: IvParameterSpec
    private val encryptionDecryption = EncryptionDecryption()
    var ip:String = ""


    init {
        thread {
            clientSocket = Socket("192.168.49.1", Server.PORT)
            reader = clientSocket.inputStream.bufferedReader()
            writer = clientSocket.outputStream.bufferedWriter()
            ip = clientSocket.inetAddress.hostAddress!!
            val hashedID = encryptionDecryption.hashStrSha256(studentID)
            aesKey = encryptionDecryption.generateAESKey(hashedID)
            aesIV = encryptionDecryption.generateIV(hashedID)

            try {
                val receivedR = reader.readLine()
                if (receivedR != null) {
                    Log.d("CLIENT", "Received R: $receivedR")
                    val encryptedR = encryptionDecryption.encryptMessage(receivedR, aesKey, aesIV)
                    val content = ContentModel(encryptedR,ip)
                    sendMessage(content)
                }

            }catch (e: Exception){
                Log.e("CLIENT", "Error receiving R")
                e.printStackTrace()
            }
            while(true){
                try{
                    val serverResponse = reader.readLine()
                    if (serverResponse != null){
                        val decryptedMessage = encryptionDecryption.decryptMessage(serverResponse,aesKey,aesIV)
                        val serverContent = Gson().fromJson(decryptedMessage, ContentModel::class.java)
                        networkMessageInterface.onContent(serverContent)
                    }
                } catch(e: Exception){
                    Log.e("CLIENT", "An error has occurred in the client")
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    fun sendMessage(content: ContentModel) {
        thread {
            if (!clientSocket.isConnected) {
                throw Exception("We aren't currently connected to the server!")
            }
            val contentAsStr: String = Gson().toJson(content)
            val encryptedMessage = encryptionDecryption.encryptMessage(contentAsStr, aesKey, aesIV)
            writer.write("$encryptedMessage\n")
            writer.flush()
        }
    }

    fun sendHiddenMessage(content: ContentModel) {
        thread {
            if (!clientSocket.isConnected) {
                throw Exception("We aren't currently connected to the server!")
            }
            val contentAsStr: String = Gson().toJson(content)
            writer.write("$contentAsStr\n")
            writer.flush()
        }
    }

    fun close(){
        clientSocket.close()
    }
}