// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network

import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder

/**
 * Appends the 2.x product token to the `User-Agent` header of every Connect Participant request.
 *
 * The token contains a space and a slash, which the AWS SDK for Kotlin `applicationId` setting
 * sanitizes, so `applicationId` alone cannot reproduce the value the AWS Mobile SDK sent. Keeping the
 * exact token preserves service-side attribution keyed on it.
 */
internal class UserAgentProductTokenInterceptor(private val productToken: String) : HttpInterceptor {

    override suspend fun modifyBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        val request = context.protocolRequest.toBuilder()
        val userAgent = request.headers[USER_AGENT_HEADER]
        if (userAgent?.contains(productToken) == true) {
            return context.protocolRequest
        }
        request.headers[USER_AGENT_HEADER] = if (userAgent.isNullOrBlank()) productToken else "$userAgent $productToken"
        return request.build()
    }

    private companion object {
        const val USER_AGENT_HEADER = "User-Agent"
    }
}
