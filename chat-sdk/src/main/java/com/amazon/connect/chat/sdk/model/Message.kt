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

class Message(
    override var participant: String,
    override var text: String,
    contentType: String,
    override var displayName: String? = null,
    override var messageDirection: MessageDirection? = null,
    timeStamp: String,
    var attachmentId: String? = null,
    id: String,
    override var metadata: MessageMetadataProtocol? = null,
    serializedContent: Map<String, Any>? = null
) : TranscriptItem(
    id = id,
    timeStamp = timeStamp,
    contentType = contentType,
    serializedContent = serializedContent
), MessageProtocol {

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
                QuickReplyContent.templateType -> QuickReplyContent.decode(text)
                ListPickerContent.templateType -> ListPickerContent.decode(text)
                // Add cases for each interactive message type, decoding as appropriate.
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