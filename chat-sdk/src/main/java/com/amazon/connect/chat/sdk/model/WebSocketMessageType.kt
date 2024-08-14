package com.amazon.connect.chat.sdk.model

enum class WebSocketMessageType(val type: String) {
    MESSAGE("MESSAGE"),
    EVENT("EVENT"),
    ATTACHMENT("ATTACHMENT"),
    MESSAGE_METADATA("MESSAGEMETADATA");

    companion object {
        fun fromType(type: String): WebSocketMessageType? {
            return values().find { it.type.equals(type, ignoreCase = true) }
        }
    }
}