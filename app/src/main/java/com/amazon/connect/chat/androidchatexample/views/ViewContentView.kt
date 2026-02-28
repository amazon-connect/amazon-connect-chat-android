// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.androidchatexample.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amazon.connect.chat.androidchatexample.utils.CommonUtils
import com.amazon.connect.chat.androidchatexample.viewmodel.ChatViewModel
import com.amazon.connect.chat.sdk.model.Message
import com.amazon.connect.chat.sdk.model.View
import com.amazon.connect.chat.sdk.model.ViewResourceContent

/**
 * View component for rendering ShowView interactive messages.
 */
@Composable
fun ViewContentView(
    message: Message,
    content: ViewResourceContent
) {
    val viewModel: ChatViewModel = hiltViewModel()

    var view by remember(message.id) { mutableStateOf<View?>(null) }
    var error by remember(message.id) { mutableStateOf<String?>(null) }
    var isLoading by remember(message.id) { mutableStateOf(true) }

    LaunchedEffect(message.id) {
        content.viewToken?.let { token ->
            viewModel.describeView(token)
                .onSuccess { fetchedView ->
                    view = fetchedView
                    isLoading = false
                }
                .onFailure { e ->
                    error = e.message
                    isLoading = false
                }
        } ?: run {
            error = "No viewToken available"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .fillMaxWidth(0.80f),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            message.displayName?.let {
                Text(
                    text = it.ifEmpty { message.participant },
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = CommonUtils.formatTime(message.timeStamp) ?: "",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Surface(
            color = Color(0xFFEDEDED),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.75f)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                    error != null -> {
                        Text(
                            text = "Error: $error",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    view != null -> {
                        view?.name?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        view?.content?.template?.let { template ->
                            Text(
                                text = template,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
