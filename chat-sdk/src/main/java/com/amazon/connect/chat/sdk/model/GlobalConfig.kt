// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.model

import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazon.connect.chat.sdk.utils.Constants
import com.amazonaws.regions.Regions

/**
 * Global configuration for the Amazon Connect Chat SDK.
 *
 * @property region The AWS region for the Connect instance.
 * @property features Feature configuration for the chat session.
 * @property disableCsm Flag to disable client-side metrics.
 * @property customAWSClient Optional custom AWSClient implementation for routing API calls
 *                           through a backend proxy with certificate pinning.
 * @property customWebSocketURLProvider Optional function to transform WebSocket URLs
 *                                      (e.g., to route through a proxy).
 */
data class GlobalConfig(
    var region: Regions = defaultRegion,
    var features: Features = Features.defaultFeatures,
    var disableCsm: Boolean = false,
    var customAWSClient: AWSClient? = null,
    var customWebSocketURLProvider: ((String) -> String)? = null
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
    var throttleTime: Double = Constants.MESSAGE_RECEIPT_THROTTLE_TIME,
    var deliveredThrottleTime: Double = Constants.MESSAGE_RECEIPT_DELIVERED_THROTTLE_TIME
) {
    companion object {
        val defaultReceipts: MessageReceipts
            get() = MessageReceipts(
                shouldSendMessageReceipts = true,
                throttleTime = Constants.MESSAGE_RECEIPT_THROTTLE_TIME
            )
    }
}
