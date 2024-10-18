// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.utils

import com.amazonaws.regions.Regions

object Constants {
    const val AWS_CONNECT_PARTICIPANT_KEY = "AWSConnectParticipant"
    val ACPS_REQUEST_TYPES = listOf("WEBSOCKET", "CONNECTION_CREDENTIALS")
    const val AGENT = "AGENT"
    const val CUSTOMER = "CUSTOMER"
    const val SYSTEM = "SYSTEM"
    const val UNKNOWN = "UNKNOWN"
    const val MESSAGE = "MESSAGE"
    const val ATTACHMENT = "ATTACHMENT"
    const val EVENT = "EVENT"
    const val MESSAGE_RECEIPT_THROTTLE_TIME = 5.0
    const val MESSAGE_RECEIPT_DELIVERED_THROTTLE_TIME = 3.0
    val DEFAULT_REGION: Regions = Regions.US_WEST_2
    const val QUICK_REPLY = "QuickReply"
    const val LIST_PICKER = "ListPicker"
    const val PANEL = "Panel"
    const val TIME_PICKER = "TimePicker"
    const val CAROUSEL = "Carousel"

    object Error {
        fun connectionCreated(reason: String): String {
            return "Failed to create connection: $reason."
        }
        fun connectionFailed(reason: String): String {
            return "Failed to create connection: $reason."
        }
    }

    val attachmentTypeMap = mapOf(
        "csv" to "text/csv",
        "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "heic" to "image/heic",
        "jpg" to "image/jpeg",
        "mov" to "video/quicktime",
        "mp4" to "video/mp4",
        "pdf" to "application/pdf",
        "png" to "image/png",
        "ppt" to "application/vnd.ms-powerpoint",
        "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "rtf" to "application/rtf",
        "txt" to "text/plain",
        "wav" to "audio/wav",
        "xls" to "application/vnd.ms-excel",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
}