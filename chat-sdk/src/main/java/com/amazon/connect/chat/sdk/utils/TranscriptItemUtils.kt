// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.utils

import android.util.Log
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.model.Event
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.MessageDirection
import com.amazon.connect.chat.sdk.model.MessageMetadata
import com.amazon.connect.chat.sdk.model.MessageStatus
import com.amazon.connect.chat.sdk.model.TranscriptItem
import com.amazonaws.services.connectparticipant.model.Item
import org.json.JSONObject
import java.util.UUID

object TranscriptItemUtils {

    fun createDummyEndedEvent(): Event {
        val isoTime = CommonUtils.getCurrentISOTime()

        val serializedContent = mapOf(
            "content" to "{\"AbsoluteTime\":\"$isoTime\",\"ContentType\":\"application/vnd.amazonaws.connect.event.chat.ended\",\"Id\":\"chat-ended-event\",\"Type\":\"EVENT\",\"InitialContactId\":\"chat-ended-event-id\"}",
            "topic" to "aws/chat",
            "contentType" to "application/json"
        )

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
        val isoTime = CommonUtils.getCurrentISOTime()
        val randomId = UUID.randomUUID().toString()

        return Message(
            participant = "CUSTOMER",
            text = content,
            contentType = contentType,
            messageDirection = MessageDirection.OUTGOING,
            timeStamp = isoTime,
            attachmentId = attachmentId,
            id = randomId,
            displayName = displayName,
            serializedContent = emptyMap(),
            metadata = MessageMetadata(
                id = randomId,
                status = status,
                timeStamp = isoTime,
                contentType = contentType,
                eventDirection = MessageDirection.OUTGOING,
                serializedContent = emptyMap()
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

            val messageContentDict = mapOf(
                "Id" to (item.id ?: ""),
                "ParticipantRole" to participantRole,
                "AbsoluteTime" to (item.absoluteTime ?: ""),
                "ContentType" to (item.contentType ?: ""),
                "Content" to (item.content ?: ""),
                "Type" to item.type,
                "DisplayName" to (item.displayName ?: ""),
                "Attachments" to attachmentsArray
            )

            // Serialize the dictionary to JSON string
            val messageContentString = JSONObject(messageContentDict).toString()

            // Wrap the JSON string
            val wrappedMessageString =
                "{\"content\":\"${messageContentString.replace("\"", "\\\"")}\"}"

            // Deserialize back to JSON object
            val json = JSONObject(wrappedMessageString)

            json.optString("content")
        } catch (e: Exception) {
            Log.e("TranscriptItemUtils", "Failed to process transcript item: ${e.message}")
            null
        }
    }

}
