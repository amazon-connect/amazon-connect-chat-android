package com.amazon.connect.chat.androidchatexample

import com.amazonaws.regions.Regions

object Config {
    val connectInstanceId: String = "6ceda8ca-5e6e-4a60-9bfb-4994cc1fec79"
    val contactFlowId: String = "c8d90d07-a28c-4a97-9dfb-f4785b98d8d2"
    val startChatEndpoint: String = "https://3r4nj9r68b.execute-api.us-east-1.amazonaws.com/"
    val region: Regions = Regions.US_EAST_1
    val agentName = "AGENT"
    val customerName = "CUSTOMER"
}