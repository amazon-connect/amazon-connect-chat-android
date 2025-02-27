// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.model

data class ConnectionDetails(
    val websocketUrl: String,
    val connectionToken: String,
    val expiry: String
)