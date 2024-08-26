package com.amazon.connect.chat.sdk.model

import com.amazon.connect.chat.sdk.utils.Constants
import com.amazonaws.regions.Regions

data class GlobalConfig(
    var region: Regions = defaultRegion,
    var features: Features = Features.defaultFeatures,
    var disableCsm: Boolean = false,
    var isDevMode: Boolean = false
) {
    companion object {
        val defaultRegion: Regions
            get() = Constants.DEFAULT_REGION
    }
}

data class Features(
    var messageReceipts: MessageReceipts = MessageReceipts.defaultReceipts
) {
    companion object {
        val defaultFeatures: Features
            get() = Features(messageReceipts = MessageReceipts.defaultReceipts)
    }
}

data class MessageReceipts(
    var shouldSendMessageReceipts: Boolean = true,
    var throttleTime: Double = Constants.MESSAGE_RECEIPT_THROTTLE_TIME
) {
    companion object {
        val defaultReceipts: MessageReceipts
            get() = MessageReceipts(
                shouldSendMessageReceipts = true,
                throttleTime = Constants.MESSAGE_RECEIPT_THROTTLE_TIME
            )
    }
}
