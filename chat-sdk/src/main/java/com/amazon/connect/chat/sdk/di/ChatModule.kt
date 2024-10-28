// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.di

import android.content.Context
import com.amazon.connect.chat.sdk.ChatSession
import com.amazon.connect.chat.sdk.ChatSessionImpl
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.repository.AttachmentsManager
import com.amazon.connect.chat.sdk.repository.MessageReceiptsManager
import com.amazon.connect.chat.sdk.network.WebSocketManager
import com.amazon.connect.chat.sdk.repository.MetricsManager
import com.amazon.connect.chat.sdk.network.NetworkConnectionManager
import com.amazon.connect.chat.sdk.network.WebSocketManagerImpl
import com.amazon.connect.chat.sdk.repository.ChatService
import com.amazon.connect.chat.sdk.repository.ChatServiceImpl
import com.amazon.connect.chat.sdk.provider.ConnectionDetailsProvider
import com.amazon.connect.chat.sdk.provider.ConnectionDetailsProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
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
     * @param awsClient The AWS client for connecting to AWS services.
     * @param connectionDetailsProvider The provider for connection details.
     * @param webSocketManager The WebSocket manager for managing WebSocket connections.
     * @param metricsManager The metrics manager for managing metrics.
     * @return An instance of ChatServiceImpl.
     */
    @Provides
    @Singleton
    fun provideChatService(
        context: Context,
        awsClient: AWSClient,
        connectionDetailsProvider: ConnectionDetailsProvider,
        webSocketManager: WebSocketManager,
        metricsManager: MetricsManager,
        attachmentsManager: AttachmentsManager,
        messageReceiptsManager: MessageReceiptsManager,
    ): ChatService {
        return ChatServiceImpl(context, awsClient, connectionDetailsProvider,
            webSocketManager, metricsManager, attachmentsManager, messageReceiptsManager)
    }

    /**
     * Entry point for Hilt to provide ChatSession.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ChatSessionEntryPoint {
        fun getChatSession(): ChatSession
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
     * Provides a singleton instance of Context.
     *
     * @param appContext The application context.
     * @return An instance of Context.
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext appContext: Context): Context {
        return appContext
    }

    /**
     * Provides a singleton instance of NetworkConnectionManager.
     *
     * @param networkConnectionManager The network connection manager.
     * @return An instance of NetworkConnectionManager.
     */
    @Provides
    @Singleton
    fun provideWebSocketManager(
        networkConnectionManager: NetworkConnectionManager,
    ): WebSocketManager {
        return WebSocketManagerImpl(networkConnectionManager = networkConnectionManager)
    }
}
