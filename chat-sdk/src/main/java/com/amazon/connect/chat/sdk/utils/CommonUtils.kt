// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.sdk.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.ViewTreeObserver
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CommonUtils {
    companion object{

        @Composable
        fun keyboardAsState(): State<Boolean> {
            val view = LocalView.current
            var isImeVisible by remember { mutableStateOf(false) }
            DisposableEffect(LocalWindowInfo.current) {
                val listener = ViewTreeObserver.OnPreDrawListener {
                    isImeVisible = ViewCompat.getRootWindowInsets(view)
                        ?.isVisible(WindowInsetsCompat.Type.ime()) == true
                    true
                }
                view.viewTreeObserver.addOnPreDrawListener(listener)
                onDispose {
                    view.viewTreeObserver.removeOnPreDrawListener(listener)
                }
            }
            return rememberUpdatedState(isImeVisible)
        }

        fun parseErrorMessage(rawMessage: String?): String {
            return rawMessage ?: "An unknown error occurred"
        }


        fun getCurrentISOTime(): String {
            val currentDate = Date()
            val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
            isoFormatter.timeZone = TimeZone.getTimeZone("UTC")
            return isoFormatter.format(currentDate)
        }

        fun Uri.getOriginalFileName(context: Context): String? {
            return context.contentResolver.query(this, null, null, null, null)?.use {
                val nameColumnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                it.moveToFirst()
                it.getString(nameColumnIndex)
            }
        }

        fun getMimeType(fileName: String): String {
            val extension = fileName.substringAfterLast('.', "")
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        }
    }


}