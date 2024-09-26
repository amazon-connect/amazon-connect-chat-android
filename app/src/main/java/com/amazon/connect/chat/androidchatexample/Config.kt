package com.amazon.connect.chat.androidchatexample

import com.amazonaws.regions.Regions

object Config {
    data class ChatConfig(
        val connectInstanceId: String,
        val contactFlowId: String,
        val startChatEndpoint: String,
        val region: Regions,
        val agentName: String,
        val customerName: String
    )

    // List of available configurations
    val configurations = emptyList<ChatConfig>()
}
