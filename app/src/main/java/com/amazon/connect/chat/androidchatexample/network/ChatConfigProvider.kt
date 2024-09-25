package com.amazon.connect.chat.androidchatexample.network

import com.amazon.connect.chat.androidchatexample.Config
import com.amazon.connect.chat.androidchatexample.Config.ChatConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatConfigProvider @Inject constructor() {

    // Default configuration
    var currentConfig: ChatConfig = Config.configurations[0]

    // Update the current configuration based on user selection
    fun updateConfig(index: Int) {
        currentConfig = Config.configurations[index]
    }

    // Provide the current base URL
    fun getBaseUrl(): String {
        return currentConfig.startChatEndpoint
    }
}
