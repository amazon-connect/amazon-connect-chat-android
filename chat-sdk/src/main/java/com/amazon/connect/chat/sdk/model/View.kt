// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.model

/**
 * View content containing all content necessary to render a view except for runtime input data.
 */
data class ViewContent(
    val actions: List<String>? = null,
    val inputSchema: String? = null,
    val template: String? = null
)

/**
 * A view resource object. Contains metadata and content necessary to render the view.
 */
data class View(
    val arn: String? = null,
    val content: ViewContent? = null,
    val id: String? = null,
    val name: String? = null,
    val version: Int? = null
)
