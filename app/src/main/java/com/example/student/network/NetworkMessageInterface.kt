package com.example.student.network

import com.example.student.models.ContentModel

/// This [NetworkMessageInterface] acts as an interface.
interface NetworkMessageInterface {
    fun onContent(content: ContentModel)
}