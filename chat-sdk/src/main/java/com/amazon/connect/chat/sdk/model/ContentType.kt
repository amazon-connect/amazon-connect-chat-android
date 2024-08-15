package com.amazon.connect.chat.sdk.model

enum class ContentType(val type: String){
    TYPING("application/vnd.amazonaws.connect.event.typing"),
    CONNECTION_ACKNOWLEDGED("application/vnd.amazonaws.connect.event.connection.acknowledged"),
    MESSAGE_DELIVERED("application/vnd.amazonaws.connect.event.message.delivered"),
    MESSAGE_READ("application/vnd.amazonaws.connect.event.message.read"),
    META_DATA("application/vnd.amazonaws.connect.event.message.metadata"),
    JOINED("application/vnd.amazonaws.connect.event.participant.joined"),
    LEFT("application/vnd.amazonaws.connect.event.participant.left"),
    ENDED("application/vnd.amazonaws.connect.event.chat.ended"),
    PLAIN_TEXT("text/plain"),
    RICH_TEXT("text/markdown"),
    INTERACTIVE_TEXT("application/vnd.amazonaws.connect.message.interactive");

    companion object {
        fun fromType(type: String): ContentType? {
            return values().find { it.type.equals(type, ignoreCase = true) }
        }
    }
}

enum class ChatEvent {
    ConnectionEstablished,
    ConnectionReEstablished,
    ChatEnded,
    ConnectionBroken,
}
