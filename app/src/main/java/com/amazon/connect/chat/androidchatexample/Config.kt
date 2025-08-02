package com.amazon.connect.chat.androidchatexample

import com.amazonaws.regions.Regions

object Config {
    val connectInstanceId: String = "9e330fe8-389b-4e51-b6b2-c249462d7e33"
    val contactFlowId: String = "a1acd643-869d-45a0-b758-d380214580cd"
    val startChatEndpoint: String = "https://kopg30tz0m.execute-api.us-west-2.amazonaws.com/Prod/"
    val region: Regions = Regions.US_WEST_2
    val agentName = "AGENT"
    val customerName = "CUSTOMER"
}
