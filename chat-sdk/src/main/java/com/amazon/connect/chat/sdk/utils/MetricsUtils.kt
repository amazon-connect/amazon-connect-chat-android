// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package com.amazon.connect.chat.sdk.utils

import java.text.SimpleDateFormat
import java.util.*
import com.amazon.connect.chat.sdk.Config
import com.amazon.connect.chat.sdk.network.MetricsInterface

object MetricsUtils {
    fun getCurrentMetricTimestamp(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        val now = Date()
        return formatter.format(now)
    }

    fun isCsmDisabled(): Boolean {
        return Config.disableCsm
    }

    fun getMetricsEndpoint(): String {
        return if (Config.isDevMode) {
            "https://f9cskafqk3.execute-api.us-west-2.amazonaws.com/devo/"
        } else {
            "https://ieluqbvv.telemetry.connect.us-west-2.amazonaws.com/prod/"
        }
    }
}
