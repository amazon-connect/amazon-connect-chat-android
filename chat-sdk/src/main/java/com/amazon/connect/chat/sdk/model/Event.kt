// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.model

interface EventProtocol : TranscriptItemProtocol {
    var participant: String?
    var text: String?
    var displayName: String?
    var eventDirection: MessageDirection?
}

data class Event(
    override var participant: String? = null,
    override var text: String? = null,
    override var displayName: String? = null,
    override var eventDirection: MessageDirection? = MessageDirection.COMMON,
    override var timeStamp: String,
    override var contentType: String,
    override var id: String,
    override var serializedContent: String? = null
) : TranscriptItem(id, timeStamp, contentType, serializedContent), EventProtocol
