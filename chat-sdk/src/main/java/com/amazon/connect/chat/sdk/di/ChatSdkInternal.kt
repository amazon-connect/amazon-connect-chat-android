// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.di

import javax.inject.Qualifier

/**
 * Qualifies the SDK's Hilt bindings for types it does not own, such as `OkHttpClient`,
 * `Retrofit.Builder` and `Context`, so they cannot collide with an application's own bindings
 * for the same types.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class ChatSdkInternal
