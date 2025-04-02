// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

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
    INTERACTIVE_TEXT("application/vnd.amazonaws.connect.message.interactive"),
    INTERACTIVE_RESPONSE("application/vnd.amazonaws.connect.message.interactive.response"),
    AUTHENTICATION_INITIATED("application/vnd.amazonaws.connect.event.authentication.initiated"),
    AUTHENTICATION_SUCCESSFUL("application/vnd.amazonaws.connect.event.authentication.succeeded"),
    AUTHENTICATION_FAILED("application/vnd.amazonaws.connect.event.authentication.failed"),
    AUTHENTICATION_TIMEOUT("application/vnd.amazonaws.connect.event.authentication.timeout"),
    AUTHENTICATION_EXPIRED("application/vnd.amazonaws.connect.event.authentication.expired"),
    AUTHENTICATION_CANCELLED("application/vnd.amazonaws.connect.event.authentication.cancelled"),
    PARTICIPANT_DISPLAY_NAME_UPDATED("application/vnd.amazonaws.connect.event.participant.displayname.updated"),
    PARTICIPANT_ACTIVE("application/vnd.amazonaws.connect.event.participant.active"),
    PARTICIPANT_INACTIVE("application/vnd.amazonaws.connect.event.participant.inactive"),
    TRANSFER_SUCCEEDED("application/vnd.amazonaws.connect.event.transfer.succeeded"),
    TRANSFER_FAILED("application/vnd.amazonaws.connect.event.transfer.failed"),
    PARTICIPANT_IDLE("application/vnd.amazonaws.connect.event.participant.idle"),
    PARTICIPANT_RETURNED("application/vnd.amazonaws.connect.event.participant.returned"),
    PARTICIPANT_INVITED("application/vnd.amazonaws.connect.event.participant.invited"),
    AUTO_DISCONNECTION("application/vnd.amazonaws.connect.event.participant.autodisconnection"),
    CHAT_REHYDRATED("application/vnd.amazonaws.connect.event.chat.rehydrated");

    companion object {
        fun fromType(type: String): ContentType? {
            return entries.find { it.type.equals(type, ignoreCase = true) }
        }
    }
}

enum class MessageReceiptType(val type: String){
    MESSAGE_DELIVERED("application/vnd.amazonaws.connect.event.message.delivered"),
    MESSAGE_READ("application/vnd.amazonaws.connect.event.message.read");

    fun toContentType(): ContentType {
        return when (this) {
            MESSAGE_DELIVERED -> ContentType.MESSAGE_DELIVERED
            MESSAGE_READ -> ContentType.MESSAGE_READ
        }
    }
}

enum class ChatEvent {
    ConnectionEstablished,
    ConnectionReEstablished,
    ChatEnded,
    ConnectionBroken,
    DeepHeartBeatFailure,
}
