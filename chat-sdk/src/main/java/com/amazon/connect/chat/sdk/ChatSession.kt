package com.amazon.connect.chat.sdk

import com.amazon.connect.chat.sdk.repository.ChatService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatSession @Inject constructor(private val chatService: ChatService) {

    fun initializeSession(userId: String) {
        chatService.initializeSession(userId)
    }

    fun sendMessage(message: String) {
        chatService.sendMessage(message)
    }

    fun closeSession() {
        chatService.closeSession()
    }
}
