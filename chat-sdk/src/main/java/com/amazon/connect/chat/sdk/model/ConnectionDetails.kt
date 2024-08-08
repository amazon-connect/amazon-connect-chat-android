package com.amazon.connect.chat.sdk.model

data class ConnectionDetails(
    val websocketUrl: String,
    val connectionToken: String,
    val expiry: String
)