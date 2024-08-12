package com.amazon.connect.chat.sdk.model

import java.util.UUID

enum class MessageStatus {
    Delivered, Read, Sending, Failed, Sent, Unknown
}

interface MessageMetadataProtocol : TranscriptItemProtocol {
    var status: MessageStatus?
    var eventDirection: MessageDirection?
}

class MessageMetadata(
    override var status: MessageStatus? = null,
    override var eventDirection: MessageDirection? = MessageDirection.COMMON,
    timeStamp: String,
    contentType: String,
    id: String = UUID.randomUUID().toString(),
    serializedContent: Map<String, Any>? = null
) : TranscriptItem(
    id = id,
    timeStamp = timeStamp,
    contentType = contentType,
    serializedContent = serializedContent
), MessageMetadataProtocol {}
