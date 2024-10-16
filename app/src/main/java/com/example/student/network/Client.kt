package com.example.student.network

import android.util.Log
import com.example.student.encryption.EncryptionDecryption
import com.google.gson.Gson
import com.example.student.models.ContentModel
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import kotlin.concurrent.thread

class Client (private val networkMessageInterface: NetworkMessageInterface){
    private lateinit var clientSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter
    var ip:String = ""
    private val encryptionDecryption = EncryptionDecryption()

    init {
        thread {
            clientSocket = Socket("192.168.49.1", Server.PORT)
            reader = clientSocket.inputStream.bufferedReader()
            writer = clientSocket.outputStream.bufferedWriter()
            ip = clientSocket.inetAddress.hostAddress!!
            while(true){
                try{
                    val serverResponse = reader.readLine()
                    if (serverResponse != null){
                        val serverContent = Gson().fromJson(serverResponse, ContentModel::class.java)
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
            writer.write("$contentAsStr\n")
            writer.flush()
        }
    }


//            if (studentId.isNotBlank() && studentId.toInt() >= 816000000 && studentId.toInt() <= 816999999) {
//                // Example: Hash and use the student ID
//                val hashedStudentId = encryptionDecryption.hashStrSha256(studentId)
//            } else {
////                Toast.makeText(this, "Please enter a valid student ID", Toast.LENGTH_SHORT).show()
//            }
//            val secretKeySeed = "816004029"
//
//            val aesKey = encryptionDecryption.generateAESKey(secretKeySeed)
//            val aesIV = encryptionDecryption.generateIV(secretKeySeed)
//
//            val encryptedMessage = encryptionDecryption.encryptMessage(content.message, aesKey, aesIV)
//            val encryptedContent = ContentModel(encryptedMessage, content.senderIp)

    fun close(){
        clientSocket.close()
    }
}