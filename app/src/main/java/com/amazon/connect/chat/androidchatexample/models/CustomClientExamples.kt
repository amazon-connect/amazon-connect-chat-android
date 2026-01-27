// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.androidchatexample.models

import android.util.Log
import com.amazon.connect.chat.sdk.model.ConnectionDetails
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.network.AWSClient
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadRequest
import com.amazonaws.services.connectparticipant.model.CompleteAttachmentUploadResult
import com.amazonaws.services.connectparticipant.model.DisconnectParticipantResult
import com.amazonaws.services.connectparticipant.model.GetAttachmentResult
import com.amazonaws.services.connectparticipant.model.GetTranscriptRequest
import com.amazonaws.services.connectparticipant.model.GetTranscriptResult
import com.amazonaws.services.connectparticipant.model.SendEventResult
import com.amazonaws.services.connectparticipant.model.SendMessageResult
import com.amazonaws.services.connectparticipant.model.StartAttachmentUploadRequest
import com.amazonaws.services.connectparticipant.model.StartAttachmentUploadResult

/**
 * Example: Partial Custom Client
 *
 * Override only specific methods for logging/monitoring.
 * Non-overridden methods use default AWS implementation.
 */
class PartialCustomClient : AWSClient() {

    companion object {
        private const val TAG = "PartialCustomClient"
    }

    override suspend fun sendMessage(
        connectionToken: String,
        contentType: ContentType,
        message: String
    ): Result<SendMessageResult> {
        Log.d(TAG, "sendMessage intercepted: $message")
        return super.sendMessage(connectionToken, contentType, message)
    }

    override suspend fun sendEvent(
        connectionToken: String,
        contentType: ContentType,
        content: String
    ): Result<SendEventResult> {
        Log.d(TAG, "sendEvent intercepted: ${contentType.type}")
        return super.sendEvent(connectionToken, contentType, content)
    }
}

/**
 * Example: Complete Custom Client
 *
 * Override all methods for full control over API routing.
 */
class CompleteCustomClient : AWSClient() {

    companion object {
        private const val TAG = "CompleteCustomClient"
    }

    override suspend fun createParticipantConnection(participantToken: String): Result<ConnectionDetails> {
        Log.d(TAG, "createParticipantConnection intercepted")
        return super.createParticipantConnection(participantToken)
    }

    override suspend fun disconnectParticipantConnection(connectionToken: String): Result<DisconnectParticipantResult> {
        Log.d(TAG, "disconnectParticipantConnection intercepted")
        return super.disconnectParticipantConnection(connectionToken)
    }

    override suspend fun sendMessage(
        connectionToken: String,
        contentType: ContentType,
        message: String
    ): Result<SendMessageResult> {
        Log.d(TAG, "sendMessage intercepted: $message")
        return super.sendMessage(connectionToken, contentType, message)
    }

    override suspend fun sendEvent(
        connectionToken: String,
        contentType: ContentType,
        content: String
    ): Result<SendEventResult> {
        Log.d(TAG, "sendEvent intercepted: ${contentType.type}")
        return super.sendEvent(connectionToken, contentType, content)
    }

    override suspend fun startAttachmentUpload(
        connectionToken: String,
        request: StartAttachmentUploadRequest
    ): Result<StartAttachmentUploadResult> {
        Log.d(TAG, "startAttachmentUpload intercepted: ${request.attachmentName}")
        return super.startAttachmentUpload(connectionToken, request)
    }

    override suspend fun completeAttachmentUpload(
        connectionToken: String,
        request: CompleteAttachmentUploadRequest
    ): Result<CompleteAttachmentUploadResult> {
        Log.d(TAG, "completeAttachmentUpload intercepted")
        return super.completeAttachmentUpload(connectionToken, request)
    }

    override suspend fun getAttachment(
        connectionToken: String,
        attachmentId: String
    ): Result<GetAttachmentResult> {
        Log.d(TAG, "getAttachment intercepted: $attachmentId")
        return super.getAttachment(connectionToken, attachmentId)
    }

    override suspend fun getTranscript(
        request: GetTranscriptRequest
    ): Result<GetTranscriptResult> {
        Log.d(TAG, "getTranscript intercepted")
        return super.getTranscript(request)
    }
}
