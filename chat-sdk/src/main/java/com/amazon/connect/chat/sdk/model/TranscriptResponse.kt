package com.amazon.connect.chat.sdk.model

data class TranscriptResponse(
    val initialContactId: String,
    val nextToken: String?,
    val transcript: List<TranscriptItem>
)

data class Receipt(
    val deliveredTimestamp: String?,
    val readTimestamp: String?,
    val recipientParticipantId: String
)
