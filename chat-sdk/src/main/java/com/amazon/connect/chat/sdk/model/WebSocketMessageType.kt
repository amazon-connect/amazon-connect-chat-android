// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.model

enum class WebSocketMessageType(val type: String) {
    MESSAGE("MESSAGE"),
    EVENT("EVENT"),
    ATTACHMENT("ATTACHMENT"),
    MESSAGE_METADATA("MESSAGEMETADATA");

    companion object {
        fun fromType(type: String): WebSocketMessageType? {
            return entries.find { it.type.equals(type, ignoreCase = true) }
        }
    }
}