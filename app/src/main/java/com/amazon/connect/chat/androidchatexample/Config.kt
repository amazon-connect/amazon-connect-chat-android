package com.amazon.connect.chat.androidchatexample

import com.amazon.connect.chat.sdk.model.Region

object Config {
    val connectInstanceId: String = ""
    val contactFlowId: String = ""
    val startChatEndpoint: String = "https://<endpoint>.execute-api.<region>.amazonaws.com/Prod/"
    val region: Region = Region.US_WEST_2
    val agentName = "AGENT"
    val customerName = "CUSTOMER"
}
