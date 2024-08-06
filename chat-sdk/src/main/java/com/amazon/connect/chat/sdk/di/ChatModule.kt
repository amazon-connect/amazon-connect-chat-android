package com.amazon.connect.chat.sdk.di

import com.amazon.connect.chat.sdk.ChatSession
import com.amazon.connect.chat.sdk.network.APIClient
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.network.WebSocketManager
import com.amazon.connect.chat.sdk.repository.ChatService
import com.amazon.connect.chat.sdk.repository.ConnectionDetailProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides
    @Singleton
    fun provideChatSession(chatService: ChatService): ChatSession {
        return ChatSession(chatService)
    }

    @Provides
    @Singleton
    fun provideChatService(
        apiClient: APIClient,
        awsClient: AWSClient,
        connectionDetailProvider: ConnectionDetailProvider
    ): ChatService {
        return ChatService(apiClient, awsClient, connectionDetailProvider)
    }

    @Provides
    @Singleton
    fun provideWebSocketManager(): WebSocketManager {
        return WebSocketManager()
    }

    @Provides
    @Singleton
    fun provideConnectionDetailProvider(): ConnectionDetailProvider {
        return ConnectionDetailProvider()
    }
}
