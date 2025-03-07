// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.provider

import com.amazon.connect.chat.sdk.model.ChatDetails
import com.amazon.connect.chat.sdk.model.ConnectionDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface ConnectionDetailsProvider {
    fun updateChatDetails(newDetails: ChatDetails)
    fun getConnectionDetails(): ConnectionDetails?
    fun updateConnectionDetails(newDetails: ConnectionDetails)
    fun getChatDetails(): ChatDetails?
    fun isChatSessionActive(): Boolean
    fun setChatSessionState(isActive: Boolean)
    fun reset()
    var chatSessionState: StateFlow<Boolean>
}

@Singleton
class ConnectionDetailsProviderImpl @Inject constructor() : ConnectionDetailsProvider {

    private val _chatSessionState = MutableStateFlow(false)
    override var chatSessionState: StateFlow<Boolean> = _chatSessionState

    @Volatile
    private var connectionDetails: ConnectionDetails? = null

    @Volatile
    private var chatDetails: ChatDetails? = null

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
        return _chatSessionState.value
    }

    override fun setChatSessionState(isActive: Boolean) {
        _chatSessionState.value = isActive
    }

    override fun reset() {
        connectionDetails = null
        chatDetails = null
        setChatSessionState(false)
    }
}