// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.model

data class TranscriptResponse(
    val initialContactId: String = "",
    val nextToken: String? = null,
    val transcript: List<TranscriptItem> = emptyList()
)

data class Receipt(
    val deliveredTimestamp: String?,
    val readTimestamp: String?,
    val recipientParticipantId: String
)
