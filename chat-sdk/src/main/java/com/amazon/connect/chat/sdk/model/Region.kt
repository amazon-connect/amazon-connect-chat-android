// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.model

/**
 * An AWS region identifier, such as `us-west-2`.
 *
 * The AWS SDK for Kotlin expresses regions as plain strings, so any region string is accepted,
 * including regions launched after this version of the SDK:
 *
 * ```kotlin
 * GlobalConfig(region = Region.US_WEST_2)
 * GlobalConfig(region = Region("us-west-2"))
 * ```
 *
 * The constants mirror the names of `com.amazonaws.regions.Regions` from the deprecated AWS Mobile
 * SDK for Android. They are a convenience only and are not an authoritative list of the regions in
 * which Amazon Connect is available.
 */
class Region(private val name: String) {

    init {
        require(name.isNotBlank()) { "Region name must not be blank" }
    }

    /**
     * @return The region identifier, for example `us-west-2`.
     */
    fun getName(): String = name

    override fun equals(other: Any?): Boolean = other is Region && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    companion object {
        @JvmField val GovCloud = Region("us-gov-west-1")
        @JvmField val US_GOV_EAST_1 = Region("us-gov-east-1")
        @JvmField val US_EAST_1 = Region("us-east-1")
        @JvmField val US_EAST_2 = Region("us-east-2")
        @JvmField val US_WEST_1 = Region("us-west-1")
        @JvmField val US_WEST_2 = Region("us-west-2")
        @JvmField val EU_SOUTH_1 = Region("eu-south-1")
        @JvmField val EU_SOUTH_2 = Region("eu-south-2")
        @JvmField val EU_WEST_1 = Region("eu-west-1")
        @JvmField val EU_WEST_2 = Region("eu-west-2")
        @JvmField val EU_WEST_3 = Region("eu-west-3")
        @JvmField val EU_CENTRAL_1 = Region("eu-central-1")
        @JvmField val EU_CENTRAL_2 = Region("eu-central-2")
        @JvmField val EU_NORTH_1 = Region("eu-north-1")
        @JvmField val AP_EAST_1 = Region("ap-east-1")
        @JvmField val AP_EAST_2 = Region("ap-east-2")
        @JvmField val AP_SOUTH_1 = Region("ap-south-1")
        @JvmField val AP_SOUTH_2 = Region("ap-south-2")
        @JvmField val AP_SOUTHEAST_1 = Region("ap-southeast-1")
        @JvmField val AP_SOUTHEAST_2 = Region("ap-southeast-2")
        @JvmField val AP_SOUTHEAST_3 = Region("ap-southeast-3")
        @JvmField val AP_SOUTHEAST_4 = Region("ap-southeast-4")
        @JvmField val AP_SOUTHEAST_5 = Region("ap-southeast-5")
        @JvmField val AP_SOUTHEAST_7 = Region("ap-southeast-7")
        @JvmField val AP_NORTHEAST_1 = Region("ap-northeast-1")
        @JvmField val AP_NORTHEAST_2 = Region("ap-northeast-2")
        @JvmField val MX_CENTRAL_1 = Region("mx-central-1")
        @JvmField val SA_EAST_1 = Region("sa-east-1")
        @JvmField val CA_CENTRAL_1 = Region("ca-central-1")
        @JvmField val CA_WEST_1 = Region("ca-west-1")
        @JvmField val CN_NORTH_1 = Region("cn-north-1")
        @JvmField val CN_NORTHWEST_1 = Region("cn-northwest-1")
        @JvmField val ME_SOUTH_1 = Region("me-south-1")
        @JvmField val ME_CENTRAL_1 = Region("me-central-1")
        @JvmField val AF_SOUTH_1 = Region("af-south-1")
        @JvmField val IL_CENTRAL_1 = Region("il-central-1")

        @JvmField val DEFAULT_REGION = US_WEST_2

        /**
         * Returns the region for the given identifier, for example `us-west-2`.
         */
        @JvmStatic
        fun fromName(regionName: String): Region = Region(regionName)
    }
}

/**
 * Source-compatibility alias for 2.x call sites that used `com.amazonaws.regions.Regions`.
 * Replace the `com.amazonaws.regions.Regions` import with this one, then migrate to [Region].
 */
@Deprecated(
    message = "The AWS Mobile SDK for Android Regions enum was replaced by Region.",
    replaceWith = ReplaceWith("Region", "com.amazon.connect.chat.sdk.model.Region")
)
typealias Regions = Region
