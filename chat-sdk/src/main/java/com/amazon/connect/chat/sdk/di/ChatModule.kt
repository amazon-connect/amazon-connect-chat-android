package com.amazon.connect.chat.sdk.di

import com.amazon.connect.chat.sdk.ChatSession
import com.amazon.connect.chat.sdk.ChatSessionImpl
import com.amazon.connect.chat.sdk.network.WebSocketManager
import com.amazon.connect.chat.sdk.repository.ChatService
import com.amazon.connect.chat.sdk.repository.ChatServiceImpl
import com.amazon.connect.chat.sdk.repository.ConnectionDetailsProvider
import com.amazon.connect.chat.sdk.repository.ConnectionDetailsProviderImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {

    @Binds
    @Singleton
    abstract fun bindChatService(impl: ChatServiceImpl): ChatService

    @Binds
    @Singleton
    abstract fun bindChatSession(impl: ChatSessionImpl): ChatSession

    @Binds
    @Singleton
    abstract fun bindConnectionDetailsProvider(impl: ConnectionDetailsProviderImpl): ConnectionDetailsProvider

    companion object {

        @Provides
        @Singleton
        fun provideWebSocketManager(): WebSocketManager {
            return WebSocketManager()
        }
    }
}