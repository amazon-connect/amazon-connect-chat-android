package com.amazon.connect.chat.sdk

import android.content.Context
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