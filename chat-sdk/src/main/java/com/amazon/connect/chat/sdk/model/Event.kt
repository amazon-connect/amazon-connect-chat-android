package com.amazon.connect.chat.sdk.model

interface EventProtocol : TranscriptItemProtocol {
    var participant: String?
    var text: String?
    var displayName: String?
    var eventDirection: MessageDirection?
}

class Event(
    override var participant: String? = null,
    override var text: String? = null,
    override var displayName: String? = null,
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
), EventProtocol {}
