// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.utils

import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.Event
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageDirection
import com.amazon.connect.chat.sdk.model.MessageMetadata
import com.amazon.connect.chat.sdk.model.MessageStatus
import com.amazonaws.services.connectparticipant.model.Item
import com.amazon.connect.chat.sdk.utils.logger.SDKLogger
import org.json.JSONObject
import java.util.UUID

object TranscriptItemUtils {

    fun createDummyEndedEvent(): Event {
        val isoTime = CommonUtils.getCurrentISOTime()

        val serializedContent = "{\"AbsoluteTime\":\"$isoTime\",\"ContentType\":\"application/vnd.amazonaws.connect.event.chat.ended\",\"Id\":\"chat-ended-event\",\"Type\":\"EVENT\",\"InitialContactId\":\"chat-ended-event-id\"}"

        return Event(
            text = null,
            timeStamp = isoTime,
            contentType = ContentType.ENDED.type,
            id = "chat-ended-event",
            serializedContent = serializedContent
        )
    }

    fun createDummyMessage(
        content: String,
        contentType: String,
        status: MessageStatus,
        attachmentId: String? = null,
        displayName: String
    ): Message {
        val randomId = UUID.randomUUID().toString()

        return Message(
            participant = "CUSTOMER",
            text = content,
            contentType = contentType,
            messageDirection = MessageDirection.OUTGOING,
            timeStamp = "",
            attachmentId = attachmentId,
            id = randomId,
            displayName = displayName,
            serializedContent = "",
            metadata = MessageMetadata(
                id = randomId,
                status = status,
                timeStamp = "",
                contentType = contentType,
                eventDirection = MessageDirection.OUTGOING,
                serializedContent = ""
            )
        )
    }

    fun serializeTranscriptItem(item: Item): String? {
        return try {
            // Convert participant role to string
            val participantRole = item.participantRole

            // Process attachments
            val attachmentsArray = item.attachments?.map { attachment ->
                mapOf(
                    "AttachmentId" to (attachment.attachmentId ?: ""),
                    "AttachmentName" to (attachment.attachmentName ?: ""),
                    "ContentType" to (attachment.contentType ?: ""),
                    "Status" to attachment.status
                )
            } ?: emptyList()

            var messageMetadataDict: Map<String, Any>? = null
            if (item.messageMetadata != null) {
                val receiptsArray = item.messageMetadata?.receipts?.map { receipt ->
                    mapOf(
                        "ReadTimestamp" to (receipt.readTimestamp ?: ""),
                        "DeliveredTimestamp" to (receipt.deliveredTimestamp ?: ""),
                        "RecipientParticipantId" to (receipt.recipientParticipantId ?: "")
                    )
                } ?: emptyList()
                messageMetadataDict = mapOf(
                    "MessageId" to item.messageMetadata.messageId,
                    "Receipts" to receiptsArray
                )
            }

            val messageContentDict = mapOf(
                "Id" to (item.id ?: ""),
                "ParticipantRole" to participantRole,
                "AbsoluteTime" to (item.absoluteTime ?: ""),
                "ContentType" to (item.contentType ?: ""),
                "Content" to (item.content ?: ""),
                "Type" to item.type,
                "DisplayName" to (item.displayName ?: ""),
                "Attachments" to attachmentsArray,
                "isFromPastSession" to true, // Mark all these items as coming from a past session
                "MessageMetadata" to messageMetadataDict
            )

            val messageContent = mapOf(
                "content" to messageContentDict
            )
            // Serialize the dictionary to JSON string
            val messageContentString = JSONObject(messageContent).toString()

            // Deserialize back to JSON object
            val json = JSONObject(messageContentString)

            json.optString("content")
        } catch (e: Exception) {
            SDKLogger.logger.logError{"TranscriptItemUtils: Failed to process transcript item: ${e.message}"}
            null
        }
    }

}
