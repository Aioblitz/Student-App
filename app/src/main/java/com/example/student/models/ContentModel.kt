package com.example.student.models

/// The [ContentModel] class represents data that is transferred between devices when multiple
/// devices communicate with each other.
data class ContentModel(val message:String, var senderIp:String)