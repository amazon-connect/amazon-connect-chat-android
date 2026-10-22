// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.connect.chat.androidchatexample.models

import android.util.Log
import com.amazon.connect.chat.sdk.model.ConnectionDetails
import com.amazon.connect.chat.sdk.model.ContentType
import com.amazon.connect.chat.sdk.network.AWSClient
import aws.sdk.kotlin.services.connectparticipant.model.CompleteAttachmentUploadRequest
import aws.sdk.kotlin.services.connectparticipant.model.CompleteAttachmentUploadResponse
import aws.sdk.kotlin.services.connectparticipant.model.DisconnectParticipantResponse
import aws.sdk.kotlin.services.connectparticipant.model.GetAttachmentResponse
import aws.sdk.kotlin.services.connectparticipant.model.GetTranscriptRequest
import aws.sdk.kotlin.services.connectparticipant.model.GetTranscriptResponse
import aws.sdk.kotlin.services.connectparticipant.model.SendEventResponse
import aws.sdk.kotlin.services.connectparticipant.model.SendMessageResponse
import aws.sdk.kotlin.services.connectparticipant.model.StartAttachmentUploadRequest
import aws.sdk.kotlin.services.connectparticipant.model.StartAttachmentUploadResponse

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
    ): Result<SendMessageResponse> {
        Log.d(TAG, "sendMessage intercepted: $message")
        return super.sendMessage(connectionToken, contentType, message)
    }

    override suspend fun sendEvent(
        connectionToken: String,
        contentType: ContentType,
        content: String
    ): Result<SendEventResponse> {
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

    override suspend fun disconnectParticipantConnection(connectionToken: String): Result<DisconnectParticipantResponse> {
        Log.d(TAG, "disconnectParticipantConnection intercepted")
        return super.disconnectParticipantConnection(connectionToken)
    }

    override suspend fun sendMessage(
        connectionToken: String,
        contentType: ContentType,
        message: String
    ): Result<SendMessageResponse> {
        Log.d(TAG, "sendMessage intercepted: $message")
        return super.sendMessage(connectionToken, contentType, message)
    }

    override suspend fun sendEvent(
        connectionToken: String,
        contentType: ContentType,
        content: String
    ): Result<SendEventResponse> {
        Log.d(TAG, "sendEvent intercepted: ${contentType.type}")
        return super.sendEvent(connectionToken, contentType, content)
    }

    override suspend fun startAttachmentUpload(
        connectionToken: String,
        request: StartAttachmentUploadRequest
    ): Result<StartAttachmentUploadResponse> {
        Log.d(TAG, "startAttachmentUpload intercepted: ${request.attachmentName}")
        return super.startAttachmentUpload(connectionToken, request)
    }

    override suspend fun completeAttachmentUpload(
        connectionToken: String,
        request: CompleteAttachmentUploadRequest
    ): Result<CompleteAttachmentUploadResponse> {
        Log.d(TAG, "completeAttachmentUpload intercepted")
        return super.completeAttachmentUpload(connectionToken, request)
    }

    override suspend fun getAttachment(
        connectionToken: String,
        attachmentId: String
    ): Result<GetAttachmentResponse> {
        Log.d(TAG, "getAttachment intercepted: $attachmentId")
        return super.getAttachment(connectionToken, attachmentId)
    }

    override suspend fun getTranscript(
        request: GetTranscriptRequest
    ): Result<GetTranscriptResponse> {
        Log.d(TAG, "getTranscript intercepted")
        return super.getTranscript(request)
    }
}
