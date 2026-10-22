// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.model

import aws.sdk.kotlin.services.connectparticipant.model.ScanDirection as SdkScanDirection
import aws.sdk.kotlin.services.connectparticipant.model.SortKey as SdkSortKey

/**
 * The direction from the start position in which to retrieve transcript items.
 */
enum class ScanDirection(val value: String) {
    FORWARD("FORWARD"),
    BACKWARD("BACKWARD");

    override fun toString(): String = value

    internal fun toSdkType(): SdkScanDirection = SdkScanDirection.fromValue(value)

    companion object {
        @JvmStatic
        fun fromValue(value: String): ScanDirection =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Cannot create enum from $value value!")
    }
}

/**
 * The order in which to return transcript items.
 */
enum class SortKey(val value: String) {
    DESCENDING("DESCENDING"),
    ASCENDING("ASCENDING");

    override fun toString(): String = value

    internal fun toSdkType(): SdkSortKey = SdkSortKey.fromValue(value)

    companion object {
        @JvmStatic
        fun fromValue(value: String): SortKey =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Cannot create enum from $value value!")
    }
}
