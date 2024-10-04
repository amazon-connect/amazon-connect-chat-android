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

data class MessageMetadata(
    override var status: MessageStatus? = null,
    override var eventDirection: MessageDirection? = MessageDirection.COMMON,
    override var timeStamp: String,
    override var contentType: String,
    override var id: String,
    override var serializedContent: String? = null
) : TranscriptItem(id, timeStamp, contentType, serializedContent), MessageMetadataProtocol
