package com.amazon.connect.chat.sdk

import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.GlobalConfig
import com.amazon.connect.chat.sdk.repository.ChatService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ChatSession {
    fun configure(config: GlobalConfig)
    /**
     * Connects to a chat session with the specified chat details.
     * @param chatDetails The details of the chat.
     * @return A Result indicating whether the connection was successful.
     */
    suspend fun connect(chatDetails: ChatDetails): Result<Unit>

    /**
     * Disconnects the current chat session.
     * @return A Result indicating whether the disconnection was successful.
     */
    suspend fun disconnect(): Result<Unit>
}

@Singleton
class ChatSessionImpl @Inject constructor(private val chatService: ChatService) : ChatSession {

    override fun configure(config: GlobalConfig) {
        chatService.configure(config)
    }

    override suspend fun connect(chatDetails: ChatDetails): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                chatService.createChatSession(chatDetails)
                Result.success(Unit)
            }.getOrElse {
                Result.failure(it)
            }
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                chatService.disconnectChatSession()
                Result.success(Unit)
            }.getOrElse {
                Result.failure(it)
            }
        }
    }
}