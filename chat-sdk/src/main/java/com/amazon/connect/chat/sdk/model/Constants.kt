package com.amazon.connect.chat.sdk.model

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
}