// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package com.amazon.connect.chat.sdk.utils

import java.text.SimpleDateFormat
import java.util.*

object MetricsUtils {
    fun getCurrentMetricTimestamp(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        val now = Date()
        return formatter.format(now)
    }

    fun getMetricsEndpoint(): String {
        return "https://ieluqbvv.telemetry.connect.us-west-2.amazonaws.com/prod/"
    }
}
