// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.provider

import android.content.Context
import com.amazon.connect.chat.sdk.ChatSession
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatSessionEntryPoint {
    fun getChatSession(): ChatSession
}

object ChatSessionProvider {
    private var chatSession: ChatSession? = null

    fun getChatSession(context: Context): ChatSession {
        if (chatSession == null) {
            val appContext = context.applicationContext
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                ChatSessionEntryPoint::class.java
            )
            chatSession = entryPoint.getChatSession()
        }
        return chatSession!!
    }
}