package com.amazon.connect.chat.androidchatexample

import com.amazonaws.regions.Regions

object Config {
    val connectInstanceId: String = "e816d0f3-eda3-46e4-bc67-9999e621eff6"
    val contactFlowId: String = "f22bfa3b-400e-4250-939d-90a79eb1cd24"
    val startChatEndpoint: String = "https://bqo00ujzld.execute-api.us-west-2.amazonaws.com/Prod"
    val region: Regions = Regions.US_WEST_2
    val agentName = "AGENT"
    val customerName = "CUSTOMER"
}
