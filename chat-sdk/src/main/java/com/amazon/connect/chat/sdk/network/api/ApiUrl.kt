// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.network.api

// https://thinkupsoft.com/blog/retrofit-multiples-base-urls-with-annotations/

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiUrl(val url: String)
