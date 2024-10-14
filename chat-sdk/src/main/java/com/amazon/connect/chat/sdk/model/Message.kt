package com.amazon.connect.chat.sdk.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

enum class MessageDirection {
    OUTGOING,
    INCOMING,
    COMMON
}

interface MessageProtocol : TranscriptItemProtocol {
    var participant: String
    var text: String
    var displayName: String?
    var messageDirection: MessageDirection?
    var metadata: MessageMetadataProtocol?
}

data class Message(
    override var id: String,
    override var timeStamp: String,
    override var contentType: String,
    override var serializedContent: String? = null,
    override var participant: String,
    override var text: String,
    override var displayName: String? = null,
    override var messageDirection: MessageDirection? = null,
    var attachmentId: String? = null,
    override var metadata: MessageMetadataProtocol? = null
) : TranscriptItem(id, timeStamp, contentType, serializedContent), MessageProtocol {

    val content: MessageContent?
        get() = when (contentType) {
            ContentType.PLAIN_TEXT.type -> PlainTextContent.decode(text)
            ContentType.RICH_TEXT.type -> PlainTextContent.decode(text) // Replace with a rich text class later
            ContentType.INTERACTIVE_TEXT.type -> decodeInteractiveContent(text)
            else -> {
                if (attachmentId != null){
                    // Placeholder for a future rich text content class
                    PlainTextContent.decode(text)
                } else {
                    logUnsupportedContentType(contentType)
                    null
                }
            }
        }

    // Helper method to decode interactive content
    private fun decodeInteractiveContent(text: String): InteractiveContent? {
        return try {
            val jsonData = text.toByteArray(Charsets.UTF_8)
            val genericTemplate = Json { ignoreUnknownKeys = true }.decodeFromString<GenericInteractiveTemplate>(String(jsonData))
            when (genericTemplate.templateType) {
                QuickReplyContent.TEMPLATE_TYPE -> QuickReplyContent.decode(text)
                ListPickerContent.TEMPLATE_TYPE -> ListPickerContent.decode(text)
                TimePickerContent.TEMPLATE_TYPE -> TimePickerContent.decode(text)
                CarouselContent.TEMPLATE_TYPE -> CarouselContent.decode(text)
                PanelContent.TEMPLATE_TYPE -> PanelContent.decode(text)
                else -> {
                    logUnsupportedContentType(genericTemplate.templateType)
                    null
                }
            }
        } catch (e: SerializationException) {
            logSerializationException(e)
            null
        }
    }

    private fun logUnsupportedContentType(templateType: String?) {
        // Log the unsupported content type
    }

    private fun logSerializationException(e: SerializationException) {
        // Log the serialization exception
   }
}