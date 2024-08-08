package com.amazon.connect.chat.sdk.repository

import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ConnectionDetails
import javax.inject.Inject
import javax.inject.Singleton

interface ConnectionDetailsProvider {
    fun updateChatDetails(newDetails: ChatDetails)
    fun getConnectionDetails(): ConnectionDetails?
    fun updateConnectionDetails(newDetails: ConnectionDetails)
    fun getChatDetails(): ChatDetails?
    fun isChatSessionActive(): Boolean
    fun setChatSessionState(isActive: Boolean)
}

@Singleton
class ConnectionDetailsProviderImpl @Inject constructor() : ConnectionDetailsProvider {
    private var connectionDetails: ConnectionDetails? = null
    private var chatDetails: ChatDetails? = null
    private var isChatActive: Boolean = false

    override fun updateConnectionDetails(newDetails: ConnectionDetails) {
        connectionDetails = newDetails
    }

    override fun updateChatDetails(newDetails: ChatDetails) {
        chatDetails = newDetails
    }

    override fun getConnectionDetails(): ConnectionDetails? {
        return connectionDetails
    }

    override fun getChatDetails(): ChatDetails? {
        return chatDetails
    }

    override fun isChatSessionActive(): Boolean {
        return isChatActive
    }

    override fun setChatSessionState(isActive: Boolean) {
        isChatActive = isActive
    }
}