package com.amazon.connect.chat.sdk.di

import android.content.Context
import com.amazon.connect.chat.sdk.ChatSession
import com.amazon.connect.chat.sdk.ChatSessionImpl
import com.amazon.connect.chat.sdk.network.APIClient
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.network.WebSocketManager
import com.amazon.connect.chat.sdk.repository.ChatService
import com.amazon.connect.chat.sdk.repository.ChatServiceImpl
import com.amazon.connect.chat.sdk.repository.ConnectionDetailsProvider
import com.amazon.connect.chat.sdk.repository.ConnectionDetailsProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    /**
     * Provides a singleton instance of ChatService.
     *
     * @param apiClient The API client for network operations.
     * @param awsClient The AWS client for connecting to AWS services.
     * @param connectionDetailsProvider The provider for connection details.
     * @return An instance of ChatServiceImpl.
     */
    @Provides
    @Singleton
    fun provideChatService(
        apiClient: APIClient,
        awsClient: AWSClient,
        connectionDetailsProvider: ConnectionDetailsProvider
    ): ChatService {
        return ChatServiceImpl(apiClient, awsClient, connectionDetailsProvider)
    }

    /**
     * Provides a singleton instance of ChatSession.
     *
     * @param chatService The chat service for managing chat sessions.
     * @return An instance of ChatSessionImpl.
     */
    @Provides
    @Singleton
    fun provideChatSession(chatService: ChatService): ChatSession {
        return ChatSessionImpl(chatService)
    }

    /**
     * Provides a singleton instance of ConnectionDetailsProvider.
     *
     * @return An instance of ConnectionDetailsProviderImpl.
     */
    @Provides
    @Singleton
    fun provideConnectionDetailsProvider(): ConnectionDetailsProvider {
        return ConnectionDetailsProviderImpl()
    }

    /**
     * Provides a singleton instance of WebSocketManager.
     *
     * @return An instance of WebSocketManager.
     */
    @Provides
    @Singleton
    fun provideConnectionDetailProvider(): ConnectionDetailProvider {
        return ConnectionDetailProvider()
    }

    // Provide the Context dependency
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext appContext: Context): Context {
        return appContext
    }

    @Provides
    @Singleton
    fun provideWebSocketManager(
        context: Context,
    ): WebSocketManager {
        return WebSocketManager(context, {})
    }
}
