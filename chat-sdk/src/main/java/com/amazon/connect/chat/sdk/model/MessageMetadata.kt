// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.model

enum class MessageStatus(val status: String, var customValue: String? = null) {
    Delivered("Delivered"),
    Read("Read"),
    Sending("Sending"),
    Failed("Failed"),
    Sent("Sent"),
    Unknown("Unknown"),

    Custom("Custom", null);

    companion object {
        fun custom(message: String): MessageStatus {
            return Custom.apply {
                Custom.customValue = message
            }
        }
    }

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
