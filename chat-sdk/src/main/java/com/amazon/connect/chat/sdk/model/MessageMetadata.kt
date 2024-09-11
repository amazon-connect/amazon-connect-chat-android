package com.amazon.connect.chat.sdk.model

enum class MessageStatus(val status: String) {
    Delivered("Delivered"),
    Read("Read"),
    Sending("Sending"),
    Failed("Failed"),
    Sent("Sent"),
    Unknown("Unknown")
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
    id: String,
    serializedContent: Map<String, Any>? = null
) : TranscriptItem(
    id = id,
    timeStamp = timeStamp,
    contentType = contentType,
    serializedContent = serializedContent
), MessageMetadataProtocol {}
