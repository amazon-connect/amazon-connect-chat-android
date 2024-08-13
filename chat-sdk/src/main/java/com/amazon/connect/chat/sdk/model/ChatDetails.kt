package com.amazon.connect.chat.sdk.model

import com.amazon.connect.chat.sdk.utils.Constants
import com.amazonaws.regions.Regions

data class ChatDetails(
    var contactId: String? = null,
    var participantId: String? = null,
    var participantToken: String
)

data class ChatSessionOptions(
    var region: Regions = Constants.DEFAULT_REGION
)